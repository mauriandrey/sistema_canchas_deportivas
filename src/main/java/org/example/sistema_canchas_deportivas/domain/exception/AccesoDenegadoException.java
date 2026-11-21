package org.example.sistema_canchas_deportivas.domain.exception;
public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException() { super("No tienes permiso para realizar esta operación."); }
}
