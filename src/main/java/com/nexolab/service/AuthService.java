package com.nexolab.service;

import com.nexolab.dao.UserDAO;
import com.nexolab.model.Usuario;
import com.nexolab.util.JwtUtil;
import at.favre.lib.crypto.bcrypt.BCrypt;

public class AuthService {
	private UserDAO userDAO = new UserDAO();

	public Usuario register(String name, String email, String password) throws Exception {
		if (userDAO.findByEmail(email) != null) {
			throw new Exception("Email already exists");
		}

		// Validar contraseña
		if (password.length() < 8) {
			throw new Exception("La contraseña debe tener al menos 8 caracteres");
		}
		if (password.matches("^[a-zA-Z]+$")) {
			throw new Exception(
					"La contraseña no puede contener solo letras. Debe incluir números o caracteres especiales");
		}
		if (password.matches("^[0-9]+$")) {
			throw new Exception(
					"La contraseña no puede contener solo números. Debe incluir letras o caracteres especiales");
		}

		Usuario user = new Usuario();
		user.setName(name);
		user.setEmail(email);
		user.setPasswordHash(BCrypt.withDefaults().hashToString(12, password.toCharArray()));
		userDAO.save(user);
		return user;
	}

	public String login(String email, String password) throws Exception {
		Usuario user = userDAO.findByEmail(email);
		if (user == null || !BCrypt.verifyer().verify(password.toCharArray(), user.getPasswordHash()).verified) {
			throw new Exception("Invalid credentials");
		}
		return JwtUtil.generateToken(user.getId());
	}

	public Usuario getUserFromToken(String token) {
		Long userId = JwtUtil.getUserIdFromToken(token);
		return userDAO.findById(userId);
	}
}