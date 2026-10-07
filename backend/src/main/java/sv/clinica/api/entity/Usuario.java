package sv.clinica.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Persona del equipo de la clínica con acceso al software de gestión.
 * Nunca se borra: se desactiva. La contraseña solo se guarda cifrada con BCrypt.
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @ManyToMany
    @JoinTable(name = "usuario_roles",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id"))
    private Set<Rol> roles = new LinkedHashSet<>();

    protected Usuario() {
    }

    public Usuario(String username, String email, String nombre, String passwordHash, LocalDateTime ahora) {
        this.username = username;
        this.email = email;
        this.nombre = nombre;
        this.passwordHash = passwordHash;
        this.creadoEn = ahora;
        this.actualizadoEn = ahora;
    }

    public boolean estaBloqueado(LocalDateTime ahora) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
    }

    public boolean puedeEntrar(LocalDateTime ahora) {
        return activo && !estaBloqueado(ahora);
    }

    public boolean tieneRol(String codigo) {
        return roles.stream().anyMatch(rol -> rol.getCodigo().equals(codigo));
    }

    /**
     * Cuenta una contraseña incorrecta. Al llegar al máximo, bloquea la cuenta un tiempo.
     * @return true si este intento ha bloqueado la cuenta
     */
    public boolean registrarIntentoFallido(int maximo, Duration bloqueo, LocalDateTime ahora) {
        intentosFallidos++;
        if (intentosFallidos < maximo) return false;
        intentosFallidos = 0;
        bloqueadoHasta = ahora.plus(bloqueo);
        return true;
    }

    /** Quita un bloqueo por intentos fallidos (por ejemplo, al restablecer la contraseña). */
    public void desbloquear() {
        intentosFallidos = 0;
        bloqueadoHasta = null;
    }

    public void registrarAcceso(LocalDateTime ahora) {
        desbloquear();
        ultimoAcceso = ahora;
    }

    public void editar(String username, String email, String nombre, LocalDateTime ahora) {
        this.username = username;
        this.email = email;
        this.nombre = nombre;
        this.actualizadoEn = ahora;
    }

    public void cambiarPassword(String passwordHash, LocalDateTime ahora) {
        this.passwordHash = passwordHash;
        this.actualizadoEn = ahora;
    }

    /** Activar también quita un bloqueo por intentos fallidos. */
    public void cambiarEstado(boolean activo, LocalDateTime ahora) {
        this.activo = activo;
        if (activo) desbloquear();
        this.actualizadoEn = ahora;
    }

    public void cambiarRoles(Collection<Rol> nuevos, LocalDateTime ahora) {
        roles.clear();
        roles.addAll(nuevos);
        this.actualizadoEn = ahora;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getNombre() { return nombre; }
    public boolean isActivo() { return activo; }
    public int getIntentosFallidos() { return intentosFallidos; }
    public LocalDateTime getBloqueadoHasta() { return bloqueadoHasta; }
    public LocalDateTime getUltimoAcceso() { return ultimoAcceso; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
    public Set<Rol> getRoles() { return roles; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Usuario otro && id != null && id.equals(otro.getId()));
    }

    @Override
    public int hashCode() {
        return Usuario.class.hashCode();
    }
}
