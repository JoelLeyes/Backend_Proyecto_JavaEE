package com.nexolab.service;

import com.nexolab.dao.ChatDAO;
import com.nexolab.model.Chat;
import com.nexolab.model.Mensaje;
import com.nexolab.model.Participa;
import com.nexolab.model.RolUsuario;
import com.nexolab.model.TipoChat;
import com.nexolab.model.Usuario;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

	public Chat crearChatGrupal(Usuario creador, String nombre, List<Usuario> miembros) {
		Chat chat = new Chat(nombre, TipoChat.GRUPAL, new Date());

		Set<Usuario> participantes = new HashSet<>();
		participantes.add(creador);
		participantes.addAll(miembros);
		chat.setParticipantes(participantes);

		List<Participa> participaciones = new ArrayList<>();
		participaciones.add(new Participa(new Date(), RolUsuario.ADMINISTRADOR, creador, chat));
		for (Usuario m : miembros) {
			participaciones.add(new Participa(new Date(), RolUsuario.MIEMBRO, m, chat));
		}

		return chatDAO.saveWithParticipantes(chat, participaciones);
	}

	public List<Participa> obtenerParticipaciones(Long chatId) {
		return chatDAO.findParticipacionesByChat(chatId);
	}

	public boolean esAdmin(Long chatId, Usuario usuario) {
		Participa p = chatDAO.findParticipacion(chatId, usuario.getIdUsuario());
		return p != null && p.getRolUsuario() == RolUsuario.ADMINISTRADOR;
	}

	public void agregarParticipante(Long chatId, Usuario solicitante, Usuario nuevo) {
		if (!esAdmin(chatId, solicitante)) {
			throw new SecurityException("Solo el administrador puede agregar participantes");
		}
		Participa existing = chatDAO.findParticipacion(chatId, nuevo.getIdUsuario());
		if (existing != null) return;
		chatDAO.agregarParticipante(chatId, nuevo.getIdUsuario(), RolUsuario.MIEMBRO);
	}

	public void abandonarChat(Long chatId, Usuario usuario) {
		chatDAO.removeParticipante(chatId, usuario.getIdUsuario());
	}

	public Chat crearChatPrivado(Usuario creador, Usuario otro) {
		Chat existing = chatDAO.findPrivateChat(creador.getIdUsuario(), otro.getIdUsuario());
		if (existing != null) return existing;

		String nombre = (otro.getNombre() + " " + otro.getApellido()).trim();
		Chat chat = new Chat(nombre, TipoChat.PRIVADO, new Date());

		Set<Usuario> participantes = new HashSet<>();
		participantes.add(creador);
		participantes.add(otro);
		chat.setParticipantes(participantes);

		List<Participa> participaciones = List.of(
			new Participa(new Date(), RolUsuario.ADMINISTRADOR, creador, chat),
			new Participa(new Date(), RolUsuario.MIEMBRO, otro, chat)
		);

		return chatDAO.saveWithParticipantes(chat, participaciones);
	}
}