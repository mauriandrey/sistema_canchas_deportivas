package org.example.sistema_canchas_deportivas.application.ports.out;

import java.util.List;
import java.util.Optional;
import org.example.sistema_canchas_deportivas.domain.models.Cancha;

public interface CanchaOut {
    boolean existePorNombre(String nombre);
    Cancha guardar(Cancha cancha);
    Optional<Cancha> buscarPorId(Long id);
    List<Cancha> listar();
    boolean existePorNombreExcluyendoId(String nombre, Long id);
    Optional<Cancha> bloquearPorId(Long id);
    void eliminar(Long id);
}

