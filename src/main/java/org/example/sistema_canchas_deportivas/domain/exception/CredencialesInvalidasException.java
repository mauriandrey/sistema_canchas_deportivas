package org.example.sistema_canchas_deportivas.domain.exception;

public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Credenciales inválidas o usuario inactivo.");
    }
}
