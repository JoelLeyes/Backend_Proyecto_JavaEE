package com.nexolab.servlet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexolab.dao.UserDAO;
import com.nexolab.model.Chat;
import com.nexolab.model.TipoChat;
import com.nexolab.model.Usuario;
import com.nexolab.service.AuthService;
import com.nexolab.service.ChatService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/chats")
public class ChatServlet extends HttpServlet {
	private final ChatService chatService = new ChatService();
	private final AuthService authService = new AuthService();
	private final UserDAO userDAO = new UserDAO();
	private final ObjectMapper objectMapper = new ObjectMapper();

	private static String firstNonBlank(Object... values) {
		for (Object v : values) {
			if (v == null) continue;
			String s = String.valueOf(v).trim();
			if (!s.isBlank()) return s;
		}
		return null;
	}

	private static Long toLongId(Object value) {
		if (value == null) return null;
		if (value instanceof Number n) return n.longValue();
		String s = String.valueOf(value).trim();
		if (s.isBlank()) return null;
		// tolerancia: "1.0" -> 1
		int dot = s.indexOf('.');
		if (dot > 0) s = s.substring(0, dot);
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String normalizeTipo(String raw) {
		if (raw == null) return "PRIVADO";
		String t = raw.trim().toUpperCase();
		return switch (t) {
			case "GRUPAL", "GROUP" -> "GRUPAL";
			case "PRIVADO", "PRIVATE" -> "PRIVADO";
			default -> t;
		};
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String token = req.getHeader("Authorization");
		if (token == null || !token.startsWith("Bearer ")) {
			resp.setStatus(401);
			return;
		}
		token = token.substring(7);
		Usuario usuario = authService.getUserFromToken(token);
		if (usuario == null) {
			resp.setStatus(401);
			return;
		}

		List<Chat> chats = chatService.obtenerChatsDelUsuario(usuario);
		List<Map<String, Object>> chatsList = chats.stream().map(c -> {
			Map<String, Object> map = new HashMap<>();
			map.put("idChat", c.getIdChat());

			// Para chats privados: mostrar el nombre del OTRO participante, no el guardado en DB
			String displayName = c.getNombreChat();
			if (c.getTipoChat() == TipoChat.PRIVADO && c.getParticipantes() != null) {
				displayName = c.getParticipantes().stream()
						.filter(p -> !p.getIdUsuario().equals(usuario.getIdUsuario()))
						.map(p -> (p.getNombre() + " " + p.getApellido()).trim())
						.findFirst()
						.orElse(c.getNombreChat());
			}
			map.put("nombreChat", displayName);
			map.put("tipoChat", c.getTipoChat() == null ? null : c.getTipoChat().toString());
			map.put("ultimoMensaje", c.getUltimoMensaje());
			map.put("horaUltimoMensaje", c.getHoraUltimoMensaje() == null ? null : c.getHoraUltimoMensaje().toString());
			map.put("mensajesSinLeer", c.getMensajesSinLeer());
			return map;
		}).collect(Collectors.toList());

		resp.setContentType("application/json");
		resp.getWriter().write(objectMapper.writeValueAsString(chatsList));
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("application/json");
		String token = req.getHeader("Authorization");
		if (token == null || !token.startsWith("Bearer ")) { resp.setStatus(401); return; }
		Usuario creador = authService.getUserFromToken(token.substring(7));
		if (creador == null) { resp.setStatus(401); return; }

		Map<String, Object> body;
		try {
			body = objectMapper.readValue(req.getInputStream(), new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			resp.setStatus(400);
			resp.getWriter().write("{\"message\":\"Cuerpo JSON inválido\"}");
			return;
		}
		String tipoRaw = firstNonBlank(body.get("tipo"), body.get("tipoChat"), body.get("chatType"));
		String tipo = normalizeTipo(tipoRaw);

		if ("GRUPAL".equalsIgnoreCase(tipo)) {
			String nombre = firstNonBlank(body.get("nombre"), body.get("nombreChat"), body.get("name"));
			if (nombre == null || nombre.isBlank()) {
				resp.setStatus(400);
				resp.getWriter().write("{\"message\":\"nombre requerido para chat grupal\"}");
				return;
			}

			List<Usuario> miembros = new ArrayList<>();
			Object miembrosObj = body.containsKey("miembros") ? body.get("miembros") :
					(body.containsKey("members") ? body.get("members") : body.get("miembrosIds"));
			if (miembrosObj instanceof List<?> list) {
				for (Object item : list) {
					Long uid = null;
					if (item instanceof Map<?, ?> map) {
						Object id = map.containsKey("idUsuario") ? map.get("idUsuario") : (map.containsKey("id") ? map.get("id") : map.get("userId"));
						uid = toLongId(id);
					} else {
						uid = toLongId(item);
					}
					if (uid == null) continue;
					Usuario u = userDAO.findById(uid);
					if (u != null) miembros.add(u);
				}
			}

			Chat chat;
			try {
				chat = chatService.crearChatGrupal(creador, nombre, miembros);
			} catch (Exception e) {
				resp.setStatus(500);
				resp.getWriter().write("{\"message\":\"No se pudo crear el chat grupal\"}");
				return;
			}
			Map<String, Object> response = new HashMap<>();
			response.put("idChat", chat.getIdChat());
			response.put("nombreChat", chat.getNombreChat());
			response.put("tipoChat", chat.getTipoChat().toString());
			resp.setStatus(201);
			resp.getWriter().write(objectMapper.writeValueAsString(response));
			return;
		}

		// Chat privado
		Object otroIdObj = body.containsKey("otroUsuarioId") ? body.get("otroUsuarioId") :
				(body.containsKey("usuarioId") ? body.get("usuarioId") : body.get("userId"));
		if (otroIdObj == null) {
			resp.setStatus(400);
			resp.getWriter().write("{\"message\":\"otroUsuarioId requerido\"}");
			return;
		}

		Long otroId = toLongId(otroIdObj);
		if (otroId == null) {
			resp.setStatus(400);
			resp.getWriter().write("{\"message\":\"otroUsuarioId inválido\"}");
			return;
		}

		Usuario otro = userDAO.findById(otroId);
		if (otro == null) {
			resp.setStatus(404);
			resp.getWriter().write("{\"message\":\"Usuario no encontrado\"}");
			return;
		}

		Chat chat;
		try {
			chat = chatService.crearChatPrivado(creador, otro);
		} catch (Exception e) {
			resp.setStatus(500);
			resp.getWriter().write("{\"message\":\"No se pudo crear el chat privado\"}");
			return;
		}

		String nombreParaCreador = (otro.getNombre() + " " + otro.getApellido()).trim();
		Map<String, Object> response = new HashMap<>();
		response.put("idChat",     chat.getIdChat());
		response.put("nombreChat", nombreParaCreador);
		response.put("tipoChat",   chat.getTipoChat() == null ? null : chat.getTipoChat().toString());
		resp.setStatus(201);
		resp.getWriter().write(objectMapper.writeValueAsString(response));
	}
}