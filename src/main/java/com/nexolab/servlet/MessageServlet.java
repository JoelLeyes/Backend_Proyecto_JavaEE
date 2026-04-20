package com.nexolab.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexolab.model.Adjunto;
import com.nexolab.model.Chat;
import com.nexolab.model.EstadoMensaje;
import com.nexolab.model.Mensaje;
import com.nexolab.model.Usuario;
import com.nexolab.service.AuthService;
import com.nexolab.service.ChatService;
import com.nexolab.service.MessageService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/chats/*")
public class MessageServlet extends HttpServlet {
	private final MessageService messageService = new MessageService();
	private final ChatService chatService = new ChatService();
	private final AuthService authService = new AuthService();
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		RequestContext ctx = authorizeAndResolveChat(req, resp);
		if (ctx == null) {
			return;
		}

		Date since = parseSince(req.getParameter("since"));
		List<Mensaje> messages = messageService.obtenerMensajesDesdeFecha(ctx.chat, since);

		List<Map<String, Object>> msgList = messages.stream().map(m -> {
			Map<String, Object> map = new HashMap<>();
			map.put("idMensaje", m.getIdMensaje());
			map.put("fechaEnviado", m.getFechaEnviado());
			map.put("contenido", m.getContenido());

			List<Map<String, Object>> adjuntos = (m.getAdjuntos() == null ? java.util.Set.<Adjunto>of() : m.getAdjuntos()).stream()
					.map(a -> {
						Map<String, Object> am = new HashMap<>();
						am.put("idAdjunto", a.getIdAdjunto());
						am.put("tipoArchivo", a.getTipoArchivo());
						am.put("nombreArchivo", a.getNombreArchivo());
						am.put("urlArchivo", a.getUrlArchivo());
						return am;
					})
					.collect(Collectors.toList());
			map.put("adjuntos", adjuntos);

			List<Map<String, Object>> estados = (m.getEstados() == null ? List.<EstadoMensaje>of() : m.getEstados()).stream()
					.map(e -> {
						Map<String, Object> em = new HashMap<>();
						em.put("idEstadoMensaje", e.getIdEstadoMensaje());
						em.put("estado", e.getEstado() == null ? null : e.getEstado().toString());
						em.put("fechaEntregado", e.getFechaEntregado());
						Usuario u = e.getUsuario();
						em.put("usuarioIdUsuario", u == null ? null : u.getIdUsuario());
						if (u != null) {
							Map<String, Object> um = new HashMap<>();
							um.put("idUsuario", u.getIdUsuario());
							um.put("nombre", u.getNombre());
							um.put("apellido", u.getApellido());
							em.put("usuario", um);
						}
						return em;
					})
					.collect(Collectors.toList());
			map.put("estados", estados);

			return map;
		}).collect(Collectors.toList());

		resp.setContentType("application/json");
		resp.getWriter().write(objectMapper.writeValueAsString(msgList));
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		RequestContext ctx = authorizeAndResolveChat(req, resp);
		if (ctx == null) {
			return;
		}

		Map<String, Object> body = objectMapper.readValue(req.getInputStream(), Map.class);
		String contenido = body.get("contenido") == null ? null : body.get("contenido").toString();
		if (contenido == null) {
			Object c = body.get("content");
			contenido = c == null ? null : c.toString();
		}

		messageService.enviarMensaje(ctx.chat, ctx.usuario, contenido);
		resp.setStatus(201);
	}

	private RequestContext authorizeAndResolveChat(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String token = req.getHeader("Authorization");
		if (token == null || !token.startsWith("Bearer ")) {
			resp.setStatus(401);
			return null;
		}
		token = token.substring(7);
		Usuario usuario = authService.getUserFromToken(token);
		if (usuario == null) {
			resp.setStatus(401);
			return null;
		}

		String pathInfo = req.getPathInfo();
		String[] parts = (pathInfo == null ? "" : pathInfo).split("/");
		// Esperado: /{chatId}/messages
		if (parts.length < 3 || parts[1].isBlank() || !"messages".equals(parts[2])) {
			resp.setStatus(404);
			return null;
		}

		Long chatId;
		try {
			chatId = Long.parseLong(parts[1]);
		} catch (NumberFormatException ex) {
			resp.setStatus(400);
			return null;
		}

		Chat chat = chatService.obtenerChatPorId(chatId);
		boolean participa = chat != null
				&& chat.getParticipantes() != null
				&& chat.getParticipantes().stream().anyMatch(p -> p != null
						&& p.getIdUsuario() != null
						&& p.getIdUsuario().equals(usuario.getIdUsuario()));
		if (!participa) {
			resp.setStatus(403);
			return null;
		}

		return new RequestContext(usuario, chat);
	}

	private Date parseSince(String sinceStr) {
		if (sinceStr == null || sinceStr.isBlank()) {
			return new Date(0);
		}

		try {
			Instant i = Instant.parse(sinceStr);
			return Date.from(i);
		} catch (DateTimeParseException ignored) {
			// sigue
		}

		try {
			LocalDateTime ldt = LocalDateTime.parse(sinceStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
			return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
		} catch (DateTimeParseException ignored) {
			// sigue
		}

		try {
			long ms = Long.parseLong(sinceStr);
			return new Date(ms);
		} catch (NumberFormatException ignored) {
			return new Date(0);
		}
	}

	private static final class RequestContext {
		final Usuario usuario;
		final Chat chat;

		RequestContext(Usuario usuario, Chat chat) {
			this.usuario = usuario;
			this.chat = chat;
		}
	}
}
