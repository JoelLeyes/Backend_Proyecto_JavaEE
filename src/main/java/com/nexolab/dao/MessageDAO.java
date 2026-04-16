package com.nexolab.dao;

import com.nexolab.model.Mensaje;
import com.nexolab.model.Chat;
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
}