package com.nexolab.dao;

import com.nexolab.model.Mensaje;
import com.nexolab.model.Chat;
import com.nexolab.model.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.time.LocalDateTime;
import java.util.List;

public class MessageDAO {
	private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("NexoLabPU");

	public void save(Mensaje message) {
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		em.persist(message);
		em.getTransaction().commit();
		em.close();
	}

	public List<Mensaje> findByConversationSince(Chat chat, LocalDateTime since) {
		EntityManager em = emf.createEntityManager();
		List<Mensaje> messages = em.createQuery(
				"SELECT m FROM Mensaje m WHERE m.conversacion = :chat AND m.timestamp > :since ORDER BY m.timestamp",
				Mensaje.class)
				.setParameter("chat", chat)
				.setParameter("since", since)
				.getResultList();
		em.close();
		return messages;
	}

	/**
	 * Obtener últimos N mensajes de un usuario
	 */
	public List<Mensaje> findByUserLimit(Usuario usuario, int limit) {
		EntityManager em = emf.createEntityManager();
		List<Mensaje> mensajes = em.createQuery(
				"SELECT m FROM Mensaje m WHERE m.sender = :usuario ORDER BY m.timestamp DESC",
				Mensaje.class)
				.setParameter("usuario", usuario)
				.setMaxResults(limit)
				.getResultList();
		em.close();
		return mensajes;
	}

	/**
	 * Contar total de mensajes de un usuario
	 */
	public long countByUser(Usuario usuario) {
		EntityManager em = emf.createEntityManager();
		Long count = em.createQuery(
				"SELECT COUNT(m) FROM Mensaje m WHERE m.sender = :usuario",
				Long.class)
				.setParameter("usuario", usuario)
				.getSingleResult();
		em.close();
		return count;
	}
}