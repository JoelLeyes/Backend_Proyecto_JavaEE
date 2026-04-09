import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Chat {
    private int idChat;
    private String nombreChat;
    private TipoChat tipoChat;
    private Date fechaCreacion;

    private Set<Participa> participaciones = new HashSet<>();
    private List<Mensaje> mensajes = new ArrayList<>();

    public Chat() {}
    public Chat(int idChat, String nombreChat, TipoChat tipoChat, Date fechaCreacion) {
        this.idChat = idChat;
        this.nombreChat = nombreChat;
        this.tipoChat = tipoChat;
        this.fechaCreacion = fechaCreacion;
    }

    public void setIdChat(int idChat) { this.idChat = idChat; }
    public void setNombreChat(String nombreChat) { this.nombreChat = nombreChat; }
    public void setTipoChat(TipoChat tipoChat) { this.tipoChat = tipoChat; }
    public void setFechaCreacion(Date fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public void setParticipaciones(Set<Participa> participaciones) {
        this.participaciones = (participaciones == null) ? new HashSet<>() : participaciones;
    }
    public void setMensajes(List<Mensaje> mensajes) {
        this.mensajes = (mensajes == null) ? new ArrayList<>() : mensajes;
    }

    public int getIdChat() { return idChat; }
    public String getNombreChat() { return nombreChat; }
    public TipoChat getTipoChat() { return tipoChat; }
    public Date getFechaCreacion() { return fechaCreacion; }
    public Set<Participa> getParticipaciones() { return participaciones; }
    public List<Mensaje> getMensajes() { return mensajes; }

}