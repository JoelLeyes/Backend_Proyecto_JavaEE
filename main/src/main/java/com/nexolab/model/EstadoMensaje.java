package com.nexolab.model;

import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "estados_mensaje")
public class EstadoMensaje {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEstadoMensaje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_entregado", nullable = false)
    private Date fechaEntregado;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "mensaje_id", nullable = false)
    private Mensaje mensaje;

    public EstadoMensaje() {}
    public EstadoMensaje(Estado estado, Date fechaEntregado) {
        this.estado = estado;
        this.fechaEntregado = fechaEntregado;
        this.usuario = usuario;
        this.mensaje = mensaje;
    }

    public void setIdEstadoMensaje(Long idEstadoMensaje) { this.idEstadoMensaje = idEstadoMensaje;}
    public void setEstado(Estado estado) {
        this.estado = estado;
    }
    public void setFechaEntregado(Date fechaEntregado) {
        this.fechaEntregado = fechaEntregado;
    }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public void setMensaje(Mensaje mensaje) { this.mensaje = mensaje; }

    public Long getIdEstadoMensaje() { return idEstadoMensaje; }
    public Estado getEstado() {
        return estado;
    }
    public Date getFechaEntregado() {
        return fechaEntregado;
    }
    public Usuario getUsuario() { return usuario;}
    public Mensaje getMensaje() { return mensaje;}
}