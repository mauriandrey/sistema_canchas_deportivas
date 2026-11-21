package org.example.sistema_canchas_deportivas.infraestructure.controllers;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.example.sistema_canchas_deportivas.application.ports.in.*;
import org.example.sistema_canchas_deportivas.application.ports.out.*;
import org.example.sistema_canchas_deportivas.domain.models.DisponibilidadCancha;
import org.example.sistema_canchas_deportivas.domain.models.*;
import org.example.sistema_canchas_deportivas.domain.exception.*;
import org.example.sistema_canchas_deportivas.infraestructure.config.*;
import org.example.sistema_canchas_deportivas.infraestructure.exceptions.GlobalExceptionHandler;
import org.example.sistema_canchas_deportivas.infraestructure.mappers.*;
import org.example.sistema_canchas_deportivas.infraestructure.securities.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {CanchaController.class, HorarioAtencionController.class})
@Import({SecurityConfig.class, WebConfig.class, JWTServices.class, UsuarioDetailsService.class,
        SecurityErrorHandler.class, AutorizacionReserva.class, CanchaMapper.class, HorarioAtencionMapper.class,
        GlobalExceptionHandler.class})
@ActiveProfiles("test")
class CanchasHttpTest {
    @Autowired MockMvc mvc;
    @MockitoBean CanchaIn canchas;
    @MockitoBean HorarioAtencionIn horarios;
    @MockitoBean UsuarioOut usuarios;
    @MockitoBean ReservaOut reservas;
    private final Cancha cancha = new Cancha(1L, "Tenis", TipoCancha.TENIS, null, true);
    private static final String JSON = "{\"nombre\":\"Tenis\",\"tipo\":\"TENIS\",\"superficie\":null,\"activa\":true}";

    @BeforeEach void preparar() {
        when(canchas.listar()).thenReturn(List.of(cancha));
        when(canchas.buscarPorId(1L)).thenReturn(cancha);
        when(canchas.crear(anyString(), any(), any(), anyBoolean())).thenReturn(cancha);
        when(canchas.actualizar(anyLong(), anyString(), any(), any(), anyBoolean())).thenReturn(cancha);
        when(canchas.cambiarEstado(1L, false)).thenReturn(new Cancha(1L, "Tenis", TipoCancha.TENIS, null, false));
    }

    @Test void catalogoYDetalleSonPublicosYExponenCompatibilidades() throws Exception {
        mvc.perform(get("/api/canchas")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].disciplinasCompatibles[0]").value("TENIS"));
        mvc.perform(get("/api/canchas/1")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
    }

    @Test void disponibilidadPublicaTieneFechaDisciplinaDuracionYBloques() throws Exception {
        var fecha = LocalDate.of(2027, 1, 4);
        when(canchas.disponibilidad(fecha, Disciplina.TENIS, 2)).thenReturn(List.of(
                new DisponibilidadCancha(cancha, List.of(new DisponibilidadCancha.Bloque(LocalTime.of(8, 0), LocalTime.of(10, 0))))));
        mvc.perform(get("/api/canchas/disponibilidad").param("fecha", "2027-01-04").param("disciplina", "TENIS").param("duracion", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.duracion").value(2))
                .andExpect(jsonPath("$.canchas[0].horarios[0].inicio").value("08:00:00"));
    }

    @Test void administracionExigeAdminEnLosTresMetodos() throws Exception {
        mvc.perform(post("/api/admin/canchas").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isUnauthorized());
        for (var peticion : List.of(post("/api/admin/canchas"), put("/api/admin/canchas/1"), patch("/api/admin/canchas/1/estado"))) {
            mvc.perform(peticion.with(user("cliente").roles("CLIENTE")).contentType(MediaType.APPLICATION_JSON).content(JSON))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/admin/canchas").with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/canchas/1"));
        mvc.perform(put("/api/admin/canchas/1").with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/admin/canchas/1/estado").with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content("{\"activa\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activa").value(false));
    }

    @Test void horarioSeConsultaAutenticadoYSeActualizaSoloComoAdmin() throws Exception {
        mvc.perform(get("/api/horarios")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/horarios").with(user("cliente").roles("CLIENTE"))).andExpect(status().isOk());
        mvc.perform(put("/api/admin/horarios/MONDAY").with(user("cliente").roles("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"apertura\":\"08:00\",\"cierre\":\"16:00\"}"))
                .andExpect(status().isForbidden());
        when(horarios.actualizar(any(), any(), any())).thenReturn(new HorarioAtencion(1L, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(16, 0)));
        mvc.perform(put("/api/admin/horarios/MONDAY").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"apertura\":\"08:00\",\"cierre\":\"16:00\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dia").value("MONDAY"));
    }

    @Test void parametrosYJsonInvalidosSon400() throws Exception {
        mvc.perform(get("/api/canchas/disponibilidad").param("fecha", "no-fecha").param("disciplina", "TENIS").param("duracion", "1"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/canchas/disponibilidad").param("fecha", "2027-01-04").param("disciplina", "AJEDREZ").param("duracion", "1"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/canchas/disponibilidad")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/canchas").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\",\"tipo\":\"TENIS\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles.activa").exists());
        mvc.perform(patch("/api/admin/canchas/1/estado").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activa\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test void recursosAusentesYConflictosDevuelvenErroresJson() throws Exception {
        when(canchas.buscarPorId(9L)).thenThrow(new RecursoNoEncontradoException("Cancha no encontrada."));
        mvc.perform(get("/api/canchas/9")).andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
        when(canchas.crear(anyString(), any(), any(), anyBoolean())).thenThrow(new ReglaNegocioException("Nombre duplicado."));
        mvc.perform(post("/api/admin/canchas").with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CONFLICTO"));
    }
}
