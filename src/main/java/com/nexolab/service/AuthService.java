package com.nexolab.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.nexolab.dao.UserDAO;
import com.nexolab.model.Sector;
import com.nexolab.model.TipoEstado;
import com.nexolab.model.Usuario;
import com.nexolab.util.JwtUtil;

import java.util.Date;

public class AuthService {
	private final UserDAO userDAO = new UserDAO();

	/**
	 * Registro "nuevo": soporta los campos del modelo actual.
	 */
	public Usuario register(String nombre, String apellido, String email, String password, String cargo) throws Exception {
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("Email is required");
		}
		if (password == null || password.isBlank()) {
			throw new IllegalArgumentException("Password is required");
		}
		if (userDAO.findByEmail(email) != null) {
			throw new Exception("Email already exists");
		}

		Usuario user = new Usuario();
		user.setNombre(nombre == null ? "" : nombre);
		user.setApellido(apellido == null ? "" : apellido);
		user.setEmail(email);
		user.setPasswordHash(BCrypt.withDefaults().hashToString(12, password.toCharArray()));
		// El hash bcrypt ya incluye el salt. Este campo existe en el modelo y es NOT NULL, así que lo dejamos no-nulo.
		user.setPasswordSalt("BCRYPT");
		user.setCargo(cargo == null ? "" : cargo);
		user.setFotoPerfilUrl(null);
		user.setSector(Sector.SIN_ASIGNAR);
		user.setTipoEstado(TipoEstado.DESCONECTADO);
		user.setFechaCreacion(new Date());

		userDAO.save(user);
		return user;
	}

	/**
	 * Compatibilidad con el frontend viejo: name/email/password.
	 */
	public Usuario register(String name, String email, String password) throws Exception {
		String nombre = name;
		String apellido = "";
		if (name != null) {
			String trimmed = name.trim();
			int space = trimmed.indexOf(' ');
			if (space > 0) {
				nombre = trimmed.substring(0, space);
				apellido = trimmed.substring(space + 1).trim();
			}
		}
		return register(nombre, apellido, email, password, "");
	}

	public String login(String email, String password) throws Exception {
		Usuario user = userDAO.findByEmail(email);
		if (user == null || !BCrypt.verifyer().verify(password.toCharArray(), user.getPasswordHash()).verified) {
			throw new Exception("Invalid credentials");
		}
		return JwtUtil.generateToken(user.getIdUsuario());
	}

	public Usuario getUserFromToken(String token) {
		Long userId = JwtUtil.getUserIdFromToken(token);
		return userDAO.findById(userId);
	}
}