package com.nexolab.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Servlet que sirve los archivos guardados en la carpeta "uploads"
 * Ej: GET /uploads/abc123_foto.jpg
 */
@WebServlet("/uploads/*")
public class FileDownloadServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || pathInfo.length() <= 1) {
                resp.setStatus(404);
                resp.getWriter().write("{\"error\":\"Archivo no encontrado\"}");
                return;
            }

            // Obtener nombre del archivo (ej: "abc123_foto.jpg")
            String nombreArchivo = pathInfo.substring(1);
            
            // Validar que no contiene path traversal
            if (nombreArchivo.contains("..") || nombreArchivo.contains("/") || nombreArchivo.contains("\\")) {
                resp.setStatus(400);
                resp.getWriter().write("{\"error\":\"Nombre de archivo inválido\"}");
                return;
            }
            
            // Usar la misma ruta que FileStorageUtil: carpeta "uploads" con ruta absoluta
            String rutaCarpeta = new java.io.File("uploads").getAbsolutePath();
            File archivo = new File(rutaCarpeta + File.separator + nombreArchivo);

            // Validar que existe, es un archivo y está dentro de la carpeta uploads (seguridad)
            if (!archivo.exists() || !archivo.isFile() || !archivo.getAbsolutePath().startsWith(rutaCarpeta)) {
                resp.setStatus(404);
                resp.getWriter().write("{\"error\":\"Archivo no encontrado\"}");
                return;
            }

            // Detectar tipo MIME del archivo
            String tipoMime = getServletContext().getMimeType(archivo.getName());
            if (tipoMime == null) {
                tipoMime = "application/octet-stream";
            }

            // Configurar respuesta
            resp.setContentType(tipoMime);
            resp.setContentLengthLong(archivo.length()); // Usar setContentLengthLong en lugar de setContentLength
            resp.setHeader("Content-Disposition", "attachment; filename=\"" + archivo.getName() + "\"");
            resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            resp.setHeader("Pragma", "no-cache");
            resp.setHeader("Expires", "0");

            // Enviar archivo al cliente
            try (FileInputStream fis = new FileInputStream(archivo);
                 OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesLeidos;
                while ((bytesLeidos = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesLeidos);
                }
                out.flush();
            }
        } catch (IOException e) {
            // Loguear el error pero no permitir que caiga la aplicación
            System.err.println("Error al descargar archivo: " + e.getMessage());
            e.printStackTrace();
            try {
                resp.setStatus(500);
                resp.setContentType("application/json");
                resp.getWriter().write("{\"error\":\"Error al descargar el archivo\"}");
            } catch (IOException ignored) {
                // El cliente ya desconectó, ignorar
            }
        } catch (Exception e) {
            System.err.println("Error inesperado en FileDownloadServlet: " + e.getMessage());
            e.printStackTrace();
            try {
                resp.setStatus(500);
            } catch (Exception ignored) {
                // El cliente ya desconectó
            }
        }
    }
}
