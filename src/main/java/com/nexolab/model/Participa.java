package com.nexolab.model;

import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "participaciones")
public class Participa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idParticipacion;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_union", nullable = false)
    private Date fechaUnion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolUsuario rolUsuario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    public Participa() {}
    public Participa(Date fechaUnion, RolUsuario rolUsuario, Usuario usuario, Chat chat) {
        this.fechaUnion = fechaUnion;
        this.rolUsuario = rolUsuario;
        this.usuario = usuario;
        this.chat = chat;
    }

    public void setIdParticipacion(Long idParticipacion) { this.idParticipacion = idParticipacion; }
    public void setFechaUnion(Date fechaUnion) { this.fechaUnion = fechaUnion; }
    public void setRolUsuario(RolUsuario rolUsuario) { this.rolUsuario = rolUsuario; }
    public void setChat(Chat chat) { this.chat = chat; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Long getIdParticipacion() { return idParticipacion; }
    public Date getFechaUnion() { return fechaUnion; }
    public RolUsuario getRolUsuario() { return rolUsuario; }
    public Chat getChat() { return chat; }
    public Usuario getUsuario() { return usuario; }
}