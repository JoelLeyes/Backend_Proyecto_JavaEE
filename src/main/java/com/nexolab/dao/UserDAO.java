package com.nexolab.dao;

import com.nexolab.model.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.sql.Timestamp;
import java.util.Date;

public class UserDAO {
	private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("NexoLabPU");

	public static class LegacyUser {
		private Long id;
		private String name;
		private String email;
		private String passwordHash;
		private String role;
		private String sector;
		private Date createdAt;

		public Long getId() { return id; }
		public String getName() { return name; }
		public String getEmail() { return email; }
		public String getPasswordHash() { return passwordHash; }
		public String getRole() { return role; }
		public String getSector() { return sector; }
		public Date getCreatedAt() { return createdAt; }
	}

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

	/**
	 * Compatibilidad: usuarios legacy guardados en la tabla antigua `users`.
	 *
	 * Columnas esperadas: id, name, email, passwordhash, role, sector, createdat.
	 */
	public LegacyUser findLegacyByEmail(String email) {
		EntityManager em = emf.createEntityManager();
		try {
			Object[] row;
			try {
				row = (Object[]) em.createNativeQuery(
						"SELECT id, name, email, passwordhash, role, sector, createdat FROM users WHERE email = ?1")
						.setParameter(1, email)
						.getResultStream()
						.findFirst()
						.orElse(null);
			} catch (RuntimeException ex) {
				// Si la tabla legacy ya no existe (o hay un problema de esquema),
				// tratamos como "no encontrado" para no romper el login.
				return null;
			}
			if (row == null) {
				return null;
			}

			LegacyUser u = new LegacyUser();
			u.id = row[0] == null ? null : ((Number) row[0]).longValue();
			u.name = row[1] == null ? null : String.valueOf(row[1]);
			u.email = row[2] == null ? null : String.valueOf(row[2]);
			u.passwordHash = row[3] == null ? null : String.valueOf(row[3]);
			u.role = row[4] == null ? null : String.valueOf(row[4]);
			u.sector = row[5] == null ? null : String.valueOf(row[5]);
			if (row[6] instanceof Timestamp ts) {
				u.createdAt = new Date(ts.getTime());
			} else if (row[6] instanceof Date d) {
				u.createdAt = d;
			} else {
				u.createdAt = null;
			}
			return u;
		} finally {
			em.close();
		}
	}

	public Usuario findById(Long id) {
		EntityManager em = emf.createEntityManager();
		Usuario user = em.find(Usuario.class, id);
		em.close();
		return user;
	}

	public java.util.List<Usuario> findByQuery(String q, Long excludeId) {
		EntityManager em = emf.createEntityManager();
		String pattern = "%" + q.toLowerCase() + "%";
		java.util.List<Usuario> users = em.createQuery(
				"SELECT u FROM Usuario u WHERE u.rolSistema = com.nexolab.model.RolSistema.USUARIO " +
				"AND u.idUsuario <> :excludeId " +
				"AND (LOWER(u.nombre) LIKE :q OR LOWER(u.apellido) LIKE :q OR LOWER(u.email) LIKE :q)",
				Usuario.class)
				.setParameter("q", pattern)
				.setParameter("excludeId", excludeId)
				.setMaxResults(20)
				.getResultList();
		em.close();
		return users;
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