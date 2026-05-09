package com.nexolab.service;

import com.nexolab.dao.UserDAO;
import com.nexolab.model.Sector;
import com.nexolab.model.Usuario;
import com.nexolab.util.JwtUtil;
import at.favre.lib.crypto.bcrypt.BCrypt;

import java.util.Date;
import java.util.UUID;

public class AuthService {
    private final UserDAO userDAO = new UserDAO();
    public Usuario register(String nombre, String apellido, String email, String password, String fotoPerfilUrl) throws Exception {
        if (userDAO.findByEmail(email) != null) {
            throw new Exception("Email already exists");
        }
        String passwordSalt = UUID.randomUUID().toString().replace("-", "");
        String passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray());

        Usuario user = new Usuario(nombre, apellido, email, passwordHash, passwordSalt,
                null,              // cargo (admin lo asigna después)
                fotoPerfilUrl,     // opcional
                Sector.SIN_ASIGNAR,
                null,              // tipoEstado -> constructor lo pone DESCONECTADO
                null               // fechaCreacion -> constructor pone new Date()
        );
        userDAO.save(user);
        return user;
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