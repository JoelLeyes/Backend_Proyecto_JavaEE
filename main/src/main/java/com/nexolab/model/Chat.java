package com.nexolab.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "chats")
public class Chat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idChat;

    @Column(nullable = false)
    private String nombreChat;

    @Enumerated(EnumType.STRING)
    private TipoChat tipoChat;

    // no hay que hacer nada con jpa?
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_creacion", nullable = false)
    private Date fechaCreacion;

    @Transient
    private String ultimoMensaje;

    @Transient
    private LocalDateTime horaUltimoMensaje;

    @Transient
    private Integer mensajesSinLeer = 0;

    @ManyToMany
    @JoinTable(name = "chat_usuarios", joinColumns = @JoinColumn(name = "chat_id"), inverseJoinColumns = @JoinColumn(name = "usuario_id"))
    private Set<Usuario> participantes;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Participa> participaciones = new HashSet<>();

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Mensaje> mensajes = new ArrayList<>();

    public Chat() {}
    public Chat(String nombreChat, TipoChat tipoChat, Date fechaCreacion) {
        this.nombreChat = nombreChat;
        this.tipoChat = tipoChat;
        this.fechaCreacion = fechaCreacion;
    }

    public void setIdChat(Long idChat) { this.idChat = idChat; }
    public void setNombreChat(String nombreChat) { this.nombreChat = nombreChat; }
    public void setTipoChat(TipoChat tipoChat) { this.tipoChat = tipoChat; }
    public void setFechaCreacion(Date fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public void setParticipaciones(Set<Participa> participaciones) {
        this.participaciones = (participaciones == null) ? new HashSet<>() : participaciones;
    }
    public void setMensajes(List<Mensaje> mensajes) {
        this.mensajes = (mensajes == null) ? new ArrayList<>() : mensajes;
    }
    public void setParticipantes(Set<Usuario> participantes) { this.participantes = participantes; }
    public void setUltimoMensaje(String ultimoMensaje) { this.ultimoMensaje = ultimoMensaje; }
    public void setHoraUltimoMensaje(LocalDateTime horaUltimoMensaje) { this.horaUltimoMensaje = horaUltimoMensaje; }
    public void setMensajesSinLeer(Integer mensajesSinLeer) { this.mensajesSinLeer = mensajesSinLeer; }


    public Long getIdChat() { return idChat; }
    public String getNombreChat() { return nombreChat; }
    public TipoChat getTipoChat() { return tipoChat; }
    public Date getFechaCreacion() { return fechaCreacion; }
    public Set<Usuario> getParticipantes() { return participantes; }
    public Set<Participa> getParticipaciones() { return participaciones; }
    public List<Mensaje> getMensajes() { return mensajes; }
    public String getUltimoMensaje() { return ultimoMensaje; }
    public LocalDateTime getHoraUltimoMensaje() { return horaUltimoMensaje; }
    public Integer getMensajesSinLeer() { return mensajesSinLeer; }
}