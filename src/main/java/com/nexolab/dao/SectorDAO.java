package com.nexolab.dao;

import com.nexolab.model.Sector;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.List;

public class SectorDAO {
	private static final EntityManagerFactory emf = Persistence.createEntityManagerFactory("NexoLabPU");

	public void save(Sector sector) {
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		em.persist(sector);
		em.getTransaction().commit();
		em.close();
	}

	public Sector findByName(String name) {
		EntityManager em = emf.createEntityManager();
		Sector sector = em.createQuery("SELECT s FROM Sector s WHERE s.name = :name", Sector.class)
				.setParameter("name", name)
				.getResultStream()
				.findFirst()
				.orElse(null);
		em.close();
		return sector;
	}

	public List<Sector> findAll() {
		EntityManager em = emf.createEntityManager();
		List<Sector> sectors = em.createQuery("SELECT s FROM Sector s ORDER BY s.name", Sector.class)
				.getResultList();
		em.close();
		return sectors;
	}

	public void update(Sector sector) {
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		em.merge(sector);
		em.getTransaction().commit();
		em.close();
	}

	public void delete(String name) {
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		Sector sector = em.find(Sector.class, name);
		if (sector != null) {
			em.remove(sector);
		}
		em.getTransaction().commit();
		em.close();
	}
}
