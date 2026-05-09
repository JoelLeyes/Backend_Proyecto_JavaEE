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
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            resp.setStatus(404);
            return;
        }

        // Obtener nombre del archivo (ej: "abc123_foto.jpg")
        String nombreArchivo = pathInfo.substring(1);
        
        // Usar la misma ruta que FileStorageUtil: carpeta "uploads" con ruta absoluta
        String rutaCarpeta = new java.io.File("uploads").getAbsolutePath();
        File archivo = new File(rutaCarpeta + File.separator + nombreArchivo);

        // Validar que existe, es un archivo y está dentro de la carpeta uploads (seguridad)
        if (!archivo.exists() || !archivo.isFile() || !archivo.getAbsolutePath().startsWith(rutaCarpeta)) {
            resp.setStatus(404);
            return;
        }

        // Detectar tipo MIME del archivo
        String tipoMime = getServletContext().getMimeType(archivo.getName());
        if (tipoMime == null) {
            tipoMime = "application/octet-stream";
        }

        // Configurar respuesta
        resp.setContentType(tipoMime);
        resp.setContentLength((int) archivo.length());
        resp.setHeader("Content-Disposition", "inline; filename=\"" + archivo.getName() + "\"");

        // Enviar archivo al cliente
        try (FileInputStream fis = new FileInputStream(archivo);
             OutputStream out = resp.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesLeidos;
            while ((bytesLeidos = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesLeidos);
            }
            out.flush();
        }
    }
}
