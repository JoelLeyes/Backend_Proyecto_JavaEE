package com.nexolab.model;

import jakarta.persistence.*;

@Entity
@Table(name = "reacciones",
       uniqueConstraints = @UniqueConstraint(columnNames = {"mensaje_id", "usuario_id", "emoji"}))
public class Reaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idReaccion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "mensaje_id", nullable = false)
    private Mensaje mensaje;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 10)
    private String emoji;

    public Reaccion() {}

    public Reaccion(Mensaje mensaje, Usuario usuario, String emoji) {
        this.mensaje = mensaje;
        this.usuario = usuario;
        this.emoji   = emoji;
    }

    public Long    getIdReaccion() { return idReaccion; }
    public Mensaje getMensaje()    { return mensaje; }
    public Usuario getUsuario()    { return usuario; }
    public String  getEmoji()      { return emoji; }
}
