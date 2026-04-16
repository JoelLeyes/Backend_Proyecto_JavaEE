package com.nexolab.model;

import jakarta.persistence.*;

@Entity
@Table(name = "adjuntos")
public class Adjunto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAdjunto;

    @Column(nullable = false)
    private String tipoArchivo;

    @Column(nullable = false)
    private String nombreArchivo;

    @Column(nullable = false)
    private String urlArchivo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private Mensaje mensaje;

    public Adjunto() {}
    public Adjunto(String tipoArchivo, String nombreArchivo, String urlArchivo, Mensaje mensaje) {
        this.tipoArchivo = tipoArchivo;
        this.nombreArchivo = nombreArchivo;
        this.urlArchivo = urlArchivo;
        this.mensaje = mensaje;
    }

    public void setIdAdjunto(Long idAdjunto) { this.idAdjunto = idAdjunto; }
    public void setTipoArchivo(String tipoArchivo) { this.tipoArchivo = tipoArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
    public void setUrlArchivo(String urlArchivo) { this.urlArchivo = urlArchivo; }
    public void setMensaje(Mensaje mensaje) { this.mensaje = mensaje; }

    public Long getIdAdjunto() { return idAdjunto; }
    public String getTipoArchivo() { return tipoArchivo; }
    public String getNombreArchivo() { return nombreArchivo; }
    public String getUrlArchivo() { return urlArchivo; }
    public Mensaje getMensaje() { return mensaje; }

}