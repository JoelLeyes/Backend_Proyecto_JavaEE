package com.nexolab.service;

import com.nexolab.dao.ChatDAO;
import com.nexolab.model.Chat;
import com.nexolab.model.Usuario;
import java.util.List;

public class ChatService {
	private ChatDAO chatDAO = new ChatDAO();

	public List<Chat> obtenerChatsDelUsuario(Usuario usuario) {
		return chatDAO.findByUsuario(usuario);
	}

	public Chat obtenerChatPorId(Long id) {
		return chatDAO.findById(id);
	}
}