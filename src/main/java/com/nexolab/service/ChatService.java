package com.nexolab.service;
import com.nexolab.dao.ChatDAO;
import com.nexolab.model.Chat;
import com.nexolab.model.TipoChat;
import com.nexolab.model.Usuario;
import java.util.List;
import java.util.Set;

public class ChatService {
	private ChatDAO chatDAO = new ChatDAO();

	public List<Chat> obtenerChatsDelUsuario(Usuario usuario) {
		return chatDAO.findByUsuario(usuario);
	}

	public Chat obtenerChatPorId(Long id) {
		return chatDAO.findById(id);
	}

	public Chat crearChat(String name, TipoChat type, Set<Usuario> participantes) {
		Chat chat = new Chat();
		chat.setName(name);
		chat.setType(type);
		chat.setParticipantes(participantes);
		chatDAO.save(chat);
		return chat;
	}
}