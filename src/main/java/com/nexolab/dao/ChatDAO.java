package com.nexolab.dao;

import com.nexolab.model.Chat;
import com.nexolab.model.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.List;

public class ChatDAO {
	private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("NexoLabPU");

	public void save(Chat chat) {
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		em.persist(chat);
		em.getTransaction().commit();
		em.close();
	}

	public Chat findById(Long id) {
		EntityManager em = emf.createEntityManager();
		Chat chat = em.createQuery(
				"SELECT c FROM Chat c LEFT JOIN FETCH c.participantes WHERE c.idChat = :id",
				Chat.class)
				.setParameter("id", id)
				.getResultStream()
				.findFirst()
				.orElse(null);
		em.close();
		return chat;
	}

	public Chat findPrivateChat(Long userId1, Long userId2) {
		EntityManager em = emf.createEntityManager();
		Chat chat = em.createQuery(
				"SELECT c FROM Chat c JOIN c.participantes p1 JOIN c.participantes p2 " +
				"WHERE c.tipoChat = com.nexolab.model.TipoChat.PRIVADO " +
				"AND p1.idUsuario = :userId1 AND p2.idUsuario = :userId2",
				Chat.class)
				.setParameter("userId1", userId1)
				.setParameter("userId2", userId2)
				.getResultStream()
				.findFirst()
				.orElse(null);
		em.close();
		return chat;
	}

	public Chat saveWithParticipantes(Chat chat, java.util.List<com.nexolab.model.Participa> participaciones) {
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		em.persist(chat);
		em.flush();
		for (com.nexolab.model.Participa p : participaciones) {
			em.persist(p);
		}
		em.getTransaction().commit();
		em.close();
		return chat;
	}

	public List<Chat> findByUsuario(Usuario usuario) {
		EntityManager em = emf.createEntityManager();
		Long userId = usuario == null ? null : usuario.getIdUsuario();
		List<Chat> chats = em.createQuery(
				"SELECT DISTINCT c FROM Chat c " +
						"JOIN c.participantes p " +
						"LEFT JOIN FETCH c.participantes " +
						"LEFT JOIN FETCH c.mensajes " +
						"WHERE p.idUsuario = :userId",
				Chat.class)
				.setParameter("userId", userId)
				.getResultList();
		em.close();
		return chats;
	}
}