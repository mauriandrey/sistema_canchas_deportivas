package org.example.sistema_canchas_deportivas.infraestructure.controllers;

import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.example.sistema_canchas_deportivas.application.ports.in.*;
import org.example.sistema_canchas_deportivas.application.ports.out.*;
import org.example.sistema_canchas_deportivas.infraestructure.config.*;
import org.example.sistema_canchas_deportivas.infraestructure.mappers.*;
import org.example.sistema_canchas_deportivas.infraestructure.securities.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {CanchaController.class, HorarioAtencionController.class, UtileriaController.class,
        ClienteController.class, UsuarioController.class, ReservaController.class})
@Import({SecurityConfig.class, WebConfig.class, JWTServices.class, UsuarioDetailsService.class,
        SecurityErrorHandler.class, AutorizacionReserva.class, CanchaMapper.class, HorarioAtencionMapper.class,
        UtileriaMapper.class, ReservaMapper.class, ReservaUtileriaMapper.class})
@ActiveProfiles("test")
class CrudAdminHttpTest {
    @Autowired MockMvc mvc;
    @MockitoBean CanchaIn canchas;
    @MockitoBean HorarioAtencionIn horarios;
    @MockitoBean UtileriaIn utileria;
    @MockitoBean ClienteIn clientes;
    @MockitoBean UsuarioIn cuentas;
    @MockitoBean ReservaIn reservas;
    @MockitoBean UsuarioOut usuarios;
    @MockitoBean ReservaOut reservasOut;

    static Stream<String> rutas() {
        List<String> rutas = new ArrayList<>();
        for (String entidad : List.of("canchas", "horarios", "utileria", "clientes", "usuarios", "reservas", "reservas/1/utileria")) {
            String id = entidad.equals("horarios") ? "MONDAY" : "1";
            rutas.add("GET /api/admin/" + entidad);
            rutas.add("GET /api/admin/" + entidad + "/" + id);
            rutas.add("POST /api/admin/" + entidad);
            rutas.add("PUT /api/admin/" + entidad + "/" + id);
            rutas.add("DELETE /api/admin/" + entidad + "/" + id);
        }
        rutas.add("PATCH /api/admin/reservas/1/cancelar");
        return rutas.stream();
    }

    @ParameterizedTest @MethodSource("rutas")
    void crudRechazaAnonimoYClienteAntesDeEjecutarLaOperacion(String ruta) throws Exception {
        var partes = ruta.split(" ");
        mvc.perform(request(HttpMethod.valueOf(partes[0]), partes[1]).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(request(HttpMethod.valueOf(partes[0]), partes[1]).with(user("cliente").roles("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(canchas, horarios, utileria, clientes, cuentas, reservas);
    }

    @Test void registroExigeTodosLosDatosDePerfilYNoPermiteAsignarPerfilAjeno() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"persona\",\"password\":\"ClaveSoloPruebas2026!\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles.cedula").exists())
                .andExpect(jsonPath("$.detalles.nombres").exists()).andExpect(jsonPath("$.detalles.apellidos").exists())
                .andExpect(jsonPath("$.detalles.email").exists()).andExpect(jsonPath("$.detalles.telefono").exists());
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"persona\",\"password\":\"ClaveSoloPruebas2026!\",\"clienteId\":1}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(cuentas);
    }

    @Test void adminPuedeCrearConsultarEditarYEliminarHorarioPorDia() throws Exception {
        var dia = java.time.DayOfWeek.SUNDAY;
        var apertura = java.time.LocalTime.of(8, 0);
        var cierre = java.time.LocalTime.of(22, 0);
        var horario = new org.example.sistema_canchas_deportivas.domain.models.HorarioAtencion(7L, dia, apertura, cierre);
        when(horarios.crear(dia, apertura, cierre)).thenReturn(horario);
        when(horarios.buscarPorDia(dia)).thenReturn(horario);
        when(horarios.actualizar(dia, apertura, cierre)).thenReturn(horario);
        mvc.perform(post("/api/admin/horarios").with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dia\":\"SUNDAY\",\"apertura\":\"08:00\",\"cierre\":\"22:00\"}"))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/admin/horarios/SUNDAY"));
        mvc.perform(get("/api/admin/horarios/SUNDAY").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dia").value("SUNDAY"));
        mvc.perform(put("/api/admin/horarios/SUNDAY").with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apertura\":\"08:00\",\"cierre\":\"22:00\"}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/admin/horarios/SUNDAY").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());
        verify(horarios).eliminar(dia);
    }

    @Test void cedulaInvalidaDevuelveErrorDelCampoEnClientesYRegistroSinEjecutarServicios() throws Exception {
        for (String cedula : List.of("1750397803", "175039780", "1750397802001", "0000000000", "1760000008", "175039780A", " 1750397802")) {
            String perfil = "\"cedula\":\"" + cedula + "\",\"nombres\":\"Persona\",\"apellidos\":\"Prueba\",\"email\":\"persona@example.test\",\"telefono\":\"0991234567\"";
            for (var req : List.of(post("/api/admin/clientes"), put("/api/admin/clientes/1"))) {
                mvc.perform(req.with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content("{" + perfil + "}"))
                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("VALIDACION"))
                        .andExpect(jsonPath("$.detalles.cedula").value("Ingresa una cédula ecuatoriana válida de 10 dígitos."));
            }
            for (String ruta : List.of("/api/auth/registro", "/api/admin/usuarios")) {
                mvc.perform(post(ruta).with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"persona\",\"password\":\"ClaveSoloPruebas2026!\"," + perfil + "}"))
                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detalles.cedula").exists());
            }
        }
        verifyNoInteractions(clientes, cuentas);
    }
}
