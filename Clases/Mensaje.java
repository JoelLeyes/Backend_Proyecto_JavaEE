import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Mensaje {
    private int idMensaje;
    private Date fechaEnviado;
    private String contenido;

    private List<Adjunto> adjuntos = new ArrayList<>();
    private List<EstadoMensaje> estados = new ArrayList<>();

    public Mensaje() {}
    public Mensaje(int idMensaje, Date fechaEnviado, String contenido) {
        this.idMensaje = idMensaje;
        this.fechaEnviado = fechaEnviado;
        this.contenido = contenido;
    }

    public void setIdMensaje(int idMensaje) { this.idMensaje = idMensaje; }
    public void setFechaEnviado(Date fechaEnviado) { this.fechaEnviado = fechaEnviado; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public void setAdjunto(Adjunto adjunto) { this.adjunto = adjunto; }
    public void setEstadoMensaje(Map<String, Usuario> EstadoMensaje) { this.EstadoMensaje = EstadoMensaje; }
    public void setAdjuntos(List<Adjunto> adjuntos) {
        this.adjuntos = (adjuntos == null) ? new ArrayList<>() : adjuntos;
    }
    public void setEstados(List<EstadoMensaje> estados) {
        this.estados = (estados == null) ? new ArrayList<>() : estados;
    }

    public int getIdMensaje() { return idMensaje; }
    public Date getFechaEnviado() { return fechaEnviado; }
    public String getContenido() { return contenido; }
    public List<Adjunto> getAdjuntos() { return adjuntos; }
    public List<EstadoMensaje> getEstados() { return estados; }

}