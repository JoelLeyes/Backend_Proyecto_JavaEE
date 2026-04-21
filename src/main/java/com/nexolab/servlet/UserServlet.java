package com.nexolab.servlet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexolab.dao.UserDAO;
import com.nexolab.model.TipoEstado;
import com.nexolab.model.Usuario;
import com.nexolab.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/api/usuarios/*")
public class UserServlet extends HttpServlet {

    private final AuthService  authService  = new AuthService();
    private final UserDAO      userDAO      = new UserDAO();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");

        Usuario current = requireUser(req, resp);
        if (current == null) return;

        String path = req.getPathInfo();

        if ("/me".equals(path)) {
            resp.getWriter().write(objectMapper.writeValueAsString(usuarioToMap(current)));
            return;
        }

        // búsqueda: GET /api/usuarios?q=texto  (path es null o "/")
        String q = req.getParameter("q");
        if (q == null || q.trim().length() < 2) {
            resp.getWriter().write("[]");
            return;
        }

        List<Usuario> usuarios = userDAO.findByQuery(q.trim(), current.getIdUsuario());
        List<Map<String, Object>> result = usuarios.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("idUsuario", u.getIdUsuario());
            map.put("nombre",    u.getNombre());
            map.put("apellido",  u.getApellido());
            map.put("email",     u.getEmail());
            map.put("cargo",     u.getCargo());
            map.put("sector",    u.getSector() == null ? null : u.getSector().toString());
            return map;
        }).collect(Collectors.toList());

        resp.getWriter().write(objectMapper.writeValueAsString(result));
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");

        Usuario current = requireUser(req, resp);
        if (current == null) return;

        String path = req.getPathInfo();
        if (!"/me/estado".equals(path)) {
            resp.setStatus(404);
            return;
        }

        Map<String, Object> body;
        try {
            body = objectMapper.readValue(req.getInputStream(), new TypeReference<>() {});
        } catch (Exception e) {
            resp.setStatus(400);
            resp.getWriter().write("{\"message\":\"Cuerpo JSON inválido\"}");
            return;
        }

        if (!body.containsKey("tipoEstado")) {
            resp.setStatus(400);
            resp.getWriter().write("{\"message\":\"Se requiere el campo tipoEstado\"}");
            return;
        }

        TipoEstado nuevoEstado;
        try {
            nuevoEstado = TipoEstado.valueOf(body.get("tipoEstado").toString());
        } catch (IllegalArgumentException e) {
            resp.setStatus(400);
            resp.getWriter().write("{\"message\":\"Valor de tipoEstado inválido\"}");
            return;
        }

        current.setTipoEstado(nuevoEstado);
        userDAO.update(current);

        resp.getWriter().write(objectMapper.writeValueAsString(usuarioToMap(current)));
    }

    private Map<String, Object> usuarioToMap(Usuario u) {
        Map<String, Object> map = new HashMap<>();
        map.put("idUsuario",    u.getIdUsuario());
        map.put("nombre",       u.getNombre());
        map.put("apellido",     u.getApellido());
        map.put("email",        u.getEmail());
        map.put("cargo",        u.getCargo());
        map.put("sector",       u.getSector()     == null ? null : u.getSector().toString());
        map.put("tipoEstado",   u.getTipoEstado() == null ? null : u.getTipoEstado().toString());
        map.put("rolSistema",   u.getRolSistema() == null ? "USUARIO" : u.getRolSistema().toString());
        return map;
    }

    private Usuario requireUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            resp.setStatus(401);
            resp.getWriter().write("{\"message\":\"Token requerido\"}");
            return null;
        }
        Usuario u = authService.getUserFromToken(header.substring(7));
        if (u == null) {
            resp.setStatus(401);
            resp.getWriter().write("{\"message\":\"Token inválido\"}");
            return null;
        }
        return u;
    }
}
