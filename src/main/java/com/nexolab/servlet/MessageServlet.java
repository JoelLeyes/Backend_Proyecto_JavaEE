package com.nexolab.servlet;

import com.nexolab.service.AuthService;
import com.nexolab.service.ChatService;
import com.nexolab.service.MessageService;
import com.nexolab.model.Chat;
import com.nexolab.model.Usuario;
import com.nexolab.model.Mensaje;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@WebServlet("/api/chats/*/messages")
public class MessageServlet extends HttpServlet {
	private MessageService messageService = new MessageService();
	private ChatService chatService = new ChatService();
	private AuthService authService = new AuthService();
	private ObjectMapper objectMapper = new ObjectMapper();

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

		String path = req.getPathInfo();
		Long chatId = Long.parseLong(path.split("/")[1]);
		Chat chat = chatService.obtenerChatPorId(chatId);
		if (chat == null || !chat.getParticipantes().contains(usuario)) {
			resp.setStatus(403);
			return;
		}

		String sinceStr = req.getParameter("since");
		LocalDateTime since = sinceStr != null ? LocalDateTime.parse(sinceStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
				: LocalDateTime.MIN;

		List<Mensaje> messages = messageService.obtenerMensajesDesdeFecha(chat, since);
		List<Map<String, Object>> msgList = messages.stream().map(m -> {
			Map<String, Object> map = new HashMap<>();
			map.put("id", m.getId());
			map.put("senderId", m.getSender().getId());
			map.put("senderName", m.getSender().getName());
			map.put("content", m.getContent());
			map.put("timestamp", m.getTimestamp());
			map.put("read", m.getRead());
			return map;
		}).collect(Collectors.toList());

		resp.setContentType("application/json");
		resp.getWriter().write(objectMapper.writeValueAsString(msgList));
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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

		String path = req.getPathInfo();
		Long chatId = Long.parseLong(path.split("/")[1]);
		Chat chat = chatService.obtenerChatPorId(chatId);
		if (chat == null || !chat.getParticipantes().contains(usuario)) {
			resp.setStatus(403);
			return;
		}

		Map<String, String> body = objectMapper.readValue(req.getInputStream(),
				new TypeReference<Map<String, String>>() {
				});
		messageService.enviarMensaje(chat, usuario, body.get("content"), body.get("type"));

		resp.setStatus(201);
	}
}