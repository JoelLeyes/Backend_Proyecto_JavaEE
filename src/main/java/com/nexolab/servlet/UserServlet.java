package com.nexolab.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexolab.dao.UserDAO;
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

@WebServlet("/usuarios")
public class UserServlet extends HttpServlet {

    private final AuthService authService = new AuthService();
    private final UserDAO userDAO = new UserDAO();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");

        String token = req.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) { resp.setStatus(401); return; }
        Usuario current = authService.getUserFromToken(token.substring(7));
        if (current == null) { resp.setStatus(401); return; }

        String q = req.getParameter("q");
        if (q == null || q.trim().length() < 2) {
            resp.getWriter().write("[]");
            return;
        }

        List<Usuario> usuarios = userDAO.findByQuery(q.trim(), current.getIdUsuario());

        List<Map<String, Object>> result = usuarios.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("idUsuario", u.getIdUsuario());
            map.put("nombre", u.getNombre());
            map.put("apellido", u.getApellido());
            map.put("email", u.getEmail());
            map.put("cargo", u.getCargo());
            map.put("sector", u.getSector() == null ? null : u.getSector().toString());
            return map;
        }).collect(Collectors.toList());

        resp.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
