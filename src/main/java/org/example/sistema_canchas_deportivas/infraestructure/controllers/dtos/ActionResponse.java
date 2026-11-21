package org.example.sistema_canchas_deportivas.infraestructure.controllers.dtos;

/** Respuesta de una acción: mensaje y resultado, como en el proyecto de clase. */
public record ActionResponse<T>(String message, T result) {}

