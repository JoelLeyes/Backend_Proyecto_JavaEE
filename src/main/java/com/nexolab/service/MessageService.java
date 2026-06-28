package com.nexolab.service;

import com.nexolab.dao.MessageDAO;
import com.nexolab.model.*;
import com.nexolab.util.FileStorageUtil;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MessageService {
	private final MessageDAO messageDAO = new MessageDAO();
	private final PushNotificationService pushNotificationService = new PushNotificationService();
	private final MongoAuditService auditService = MongoAuditService.getInstance();

	public Mensaje enviarMensaje(Chat chat, Usuario emisor, String contenido) {
		return enviarMensaje(chat, emisor, contenido, null);
	}

	public Mensaje enviarMensaje(Chat chat, Usuario emisor, String contenido, Long respondaMensajeId) {
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
		boolean groupChat = TipoChat.GRUPAL.equals(chat.getTipoChat());
		String chatType = chat.getTipoChat() == null ? null : chat.getTipoChat().toString();
		String recipientEmail = groupChat ? null : MongoAuditService.primaryRecipientEmail(chat, emisor.getIdUsuario());
		List<String> recipientEmails = groupChat ? MongoAuditService.recipientEmails(chat, emisor.getIdUsuario()) : null;
		auditService.logMessageAction(
			"MessageService",
			"MESSAGE_SEND",
			true,
			chat.getIdChat(),
			chat.getNombreChat(),
			chatType,
			emisor.getIdUsuario(),
			emisor.getEmail(),
			recipientEmail,
			recipientEmails,
			"Mensaje enviado correctamente",
			null
		);
		return mensaje;
	}

	/**
	 * Envía un mensaje con adjunto (archivo)
	 * @param chat Chat donde se envía
	 * @param emisor Usuario que envía
	 * @param contenido Texto del mensaje
	 * @param archivo El archivo a adjuntar (opcional)
	 */
	public Mensaje enviarMensajeConAdjunto(Chat chat, Usuario emisor, String contenido, Part archivo) {
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
		boolean groupChat = TipoChat.GRUPAL.equals(chat.getTipoChat());
		String chatType = chat.getTipoChat() == null ? null : chat.getTipoChat().toString();
		String recipientEmail = groupChat ? null : MongoAuditService.primaryRecipientEmail(chat, emisor.getIdUsuario());
		List<String> recipientEmails = groupChat ? MongoAuditService.recipientEmails(chat, emisor.getIdUsuario()) : null;
		auditService.logMessageAction(
			"MessageService",
			"MESSAGE_SEND",
			true,
			chat.getIdChat(),
			chat.getNombreChat(),
			chatType,
			emisor.getIdUsuario(),
			emisor.getEmail(),
			recipientEmail,
			recipientEmails,
			"Mensaje enviado correctamente",
			null
		);
		return mensaje;
	}

	/**
	 * Envía un mensaje con MÚLTIPLES adjuntos (archivos)
	 * @param chat Chat donde se envía
	 * @param emisor Usuario que envía
	 * @param contenido Texto del mensaje
	 * @param archivos Lista de archivos a adjuntar
	 */
	public Mensaje enviarMensajeConAdjuntos(Chat chat, Usuario emisor, String contenido, java.util.List<Part> archivos) {
		return enviarMensajeConAdjuntos(chat, emisor, contenido, archivos, null);
	}

	public Mensaje enviarMensajeConAdjuntos(Chat chat, Usuario emisor, String contenido, java.util.List<Part> archivos, Long respondaMensajeId) {
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
		boolean groupChat = TipoChat.GRUPAL.equals(chat.getTipoChat());
		String chatType = chat.getTipoChat() == null ? null : chat.getTipoChat().toString();
		String recipientEmail = groupChat ? null : MongoAuditService.primaryRecipientEmail(chat, emisor.getIdUsuario());
		List<String> recipientEmails = groupChat ? MongoAuditService.recipientEmails(chat, emisor.getIdUsuario()) : null;
		auditService.logMessageAction(
			"MessageService",
			"MESSAGE_SEND",
			true,
			chat.getIdChat(),
			chat.getNombreChat(),
			chatType,
			emisor.getIdUsuario(),
			emisor.getEmail(),
			recipientEmail,
			recipientEmails,
			"Mensaje enviado correctamente",
			null
		);
		return mensaje;
	}

	private Map<String, Object> buildMessageMetadata(String contenido,
	                                                int attachmentCount,
	                                                boolean hasAttachments,
	                                                Long replyToMessageId,
	                                                int attemptedAttachmentCount) {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("contentLength", contenido == null ? 0 : contenido.length());
		metadata.put("hasAttachments", hasAttachments);
		metadata.put("attachmentCount", attachmentCount);
		metadata.put("attemptedAttachmentCount", attemptedAttachmentCount);
		if (replyToMessageId != null) {
			metadata.put("replyToMessageId", replyToMessageId);
		}
		return metadata;
	}

	public List<Mensaje> obtenerMensajesDesdeFecha(Chat chat, Date desdeFecha) {
		return messageDAO.findByChatSince(chat, desdeFecha);
	}

	public List<Mensaje> obtenerMensajesDesdeFecha(Chat chat, LocalDateTime desdeFecha) {
		Date since = Date.from(desdeFecha.atZone(ZoneId.systemDefault()).toInstant());
		return obtenerMensajesDesdeFecha(chat, since);
	}
}