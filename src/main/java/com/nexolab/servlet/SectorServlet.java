package com.nexolab.servlet;

import com.nexolab.service.AuthService;
import com.nexolab.dao.SectorDAO;
import com.nexolab.model.Sector;
import com.nexolab.model.Usuario;
import com.nexolab.model.RolUsuario;
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
import java.util.stream.Collectors;

@WebServlet("/api/sectors/*")
public class SectorServlet extends HttpServlet {
	private AuthService authService = new AuthService();
	private SectorDAO sectorDAO = new SectorDAO();
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

		resp.setContentType("application/json");

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

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Usuario admin = verifyAdminAccess(req, resp);
		if (admin == null)
			return;

		resp.setContentType("application/json");

		try {
			Map<String, String> body = objectMapper.readValue(req.getInputStream(),
					new TypeReference<Map<String, String>>() {
					});
			String name = body.get("name");

			if (name == null || name.trim().isEmpty()) {
				resp.setStatus(400);
				resp.getWriter().write("{\"error\": \"El nombre es requerido\"}");
				return;
			}

			// Verificar si ya existe
			if (sectorDAO.findByName(name) != null) {
				resp.setStatus(400);
				resp.getWriter().write("{\"error\": \"El sector ya existe\"}");
				return;
			}

			Sector sector = new Sector(name);
			sectorDAO.save(sector);

			Map<String, Object> response = new HashMap<>();
			response.put("message", "Sector creado");
			response.put("name", sector.getName());
			resp.setStatus(201);
			resp.getWriter().write(objectMapper.writeValueAsString(response));
		} catch (Exception e) {
			resp.setStatus(400);
			resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
		}
	}
}
