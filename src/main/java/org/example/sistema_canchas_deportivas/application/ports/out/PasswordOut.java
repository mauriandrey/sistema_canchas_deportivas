package org.example.sistema_canchas_deportivas.application.ports.out;

/** Puerto de codificación; la aplicación no depende de una implementación de seguridad concreta. */
public interface PasswordOut {
    String encriptar(String passwordPlano);
    boolean coincide(String passwordPlano, String passwordEncriptado);
}

