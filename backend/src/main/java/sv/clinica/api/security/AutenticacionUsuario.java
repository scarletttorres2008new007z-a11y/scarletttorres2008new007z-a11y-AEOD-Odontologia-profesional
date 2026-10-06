package sv.clinica.api.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/** Autenticación de una petición con token válido. Las "authorities" son los códigos de permiso del usuario. */
public class AutenticacionUsuario extends AbstractAuthenticationToken {

    private final UsuarioAutenticado usuario;
    private final Jwt token;

    public AutenticacionUsuario(UsuarioAutenticado usuario, Jwt token) {
        super(usuario.permisos().stream().map(SimpleGrantedAuthority::new).toList());
        this.usuario = usuario;
        this.token = token;
        setAuthenticated(true);
    }

    @Override
    public UsuarioAutenticado getPrincipal() {
        return usuario;
    }

    @Override
    public Jwt getCredentials() {
        return token;
    }

    @Override
    public String getName() {
        return usuario.username();
    }
}
