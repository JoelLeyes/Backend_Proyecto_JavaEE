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
		email = email.trim();
		if (password == null || password.isBlank()) {
			throw new IllegalArgumentException("Password is required");
		}
		validatePasswordStrength(password);
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

	private void validatePasswordStrength(String password) {
		if (password.length() < 8)
			throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
		if (password.chars().noneMatch(Character::isUpperCase))
			throw new IllegalArgumentException("La contraseña debe contener al menos una letra mayúscula");
		if (password.chars().noneMatch(Character::isDigit))
			throw new IllegalArgumentException("La contraseña debe contener al menos un número");
		if (password.chars().allMatch(c -> Character.isLetterOrDigit(c)))
			throw new IllegalArgumentException("La contraseña debe contener al menos un carácter especial");
	}

	public String login(String email, String password) throws Exception {
		if (email == null || email.isBlank() || password == null) {
			throw new Exception("Invalid credentials");
		}
		email = email.trim();

		Usuario user = userDAO.findByEmail(email);
		if (user != null) {
			if (!BCrypt.verifyer().verify(password.toCharArray(), user.getPasswordHash()).verified) {
				throw new Exception("Invalid credentials");
			}
			return JwtUtil.generateToken(user.getIdUsuario());
		}

		// Compatibilidad: cuentas viejas guardadas en la tabla legacy `users`.
		UserDAO.LegacyUser legacy = userDAO.findLegacyByEmail(email);
		if (legacy == null || legacy.getPasswordHash() == null) {
			throw new Exception("Invalid credentials");
		}
		if (!BCrypt.verifyer().verify(password.toCharArray(), legacy.getPasswordHash()).verified) {
			throw new Exception("Invalid credentials");
		}

		// Migración automática a la tabla nueva `usuarios` para que el resto del sistema funcione.
		Usuario migrated = migrateLegacyUser(legacy);
		return JwtUtil.generateToken(migrated.getIdUsuario());
	}

	private Usuario migrateLegacyUser(UserDAO.LegacyUser legacy) {
		// Si otro request ya migró, devolvemos el existente.
		Usuario existing = userDAO.findByEmail(legacy.getEmail());
		if (existing != null) {
			return existing;
		}

		String fullName = legacy.getName() == null ? "" : legacy.getName().trim();
		String nombre = fullName;
		String apellido = "";
		if (!fullName.isBlank()) {
			int space = fullName.indexOf(' ');
			if (space > 0) {
				nombre = fullName.substring(0, space).trim();
				apellido = fullName.substring(space + 1).trim();
			}
		}

		Usuario u = new Usuario();
		u.setNombre(nombre.isBlank() ? "" : nombre);
		u.setApellido(apellido);
		u.setEmail(legacy.getEmail());
		u.setPasswordHash(legacy.getPasswordHash());
		u.setPasswordSalt("BCRYPT");
		u.setCargo(legacy.getRole() == null ? "" : legacy.getRole());
		u.setFotoPerfilUrl(null);

		Sector sector = Sector.SIN_ASIGNAR;
		String legacySector = legacy.getSector();
		if (legacySector != null && !legacySector.isBlank()) {
			String normalized = legacySector.trim();
			if ("SIN_SECTOR".equalsIgnoreCase(normalized)) {
				normalized = "SIN_ASIGNAR";
			}
			try {
				sector = Sector.valueOf(normalized.toUpperCase());
			} catch (Exception ignored) {
				sector = Sector.SIN_ASIGNAR;
			}
		}
		u.setSector(sector);
		u.setTipoEstado(TipoEstado.DESCONECTADO);
		u.setFechaCreacion(legacy.getCreatedAt() == null ? new Date() : legacy.getCreatedAt());

		userDAO.save(u);
		return u;
	}

	public Usuario getUserFromToken(String token) {
		Long userId = JwtUtil.getUserIdFromToken(token);
		return userDAO.findById(userId);
	}
}