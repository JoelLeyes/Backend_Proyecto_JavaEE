package com.nexolab.model;

import jakarta.persistence.*;

@Entity
@Table(name = "sector")
public class Sector {
	@Id
	@Column(nullable = false, unique = true)
	private String name;

	public Sector() {
	}

	public Sector(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
