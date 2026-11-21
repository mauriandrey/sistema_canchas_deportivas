package org.example.sistema_canchas_deportivas.infraestructure.securities;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import javax.crypto.spec.SecretKeySpec;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.example.sistema_canchas_deportivas.domain.models.*;

import static org.junit.jupiter.api.Assertions.*;

class JWTServicesTest {
    private static final Instant AHORA = Instant.parse("2026-11-15T15:00:00Z");
    private static final String SECRET = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    private static final String ISSUER = "sistema_canchas_deportivas";
    private final Clock clock = Clock.fixed(AHORA, ZoneOffset.UTC);
    private final Usuario usuario = new Usuario(7L, "cliente", "hash-privado", Rol.CLIENTE, true);

    @Test void generaTokenFirmadoConIdYExpiracionSinPassword() {
        var service = new JWTServices(SECRET, 3_600_000, ISSUER, clock);
        var sesion = service.generar(usuario);
        assertEquals(7L, service.validarYObtenerUsuarioId(sesion));
        assertEquals("cliente", service.leer(sesion).getSubject());
        assertEquals("CLIENTE", service.leer(sesion).get("rol", String.class));
        assertEquals(AHORA.plusSeconds(3600), service.leer(sesion).getExpiration().toInstant());
        String payload = new String(Base64.getUrlDecoder().decode(sesion.split("\\.")[1]), java.nio.charset.StandardCharsets.UTF_8);
        assertFalse(payload.contains("password"));
        assertFalse(payload.contains("hash-privado"));
        assertFalse(new org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos.LoginResponse(
                "cliente", "CLIENTE", sesion).toString().contains(sesion));
    }

    @Test void rechazaTokenExpirado() {
        var pasado = new JWTServices(SECRET, 10_000, ISSUER, Clock.fixed(AHORA.minusSeconds(20), ZoneOffset.UTC));
        var service = new JWTServices(SECRET, 3_600_000, ISSUER, clock);
        assertThrows(JwtException.class, () -> service.validarYObtenerUsuarioId(pasado.generar(usuario)));
    }

    @Test void rechazaFirmaAjenaYTokenAlteradoOMalformado() {
        String otroSecret = Base64.getEncoder().encodeToString(new byte[32]);
        var ajeno = new JWTServices(otroSecret, 3_600_000, ISSUER, clock);
        var service = new JWTServices(SECRET, 3_600_000, ISSUER, clock);
        assertThrows(JwtException.class, () -> service.validarYObtenerUsuarioId(ajeno.generar(usuario)));
        String token = service.generar(usuario);
        String[] partes = token.split("\\.");
        String alterado = partes[0] + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"1\",\"rol\":\"ADMIN\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8)) + "." + partes[2];
        assertThrows(JwtException.class, () -> service.validarYObtenerUsuarioId(alterado));
        assertThrows(JwtException.class, () -> service.validarYObtenerUsuarioId("no.es.jwt"));
    }

    @Test void rechazaEmisorIncorrecto() {
        var otro = new JWTServices(SECRET, 3_600_000, "otro-emisor", clock);
        var service = new JWTServices(SECRET, 3_600_000, ISSUER, clock);
        assertThrows(JwtException.class, () -> service.validarYObtenerUsuarioId(otro.generar(usuario)));
    }

    @Test void exigeExpiracionEmisionYIdentidad() {
        var key = new SecretKeySpec(Base64.getDecoder().decode(SECRET), "HmacSHA256");
        var service = new JWTServices(SECRET, 3_600_000, ISSUER, clock);
        String sinExp = Jwts.builder().issuer(ISSUER).subject("cliente").claim("usuarioId", 7L).claim("rol", "CLIENTE").issuedAt(Date.from(AHORA))
                .signWith(key, Jwts.SIG.HS256).compact();
        String sinIat = Jwts.builder().issuer(ISSUER).subject("cliente").claim("usuarioId", 7L).claim("rol", "CLIENTE").expiration(Date.from(AHORA.plusSeconds(10)))
                .signWith(key, Jwts.SIG.HS256).compact();
        String malId = Jwts.builder().issuer(ISSUER).subject("cliente").claim("usuarioId", "id-invalido").claim("rol", "CLIENTE").issuedAt(Date.from(AHORA))
                .expiration(Date.from(AHORA.plusSeconds(10))).signWith(key, Jwts.SIG.HS256).compact();
        for (String token : new String[]{sinExp, sinIat, malId}) {
            assertThrows(JwtException.class, () -> service.validarYObtenerUsuarioId(token));
        }
    }

    @Test void noAceptaOtroAlgoritmoAunqueCompartaLaClave() {
        String secret64 = Base64.getEncoder().encodeToString(new byte[64]);
        var key = new SecretKeySpec(new byte[64], "HmacSHA384");
        String token = Jwts.builder().issuer(ISSUER).subject("cliente").claim("usuarioId", 7L).claim("rol", "CLIENTE").issuedAt(Date.from(AHORA))
                .expiration(Date.from(AHORA.plusSeconds(10))).signWith(key, Jwts.SIG.HS384).compact();
        var service = new JWTServices(secret64, 3_600_000, ISSUER, clock);
        assertThrows(JwtException.class, () -> service.validarYObtenerUsuarioId(token));
    }

    @Test void exigeClaveFuerteYConfiguracionValida() {
        for (String secret : new String[]{"", "no base64!", Base64.getEncoder().encodeToString(new byte[16])}) {
            assertThrows(IllegalStateException.class, () -> new JWTServices(secret, 3_600_000, ISSUER, clock));
        }
        assertThrows(IllegalStateException.class, () -> new JWTServices(SECRET, 0, ISSUER, clock));
        assertThrows(IllegalStateException.class, () -> new JWTServices(SECRET, 86_400_001, ISSUER, clock));
    }
}
