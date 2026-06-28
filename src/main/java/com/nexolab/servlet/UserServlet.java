package com.nexolab.servlet;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexolab.dao.UserDAO;
import com.nexolab.model.TipoEstado;
import com.nexolab.model.Usuario;
import com.nexolab.service.AuthService;
import com.nexolab.util.FileStorageUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet("/usuarios/*")
@MultipartConfig
public class UserServlet extends HttpServlet {

    private final AuthService  authService  = new AuthService();
    private final UserDAO      userDAO      = new UserDAO();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void init() throws ServletException {
        String envDir = System.getenv("UPLOAD_DIR");
        if (envDir != null && !envDir.isBlank()) {
            FileStorageUtil.setUploadDir(envDir.trim());
        } else {
            String realPath = getServletContext().getRealPath("/uploads");
            if (realPath != null) FileStorageUtil.setUploadDir(realPath);
        }
        String envPrefix = System.getenv("UPLOAD_URL_PREFIX");
        if (envPrefix != null && !envPrefix.isBlank()) {
            FileStorageUtil.setUrlPrefix(envPrefix.trim());
        } else {
            String contextPath = getServletContext().getContextPath();
            FileStorageUtil.setUrlPrefix((contextPath.isEmpty() ? "/api" : contextPath) + "/uploads/");
        }
    }

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

        // búsqueda: GET /usuarios?q=texto  (path es null o "/")
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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");
        Usuario current = requireUser(req, resp);
        if (current == null) return;

        String path = req.getPathInfo();
        if ("/me/foto".equals(path)) {
            handleSubirFoto(req, resp, current);
        } else {
            resp.setStatus(404);
        }
    }

    private static final long MAX_FOTO_BYTES = 2 * 1024 * 1024; // 2 MB

    private void handleSubirFoto(HttpServletRequest req, HttpServletResponse resp, Usuario usuario)
            throws IOException, ServletException {
        Part foto;
        try {
            foto = req.getPart("foto");
        } catch (Exception e) {
            resp.setStatus(400);
            resp.getWriter().write("{\"message\":\"Error al leer el archivo\"}");
            return;
        }

        if (foto == null || foto.getSize() == 0) {
            resp.setStatus(400);
            resp.getWriter().write("{\"message\":\"No se recibió ninguna foto\"}");
            return;
        }

        String contentType = foto.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            resp.setStatus(400);
            resp.getWriter().write("{\"message\":\"Solo se permiten imágenes (JPG, PNG, GIF)\"}");
            return;
        }

        if (foto.getSize() > MAX_FOTO_BYTES) {
            resp.setStatus(400);
            resp.getWriter().write("{\"message\":\"La imagen no puede superar los 2 MB\"}");
            return;
        }

        try {
            byte[] bytes = foto.getInputStream().readAllBytes();
            String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
            String dataUrl = "data:" + contentType + ";base64," + base64;
            usuario.setFotoPerfilUrl(dataUrl);
            userDAO.update(usuario);
            resp.getWriter().write(objectMapper.writeValueAsString(usuarioToMap(usuario)));
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"message\":\"Error al procesar la imagen\"}");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("application/json");

        Usuario current = requireUser(req, resp);
        if (current == null) return;

        String path = req.getPathInfo();

        if ("/me/password".equals(path)) {
            handleCambiarPassword(req, resp, current);
            return;
        }

        if ("/me/push-token".equals(path)) {
            handleActualizarPushToken(req, resp, current);
            return;
        }

        if ("/me".equals(path)) {
            handleActualizarPerfil(req, resp, current);
            return;
        }

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

        userDAO.updateTipoEstado(current.getIdUsuario(), nuevoEstado);
        current.setTipoEstado(nuevoEstado);

        resp.getWriter().write(objectMapper.writeValueAsString(usuarioToMap(current)));
    }

    private void handleActualizarPerfil(HttpServletRequest req, HttpServletResponse resp, Usuario usuario)
            throws IOException {
        Map<String, Object> body;
        try {
            body = objectMapper.readValue(req.getInputStream(), new TypeReference<>() {});
        } catch (Exception e) {
            resp.setStatus(400);
            resp.getWriter().write("{\"message\":\"Cuerpo JSON inválido\"}");
            return;
        }

        String nombre   = body.containsKey("nombre")   ? body.get("nombre").toString().trim()   : null;
        String apellido = body.containsKey("apellido")  ? body.get("apellido").toString().trim()  : null;

        if (nombre == null || nombre.isEmpty()) {
            resp.setStatus(400);
            resp.getWriter().write("{\"message\":\"El nombre no puede estar vacío\"}");
            return;
        }

        usuario.setNombre(nombre);
        if (apellido != null) {
            usuario.setApellido(apellido);
        }
        userDAO.update(usuario);

        resp.getWriter().write(objectMapper.writeValueAsString(usuarioToMap(usuario)));
    }

    private void handleCambiarPassword(HttpServletRequest req, HttpServletResponse resp, Usuario usuario)
            throws IOException {
        Map<String, Object> body;
        try {
            body = objectMapper.readValue(req.getInputStream(), new TypeReference<>() {});
        } catch (Exception e) {
            resp.setStatus(400);
            resp.getWriter().write("{\"message\":\"Cuerpo JSON inválido\"}");
            return;
        }

        String passwordActual = body.containsKey("passwordActual") ? body.get("passwordActual").toString() : null;
        String passwordNueva  = body.containsKey("passwordNueva")  ? body.get("passwordNueva").toString()  : null;

        if (passwordActual == null || passwordNueva == null) {
            resp.setStatus(400);
            resp.getWriter().write("{\"message\":\"Se requieren passwordActual y passwordNueva\"}");
            return;
        }
        if (!BCrypt.verifyer().verify(passwordActual.toCharArray(), usuario.getPasswordHash()).verified) {
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

        usuario.setPasswordHash(BCrypt.withDefaults().hashToString(12, passwordNueva.toCharArray()));
        userDAO.update(usuario);
        resp.getWriter().write("{\"message\":\"Contraseña actualizada correctamente\"}");
    }

    private void handleActualizarPushToken(HttpServletRequest req, HttpServletResponse resp, Usuario usuario)
            throws IOException {
        Map<String, Object> body;
        try {
            body = objectMapper.readValue(req.getInputStream(), new TypeReference<>() {});
        } catch (Exception e) {
            resp.setStatus(400);
            resp.getWriter().write("{\"message\":\"Cuerpo JSON inválido\"}");
            return;
        }

        String pushToken = body.containsKey("pushToken") ? body.get("pushToken").toString().trim() : null;
        usuario.setPushToken(pushToken);
        userDAO.update(usuario);

        resp.getWriter().write(objectMapper.writeValueAsString(usuarioToMap(usuario)));
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

    private Map<String, Object> usuarioToMap(Usuario u) {
        Map<String, Object> map = new HashMap<>();
        map.put("idUsuario",     u.getIdUsuario());
        map.put("nombre",        u.getNombre());
        map.put("apellido",      u.getApellido());
        map.put("email",         u.getEmail());
        map.put("cargo",         u.getCargo());
        map.put("sector",        u.getSector()     == null ? null : u.getSector().toString());
        map.put("tipoEstado",    u.getTipoEstado() == null ? null : u.getTipoEstado().toString());
        map.put("rolSistema",    u.getRolSistema() == null ? "USUARIO" : u.getRolSistema().toString());
        map.put("pushToken",     u.getPushToken());
        map.put("fotoPerfilUrl", u.getFotoPerfilUrl());
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
