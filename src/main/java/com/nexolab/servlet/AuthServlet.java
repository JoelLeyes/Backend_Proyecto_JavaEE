package com.nexolab.servlet;

import com.nexolab.service.AuthService;
import com.nexolab.model.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/auth/*")
public class AuthServlet extends HttpServlet {
	private AuthService authService = new AuthService();
	private ObjectMapper objectMapper = new ObjectMapper();

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String path = req.getPathInfo();
		resp.setContentType("application/json");

		if ("/register".equals(path)) {
			handleRegister(req, resp);
		} else if ("/login".equals(path)) {
			handleLogin(req, resp);
		} else if ("/forgot-password".equals(path)) {
			handleForgotPassword(req, resp);
		} else {
			resp.setStatus(404);
		}
	}

	private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			Map<String, String> body = objectMapper.readValue(req.getInputStream(), Map.class);

			String nombre = body.get("nombre");
			String apellido = body.get("apellido");
			String cargo = body.get("cargo");
			String email = body.get("email");
			String password = body.get("password");

			// Compatibilidad con frontend viejo
			if (nombre == null || nombre.isBlank()) {
				nombre = body.get("name");
			}

			authService.register(nombre, apellido, email, password, cargo);

			Map<String, Object> response = new HashMap<>();
			response.put("message", "User registered successfully");
			resp.getWriter().write(objectMapper.writeValueAsString(response));
		} catch (Exception e) {
			resp.setStatus(400);
			Map<String, String> error = new HashMap<>();
			error.put("message", e.getMessage());
			resp.getWriter().write(objectMapper.writeValueAsString(error));
		}
	}

	private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			Map<String, String> body = objectMapper.readValue(req.getInputStream(), Map.class);
			String token = authService.login(body.get("email"), body.get("password"));
			Usuario user = authService.getUserFromToken(token);

			Map<String, Object> response = new HashMap<>();
			response.put("token", token);

			Map<String, Object> userMap = new HashMap<>();
			userMap.put("idUsuario", user.getIdUsuario());
			userMap.put("nombre", user.getNombre());
			userMap.put("apellido", user.getApellido());
			userMap.put("email", user.getEmail());
			userMap.put("cargo", user.getCargo());
			userMap.put("sector", user.getSector() == null ? null : user.getSector().toString());
			userMap.put("tipoEstado", user.getTipoEstado() == null ? null : user.getTipoEstado().toString());
			userMap.put("rolSistema", user.getRolSistema() == null ? "USUARIO" : user.getRolSistema().toString());
			response.put("user", userMap);

			resp.getWriter().write(objectMapper.writeValueAsString(response));
		} catch (Exception e) {
			resp.setStatus(401);
			Map<String, String> error = new HashMap<>();
			error.put("message", e.getMessage());
			resp.getWriter().write(objectMapper.writeValueAsString(error));
		}
	}

	private void handleForgotPassword(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			Map<String, String> body = objectMapper.readValue(req.getInputStream(), Map.class);
			String email = body.get("email");

			if (email == null || email.isBlank()) {
				throw new IllegalArgumentException("Email is required");
			}

			Map<String, Object> response = new HashMap<>();
			response.put("message", "Password reset request received for " + email);
			resp.getWriter().write(objectMapper.writeValueAsString(response));
		} catch (Exception e) {
			resp.setStatus(400);
			Map<String, String> error = new HashMap<>();
			error.put("message", e.getMessage());
			resp.getWriter().write(objectMapper.writeValueAsString(error));
		}
	}
}