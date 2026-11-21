package org.example.sistema_canchas_deportivas.application.ports.out;

import org.example.sistema_canchas_deportivas.domain.models.CompromisoUtileria;
import java.time.LocalDate;

import java.util.List;
import java.util.Optional;
import org.example.sistema_canchas_deportivas.domain.models.Utileria;

public interface UtileriaOut {
    boolean existePorNombre(String nombre);
    Utileria guardar(Utileria utileria);
    Optional<Utileria> buscarPorId(Long id);
    List<Utileria> listar();
    boolean existePorNombreExcluyendoId(String nombre, Long id);
    Optional<Utileria> bloquearPorId(Long id);
    boolean tieneHistorial(Long id);
    List<CompromisoUtileria> compromisosConfirmados(
            Long id, LocalDate desde, LocalDate hasta);
    void eliminar(Long id);
    List<CompromisoUtileria> compromisosExcluyendo(Long id, LocalDate desde, LocalDate hasta, Long excluir);
}

