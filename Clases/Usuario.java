import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class Usuario {
    private String nombre;
    private String apellido;
    private String email;
    private String passwordHash;
    private String passwordSalt;
    private String cargo;
    private String fotoPerfilUrl;
    private Sector sector;
    private TipoEstado tipoEstado;

    private Set<Chat> chats = new HashSet<>();
    private Set<Participa> participaciones = new HashSet<>();

    public Usuario() {}
    public Usuario(String nombre, String apellido, String email, String passwordHash, String passwordSalt,
                   String cargo, String fotoPerfilUrl, Sector sector, TipoEstado tipoEstado) {
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

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public void setEmail(String email) { this.email = email; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public void setFotoPerfilUrl(String fotoPerfilUrl) { this.fotoPerfilUrl = fotoPerfilUrl; }
    public void setSector(Sector sector) { this.sector = sector; }
    public void setTipoEstado(TipoEstado tipoEstado) { this.tipoEstado = tipoEstado; }
    public void setChats(Set<Chat> chats) { this.chats = chats; }
    public void setParticipaciones(Set<Participa> participaciones) { this.participaciones = participaciones; }

    public String getNombre() {
        return nombre;
    }
    public String getApellido() {
        return apellido;
    }
    public String getEmail() { return email; }
    public String getCargo() { return cargo; }
    public String getFotoPerfilUrl() { return fotoPerfilUrl; }
    public Sector getSector() { return sector; }
    public TipoEstado getTipoEstado() { return tipoEstado; }
    public Set<Chat> getChats() { return chats; }
    public Set<Participa> getParticipaciones() { return participaciones; }

}