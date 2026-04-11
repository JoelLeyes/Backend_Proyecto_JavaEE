package com.nexolab.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "participaciones")
public class Participa {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private LocalDateTime fechaUnion = LocalDateTime.now();

	@Enumerated(EnumType.STRING)
	private RolUsuario rolUsuario;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private Usuario usuario;

	@ManyToOne
	@JoinColumn(name = "chat_id", nullable = false)
	private Chat chat;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public LocalDateTime getFechaUnion() {
		return fechaUnion;
	}

	public void setFechaUnion(LocalDateTime fechaUnion) {
		this.fechaUnion = fechaUnion;
	}

	public RolUsuario getRolUsuario() {
		return rolUsuario;
	}

	public void setRolUsuario(RolUsuario rolUsuario) {
		this.rolUsuario = rolUsuario;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public void setUsuario(Usuario usuario) {
		this.usuario = usuario;
	}

	public Chat getChat() {
		return chat;
	}

	public void setChat(Chat chat) {
		this.chat = chat;
	}
}
