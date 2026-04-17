package com.nexolab.dao;

import com.nexolab.model.Chat;
import com.nexolab.model.Mensaje;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.Comparator;
import java.util.Date;
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

	public List<Mensaje> findByChatSince(Chat chat, Date since) {
		EntityManager em = emf.createEntityManager();
		List<Mensaje> messages = em.createQuery(
				"SELECT DISTINCT m FROM Mensaje m " +
						"LEFT JOIN FETCH m.adjuntos " +
						"LEFT JOIN FETCH m.estados e " +
						"LEFT JOIN FETCH e.usuario " +
						"WHERE m.chat = :chat AND m.fechaEnviado > :since",
				Mensaje.class)
				.setParameter("chat", chat)
				.setParameter("since", since)
				.getResultList();
		em.close();

		messages.sort(Comparator.comparing(Mensaje::getFechaEnviado));
		return messages;
	}
}