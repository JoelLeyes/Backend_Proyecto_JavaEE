import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class RegistroUsuarioService {
    private static final int PBKDF2_ITERACIONES = 65_536;
    private static final int PBKDF2_KEY_LENGTH = 256;

    private final UsuarioDAO usuarioDAO;

    public RegistroUsuarioService(UsuarioDAO usuarioDAO) {
        if (usuarioDAO == null) {
            throw new IllegalArgumentException("El dao es obligatorio");
        }
        this.usuarioDAO = usuarioDAO;
    }

    public Usuario registrarUsuarioNormal(String nombre, String apellido, String email,
                                          String passwordPlano, String fotoPerfilUrl) {
        validarTextoObligatorio(nombre, "nombre");
        validarTextoObligatorio(apellido, "apellido");
        validarEmail(email);
        validarPassword(passwordPlano);

        String emailNormalizado = email.trim().toLowerCase();
        if (usuarioDAO.existePorEmail(emailNormalizado)) {
            throw new IllegalArgumentException("Ya existe una cuenta para ese email");
        }

        PasswordProtegida passwordProtegida = generarPasswordProtegida(passwordPlano);
        Usuario nuevoUsuario = Usuario.crearParaRegistro(
                usuarioDAO.generarId(),
                nombre.trim(),
                apellido.trim(),
                emailNormalizado,
                passwordProtegida.hash,
                passwordProtegida.salt,
                limpiarOpcional(fotoPerfilUrl)
        );
        return usuarioDAO.guardar(nuevoUsuario);
    }

    public void asignarCargoYSectorComoAdmin(Usuario admin, Usuario usuarioObjetivo, String cargo, Sector sector) {
        validarAdmin(admin);
        if (usuarioObjetivo == null) {
            throw new IllegalArgumentException("El usuario objetivo es obligatorio");
        }
        validarTextoObligatorio(cargo, "cargo");
        usuarioObjetivo.asignarCargoYSector(cargo.trim(), sector);
    }

    public Participa asignarRolEnChatComoAdmin(Usuario admin, Usuario usuarioObjetivo, Chat chat, RolUsuario rol) {
        validarAdmin(admin);
        if (usuarioObjetivo == null) {
            throw new IllegalArgumentException("El usuario objetivo es obligatorio");
        }
        if (chat == null) {
            throw new IllegalArgumentException("El chat es obligatorio");
        }

        RolUsuario rolSeguro = (rol == null) ? RolUsuario.MIEMBRO : rol;
        Participa participacion = new Participa(new Date(), rolSeguro, usuarioObjetivo, chat);
        usuarioObjetivo.getParticipaciones().add(participacion);
        chat.getParticipaciones().add(participacion);
        return participacion;
    }

    private void validarAdmin(Usuario admin) {
        if (admin == null) {
            throw new IllegalArgumentException("Solo un admin puede ejecutar esta accion");
        }
    }

    private void validarTextoObligatorio(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("El campo " + campo + " es obligatorio");
        }
    }

    private void validarEmaGet-ChildItem -Path .\main -Recurse -Filter *.class | Remove-Itemzado.indexOf('@') < normalizado.length() - 1;
        if (!formatoBasicoValido) {
            throw new IllegalArgumentException("Formato de email invalido");
        }
    }

    private void validarPassword(String passwordPlano) {
        if (passwordPlano == null || passwordPlano.length() < 8) {
            throw new IllegalArgumentException("La password debe tener al menos 8 caracteres");
        }
    }

    private String limpiarOpcional(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private PasswordProtegida generarPasswordProtegida(String passwordPlano) {
        try {
            byte[] saltBytes = new byte[16];
            SecureRandom.getInstanceStrong().nextBytes(saltBytes);

            PBEKeySpec spec = new PBEKeySpec(passwordPlano.toCharArray(), saltBytes,
                    PBKDF2_ITERACIONES, PBKDF2_KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hashBytes = skf.generateSecret(spec).getEncoded();

            String salt = Base64.getEncoder().encodeToString(saltBytes);
            String hash = Base64.getEncoder().encodeToString(hashBytes);
            return new PasswordProtegida(hash, salt);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("No se pudo proteger la password", e);
        }
    }

    private static final class PasswordProtegida {
        private final String hash;
        private final String salt;

        private PasswordProtegida(String hash, String salt) {
            this.hash = hash;
            this.salt = salt;
        }
    }
}

