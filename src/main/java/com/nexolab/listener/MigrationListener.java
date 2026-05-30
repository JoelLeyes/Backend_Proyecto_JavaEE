package com.nexolab.listener;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.logging.Logger;

@WebListener
public class MigrationListener implements ServletContextListener {

    private static final Logger log = Logger.getLogger(MigrationListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        EntityManagerFactory emf = null;
        try {
            emf = Persistence.createEntityManagerFactory("NexoLabPU");
            // Hibernate hbm2ddl=update never widens VARCHAR to TEXT; do it manually.
            runMigration(emf,
                "ALTER TABLE usuarios ALTER COLUMN foto_perfil_url TYPE TEXT",
                "foto_perfil_url → TEXT");
            // Ensures push_token column exists even if Hibernate hasn't run yet.
            runMigration(emf,
                "ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS push_token VARCHAR(512) NULL",
                "push_token VARCHAR(512)");
        } finally {
            if (emf != null) try { emf.close(); } catch (Exception ignored) {}
        }
    }

    private void runMigration(EntityManagerFactory emf, String sql, String description) {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            em.createNativeQuery(sql).executeUpdate();
            em.getTransaction().commit();
            log.info("MigrationListener: " + description + " aplicada correctamente.");
        } catch (Exception e) {
            log.info("MigrationListener: " + description + " — ya aplicada o no necesaria: " + e.getMessage());
            if (em != null && em.getTransaction().isActive()) {
                try { em.getTransaction().rollback(); } catch (Exception ignored) {}
            }
        } finally {
            if (em != null) try { em.close(); } catch (Exception ignored) {}
        }
    }
}
