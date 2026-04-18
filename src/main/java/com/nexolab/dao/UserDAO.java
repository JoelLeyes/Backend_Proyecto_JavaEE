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

	public java.util.List<Usuario> findAll() {
		EntityManager em = emf.createEntityManager();
		java.util.List<Usuario> users = em.createQuery("SELECT u FROM Usuario u ORDER BY u.fechaCreacion DESC", Usuario.class)
				.getResultList();
		em.close();
		return users;
	}

	public Usuario findAdminSistema() {
		EntityManager em = emf.createEntityManager();
		Usuario admin = em.createQuery("SELECT u FROM Usuario u WHERE u.rolSistema = com.nexolab.model.RolSistema.ADMIN_SISTEMA", Usuario.class)
				.getResultStream()
				.findFirst()
				.orElse(null);
		em.close();
		return admin;
	}

	public void update(Usuario user) {
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		em.merge(user);
		em.getTransaction().commit();
		em.close();
	}
}