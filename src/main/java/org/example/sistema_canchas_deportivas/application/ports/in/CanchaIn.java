package org.example.sistema_canchas_deportivas.application.ports.in;

import java.time.LocalDate;
import java.util.List;
import org.example.sistema_canchas_deportivas.domain.models.*;
import org.example.sistema_canchas_deportivas.domain.models.DisponibilidadCancha;

public interface CanchaIn {
    List<Cancha> listar();
    Cancha buscarPorId(Long id);
    Cancha crear(String nombre, TipoCancha tipo, Superficie superficie, boolean activa);
    Cancha actualizar(Long id, String nombre, TipoCancha tipo, Superficie superficie, boolean activa);
    Cancha cambiarEstado(Long id, boolean activa);
    List<DisponibilidadCancha> disponibilidad(LocalDate fecha, Disciplina disciplina, int duracion);
    void eliminar(Long id);
}

