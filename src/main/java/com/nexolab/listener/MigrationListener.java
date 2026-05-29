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
        EntityManager em = null;
        try {
            emf = Persistence.createEntityManagerFactory("NexoLabPU");
            em  = emf.createEntityManager();
            em.getTransaction().begin();
            // Hibernate hbm2ddl=update never widens VARCHAR to TEXT; do it manually.
            em.createNativeQuery(
                "ALTER TABLE usuarios ALTER COLUMN foto_perfil_url TYPE TEXT"
            ).executeUpdate();
            em.getTransaction().commit();
            log.info("MigrationListener: foto_perfil_url migrada a TEXT correctamente.");
        } catch (Exception e) {
            // Column is already TEXT or migration already ran — safe to ignore.
            log.info("MigrationListener: foto_perfil_url ya es TEXT (o migración no necesaria): " + e.getMessage());
            if (em != null && em.getTransaction().isActive()) {
                try { em.getTransaction().rollback(); } catch (Exception ignored) {}
            }
        } finally {
            if (em  != null) try { em.close();  } catch (Exception ignored) {}
            if (emf != null) try { emf.close(); } catch (Exception ignored) {}
        }
    }
}
