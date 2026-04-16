package com.nexolab.servlet;

import com.nexolab.service.AuthService;
import com.nexolab.service.ChatService;
import com.nexolab.dao.UserDAO;
import com.nexolab.dao.SectorDAO;
import com.nexolab.dao.MessageDAO;
import com.nexolab.model.Usuario;
import com.nexolab.model.RolUsuario;
import com.nexolab.model.Sector;
import com.nexolab.model.Mensaje;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.stream.Collectors;

@WebServlet("/api/admin/*")
public class AdminServlet extends HttpServlet {
	private AuthService authService = new AuthService();
	private UserDAO userDAO = new UserDAO();
	private SectorDAO sectorDAO = new SectorDAO();
	private MessageDAO messageDAO = new MessageDAO();
	private ChatService chatService = new ChatService();
	private ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Verifica si el usuario es administrador
	 */
	private Usuario verifyAdminAccess(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String token = req.getHeader("Authorization");
		if (token == null || !token.startsWith("Bearer ")) {
			resp.setStatus(401);
			return null;
		}
		token = token.substring(7);
		Usuario usuario = authService.getUserFromToken(token);
		if (usuario == null || usuario.getRole() != RolUsuario.ADMINISTRADOR) {
			resp.setStatus(403);
			return null;
		}
		return usuario;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Usuario admin = verifyAdminAccess(req, resp);
		if (admin == null)
			return;

		String path = req.getPathInfo();
		resp.setContentType("application/json");

		if ("/users".equals(path)) {
			getAllUsers(resp);
		} else if ("/stats".equals(path)) {
			getStats(resp);
		} else if (path != null && path.startsWith("/users/")) {
			getUserDetail(req, path, resp);
		} else if ("/sectors".equals(path)) {
			getSectors(resp);
		} else {
			resp.setStatus(404);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Usuario admin = verifyAdminAccess(req, resp);
		if (admin == null)
			return;

		String path = req.getPathInfo();
		resp.setContentType("application/json");

		if ("/users/setRole".equals(path)) {
			setUserRole(req, resp);
		} else if ("/users/setSector".equals(path)) {
			setUserSector(req, resp);
		} else if ("/users/delete".equals(path)) {
			deleteUser(req, resp);
		} else {
			resp.setStatus(404);
		}
	}

	/**
	 * GET /admin/users - Obtener lista de usuarios
	 */
	private void getAllUsers(HttpServletResponse resp) throws IOException {
		try {
			List<Usuario> users = userDAO.findAll();
			List<Map<String, Object>> usersList = users.stream().map(u -> {
				Map<String, Object> map = new HashMap<>();
				map.put("id", u.getId());
				map.put("name", u.getName());
				map.put("email", u.getEmail());
				map.put("role", u.getRole().toString());
				map.put("sector", u.getSector() != null ? u.getSector().getName() : null);
				map.put("createdAt", u.getCreatedAt());
				return map;
			}).collect(Collectors.toList());

			resp.getWriter().write(objectMapper.writeValueAsString(usersList));
		} catch (Exception e) {
			resp.setStatus(500);
			resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
		}
	}

	/**
	 * GET /admin/stats - Obtener estadísticas
	 */
	private void getStats(HttpServletResponse resp) throws IOException {
		try {
			List<Usuario> allUsers = userDAO.findAll();
			long adminCount = allUsers.stream().filter(u -> u.getRole() == RolUsuario.ADMINISTRADOR).count();
			long userCount = allUsers.stream().filter(u -> u.getRole() == RolUsuario.USER).count();

			Map<String, Object> stats = new HashMap<>();
			stats.put("totalUsers", allUsers.size());
			stats.put("admins", adminCount);
			stats.put("users", userCount);
			stats.put("totalChats", chatService.obtenerTodosChats().size());

			resp.getWriter().write(objectMapper.writeValueAsString(stats));
		} catch (Exception e) {
			resp.setStatus(500);
			resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
		}
	}

	/**
	 * POST /admin/users/setRole - Cambiar rol de usuario
	 */
	private void setUserRole(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			Map<String, String> body = objectMapper.readValue(req.getInputStream(),
					new TypeReference<Map<String, String>>() {
					});
			Long userId = Long.valueOf(body.get("userId"));
			String newRole = body.get("role").toUpperCase();

			Usuario user = userDAO.findById(userId);
			if (user == null) {
				resp.setStatus(404);
				resp.getWriter().write("{\"error\": \"Usuario no encontrado\"}");
				return;
			}

			user.setRole(RolUsuario.valueOf(newRole));
			userDAO.update(user);

			Map<String, Object> response = new HashMap<>();
			response.put("message", "Rol actualizado");
			response.put("userId", userId);
			response.put("newRole", newRole);
			resp.getWriter().write(objectMapper.writeValueAsString(response));
		} catch (Exception e) {
			resp.setStatus(400);
			resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
		}
	}

