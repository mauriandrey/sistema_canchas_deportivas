package org.example.sistema_canchas_deportivas.infraestructure.exceptions;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.TransactionTimedOutException;
import org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.example.sistema_canchas_deportivas.domain.exception.*;

/** Contrato de errores único para todos los controladores; los filtros usan SecurityErrorHandler. */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) { this.clock = clock; }

    @ExceptionHandler({CredencialesInvalidasException.class, AuthenticationException.class})
    public ResponseEntity<ErrorResponse> credenciales() {
        return respuesta(401, "CREDENCIALES_INVALIDAS", "Credenciales inválidas o usuario inactivo.", Map.of());
    }

    @ExceptionHandler({AccesoDenegadoException.class, AccessDeniedException.class})
    public ResponseEntity<?> denegado() {
        return respuesta(403, "ACCESO_DENEGADO", "No tienes permiso para realizar esta operación.", Map.of());
    }

    @ExceptionHandler(DatoInvalidoException.class)
    public ResponseEntity<?> invalido(DatoInvalidoException e) {
        return respuesta(400, "DATO_INVALIDO", e.getMessage(), Map.of());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<?> noEncontrado(RecursoNoEncontradoException e) {
        return respuesta(404, "NO_ENCONTRADO", e.getMessage(), Map.of());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<?> conflicto(ReglaNegocioException e) {
        return respuesta(409, "CONFLICTO", e.getMessage(), Map.of());
    }

    @ExceptionHandler({DataIntegrityViolationException.class, ConcurrencyFailureException.class})
    public ResponseEntity<?> persistencia() {
        return respuesta(409, "CONFLICTO", "Los datos están en uso o fueron modificados; revisa la solicitud.", Map.of());
    }

    @ExceptionHandler({TransactionTimedOutException.class, QueryTimeoutException.class})
    public ResponseEntity<?> esperaAgotada() {
        return respuesta(409, "CONFLICTO", "La operación no pudo obtener los recursos a tiempo; vuelve a intentarlo.", Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validacion(MethodArgumentNotValidException e) {
        Map<String, String> detalles = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error ->
                detalles.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return respuesta(400, "VALIDACION", "Revisa los campos de la solicitud.", detalles);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> jsonInvalido() {
        return respuesta(400, "JSON_INVALIDO", "JSON inválido: revisa los campos permitidos, tipos y formatos.", Map.of());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<?> formato() {
        return respuesta(400, "SOLICITUD_INVALIDA", "Revisa los parámetros obligatorios y sus formatos.", Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> inesperado(Exception e) {
        // Los errores HTTP del framework conservan su estado y cabeceras (por ejemplo Allow en 405).
        if (e instanceof org.springframework.web.ErrorResponse error && error.getStatusCode().is4xxClientError()) {
            int status = error.getStatusCode().value();
            String codigo = switch (status) {
                case 404 -> "NO_ENCONTRADO";
                case 405 -> "METODO_NO_PERMITIDO";
                case 406 -> "RESPUESTA_NO_ACEPTABLE";
                case 415 -> "CONTENIDO_NO_SOPORTADO";
                default -> "SOLICITUD_INVALIDA";
            };
            String mensaje = switch (status) {
                case 404 -> "No se encontró el recurso solicitado.";
                case 405 -> "El método HTTP no está permitido para esta ruta.";
                case 406 -> "Solicita una respuesta en formato JSON mediante Accept: application/json.";
                case 415 -> "Envía el cuerpo en formato JSON con Content-Type: application/json.";
                default -> "Revisa la solicitud y sus parámetros.";
            };
            return ResponseEntity.status(status).headers(error.getHeaders()).contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo(codigo, mensaje, Map.of()));
        }
        // No registrar mensajes de excepciones que puedan contener SQL, contraseñas o cuerpos recibidos.
        LOGGER.error("No se pudo completar la operación; tipo de excepción: {}", e.getClass().getName());
        return respuesta(500, "ERROR_INTERNO", "No se pudo completar la operación.", Map.of());
    }

    private ResponseEntity<ErrorResponse> respuesta(
            int status, String codigo, String mensaje, Map<String, String> detalles) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(cuerpo(codigo, mensaje, detalles));
    }

    private ErrorResponse cuerpo(
            String codigo, String mensaje, Map<String, String> detalles) {
        return new ErrorResponse(
                codigo, mensaje, clock.instant(), detalles);
    }
}
