import java.util.Date;

public class Participa {
    private Date fechaUnion;
    private RolUsuario rolUsuario;

    private Usuario usuario;
    private Chat chat;

    public Participa() {}
    public Participa(Date fechaUnion, RolUsuario rolUsuario, Usuario usuario, Chat chat) {
        this.fechaUnion = fechaUnion;
        this.rolUsuario = rolUsuario;
        this.usuario = usuario;
        this.chat = chat;
    }

    public void setFechaUnion(Date fechaUnion) { this.fechaUnion = fechaUnion; }
    public void setRolUsuario(RolUsuario rolUsuario) { this.rolUsuario = rolUsuario; }
    public void setChat(Chat chat) { this.chat = chat; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Date getFechaUnion() { return fechaUnion; }
    public RolUsuario getRolUsuario() { return rolUsuario; }
    public Chat getChat() { return chat; }
    public Usuario getUsuario() { return usuario; }
}