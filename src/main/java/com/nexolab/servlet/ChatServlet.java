package com.nexolab.servlet;

import com.nexolab.service.AuthService;
import com.nexolab.service.ChatService;
import com.nexolab.model.Chat;
import com.nexolab.model.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
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

		List<Chat> chats = chatService.obtenerChatsDelUsuario(usuario);
		List<Map<String, Object>> chatsList = chats.stream().map(c -> {
			Map<String, Object> map = new HashMap<>();
			map.put("idChat", c.getIdChat());
			map.put("nombreChat", c.getNombreChat());
			map.put("tipoChat", c.getTipoChat() == null ? null : c.getTipoChat().toString());
			map.put("ultimoMensaje", c.getUltimoMensaje());
			map.put("horaUltimoMensaje", c.getHoraUltimoMensaje() == null ? null : c.getHoraUltimoMensaje().toString());
			map.put("mensajesSinLeer", c.getMensajesSinLeer());
			return map;
		}).collect(Collectors.toList());

		resp.setContentType("application/json");
		resp.getWriter().write(objectMapper.writeValueAsString(chatsList));
	}
}