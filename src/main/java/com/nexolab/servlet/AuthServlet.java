package com.nexolab.servlet;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.nexolab.dao.UserDAO;
import com.nexolab.service.AuthService;
import com.nexolab.service.EmailService;
import com.nexolab.model.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/auth/*")
public class AuthServlet extends HttpServlet {
	private AuthService authService = new AuthService();
	private UserDAO userDAO = new UserDAO();
	private EmailService emailService = new EmailService();
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

			// Enviar email de bienvenida si SMTP está configurado
			EmailService.SmtpConfig cfg = emailService.loadFromEnv();
			if (cfg != null && email != null) {
				String nombreCompleto = ((nombre == null ? "" : nombre) + " " + (apellido == null ? "" : apellido)).trim();
				String subject = "Bienvenido a NexoLab";
				String msg = "Hola " + nombreCompleto + ",\n\n" +
						"Tu cuenta en NexoLab fue creada exitosamente.\n\n" +
						"Podés iniciar sesión con tu email: " + email + "\n\n" +
						"Si tenés algún problema para acceder, contactá al administrador del sistema.\n\n" +
						"— Equipo NexoLab";
				try {
					emailService.sendTextEmail(cfg, email, subject, msg);
				} catch (Exception mailEx) {
					// El registro fue exitoso; solo falló el envío del email
					System.err.println("[EmailService] No se pudo enviar email de bienvenida a " + email + ": " + mailEx.getMessage());
				}
			}

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
		Map<String, String> body;
		try {
			body = objectMapper.readValue(req.getInputStream(), Map.class);
		} catch (Exception e) {
			resp.setStatus(400);
			resp.getWriter().write("{\"message\":\"Cuerpo JSON inválido\"}");
			return;
		}

		String email = body.get("email");
		if (email == null || email.isBlank()) {
			resp.setStatus(400);
			resp.getWriter().write("{\"message\":\"Email es requerido\"}");
			return;
		}

		EmailService.SmtpConfig cfg = emailService.loadFromEnv();
		if (cfg == null) {
			resp.setStatus(500);
			resp.getWriter().write("{\"message\":\"Recuperación de contraseña no configurada en el servidor\"}");
			return;
		}

		// Respuesta genérica: no filtramos si el email existe o no.
		Map<String, Object> response = new HashMap<>();
		response.put("message", "Si el correo existe, vas a recibir instrucciones para recuperar el acceso.");

		Usuario user = userDAO.findByEmail(email);
		if (user != null) {
			String tempPassword = generateTempPassword();
			String subject = "NexoLab - Recuperación de contraseña";
			String msg = "Hola,\n\n" +
					"Se generó una contraseña temporal para tu cuenta.\n\n" +
					"Contraseña temporal: " + tempPassword + "\n\n" +
					"Ingresá con esta contraseña y cambiala desde tu perfil.\n\n" +
					"Si no solicitaste esto, podés ignorar este correo.";

			try {
				// Primero enviamos; si falla SMTP, no tocamos el password (evita lockout).
				emailService.sendTextEmail(cfg, email, subject, msg);
				user.setPasswordHash(BCrypt.withDefaults().hashToString(12, tempPassword.toCharArray()));
				userDAO.update(user);
			} catch (Exception e) {
				resp.setStatus(500);
				resp.getWriter().write("{\"message\":\"No se pudo enviar el correo de recuperación\"}");
				return;
			}
		}

		resp.getWriter().write(objectMapper.writeValueAsString(response));
	}

	private static String generateTempPassword() {
		final String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		final String lower = "abcdefghijklmnopqrstuvwxyz";
		final String digits = "0123456789";
		final String special = "!@#$%&*_-+";
		final String all = upper + lower + digits + special;

		SecureRandom rnd = new SecureRandom();
		StringBuilder sb = new StringBuilder();
		sb.append(upper.charAt(rnd.nextInt(upper.length())));
		sb.append(lower.charAt(rnd.nextInt(lower.length())));
		sb.append(digits.charAt(rnd.nextInt(digits.length())));
		sb.append(special.charAt(rnd.nextInt(special.length())));
		for (int i = 0; i < 8; i++) {
			sb.append(all.charAt(rnd.nextInt(all.length())));
		}

		// Mezcla simple
		char[] chars = sb.toString().toCharArray();
		for (int i = chars.length - 1; i > 0; i--) {
			int j = rnd.nextInt(i + 1);
			char tmp = chars[i];
			chars[i] = chars[j];
			chars[j] = tmp;
		}
		return new String(chars);
	}
}