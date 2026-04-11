package com.nexolab.service;

import com.nexolab.dao.MessageDAO;
import com.nexolab.model.Mensaje;
import com.nexolab.model.Chat;
import com.nexolab.model.Usuario;
import java.time.LocalDateTime;
import java.util.List;

public class MessageService {
	private MessageDAO messageDAO = new MessageDAO();

	public void enviarMensaje(Chat chat, Usuario emisor, String contenido, String tipo) {
		Mensaje mensaje = new Mensaje();
		mensaje.setConversacion(chat);
		mensaje.setSender(emisor);
		mensaje.setContent(contenido);
		mensaje.setTipo(Enum.valueOf(com.nexolab.model.EstadoMensaje.class, tipo));
		messageDAO.save(mensaje);

		// Actualizar último mensaje en chat
		chat.setLastMessage(contenido);
		chat.setLastMessageAt(mensaje.getTimestamp());
		// Note: You might need to update unread count, etc.
	}

	public List<Mensaje> obtenerMensajesDesdeFecha(Chat chat, LocalDateTime desdeFecha) {
		return messageDAO.findByConversationSince(chat, desdeFecha);
	}
}