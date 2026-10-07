package sv.clinica.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Inicio de sesión en el software. Cada token de acceso lleva el id de su sesión, así que revocarla
 * (cerrar sesión, cambiar la contraseña, desactivar el usuario) corta el acceso al momento.
 */
@Entity
@Table(name = "sesiones")
public class Sesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Huella SHA-256 del token de refresco; el token en sí solo lo tiene el navegador. */
    @Column(name = "refresh_token_hash", nullable = false, length = 64, columnDefinition = "char(64)")
    private String refreshTokenHash;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @Column(name = "revocada_en")
    private LocalDateTime revocadaEn;

    @Column(length = 45)
    private String ip;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    protected Sesion() {
    }

    public Sesion(Usuario usuario, String refreshTokenHash, LocalDateTime ahora, LocalDateTime expiraEn,
                  String ip, String userAgent) {
        this.usuario = usuario;
        this.refreshTokenHash = refreshTokenHash;
        this.creadaEn = ahora;
        this.expiraEn = expiraEn;
        this.ip = ip;
        this.userAgent = userAgent;
    }

    public boolean estaActiva(LocalDateTime ahora) {
        return revocadaEn == null && expiraEn.isAfter(ahora);
    }

    public void revocar(LocalDateTime ahora) {
        if (revocadaEn == null) revocadaEn = ahora;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public LocalDateTime getCreadaEn() { return creadaEn; }
    public LocalDateTime getExpiraEn() { return expiraEn; }
    public LocalDateTime getRevocadaEn() { return revocadaEn; }
}
