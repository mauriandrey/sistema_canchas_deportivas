package org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos;

public record LoginResponse(String username, String rol, String token) {
    @Override public String toString() { return "LoginResponse[token=PROTEGIDO]"; }
}

