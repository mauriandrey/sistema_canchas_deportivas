package org.example.sistema_canchas_deportivas.infraestructure.controllers;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.example.sistema_canchas_deportivas.application.ports.in.ClienteIn;
import org.example.sistema_canchas_deportivas.application.ports.out.*;
import org.example.sistema_canchas_deportivas.domain.models.Cliente;
import org.example.sistema_canchas_deportivas.domain.exception.*;
import org.example.sistema_canchas_deportivas.infraestructure.config.*;
import org.example.sistema_canchas_deportivas.infraestructure.exceptions.GlobalExceptionHandler;
import org.example.sistema_canchas_deportivas.infraestructure.securities.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClienteController.class)
@Import({SecurityConfig.class, WebConfig.class, JWTServices.class, UsuarioDetailsService.class,
        SecurityErrorHandler.class, AutorizacionReserva.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class ClientesHttpTest {
    @Autowired MockMvc mvc;
    @MockitoBean ClienteIn clientes;
    @MockitoBean UsuarioOut usuarios;
    @MockitoBean ReservaOut reservas;
    private static final String JSON = "{\"cedula\":\"1750397802\",\"nombres\":\"Ana\",\"apellidos\":\"Pérez\",\"email\":\"ana@example.test\",\"telefono\":\"0991234567\"}";
    private Cliente cliente() {
        var fecha = LocalDateTime.of(2026, 1, 1, 10, 0);
        return new Cliente(1L, "1750397802", "Ana", "Pérez", "ana@example.test", "0991234567", fecha, fecha);
    }

    @Test void todasLasOperacionesExigenAdminYNoExponenDatosPersonales() throws Exception {
        for (var peticion : List.of(get("/api/admin/clientes"), get("/api/admin/clientes/1"),
                post("/api/admin/clientes"), put("/api/admin/clientes/1"), delete("/api/admin/clientes/1"))) {
            mvc.perform(peticion).andExpect(status().isUnauthorized());
            mvc.perform(peticion.with(user("cliente").roles("CLIENTE")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.result").doesNotExist());
        }
        verifyNoInteractions(clientes);
    }

    @Test void crudUsaMessageResultYAuditoriaComoEnClase() throws Exception {
        when(clientes.listar()).thenReturn(List.of(cliente()));
        when(clientes.buscarPorId(1L)).thenReturn(cliente());
        when(clientes.crear(any())).thenReturn(cliente());
        when(clientes.actualizar(eq(1L), any())).thenReturn(cliente());
        mvc.perform(get("/api/admin/clientes").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Registros encontrados: 1"))
            .andExpect(jsonPath("$.result[0].cedula").value("1750397802"));
        mvc.perform(get("/api/admin/clientes/1").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.result.creado").exists());
        mvc.perform(post("/api/admin/clientes").with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(JSON))
            .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/admin/clientes/1"))
            .andExpect(jsonPath("$.result.modificado").exists());
        mvc.perform(put("/api/admin/clientes/1").with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(JSON))
            .andExpect(status().isOk()).andExpect(jsonPath("$.result.id").value(1));
        mvc.perform(delete("/api/admin/clientes/1").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Cliente eliminado correctamente"));
        verify(clientes).eliminar(1L);
    }

    @Test void rechazaDatosInvalidosYAuditoriaEnElCuerpo() throws Exception {
        for (String json : List.of("{}", JSON.replace("ana@example.test", "invalido"),
                JSON.replace("0991234567", "abc"), JSON.replace("1750397802", "17503978021234"),
                JSON.replace("}", ",\"creado\":\"2000-01-01T00:00:00\"}"))) {
            mvc.perform(post("/api/admin/clientes").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(clientes);
    }

    @Test void duplicadosYAusentesUsanElManejadorGlobal() throws Exception {
        when(clientes.crear(any())).thenThrow(new ReglaNegocioException("La cédula ya existe."));
        when(clientes.buscarPorId(9L)).thenThrow(new RecursoNoEncontradoException("Cliente no encontrado."));
        mvc.perform(post("/api/admin/clientes").with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(JSON))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CONFLICTO"));
        mvc.perform(get("/api/admin/clientes/9").with(user("admin").roles("ADMIN")))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test void corsPermiteDeleteAdministrativoDesdeReact() throws Exception {
        mvc.perform(options("/api/admin/clientes/1").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "DELETE").header("Access-Control-Request-Headers", "authorization"))
            .andExpect(status().isOk()).andExpect(header().exists("Access-Control-Allow-Methods"));
    }
}
