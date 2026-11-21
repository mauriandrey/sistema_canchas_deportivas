package org.example.sistema_canchas_deportivas.infraestructure.securities;

import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Mide tiempos como en el proyecto de clase; no autentica ni autoriza solicitudes. */
public class LoginInterceptor implements HandlerInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginInterceptor.class);
    private static final String INICIO = LoginInterceptor.class.getName() + ".inicio";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(INICIO, System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, @Nullable Exception exception) {
        Object inicio = request.getAttribute(INICIO);
        if (inicio instanceof Long inicioNanos) {
            long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicioNanos);
            // No registra cuerpos, query strings, cabeceras Authorization, claves ni tokens.
            LOGGER.info("HTTP {} {} estado={} duración={}ms",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), duration);
        }
    }
}

