package com.nexolab.dao;

import com.nexolab.model.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class UserDAO {
	private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("NexoLabPU");

	public void save(Usuario user) {
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		em.persist(user);
		em.getTransaction().commit();
		em.close();
	}

	public Usuario findByEmail(String email) {
		EntityManager em = emf.createEntityManager();
		Usuario user = em.createQuery("SELECT u FROM Usuario u WHERE u.email = :email", Usuario.class)
				.setParameter("email", email)
				.getResultStream()
				.findFirst()
				.orElse(null);
		em.close();
		return user;
	}

	public Usuario findById(Long id) {
		EntityManager em = emf.createEntityManager();
		Usuario user = em.find(Usuario.class, id);
		em.close();
		return user;
	}
}