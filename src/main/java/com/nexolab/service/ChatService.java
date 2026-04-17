package com.nexolab.service;

import com.nexolab.dao.ChatDAO;
import com.nexolab.model.Chat;
import com.nexolab.model.Mensaje;
import com.nexolab.model.Usuario;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

public class ChatService {
	private final ChatDAO chatDAO = new ChatDAO();

	public List<Chat> obtenerChatsDelUsuario(Usuario usuario) {
		List<Chat> chats = chatDAO.findByUsuario(usuario);

		// Completar campos transient para el frontend.
		for (Chat c : chats) {
			Mensaje ultimo = c.getMensajes().stream()
					.max(Comparator.comparing(Mensaje::getFechaEnviado))
					.orElse(null);
			if (ultimo != null) {
				c.setUltimoMensaje(ultimo.getContenido());
				LocalDateTime ldt = LocalDateTime.ofInstant(ultimo.getFechaEnviado().toInstant(), ZoneId.systemDefault());
				c.setHoraUltimoMensaje(ldt);
			}
			if (c.getMensajesSinLeer() == null) {
				c.setMensajesSinLeer(0);
			}
		}

		return chats;
	}

	public Chat obtenerChatPorId(Long id) {
		return chatDAO.findById(id);
	}
}