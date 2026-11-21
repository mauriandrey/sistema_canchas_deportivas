package org.example.sistema_canchas_deportivas.application.ports.out;

import java.util.List;
import java.util.Optional;
import java.time.DayOfWeek;
import org.example.sistema_canchas_deportivas.domain.models.HorarioAtencion;

public interface HorarioAtencionOut {
    boolean existePorDia(DayOfWeek dia);
    HorarioAtencion guardar(HorarioAtencion horario);
    Optional<HorarioAtencion> buscarPorId(Long id);
    List<HorarioAtencion> listar();
    Optional<HorarioAtencion> buscarPorDia(DayOfWeek dia);
    Optional<HorarioAtencion> bloquearPorDia(DayOfWeek dia);
    void eliminar(Long id);
}

