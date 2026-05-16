package com.nexolab.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexolab.dao.MessageDAO;
import com.nexolab.dao.ReaccionDAO;
import com.nexolab.dao.UserDAO;
import com.nexolab.model.Adjunto;
import com.nexolab.model.Chat;
import com.nexolab.model.EstadoMensaje;
import com.nexolab.model.Mensaje;
import com.nexolab.model.Participa;
import com.nexolab.model.Reaccion;
import com.nexolab.model.TipoChat;
import com.nexolab.model.Usuario;
import com.nexolab.service.AuthService;
import com.nexolab.service.ChatService;
import com.nexolab.service.MessageService;
import com.nexolab.service.TypingStore;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
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

@MultipartConfig
@WebServlet("/chats/*")
public class MessageServlet extends HttpServlet {
	private final MessageService messageService = new MessageService();
	private final MessageDAO     messageDAO     = new MessageDAO();
	private final ReaccionDAO    reaccionDAO    = new ReaccionDAO();
	private final ChatService    chatService    = new ChatService();
	private final AuthService    authService    = new AuthService();
	private final UserDAO        userDAO        = new UserDAO();
	private final ObjectMapper   objectMapper   = new ObjectMapper();
	private final TypingStore    typingStore    = TypingStore.getInstance();

	@Override
	public void init() throws ServletException {
		String realPath = getServletContext().getRealPath("/uploads");
		if (realPath != null) {
			com.nexolab.util.FileStorageUtil.setUploadDir(realPath);
		}
		// Configurar prefijo de URL pública según el context path del deployment
		// Ej: si el WAR está en /api → contextPath="/api" → urlPrefix="/api/uploads/"
		String contextPath = getServletContext().getContextPath();
		com.nexolab.util.FileStorageUtil.setUrlPrefix(contextPath + "/uploads/");
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		RequestContext ctx = authorizeAndResolveChat(req, resp);
		if (ctx == null) return;

		resp.setContentType("application/json");

		if ("typing".equals(ctx.subPath)) {
			List<Map<String, Object>> typers = typingStore.getTyping(
					ctx.chat.getIdChat(), ctx.usuario.getIdUsuario())
				.stream().map(e -> {
					Map<String, Object> m = new HashMap<>();
					m.put("nombre", e.nombre());
					return m;
				}).collect(Collectors.toList());
			resp.getWriter().write(objectMapper.writeValueAsString(typers));
			return;
		}

		if ("participantes".equals(ctx.subPath)) {
			List<Participa> participaciones = chatService.obtenerParticipaciones(ctx.chat.getIdChat());
			List<Map<String, Object>> list = participaciones.stream().map(p -> {
				Map<String, Object> map = new HashMap<>();
				Usuario u = p.getUsuario();
				map.put("idUsuario", u.getIdUsuario());
				map.put("nombre", u.getNombre());
				map.put("apellido", u.getApellido());
				map.put("rol", p.getRolUsuario() == null ? null : p.getRolUsuario().toString());
				map.put("tipoEstado", u.getTipoEstado() == null ? null : u.getTipoEstado().toString());
				return map;
			}).collect(Collectors.toList());
			resp.getWriter().write(objectMapper.writeValueAsString(list));
			return;
		}

		// messages
		String search = req.getParameter("search");
		List<Mensaje> messages;
		if (search != null && search.trim().length() >= 2) {
			messages = messageDAO.findBySearch(ctx.chat.getIdChat(), search.trim());
		} else {
			Date since = parseSince(req.getParameter("since"));
			messages = messageService.obtenerMensajesDesdeFecha(ctx.chat, since);
		}

		List<Long> msgIds = messages.stream().map(Mensaje::getIdMensaje).collect(Collectors.toList());
		Map<Long, List<Reaccion>> reaccionesPorMensaje = reaccionDAO.findByMensajeIds(msgIds);
		Long myId = ctx.usuario.getIdUsuario();

		// Marcar mensajes como leídos para este usuario
		chatService.markAsRead(ctx.chat.getIdChat(), ctx.usuario.getIdUsuario());

		List<Map<String, Object>> msgList = messages.stream().map(m -> {
			Map<String, Object> map = new HashMap<>();
			map.put("idMensaje", m.getIdMensaje());
			map.put("fechaEnviado", m.getFechaEnviado());
			map.put("contenido", m.getContenido());

			if (m.getRespondeA() != null) {
				Map<String, Object> ra = new HashMap<>();
				ra.put("idMensaje", m.getRespondeA().getIdMensaje());
				String raContenido = m.getRespondeA().getContenido();
				ra.put("contenido", raContenido != null && raContenido.length() > 120
						? raContenido.substring(0, 120) + "…" : raContenido);
				map.put("respondeA", ra);
			}

			List<Map<String, Object>> adjuntos = (m.getAdjuntos() == null ? java.util.Set.<Adjunto>of() : m.getAdjuntos()).stream()
					.map(a -> {
						Map<String, Object> am = new HashMap<>();
						am.put("idAdjunto", a.getIdAdjunto());
						am.put("tipoArchivo", a.getTipoArchivo());
						am.put("nombreArchivo", a.getNombreArchivo());
						am.put("urlArchivo", a.getUrlArchivo());
						am.put("rutaArchivo", a.getUrlArchivo()); // También incluir como rutaArchivo para el frontend
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

			// Reacciones agrupadas por emoji con conteo y flag "mine"
			List<Reaccion> rList = reaccionesPorMensaje.getOrDefault(m.getIdMensaje(), List.of());
			Map<String, long[]> grouped = new java.util.LinkedHashMap<>();
			// Agrupar por emoji
			for (Reaccion r : rList) {
				grouped.computeIfAbsent(r.getEmoji(), k -> new long[]{0, 0});
				grouped.get(r.getEmoji())[0]++;
				if (r.getUsuario().getIdUsuario().equals(myId)) grouped.get(r.getEmoji())[1] = 1;
			}
			List<Map<String, Object>> reacciones = grouped.entrySet().stream().map(e -> {
				Map<String, Object> rm = new HashMap<>();
				rm.put("emoji", e.getKey());
				rm.put("count", e.getValue()[0]);
				rm.put("mine",  e.getValue()[1] == 1);
				return rm;
			}).collect(Collectors.toList());
			map.put("reacciones", reacciones);

			return map;
		}).collect(Collectors.toList());

		resp.getWriter().write(objectMapper.writeValueAsString(msgList));
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		RequestContext ctx = authorizeAndResolveChat(req, resp);
		if (ctx == null) return;

		resp.setContentType("application/json");

		if ("typing".equals(ctx.subPath)) {
			String nombre = (ctx.usuario.getNombre() + " " + ctx.usuario.getApellido()).trim();
			typingStore.markTyping(ctx.chat.getIdChat(), ctx.usuario.getIdUsuario(), nombre);
			resp.setStatus(200);
			return;
		}

		if ("reacciones".equals(ctx.subPath)) {
			Map<String, Object> body = objectMapper.readValue(req.getInputStream(),
					new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
			Object msgIdObj = body.get("msgId");
			Object emojiObj = body.get("emoji");
			if (msgIdObj == null || emojiObj == null) {
				resp.setStatus(400);
				resp.getWriter().write("{\"message\":\"msgId y emoji requeridos\"}");
				return;
			}
			Long msgId;
			try { msgId = Long.parseLong(msgIdObj.toString()); }
			catch (NumberFormatException e) { resp.setStatus(400); return; }
			String emoji = emojiObj.toString().trim();
			boolean added = reaccionDAO.toggle(msgId, ctx.usuario.getIdUsuario(), emoji);
			resp.setStatus(200);
			resp.getWriter().write("{\"added\":" + added + "}");
			return;
		}

		if ("participantes".equals(ctx.subPath)) {
			if (ctx.chat.getTipoChat() != TipoChat.GRUPAL) {
				resp.setStatus(400);
				resp.getWriter().write("{\"message\":\"Solo chats grupales\"}");
				return;
			}
			Map<String, Object> body = objectMapper.readValue(req.getInputStream(),
					new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
			Object uidObj = body.get("usuarioId");
			if (uidObj == null) {
				resp.setStatus(400);
				resp.getWriter().write("{\"message\":\"usuarioId requerido\"}");
				return;
			}
			Long uid;
			try { uid = Long.parseLong(uidObj.toString()); }
			catch (NumberFormatException e) { resp.setStatus(400); return; }

			Usuario nuevo = userDAO.findById(uid);
			if (nuevo == null) { resp.setStatus(404); resp.getWriter().write("{\"message\":\"Usuario no encontrado\"}"); return; }

			try {
				chatService.agregarParticipante(ctx.chat.getIdChat(), ctx.usuario, nuevo);
				resp.setStatus(201);
			} catch (SecurityException e) {
				resp.setStatus(403);
				resp.getWriter().write("{\"message\":\"" + e.getMessage() + "\"}");
			}
			return;
		}

        // Enviar mensaje (con 0 o más adjuntos)
        String contenido = req.getParameter("contenido");

        Long respondaMensajeId = null;
        String respondaIdStr = req.getParameter("respondaId");
        if (respondaIdStr != null && !respondaIdStr.isBlank()) {
            try { respondaMensajeId = Long.parseLong(respondaIdStr); } catch (NumberFormatException ignored) {}
        }

        // Obtener TODOS los archivos adjuntos (puede haber múltiples o ninguno)
        java.util.Collection<jakarta.servlet.http.Part> archivos = null;
        try {
            archivos = req.getParts().stream()
                .filter(p -> "archivo".equals(p.getName()))
                .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            // Sin archivos es ok, continuar igual
        }

        // Validar que haya AL MENOS texto o archivos
        boolean tieneArchivos = archivos != null && !archivos.isEmpty();
        if ((contenido == null || contenido.trim().isBlank()) && !tieneArchivos) {
            resp.setStatus(400);
            resp.getWriter().write("{\"message\":\"Debes escribir un mensaje o adjuntar archivos\"}");
            return;
        }

        // Normalizar contenido vacío a string vacío
        if (contenido == null || contenido.trim().isBlank()) {
            contenido = "";
        }

        // Si hay archivos: enviar con adjuntos, sino: enviar sin adjuntos
        if (tieneArchivos) {
            messageService.enviarMensajeConAdjuntos(ctx.chat, ctx.usuario, contenido,
                new java.util.ArrayList<>(archivos), respondaMensajeId);
        } else {
            messageService.enviarMensaje(ctx.chat, ctx.usuario, contenido, respondaMensajeId);
        }
        resp.setStatus(201);
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		RequestContext ctx = authorizeAndResolveChat(req, resp);
		if (ctx == null) return;

		resp.setContentType("application/json");

		if ("nombre".equals(ctx.subPath)) {
			if (ctx.chat.getTipoChat() != TipoChat.GRUPAL) {
				resp.setStatus(400);
				resp.getWriter().write("{\"message\":\"Solo chats grupales\"}");
				return;
			}
			Map<String, Object> body = objectMapper.readValue(req.getInputStream(),
					new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
			Object nombreObj = body.get("nombre");
			if (nombreObj == null || nombreObj.toString().isBlank()) {
				resp.setStatus(400);
				resp.getWriter().write("{\"message\":\"nombre requerido\"}");
				return;
			}
			String nombre = nombreObj.toString().trim();
			try {
				chatService.renombrarGrupo(ctx.chat.getIdChat(), ctx.usuario, nombre);
				resp.getWriter().write("{\"nombre\":\"" + nombre + "\"}");
			} catch (SecurityException e) {
				resp.setStatus(403);
				resp.getWriter().write("{\"message\":\"" + e.getMessage() + "\"}");
			}
			return;
		}

		resp.setStatus(404);
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		RequestContext ctx = authorizeAndResolveChat(req, resp);
		if (ctx == null) return;

		resp.setContentType("application/json");

		if ("participantes".equals(ctx.subPath)) {
			if (ctx.chat.getTipoChat() != TipoChat.GRUPAL) {
				resp.setStatus(400);
				resp.getWriter().write("{\"message\":\"Solo chats grupales\"}");
				return;
			}
			String kickUserIdStr = req.getParameter("kickUserId");
			if (kickUserIdStr != null) {
				Long kickUserId;
				try { kickUserId = Long.parseLong(kickUserIdStr); }
				catch (NumberFormatException e) { resp.setStatus(400); return; }
				try {
					chatService.expulsarParticipante(ctx.chat.getIdChat(), ctx.usuario, kickUserId);
					resp.setStatus(200);
					resp.getWriter().write("{\"message\":\"Participante expulsado\"}");
				} catch (SecurityException e) {
					resp.setStatus(403);
					resp.getWriter().write("{\"message\":\"" + e.getMessage() + "\"}");
				} catch (IllegalArgumentException e) {
					resp.setStatus(400);
					resp.getWriter().write("{\"message\":\"" + e.getMessage() + "\"}");
				}
			} else {
				chatService.abandonarChat(ctx.chat.getIdChat(), ctx.usuario);
				resp.setStatus(200);
				resp.getWriter().write("{\"message\":\"Has abandonado el grupo\"}");
			}
			return;
		}

		resp.setStatus(404);
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
		// Esperado: /{chatId}/messages  o  /{chatId}/participantes
		if (parts.length < 3 || parts[1].isBlank()) {
			resp.setStatus(404);
			return null;
		}
		String subPath = parts[2];
		if (!java.util.Set.of("messages","participantes","nombre","typing","reacciones").contains(subPath)) {
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

		return new RequestContext(usuario, chat, subPath);
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
		final String subPath;

		RequestContext(Usuario usuario, Chat chat, String subPath) {
			this.usuario = usuario;
			this.chat = chat;
			this.subPath = subPath;
		}
	}
}
