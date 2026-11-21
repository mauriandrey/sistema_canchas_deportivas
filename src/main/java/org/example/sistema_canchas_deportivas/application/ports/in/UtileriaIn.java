package org.example.sistema_canchas_deportivas.application.ports.in;
import java.time.LocalDateTime;
import java.util.*;
import org.example.sistema_canchas_deportivas.domain.models.*;
import org.example.sistema_canchas_deportivas.domain.models.UtileriaDisponible;

public interface UtileriaIn {
    List<Utileria> listar();
    Utileria crear(String nombre, TipoImplemento tipo, int existencias, boolean activa, Set<Disciplina> disciplinas);
    Utileria actualizar(Long id, String nombre, TipoImplemento tipo, int existencias, boolean activa, Set<Disciplina> disciplinas);
    Utileria cambiarExistencias(Long id, int existencias);
    List<UtileriaDisponible> disponibilidad(LocalDateTime inicio, int duracion, Disciplina disciplina);
    void eliminar(Long id);
    Utileria buscarPorId(Long id);
}

