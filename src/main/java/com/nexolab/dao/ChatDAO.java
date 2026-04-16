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
		Chat chat = em.find(Chat.class, id);
		em.close();
		return chat;
	}

	public List<Chat> findByUsuario(Usuario usuario) {
		EntityManager em = emf.createEntityManager();
		List<Chat> chats = em.createQuery(
				"SELECT c FROM Chat c JOIN c.participantes p WHERE p = :usuario", Chat.class)
				.setParameter("usuario", usuario)
				.getResultList();
		em.close();
		return chats;
	}
}

//HOLA