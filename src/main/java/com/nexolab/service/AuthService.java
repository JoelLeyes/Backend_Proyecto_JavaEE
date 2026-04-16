package com.nexolab.service;

import com.nexolab.dao.UserDAO;
import com.nexolab.model.Sector;
import com.nexolab.model.Usuario;
import com.nexolab.util.JwtUtil;
import at.favre.lib.crypto.bcrypt.BCrypt;

public class AuthService {
	private UserDAO userDAO = new UserDAO();

	public Usuario register(String name, String email, String password) throws Exception {
		if (userDAO.findByEmail(email) != null) {
			throw new Exception("Email already exists");
		}
		Usuario user = new Usuario();
		user.setName(name);
		user.setEmail(email);
		user.setPasswordHash(BCrypt.withDefaults().hashToString(12, password.toCharArray()));
		user.setSector(Sector.SIN_SECTOR);
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