	/**
	 * POST /admin/users/delete - Eliminar usuario
	 */
	private void deleteUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			Map<String, String> body = objectMapper.readValue(req.getInputStream(),
					new TypeReference<Map<String, String>>() {
					});
			Long userId = Long.valueOf(body.get("userId"));

			Usuario user = userDAO.findById(userId);
			if (user == null) {
				resp.setStatus(404);
				resp.getWriter().write("{\"error\": \"Usuario no encontrado\"}");
				return;
			}

			userDAO.delete(userId);

			Map<String, Object> response = new HashMap<>();
			response.put("message", "Usuario eliminado");
			response.put("userId", userId);
			resp.getWriter().write(objectMapper.writeValueAsString(response));
		} catch (Exception e) {
			resp.setStatus(400);
			resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
		}
	}

	/**
	 * POST /admin/users/setSector - Cambiar departamento/sector de usuario
	 */
	private void setUserSector(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			Map<String, String> body = objectMapper.readValue(req.getInputStream(),
					new TypeReference<Map<String, String>>() {
					});
			Long userId = Long.valueOf(body.get("userId"));
			String sectorName = body.get("sectorName");

			Usuario user = userDAO.findById(userId);
			if (user == null) {
				resp.setStatus(404);
				resp.getWriter().write("{\"error\": \"Usuario no encontrado\"}");
				return;
			}

			Sector sector = sectorDAO.findByName(sectorName);
			if (sector == null) {
				resp.setStatus(404);
				resp.getWriter().write("{\"error\": \"Sector no encontrado\"}");
				return;
			}

			user.setSector(sector);
			userDAO.update(user);

			Map<String, Object> response = new HashMap<>();
			response.put("message", "Sector actualizado");
			response.put("userId", userId);
			response.put("newSector", sector.getName());
			resp.getWriter().write(objectMapper.writeValueAsString(response));
		} catch (Exception e) {
			resp.setStatus(400);
			resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
		}
	}

	/**
	 * GET /admin/users/{userId} - Ver detalles de usuario y sus mensajes
	 */
	private void getUserDetail(HttpServletRequest req, String path, HttpServletResponse resp) throws IOException {
		try {
			String[] parts = path.split("/");
			if (parts.length < 3) {
				resp.setStatus(404);
				resp.getWriter().write("{\"error\": \"Ruta inválida\"}");
				return;
			}

			Long userId = Long.valueOf(parts[2]);
			int limit = 50; // por defecto

			String limitParam = req.getParameter("limit");
			if (limitParam != null && !limitParam.isEmpty()) {
				try {
					limit = Integer.parseInt(limitParam);
				} catch (Exception e) {
					// usar valor por defecto
				}
			}

			// Apoyo también a la forma /admin/users/{userId}/{limit}
			if (parts.length > 3) {
				try {
					limit = Integer.valueOf(parts[3]);
				} catch (Exception e) {
					// usar valor por defecto
				}
			}

			Usuario user = userDAO.findById(userId);
			if (user == null) {
				resp.setStatus(404);
				resp.getWriter().write("{\"error\": \"Usuario no encontrado\"}");
				return;
			}

			// Obtener mensajes
			List<Mensaje> messages = messageDAO.findByUserLimit(user, limit);
			long totalMessages = messageDAO.countByUser(user);

			Map<String, Object> response = new HashMap<>();
			response.put("id", user.getId());
			response.put("name", user.getName());
			response.put("email", user.getEmail());
			response.put("role", user.getRole().toString());
			response.put("sector", user.getSector() != null ? user.getSector().getName() : null);
			response.put("createdAt", user.getCreatedAt());
			response.put("totalMessages", totalMessages);
			response.put("messagesShown", messages.size());
			response.put("messages", messages.stream().map(m -> {
				Map<String, Object> msgMap = new HashMap<>();
				msgMap.put("id", m.getId());
				msgMap.put("content", m.getContent());
				msgMap.put("timestamp", m.getTimestamp());
				msgMap.put("chatId", m.getConversacion() != null ? m.getConversacion().getId() : null);
				msgMap.put("read", m.getRead());
				return msgMap;
			}).collect(Collectors.toList()));

			resp.getWriter().write(objectMapper.writeValueAsString(response));
		} catch (Exception e) {
			resp.setStatus(400);
			resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
		}
	}

	/**
	 * GET /admin/sectors - Listar todos los sectores
	 */
	private void getSectors(HttpServletResponse resp) throws IOException {
		try {
			List<Sector> sectors = sectorDAO.findAll();
			List<Map<String, Object>> sectorsList = sectors.stream().map(s -> {
				Map<String, Object> map = new HashMap<>();
				map.put("name", s.getName());
				return map;
			}).collect(Collectors.toList());

			resp.getWriter().write(objectMapper.writeValueAsString(sectorsList));
		} catch (Exception e) {
			resp.setStatus(500);
			resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
		}
	}
}
