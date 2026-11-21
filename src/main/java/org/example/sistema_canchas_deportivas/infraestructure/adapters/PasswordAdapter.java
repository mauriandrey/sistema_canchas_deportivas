package org.example.sistema_canchas_deportivas.infraestructure.adapters;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.example.sistema_canchas_deportivas.application.ports.out.PasswordOut;

@Component
public class PasswordAdapter implements PasswordOut {
    private final PasswordEncoder passwordEncoder;

    public PasswordAdapter(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String encriptar(String password) {
        return passwordEncoder.encode(password);
    }

    @Override
    public boolean coincide(String passwordPlano, String passwordEncriptado) {
        return passwordEncoder.matches(passwordPlano, passwordEncriptado);
    }
}

