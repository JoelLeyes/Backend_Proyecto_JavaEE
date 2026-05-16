package com.nexolab.service;

import com.nexolab.dao.MessageDAO;
import com.nexolab.model.*;
import com.nexolab.util.FileStorageUtil;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

public class MessageService {
	private final MessageDAO messageDAO = new MessageDAO();
	private final PushNotificationService pushNotificationService = new PushNotificationService();

	public void enviarMensaje(Chat chat, Usuario emisor, String contenido) {
		enviarMensaje(chat, emisor, contenido, null);
	}

	public void enviarMensaje(Chat chat, Usuario emisor, String contenido, Long respondaMensajeId) {
		if (chat == null) {
			throw new IllegalArgumentException("Chat is required");
		}
		if (emisor == null || emisor.getIdUsuario() == null) {
			throw new IllegalArgumentException("Sender is required");
		}
        if (contenido == null) {
            contenido = "";
        }

		Mensaje mensaje = new Mensaje();
		mensaje.setChat(chat);
		mensaje.setContenido(contenido);
		mensaje.setFechaEnviado(new Date());

		if (respondaMensajeId != null) {
			Mensaje ra = messageDAO.findById(respondaMensajeId);
			if (ra != null) mensaje.setRespondeA(ra);
		}

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
		pushNotificationService.notifyMessage(chat, emisor, mensaje);
	}

	/**
	 * Envía un mensaje con adjunto (archivo)
	 * @param chat Chat donde se envía
	 * @param emisor Usuario que envía
	 * @param contenido Texto del mensaje
	 * @param archivo El archivo a adjuntar (opcional)
	 */
	public void enviarMensajeConAdjunto(Chat chat, Usuario emisor, String contenido, Part archivo) {
		if (chat == null) {
			throw new IllegalArgumentException("Chat requerido");
		}
		if (emisor == null || emisor.getIdUsuario() == null) {
			throw new IllegalArgumentException("Emisor requerido");
		}
        // Permitir contenido vacío (especialmente si hay adjunto)
        if (contenido == null) {
            contenido = "";
        }

		// Crear mensaje
		Mensaje mensaje = new Mensaje();
		mensaje.setChat(chat);
		mensaje.setContenido(contenido);
		mensaje.setFechaEnviado(new Date());

		// Si hay archivo adjunto, guardarlo
		if (archivo != null && archivo.getSize() > 0) {
			try {
				// Guardar archivo en disco y obtener URL
				String url = FileStorageUtil.guardarArchivo(archivo);
				String nombreArchivo = archivo.getSubmittedFileName();
				String tipoArchivo = archivo.getContentType();

				// Crear objeto Adjunto y asociarlo al mensaje
				Adjunto adjunto = new Adjunto(tipoArchivo, nombreArchivo, url, mensaje);
				mensaje.agregarAdjunto(adjunto);
			} catch (IOException e) {
				throw new RuntimeException("Error al guardar archivo: " + e.getMessage(), e);
			}
		}

		// Crear estados del mensaje (entregado a todos los participantes)
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

		// Guardar mensaje con adjunto en BD
		messageDAO.save(mensaje);
		pushNotificationService.notifyMessage(chat, emisor, mensaje);
	}

	/**
	 * Envía un mensaje con MÚLTIPLES adjuntos (archivos)
	 * @param chat Chat donde se envía
	 * @param emisor Usuario que envía
	 * @param contenido Texto del mensaje
	 * @param archivos Lista de archivos a adjuntar
	 */
	public void enviarMensajeConAdjuntos(Chat chat, Usuario emisor, String contenido, java.util.List<Part> archivos) {
		enviarMensajeConAdjuntos(chat, emisor, contenido, archivos, null);
	}

	public void enviarMensajeConAdjuntos(Chat chat, Usuario emisor, String contenido, java.util.List<Part> archivos, Long respondaMensajeId) {
		if (chat == null) {
			throw new IllegalArgumentException("Chat requerido");
		}
		if (emisor == null || emisor.getIdUsuario() == null) {
			throw new IllegalArgumentException("Emisor requerido");
		}
        // Permitir contenido vacío (especialmente si hay adjuntos)
        if (contenido == null) {
            contenido = "";
        }

		// Crear mensaje
		Mensaje mensaje = new Mensaje();
		mensaje.setChat(chat);
		mensaje.setContenido(contenido);
		mensaje.setFechaEnviado(new Date());

		if (respondaMensajeId != null) {
			Mensaje ra = messageDAO.findById(respondaMensajeId);
			if (ra != null) mensaje.setRespondeA(ra);
		}

		// Guardar TODOS los archivos adjuntos
		if (archivos != null && !archivos.isEmpty()) {
			for (Part archivo : archivos) {
				if (archivo != null && archivo.getSize() > 0) {
					try {
						// Guardar archivo en disco y obtener URL
						String url = FileStorageUtil.guardarArchivo(archivo);
						String nombreArchivo = archivo.getSubmittedFileName();
						String tipoArchivo = archivo.getContentType();

						// Crear objeto Adjunto y asociarlo al mensaje
						Adjunto adjunto = new Adjunto(tipoArchivo, nombreArchivo, url, mensaje);
						mensaje.agregarAdjunto(adjunto);
					} catch (IOException e) {
						// Log error pero continuar con los demás archivos
						System.err.println("Error al guardar archivo: " + e.getMessage());
					}
				}
			}
		}

		// Crear estados del mensaje (entregado a todos los participantes)
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

		// Guardar mensaje con adjuntos en BD
		messageDAO.save(mensaje);
		pushNotificationService.notifyMessage(chat, emisor, mensaje);
	}

	public List<Mensaje> obtenerMensajesDesdeFecha(Chat chat, Date desdeFecha) {
		return messageDAO.findByChatSince(chat, desdeFecha);
	}

	public List<Mensaje> obtenerMensajesDesdeFecha(Chat chat, LocalDateTime desdeFecha) {
		Date since = Date.from(desdeFecha.atZone(ZoneId.systemDefault()).toInstant());
		return obtenerMensajesDesdeFecha(chat, since);
	}
}