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

		Map<String, Object> body = objectMapper.readValue(req.getInputStream(), new TypeReference<Map<String, Object>>() {});
		Object otroIdObj = body.get("otroUsuarioId");
		if (otroIdObj == null) {
			resp.setStatus(400);
			resp.getWriter().write("{\"message\":\"otroUsuarioId requerido\"}");
			return;
		}

		Long otroId;
		try {
			otroId = Long.parseLong(otroIdObj.toString());
		} catch (NumberFormatException e) {
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

		Chat chat = chatService.crearChatPrivado(creador, otro);

		// Nombre desde la perspectiva del creador = nombre del otro usuario
		String nombreParaCreador = (otro.getNombre() + " " + otro.getApellido()).trim();
		Map<String, Object> response = new HashMap<>();
		response.put("idChat",     chat.getIdChat());
		response.put("nombreChat", nombreParaCreador);
		response.put("tipoChat",   chat.getTipoChat() == null ? null : chat.getTipoChat().toString());
		resp.setStatus(201);
		resp.getWriter().write(objectMapper.writeValueAsString(response));
	}
}