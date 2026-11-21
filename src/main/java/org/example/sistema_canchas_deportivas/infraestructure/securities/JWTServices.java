package org.example.sistema_canchas_deportivas.infraestructure.securities;

import java.time.Clock;
import java.util.Date;
import javax.crypto.SecretKey;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.example.sistema_canchas_deportivas.domain.models.Rol;
import org.example.sistema_canchas_deportivas.domain.models.Usuario;

@Service
public class JWTServices {
    private final SecretKey claveSecreta;
    private final long expiration;
    private final String issuer;
    private final Clock clock;
    private final JwtParser parser;

    public JWTServices(@Value("${jwt.secret:}") String secret,
                       @Value("${jwt.expiration:3600000}") long expiration,
                       @Value("${jwt.issuer:sistema_canchas_deportivas}") String issuer, Clock clock) {
        byte[] decoded;
        try {
            decoded = Decoders.BASE64.decode(secret);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("JWT_SECRET debe estar codificada en Base64.");
        }
        if (decoded.length < 32) {
            throw new IllegalStateException("Configure JWT_SECRET con al menos 32 bytes aleatorios codificados en Base64.");
        }
        if (expiration < 1000 || expiration > 86400000 || issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("JWT_EXPIRATION debe estar entre 1000 y 86400000 milisegundos y JWT_ISSUER no puede estar vacío.");
        }
        this.claveSecreta = Keys.hmacShaKeyFor(decoded);
        this.expiration = expiration;
        this.issuer = issuer;
        this.clock = clock;
        var builder = Jwts.parser().verifyWith(claveSecreta).requireIssuer(issuer)
                .clock(() -> Date.from(clock.instant()));
        // Conserva únicamente HS256 sin vaciar el registro, que JJWT 0.12.6 no permite.
        var algoritmos = builder.sig();
        for (var algoritmo : Jwts.SIG.get().values()) {
            if (!algoritmo.getId().equals(Jwts.SIG.HS256.getId())) {
                algoritmos.remove(algoritmo);
            }
        }
        this.parser = algoritmos.and().build();
    }

    /** Patrón de clase: username como subject, rol como claim y expiración en milisegundos. */
    public String generar(String username, String rol, Long usuarioId) {
        if (username == null || username.isBlank() || usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException("Se requiere un usuario persistido para generar el token.");
        }
        Rol.valueOf(rol);
        Date actual = Date.from(clock.instant());
        return Jwts.builder()
                .issuer(issuer)
                .claim("rol", rol)
                // Distingue a la misma persona aunque su username se reutilice en el futuro.
                .claim("usuarioId", usuarioId)
                .subject(username)
                .issuedAt(actual)
                .expiration(new Date(actual.getTime() + expiration))
                .signWith(claveSecreta, Jwts.SIG.HS256)
                .compact();
    }

    /** Genera el JWT de un usuario persistido sin puertos de autenticación. */
    public String generar(Usuario usuario) {
        return generar(usuario.getUsername(), usuario.getRol().name(), usuario.getId());
    }

    public Claims leer(String token) {
        Claims payload = parser.parseSignedClaims(token).getPayload();
        if (payload.getExpiration() == null || payload.getIssuedAt() == null
                || !payload.getExpiration().toInstant().isAfter(clock.instant())
                || payload.getIssuedAt().toInstant().isAfter(clock.instant())) {
            throw new MalformedJwtException("Fechas del token inválidas.");
        }
        if (payload.getSubject() == null || payload.getSubject().isBlank()) {
            throw new MalformedJwtException("Username del token inválido.");
        }
        try {
            Long usuarioId = payload.get("usuarioId", Long.class);
            if (usuarioId == null || usuarioId <= 0) throw new IllegalArgumentException();
            Rol.valueOf(payload.get("rol", String.class));
        } catch (RuntimeException exception) {
            throw new MalformedJwtException("Identidad del token inválida.");
        }
        return payload;
    }

    public Long validarYObtenerUsuarioId(String token) {
        return leer(token).get("usuarioId", Long.class);
    }
}

