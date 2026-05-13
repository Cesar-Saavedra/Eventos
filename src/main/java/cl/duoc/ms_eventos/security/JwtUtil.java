package cl.duoc.ms_eventos.security;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/*
 * Clase utilitaria para leer y validar tokens JWT.
 *
 * Es una copia exacta de JwtUtil de ms-login.
 * Permite leer el token sin llamar a ms-login en cada request,
 * lo que seria muy lento.
 *
 * REGLA IMPORTANTE:
 * El valor de jwt.secret en application.properties DEBE ser
 * identico al de ms-login. Si son distintos, los tokens
 * no se podran leer aqui y todos los requests fallaran con 401.
 */
@Component
public class JwtUtil {

    // Clave secreta compartida con ms-login (leida desde application.properties)
    @Value("${jwt.secret}")
    private String secret;

    // Convierte el texto del secret en una clave criptografica que jjwt entiende
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /*
     * Extrae todos los datos (claims) del token.
     * Lanza una excepcion si el token fue alterado o ya vencio.
     */
    public Claims extraerClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseClaimsJws(token)
                .getPayload();
    }

    /*
     * Retorna true si el token es correcto y no esta vencido.
     * Retorna false si fue alterado, tiene mala firma o vencio.
     */
    public boolean esTokenValido(String token) {
        try {
            extraerClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Id del usuario autenticado (Integer, igual que en ms-login)
    public Integer extraerId(String token) {
        return extraerClaims(token).get("id", Integer.class);
    }

    // Nombre del usuario autenticado
    public String extraerNombre(String token) {
        return extraerClaims(token).get("nombre", String.class);
    }

    // Rol del usuario: JUGADOR, TIENDA u ORGANIZADOR
    public String extraerRol(String token) {
        return extraerClaims(token).get("rol", String.class);
    }

    /*
     * Extrae el token limpio del header Authorization.
     * El header llega como: "Bearer eyJhbGci..."
     * Devuelve solo:        "eyJhbGci..."
     * Devuelve null si el formato es incorrecto.
     */
    public String obtenerTokenDelHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

}
