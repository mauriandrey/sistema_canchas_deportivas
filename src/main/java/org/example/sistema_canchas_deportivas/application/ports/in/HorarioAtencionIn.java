package org.example.sistema_canchas_deportivas.application.ports.in;

import java.time.*;
import java.util.List;
import org.example.sistema_canchas_deportivas.domain.models.HorarioAtencion;

public interface HorarioAtencionIn {
    List<HorarioAtencion> listar();
    HorarioAtencion actualizar(DayOfWeek dia, LocalTime apertura, LocalTime cierre);
    HorarioAtencion buscarPorDia(DayOfWeek dia);
    HorarioAtencion crear(DayOfWeek dia, LocalTime apertura, LocalTime cierre);
    void eliminar(DayOfWeek dia);
}

