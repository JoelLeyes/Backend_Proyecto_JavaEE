import java.util.HashMap;
import java.util.Map;

public class UsuarioDAO {
    private final Map<String, Usuario> usuariosPorEmail = new HashMap<>();
    private int siguienteId = 1;

    public synchronized int generarId() {
        return siguienteId++;
    }

    public synchronized boolean existePorEmail(String email) {
        return usuariosPorEmail.containsKey(normalizarEmail(email));
    }

    public synchronized Usuario guardar(Usuario usuario) {
        usuariosPorEmail.put(normalizarEmail(usuario.getEmail()), usuario);
        return usuario;
    }

    public synchronized Usuario buscarPorEmail(String email) {
        return usuariosPorEmail.get(normalizarEmail(email));
    }

    private String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}

