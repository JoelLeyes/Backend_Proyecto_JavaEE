package com.nexolab.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "mensajes")
public class Mensaje {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idMensaje;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_enviado", nullable = false)
    private Date fechaEnviado;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @OneToMany(mappedBy = "mensaje", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Adjunto> adjuntos = new ArrayList<>();

    @OneToMany(mappedBy = "mensaje", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EstadoMensaje> estados = new ArrayList<>();

    @ManyToOne(optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    public Mensaje() {}

    public Mensaje(Date fechaEnviado, String contenido, Chat chat) {
        this.fechaEnviado = fechaEnviado;
        this.contenido = contenido;
        this.chat = chat;
    }

    public void setIdMensaje(Long idMensaje) { this.idMensaje = idMensaje; }
    public void setFechaEnviado(Date fechaEnviado) { this.fechaEnviado = fechaEnviado; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public void setChat(Chat chat) { this.chat = chat; }
    public void setAdjuntos(List<Adjunto> adjuntos) {
        this.adjuntos = (adjuntos == null) ? new ArrayList<>() : adjuntos;
    }
    public void setEstados(List<EstadoMensaje> estados) {
        this.estados = (estados == null) ? new ArrayList<>() : estados;
    }
    public void agregarAdjunto(Adjunto adjunto) {
        if (adjunto != null) {
            this.adjuntos.add(adjunto);
        }
    }

    public Long getIdMensaje() { return idMensaje; }
    public Date getFechaEnviado() { return fechaEnviado; }
    public String getContenido() { return contenido; }
    public Chat getChat() { return chat; }
    public List<Adjunto> getAdjuntos() { return adjuntos; }
    public List<EstadoMensaje> getEstados() { return estados; }

}