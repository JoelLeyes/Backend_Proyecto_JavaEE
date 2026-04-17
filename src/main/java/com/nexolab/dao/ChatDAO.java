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

	public List<Chat> findByUsuario(Usuario usuario) {
		EntityManager em = emf.createEntityManager();
		Long userId = usuario == null ? null : usuario.getIdUsuario();
		List<Chat> chats = em.createQuery(
				"SELECT DISTINCT c FROM Chat c JOIN c.participantes p LEFT JOIN FETCH c.mensajes " +
						"WHERE p.idUsuario = :userId",
				Chat.class)
				.setParameter("userId", userId)
				.getResultList();
		em.close();
		return chats;
	}
}

//HOLA