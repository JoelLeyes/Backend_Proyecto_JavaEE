package com.nexolab.dao;

import com.nexolab.model.Mensaje;
import com.nexolab.model.Reaccion;
import com.nexolab.model.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReaccionDAO {

    private static final EntityManagerFactory emf =
            Persistence.createEntityManagerFactory("NexoLabPU");

    /** Alterna la reacción: si ya existe la elimina, si no existe la crea. Retorna true si fue agregada. */
    public boolean toggle(Long msgId, Long userId, String emoji) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        List<Reaccion> existing = em.createQuery(
                "SELECT r FROM Reaccion r " +
                "WHERE r.mensaje.idMensaje = :msgId " +
                "AND r.usuario.idUsuario = :userId " +
                "AND r.emoji = :emoji", Reaccion.class)
                .setParameter("msgId",  msgId)
                .setParameter("userId", userId)
                .setParameter("emoji",  emoji)
                .getResultList();
        boolean added;
        if (!existing.isEmpty()) {
            em.remove(existing.get(0));
            added = false;
        } else {
            Mensaje msg  = em.find(Mensaje.class, msgId);
            Usuario user = em.find(Usuario.class, userId);
            if (msg == null || user == null) {
                em.getTransaction().rollback();
                em.close();
                return false;
            }
            em.persist(new Reaccion(msg, user, emoji));
            added = true;
        }
        em.getTransaction().commit();
        em.close();
        return added;
    }

    /** Devuelve todas las reacciones agrupadas por idMensaje para los IDs dados. */
    public Map<Long, List<Reaccion>> findByMensajeIds(List<Long> msgIds) {
        if (msgIds == null || msgIds.isEmpty()) return Collections.emptyMap();
        EntityManager em = emf.createEntityManager();
        List<Reaccion> list = em.createQuery(
                "SELECT r FROM Reaccion r JOIN FETCH r.usuario " +
                "WHERE r.mensaje.idMensaje IN :ids", Reaccion.class)
                .setParameter("ids", msgIds)
                .getResultList();
        em.close();
        return list.stream()
                .collect(Collectors.groupingBy(r -> r.getMensaje().getIdMensaje()));
    }
}
