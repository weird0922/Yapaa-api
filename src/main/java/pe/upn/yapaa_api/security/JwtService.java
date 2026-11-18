package pe.upn.yapaa_api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    @Value("${yapaa.jwt.secreto}")
    private String secreto;

    @Value("${yapaa.jwt.expiracion-ms}")
    private long expiracionMs;

    private SecretKey obtenerClave() {
        return Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }

    public String generarToken(String correo, List<String> roles) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expiracionMs);
        return Jwts.builder()
                .subject(correo)
                .claim("roles", roles)
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(obtenerClave())
                .compact();
    }

    public String obtenerCorreo(String token) {
        return obtenerClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> obtenerRoles(String token) {
        Object roles = obtenerClaims(token).get("roles");
        if (roles instanceof List<?>) {
            return (List<String>) roles;
        }
        return List.of();
    }

    public boolean esValido(String token) {
        try {
            Jwts.parser().verifyWith(obtenerClave()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private Claims obtenerClaims(String token) {
        return Jwts.parser().verifyWith(obtenerClave()).build()
                .parseSignedClaims(token).getPayload();
    }
}
