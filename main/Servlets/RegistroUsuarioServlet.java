public class RegistroUsuarioServlet {
    private final RegistroUsuarioService registroUsuarioService;

    public RegistroUsuarioServlet(RegistroUsuarioService registroUsuarioService) {
        if (registroUsuarioService == null) {
            throw new IllegalArgumentException("El service es obligatorio");
        }
        this.registroUsuarioService = registroUsuarioService;
    }

    public Usuario registrarUsuarioNormal(String nombre, String apellido, String email,
                                          String passwordPlano, String fotoPerfilUrl) {
        return registroUsuarioService.registrarUsuarioNormal(nombre, apellido, email, passwordPlano, fotoPerfilUrl);
    }

    public void asignarCargoYSectorComoAdmin(Usuario admin, Usuario usuarioObjetivo, String cargo, Sector sector) {
        registroUsuarioService.asignarCargoYSectorComoAdmin(admin, usuarioObjetivo, cargo, sector);
    }

    public Participa asignarRolEnChatComoAdmin(Usuario admin, Usuario usuarioObjetivo, Chat chat, RolUsuario rol) {
        return registroUsuarioService.asignarRolEnChatComoAdmin(admin, usuarioObjetivo, chat, rol);
    }
}

