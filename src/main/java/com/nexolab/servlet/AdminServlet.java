package com.nexolab.servlet;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexolab.dao.MessageDAO;
import com.nexolab.dao.UserDAO;
import com.nexolab.model.Mensaje;
import com.nexolab.model.RolSistema;
import com.nexolab.model.Sector;
import com.nexolab.model.Usuario;
import com.nexolab.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/admin/*")
public class AdminServlet extends HttpServlet {

    private final AuthService authService = new AuthService();
    private final UserDAO userDAO = new UserDAO();
    private final MessageDAO messageDAO = new MessageDAO();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        Usuario admin = requireAdmin(req, resp);
        if (admin == null) return;

        String path = req.getPathInfo();

        if ("/usuarios".equals(path)) {
            handleListUsuarios(resp);
        } else if ("/stats".equals(path)) {
            handleStats(resp);
        } else if (path != null && path.matches("/usuarios/\\d+/mensajes")) {
            Long userId = Long.parseLong(path.split("/")[2]);
            int cantidad = 10;
            try { cantidad = Integer.parseInt(req.getParameter("cantidad")); } catch (Exception ignored) {}
            cantidad = Math.min(Math.max(cantidad, 1), 100);
            handleVerMensajes(resp, userId, cantidad);
        } else {
            resp.setStatus(404);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        Usuario admin = requireAdmin(req, resp);
        if (admin == null) return;

        String path = req.getPathInfo();
        if ("/password".equals(path)) {
            handleCambiarPassword(req, resp, admin);
        } else if (path != null && path.matches("/usuarios/\\d+")) {
            Long userId = Long.parseLong(path.split("/")[2]);
            handleUpdateUsuario(req, resp, userId);
        } else {
            resp.setStatus(404);
        }
    }

    private void handleListUsuarios(HttpServletResponse resp) throws IOException {
        List<Usuario> usuarios = userDAO.findAll();
        List<Map<String, Object>> lista = usuarios.stream()
                .map(this::usuarioToMap)
                .collect(Collectors.toList());
        resp.getWriter().write(objectMapper.writeValueAsString(lista));
    }

    private void handleStats(HttpServletResponse resp) throws IOException {
        List<Usuario> todos = userDAO.findAll();
        long totalUsuarios = todos.stream()
                .filter(u -> u.getRolSistema() == RolSistema.USUARIO)
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsuarios", totalUsuarios);

        Map<String, Long> porSector = todos.stream()
                .filter(u -> u.getRolSistema() == RolSistema.USUARIO)
                .collect(Collectors.groupingBy(
                        u -> u.getSector() == null ? "SIN_ASIGNAR" : u.getSector().toString(),
                        Collectors.counting()
                ));
        stats.put("usuariosPorSector", porSector);

        resp.getWriter().write(objectMapper.writeValueAsString(stats));
    }

    private void handleCambiarPassword(HttpServletRequest req, HttpServletResponse resp, Usuario admin) throws IOException {
        Map<String, Object> body = objectMapper.readValue(req.getInputStream(), new TypeReference<Map<String, Object>>() {});
        String passwordActual = body.containsKey("passwordActual") ? body.get("passwordActual").toString() : null;
        String passwordNueva  = body.containsKey("passwordNueva")  ? body.get("passwordNueva").toString()  : null;

        if (passwordActual == null || passwordNueva == null) {
            resp.setStatus(400);
            resp.getWriter().write("{\"message\":\"Se requieren passwordActual y passwordNueva\"}");
            return;
        }
        if (!BCrypt.verifyer().verify(passwordActual.toCharArray(), admin.getPasswordHash()).verified) {
            resp.setStatus(400);
            resp.getWriter().write("{\"message\":\"La contraseña actual es incorrecta\"}");
            return;
        }
        try {
            validatePasswordStrength(passwordNueva);
        } catch (IllegalArgumentException e) {
            resp.setStatus(400);
            resp.getWriter().write("{\"message\":\"" + e.getMessage() + "\"}");
            return;
        }

        admin.setPasswordHash(BCrypt.withDefaults().hashToString(12, passwordNueva.toCharArray()));
        userDAO.update(admin);
        resp.getWriter().write("{\"message\":\"Contraseña actualizada correctamente\"}");
    }

    private void validatePasswordStrength(String password) {
        if (password.length() < 8)
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
        if (password.chars().noneMatch(Character::isUpperCase))
            throw new IllegalArgumentException("La contraseña debe contener al menos una letra mayúscula");
        if (password.chars().noneMatch(Character::isDigit))
            throw new IllegalArgumentException("La contraseña debe contener al menos un número");
        if (password.chars().allMatch(Character::isLetterOrDigit))
            throw new IllegalArgumentException("La contraseña debe contener al menos un carácter especial");
    }

    private void handleVerMensajes(HttpServletResponse resp, Long userId, int cantidad) throws IOException {
        Usuario usuario = userDAO.findById(userId);
        if (usuario == null) {
            resp.setStatus(404);
            resp.getWriter().write("{\"message\":\"Usuario no encontrado\"}");
            return;
        }

        List<Mensaje> mensajes = messageDAO.findLastNByUsuario(userId, cantidad);

        List<Map<String, Object>> result = mensajes.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("idMensaje", m.getIdMensaje());
            map.put("contenido", m.getContenido());
            map.put("fechaEnviado", m.getFechaEnviado());
            map.put("nombreChat", m.getChat() == null ? null : m.getChat().getNombreChat());
            map.put("tipoChat", m.getChat() == null ? null : m.getChat().getTipoChat().toString());
            return map;
        }).collect(Collectors.toList());

        resp.getWriter().write(objectMapper.writeValueAsString(result));
    }

    private void handleUpdateUsuario(HttpServletRequest req, HttpServletResponse resp, Long userId) throws IOException {
        Usuario usuario = userDAO.findById(userId);
        if (usuario == null) {
            resp.setStatus(404);
            resp.getWriter().write("{\"message\":\"Usuario no encontrado\"}");
            return;
        }
        if (usuario.getRolSistema() == RolSistema.ADMIN_SISTEMA) {
            resp.setStatus(403);
            resp.getWriter().write("{\"message\":\"No se puede modificar al administrador del sistema\"}");
            return;
        }

        Map<String, Object> body = objectMapper.readValue(req.getInputStream(), new TypeReference<Map<String, Object>>() {});

        if (body.containsKey("cargo")) {
            usuario.setCargo(body.get("cargo").toString());
        }
        if (body.containsKey("sector")) {
            try {
                usuario.setSector(Sector.valueOf(body.get("sector").toString()));
            } catch (IllegalArgumentException ignored) {}
        }

        userDAO.update(usuario);
        resp.getWriter().write(objectMapper.writeValueAsString(usuarioToMap(usuario)));
    }

    private Map<String, Object> usuarioToMap(Usuario u) {
        Map<String, Object> map = new HashMap<>();
        map.put("idUsuario", u.getIdUsuario());
        map.put("nombre", u.getNombre());
        map.put("apellido", u.getApellido());
        map.put("email", u.getEmail());
        map.put("cargo", u.getCargo());
        map.put("sector", u.getSector() == null ? null : u.getSector().toString());
        map.put("tipoEstado", u.getTipoEstado() == null ? null : u.getTipoEstado().toString());
        map.put("rolSistema", u.getRolSistema() == null ? "USUARIO" : u.getRolSistema().toString());
        map.put("fechaCreacion", u.getFechaCreacion());
        return map;
    }

    private Usuario requireAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            resp.setStatus(401);
            resp.getWriter().write("{\"message\":\"Token requerido\"}");
            return null;
        }
        Usuario usuario = authService.getUserFromToken(header.substring(7));
        if (usuario == null || usuario.getRolSistema() != RolSistema.ADMIN_SISTEMA) {
            resp.setStatus(403);
            resp.getWriter().write("{\"message\":\"Acceso denegado\"}");
            return null;
        }
        return usuario;
    }
}
