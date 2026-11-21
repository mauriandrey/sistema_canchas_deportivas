package org.example.sistema_canchas_deportivas.domain.exception;

public class DatoInvalidoException extends RuntimeException {
    public DatoInvalidoException(String mensaje) {
        super(mensaje);
    }
}

