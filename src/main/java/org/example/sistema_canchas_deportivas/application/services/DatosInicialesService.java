package org.example.sistema_canchas_deportivas.application.services;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.sistema_canchas_deportivas.application.ports.in.DatosInicialesIn;
import org.example.sistema_canchas_deportivas.application.ports.out.CanchaOut;
import org.example.sistema_canchas_deportivas.application.ports.out.HorarioAtencionOut;
import org.example.sistema_canchas_deportivas.application.ports.out.UtileriaOut;
import org.example.sistema_canchas_deportivas.domain.models.*;

@Service
public class DatosInicialesService implements DatosInicialesIn {
    private final CanchaOut canchaOut;
    private final HorarioAtencionOut horarioOut;
    private final UtileriaOut utileriaOut;

    public DatosInicialesService(CanchaOut canchaOut, HorarioAtencionOut horarioOut, UtileriaOut utileriaOut) {
        this.canchaOut = canchaOut;
        this.horarioOut = horarioOut;
        this.utileriaOut = utileriaOut;
    }

    @Override
    @Transactional
    public void inicializar() {
        // Cargar solo en un catálogo vacío evita recrear canchas que se hayan renombrado.
        if (canchaOut.listar().isEmpty()) {
            agregarCanchas("Fútbol 11", 2, TipoCancha.FUTBOL_11, Superficie.NATURAL);
            agregarCanchas("Fútbol 6", 5, TipoCancha.FUTBOL_6, Superficie.SINTETICA);
            agregarCanchas("Básquet", 4, TipoCancha.BASQUET, null);
            agregarCanchas("Vóley/Ecuavóley", 4, TipoCancha.VOLEY_ECUAVOLEY, null);
            agregarCanchas("Tenis", 2, TipoCancha.TENIS, null);
        }

        for (DayOfWeek dia : DayOfWeek.values()) {
            if (!horarioOut.existePorDia(dia)) {
                int cierre = dia.getValue() <= DayOfWeek.THURSDAY.getValue() ? 16 : 22;
                horarioOut.guardar(new HorarioAtencion(null, dia, LocalTime.of(8, 0), LocalTime.of(cierre, 0)));
            }
        }

        // Los nombres del catálogo son editables; un reinicio no recrea implementos renombrados.
        if (utileriaOut.listar().isEmpty()) {
            for (Utileria implemento : catalogoInicial()) {
                if (!utileriaOut.existePorNombre(implemento.getNombre())) {
                    utileriaOut.guardar(implemento);
                }
            }
        }
    }

    private void agregarCanchas(String modalidad, int cantidad, TipoCancha tipo, Superficie superficie) {
        for (int numero = 1; numero <= cantidad; numero++) {
            String nombre = modalidad + " - Cancha " + numero;
            if (!canchaOut.existePorNombre(nombre)) {
                canchaOut.guardar(new Cancha(null, nombre, tipo, superficie, true));
            }
        }
    }

    private List<Utileria> catalogoInicial() {
        return List.of(
                implemento("Balón fútbol número 5", TipoImplemento.BALON, Disciplina.FUTBOL_11),
                implemento("Balón fútbol número 4", TipoImplemento.BALON, Disciplina.FUTBOL_6),
                implemento("Balón básquet número 7", TipoImplemento.BALON, Disciplina.BASQUET),
                implemento("Balón Mikasa V200W", TipoImplemento.BALON, Disciplina.VOLEY),
                implemento("Balón Mikasa FT-5", TipoImplemento.BALON, Disciplina.ECUAVOLEY),
                implemento("Chalecos", TipoImplemento.CHALECO, Disciplina.FUTBOL_11, Disciplina.FUTBOL_6),
                implemento("Raquetas de tenis", TipoImplemento.RAQUETA, Disciplina.TENIS),
                implemento("Tubos de 3 pelotas de tenis", TipoImplemento.TUBO_PELOTAS, Disciplina.TENIS));
    }

    private Utileria implemento(String nombre, TipoImplemento tipo, Disciplina... disciplinas) {
        // Cero significa que todavía no hay unidades habilitadas para asignar.
        return new Utileria(null, nombre, tipo, 0, true, Set.of(disciplinas));
    }
}
