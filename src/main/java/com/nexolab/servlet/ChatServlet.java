package com.nexolab.servlet;

import com.nexolab.service.AuthService;
import com.nexolab.service.ChatService;
import com.nexolab.dao.UserDAO;
import com.nexolab.model.Chat;
import com.nexolab.model.TipoChat;
import com.nexolab.model.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@WebServlet("/api/chats")
public class ChatServlet extends HttpServlet {
	private ChatService chatService = new ChatService();
	private AuthService authService = new AuthService();
	private UserDAO userDAO = new UserDAO();
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

		List<Chat> chats = chatService.obtenerChatsDelUsuario(usuario);
		List<Map<String, Object>> chatsList = chats.stream().map(c -> {
			Map<String, Object> map = new HashMap<>();
			map.put("id", c.getId());
			map.put("name", c.getName());
			map.put("type", c.getType().toString());
			map.put("lastMessage", c.getLastMessage());
			map.put("lastMessageAt", c.getLastMessageAt());
			map.put("unreadCount", c.getUnreadCount());
			return map;
		}).collect(Collectors.toList());

		resp.setContentType("application/json");
		resp.getWriter().write(objectMapper.writeValueAsString(chatsList));
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

		try {
			Map<String, Object> body = objectMapper.readValue(req.getInputStream(),
					new TypeReference<Map<String, Object>>() {
					});
			String name = (String) body.get("name");
			String typeStr = (String) body.get("type");
			TipoChat type = TipoChat.valueOf(typeStr.toUpperCase());

			Set<Usuario> participantes = new HashSet<>();
			participantes.add(usuario); // Agregar creador

			// Para chats privados, agregar otro usuario si se especifica
			if (type == TipoChat.PRIVATE) {
				if (!body.containsKey("otherUserEmail") || ((String) body.get("otherUserEmail")).trim().isEmpty()) {
					throw new Exception("Para chats privados, debe especificar el email del otro usuario");
				}
				String otherUserEmail = ((String) body.get("otherUserEmail")).trim();
				if (otherUserEmail.equalsIgnoreCase(usuario.getEmail())) {
					throw new Exception("No puede crear un chat privado consigo mismo");
				}
				Usuario otherUser = userDAO.findByEmail(otherUserEmail);
				if (otherUser != null) {
					participantes.add(otherUser);
				} else {
					throw new Exception("Usuario no encontrado: " + otherUserEmail);
				}
			}

			Chat chat = chatService.crearChat(name, type, participantes);

			Map<String, Object> response = new HashMap<>();
			response.put("id", chat.getId());
			response.put("name", chat.getName());
			response.put("type", chat.getType().toString());

			resp.setContentType("application/json");
			resp.getWriter().write(objectMapper.writeValueAsString(response));
		} catch (Exception e) {
			resp.setStatus(400);
			Map<String, String> error = new HashMap<>();
			error.put("message", e.getMessage());
			resp.getWriter().write(objectMapper.writeValueAsString(error));
		}
	}
}