public class DemoRegistroUsuario {
    public static void main(String[] args) {
        UsuarioDAO dao = new UsuarioDAO();
        RegistroUsuarioService service = new RegistroUsuarioService(dao);
        RegistroUsuarioServlet servlet = new RegistroUsuarioServlet(service);

        Usuario usuario = servlet.registrarUsuarioNormal(
                "Laura",
                "Gomez",
                "laura@example.com",
                "password123",
                null
        );

        Usuario admin = servlet.registrarUsuarioNormal(
                "Admin",
                "Sistema",
                "admin@example.com",
                "admin1234",
                null
        );

        Chat chat = new Chat();
        servlet.asignarCargoYSectorComoAdmin(admin, usuario, "Analista", Sector.SISTEMAS);
        servlet.asignarRolEnChatComoAdmin(admin, usuario, chat, RolUsuario.MIEMBRO);

        System.out.println("Usuario creado: id=" + usuario.getIdUsuario());
        System.out.println("Estado inicial=" + usuario.getTipoEstado());
        System.out.println("Fecha creacion=" + usuario.getFechaCreacion());
        System.out.println("Cargo=" + usuario.getCargo() + ", Sector=" + usuario.getSector());
        System.out.println("Participaciones en chat=" + usuario.getParticipaciones().size());
    }
}

