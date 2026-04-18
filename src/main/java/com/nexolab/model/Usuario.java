package com.nexolab.model;

import jakarta.persistence.*;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUsuario;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "password_salt", nullable = false)
    private String passwordSalt;

    @Column(nullable = false)
    private String cargo;

    @Column(name = "foto_perfil_url")
    private String fotoPerfilUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Sector sector = Sector.SIN_ASIGNAR;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_estado", nullable = false)
    private TipoEstado tipoEstado =  TipoEstado.DESCONECTADO;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol_sistema", nullable = false)
    private RolSistema rolSistema = RolSistema.USUARIO;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "fecha_creacion", nullable = false)
    private Date fechaCreacion;

    @ManyToMany(mappedBy = "participantes")
    private Set<Chat> chats = new HashSet<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Participa> participaciones = new HashSet<>();

    public Usuario() {}

    public Usuario(String nombre, String apellido, String email, String passwordHash, String passwordSalt,
                   String cargo, String fotoPerfilUrl, Sector sector, TipoEstado tipoEstado, Date fechaCreacion) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
        this.cargo = cargo;
        this.fotoPerfilUrl = fotoPerfilUrl;
        this.sector = (sector == null) ? Sector.SIN_ASIGNAR : sector;
        this.tipoEstado = (tipoEstado == null) ? TipoEstado.DESCONECTADO : tipoEstado;
        this.fechaCreacion = (fechaCreacion == null) ? new Date() : fechaCreacion;
    }

    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setPasswordSalt(String passwordSalt) { this.passwordSalt = passwordSalt; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public void setFotoPerfilUrl(String fotoPerfilUrl) { this.fotoPerfilUrl = fotoPerfilUrl; }
    public void setSector(Sector sector) { this.sector = (sector == null) ? Sector.SIN_ASIGNAR : sector; }
    public void setTipoEstado(TipoEstado tipoEstado) { this.tipoEstado = tipoEstado; }
    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = (fechaCreacion == null) ? new Date() : fechaCreacion;
    }
    public void setRolSistema(RolSistema rolSistema) { this.rolSistema = (rolSistema == null) ? RolSistema.USUARIO : rolSistema; }

    public void setChats(Set<Chat> chats) {
        this.chats = (chats == null) ? new HashSet<>() : chats;
    }
    public void setParticipaciones(Set<Participa> participaciones) {
        this.participaciones = (participaciones == null) ? new HashSet<>() : participaciones;
    }

    public void asignarCargoYSector(String cargo, Sector sector) {
        this.cargo = cargo;
        this.sector = (sector == null) ? Sector.SIN_ASIGNAR : sector;
    }

    public Long getIdUsuario() { return idUsuario; }
    public String getNombre() {
        return nombre;
    }
    public String getApellido() {
        return apellido;
    }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getPasswordSalt() { return passwordSalt; }
    public String getCargo() { return cargo; }
    public String getFotoPerfilUrl() { return fotoPerfilUrl; }
    public Sector getSector() { return sector; }
    public TipoEstado getTipoEstado() { return tipoEstado; }
    public Date getFechaCreacion() { return fechaCreacion; }
    public RolSistema getRolSistema() { return rolSistema; }
    public Set<Chat> getChats() { return chats; }
    public Set<Participa> getParticipaciones() { return participaciones; }

}