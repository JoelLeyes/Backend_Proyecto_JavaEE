import java.util.Date;

public class EstadoMensaje {
    private Estado estado;
    private Date fechaEntregado;

    private Usuario usuario;
    private Mensaje mensaje;

    public EstadoMensaje() {}
    public EstadoMensaje(Estado estado, Date fechaEntregado) {
        this.estado = estado;
        this.fechaEntregado = fechaEntregado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }
    public void setFechaEntregado(Date fechaEntregado) {
        this.fechaEntregado = fechaEntregado;
    }

    public Estado getEstado() {
        return estado;
    }
    public Date getFechaEntregado() {
        return fechaEntregado;
    }

}