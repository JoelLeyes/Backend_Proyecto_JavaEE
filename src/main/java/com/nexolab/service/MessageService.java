package com.nexolab.service;

import com.nexolab.dao.MessageDAO;
import com.nexolab.model.Chat;
import com.nexolab.model.Estado;
import com.nexolab.model.EstadoMensaje;
import com.nexolab.model.Mensaje;
import com.nexolab.model.Usuario;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

public class MessageService {
	private final MessageDAO messageDAO = new MessageDAO();

	public void enviarMensaje(Chat chat, Usuario emisor, String contenido) {
		if (chat == null) {
			throw new IllegalArgumentException("Chat is required");
		}
		if (emisor == null || emisor.getIdUsuario() == null) {
			throw new IllegalArgumentException("Sender is required");
		}
		if (contenido == null || contenido.isBlank()) {
			throw new IllegalArgumentException("Content is required");
		}

		Mensaje mensaje = new Mensaje();
		mensaje.setChat(chat);
		mensaje.setContenido(contenido);
		mensaje.setFechaEnviado(new Date());

		Date ahora = new Date();
		if (chat.getParticipantes() != null && !chat.getParticipantes().isEmpty()) {
			for (Usuario u : chat.getParticipantes()) {
				if (u == null || u.getIdUsuario() == null) {
					continue;
				}
				EstadoMensaje estadoMensaje = new EstadoMensaje(
					u.getIdUsuario().equals(emisor.getIdUsuario()) ? Estado.ENVIADO : Estado.ENTREGADO,
					ahora
				);
				estadoMensaje.setUsuario(u);
				estadoMensaje.setMensaje(mensaje);
				mensaje.getEstados().add(estadoMensaje);
			}
		} else {
			EstadoMensaje estadoMensaje = new EstadoMensaje(Estado.ENVIADO, ahora);
			estadoMensaje.setUsuario(emisor);
			estadoMensaje.setMensaje(mensaje);
			mensaje.getEstados().add(estadoMensaje);
		}

		messageDAO.save(mensaje);
	}

	public List<Mensaje> obtenerMensajesDesdeFecha(Chat chat, Date desdeFecha) {
		return messageDAO.findByChatSince(chat, desdeFecha);
	}

	public List<Mensaje> obtenerMensajesDesdeFecha(Chat chat, LocalDateTime desdeFecha) {
		Date since = Date.from(desdeFecha.atZone(ZoneId.systemDefault()).toInstant());
		return obtenerMensajesDesdeFecha(chat, since);
	}
}