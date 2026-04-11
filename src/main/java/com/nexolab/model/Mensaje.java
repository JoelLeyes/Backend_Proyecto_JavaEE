package com.nexolab.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "messages")
public class Mensaje {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "conversation_id", nullable = false)
	private Chat conversacion;

	@ManyToOne
	@JoinColumn(name = "sender_id", nullable = false)
	private Usuario sender;

	@Column(nullable = false)
	private String content;

	@Enumerated(EnumType.STRING)
	private EstadoMensaje tipo = EstadoMensaje.TEXT;

	@Column(nullable = false)
	private LocalDateTime timestamp = LocalDateTime.now();

	private Boolean read = false;

	@OneToMany(mappedBy = "mensaje", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<Adjunto> adjuntos = new HashSet<>();

	// Getters and setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Chat getConversacion() {
		return conversacion;
	}

	public void setConversacion(Chat conversacion) {
		this.conversacion = conversacion;
	}

	public Usuario getSender() {
		return sender;
	}

	public void setSender(Usuario sender) {
		this.sender = sender;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public EstadoMensaje getTipo() {
		return tipo;
	}

	public void setTipo(EstadoMensaje tipo) {
		this.tipo = tipo;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public Boolean getRead() {
		return read;
	}

	public void setRead(Boolean read) {
		this.read = read;
	}

	public Set<Adjunto> getAdjuntos() {
		return adjuntos;
	}

	public void setAdjuntos(Set<Adjunto> adjuntos) {
		this.adjuntos = adjuntos;
	}
}