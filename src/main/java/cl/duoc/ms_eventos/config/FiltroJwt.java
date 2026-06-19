package cl.duoc.ms_eventos.config;

import java.io.IOException;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class FiltroJwt extends OncePerRequestFilter {

    // El mismo secret que usa ms-login para firmar los tokens
    @Value("${jwt.secret}")
    private String secreto;

    @Override
    protected void doFilterInternal(HttpServletRequest peticion,
                                    HttpServletResponse respuesta,
                                    FilterChain cadenaFiltros)
            throws ServletException, IOException {

        String headerAutorizacion = peticion.getHeader("Authorization");

        // Si no trae token, dejar pasar (Spring Security lo rechazara solo)
        if (headerAutorizacion == null || !headerAutorizacion.startsWith("Bearer ")) {
            cadenaFiltros.doFilter(peticion, respuesta);
            return;
        }

        String token = headerAutorizacion.substring(7);

        try {
            // API de jjwt 0.12.x: Jwts.parser().verifyWith(clave).build()
            SecretKey clave = Keys.hmacShaKeyFor(secreto.getBytes());

            Claims claims = Jwts.parser()
                    .verifyWith(clave)
                    .build()
                    .parseClaimsJws(token)
                    .getPayload();

            String email = claims.getSubject();
            String rol   = (String) claims.get("rol");

            // El claim "id" se genera como Integer en ms-login
            Integer idUsuario = claims.get("id", Integer.class);

            // Guardar el id en el request para que el controlador lo use
            peticion.setAttribute("X-Usuario-Id", idUsuario);

            // Registrar la autenticacion en Spring Security con el rol del usuario
            UsernamePasswordAuthenticationToken autenticacion =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + rol))
                    );
            SecurityContextHolder.getContext().setAuthentication(autenticacion);

        } catch (JwtException | IllegalArgumentException e) {
            // Token invalido, expirado o mal formado → Spring Security rechazara el request
            System.out.println("Token JWT invalido en ms-eventos: " + e.getMessage());
        }

        cadenaFiltros.doFilter(peticion, respuesta);
    }
}
