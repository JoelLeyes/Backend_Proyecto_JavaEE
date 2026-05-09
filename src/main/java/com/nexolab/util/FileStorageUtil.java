package com.nexolab.util;

import jakarta.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Utilidad para guardar archivos en disco
 */
public class FileStorageUtil {
    private static final String CARPETA_SUBIDAS = "uploads";
    private static final long TAMAÑO_MAXIMO = 5 * 1024 * 1024; // 5 MB

    // Extensiones permitidas: documentos e imágenes
    private static final Set<String> EXTENSIONES_PERMITIDAS = new HashSet<>(Arrays.asList(
            "pdf", "doc", "docx", "xls", "xlsx",        // Documentos
            "jpg", "jpeg", "png", "gif"                  // Imágenes
    ));

    /**
     * Valida que el archivo cumpla con tamaño y tipo
     * @param parte El archivo a validar
     * @throws IOException Si el archivo no es válido
     */
    private static void validarArchivo(Part parte) throws IOException {
        // Validar tamaño (máximo 5 MB)
        if (parte.getSize() > TAMAÑO_MAXIMO) {
            throw new IOException("El archivo excede el tamaño máximo de 5 MB");
        }

        // Validar extensión
        String nombreOriginal = parte.getSubmittedFileName();
        if (nombreOriginal == null || !nombreOriginal.contains(".")) {
            throw new IOException("Archivo sin extensión válida");
        }

        String extension = nombreOriginal.substring(nombreOriginal.lastIndexOf(".") + 1).toLowerCase();
        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new IOException("Tipo de archivo no permitido. Solo: documentos (.pdf, .doc, .docx, .xls, .xlsx) e imágenes (.jpg, .png, .gif)");
        }
    }
    /**
     * Guarda un archivo en disco y devuelve su URL
     * @param parte El archivo enviado desde el cliente
     * @return La URL pública del archivo (ej: /uploads/uuid_nombre.pdf)
     * @throws IOException Si el archivo no es válido o falla al guardar
     */
    public static String guardarArchivo(Part parte) throws IOException {
        // Validar archivo antes de guardarlo
        validarArchivo(parte);

        // Crear carpeta "uploads" si no existe
        String rutaCarpeta = new File(CARPETA_SUBIDAS).getAbsolutePath();
        File carpeta = new File(rutaCarpeta);
        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }

        // Generar nombre único (UUID + nombre original)
        String nombreOriginal = parte.getSubmittedFileName();
        String nombreUnico = UUID.randomUUID() + "_" + nombreOriginal;
        String rutaCompleta = rutaCarpeta + File.separator + nombreUnico;

        // Guardar el archivo en disco
        parte.write(rutaCompleta);

        // Devolver URL pública (ej: /uploads/uuid_nombre.pdf)
        return "/uploads/" + nombreUnico;
    }
}
