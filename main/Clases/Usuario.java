public class Usuario {
    private String nombre;
    private String apellido;
    private String email;
    private String passwordHash;
    private String passwordSalt;
    private String cargo;
    private String fotoPerfilUrl;
    private Sector sector;
    private String tipoEstado;
    private Set<Chat> chats = new HashSet<>();

    public Usuario(String nombre, String apellido, String email, String passwordHash, String passwordSalt,
                   String cargo, String fotoPerfilUrl, Sector sector, String tipoEstado) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
        this.cargo = cargo;
        this.fotoPerfilUrl = fotoPerfilUrl;
        this.sector = sector;
        this.tipoEstado = tipoEstado;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }
}