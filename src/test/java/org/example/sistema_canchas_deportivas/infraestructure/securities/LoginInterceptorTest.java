package org.example.sistema_canchas_deportivas.infraestructure.securities;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;

class LoginInterceptorTest {
    @Test
    void mideTiempoSinAutenticarNiRegistrarSecretos() {
        Logger logger = (Logger) LoggerFactory.getLogger(LoginInterceptor.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            var request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setQueryString("token=secreto-query");
            request.addHeader("Authorization", "Bearer secreto-cabecera");
            request.setContent("secreto-cuerpo".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var response = new MockHttpServletResponse();
            response.setStatus(401);
            var autenticacion = new TestingAuthenticationToken("cliente", null, "ROLE_CLIENTE");
            SecurityContextHolder.getContext().setAuthentication(autenticacion);

            var interceptor = new LoginInterceptor();
            assertTrue(interceptor.preHandle(request, response, this));
            interceptor.afterCompletion(request, response, this, new RuntimeException("secreto-excepcion"));

            assertSame(autenticacion, SecurityContextHolder.getContext().getAuthentication());
            assertNull(request.getSession(false));
            assertEquals(1, appender.list.size());
            String mensaje = appender.list.getFirst().getFormattedMessage();
            assertTrue(mensaje.contains("POST /api/auth/login estado=401 duración="));
            assertFalse(mensaje.contains("secreto"));
            assertFalse(mensaje.contains("Authorization"));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void finalizarSinMarcaDeInicioNoFalla() {
        assertDoesNotThrow(() -> new LoginInterceptor().afterCompletion(
                new MockHttpServletRequest(), new MockHttpServletResponse(), this, null));
    }
}

