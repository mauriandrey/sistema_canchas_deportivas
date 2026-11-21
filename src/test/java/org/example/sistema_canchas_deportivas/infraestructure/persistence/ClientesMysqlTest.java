package org.example.sistema_canchas_deportivas.infraestructure.persistence;

import static org.example.sistema_canchas_deportivas.CedulasPrueba.nueva;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;
import org.example.sistema_canchas_deportivas.application.ports.in.ClienteIn;
import org.example.sistema_canchas_deportivas.application.ports.out.UsuarioOut;
import org.example.sistema_canchas_deportivas.domain.models.Cliente;
import org.example.sistema_canchas_deportivas.domain.exception.ReglaNegocioException;
import org.example.sistema_canchas_deportivas.infraestructure.entities.ClienteEntity;
import org.example.sistema_canchas_deportivas.infraestructure.repositories.ClienteRepository;
import org.example.sistema_canchas_deportivas.infraestructure.securities.JWTServices;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.jpa.show-sql=false")
@ActiveProfiles({"local", "test"})
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_TESTS", matches = "true")
@Transactional
class ClientesMysqlTest {
    @Autowired ClienteIn clientes;
    @Autowired ClienteRepository repository;
    @Autowired UsuarioOut usuarios;
    @Autowired JWTServices jwt;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired ObjectMapper json;
    @Autowired WebApplicationContext context;
    String cedula, email;

    @BeforeEach void preparar() {
        String sufijo = UUID.randomUUID().toString().replace("-", "");
        cedula = nueva();
        email = sufijo + "@example.test";
    }
    Cliente datos() { return Cliente.nuevo(cedula, "Ana", "Pérez", email, null); }

    @Test void crudHttpConJwtRealNoCreaUsuariosNiReservas() throws Exception {
        long usuariosAntes = jdbc.queryForObject("select count(*) from usuarios", Long.class);
        long reservasAntes = jdbc.queryForObject("select count(*) from reservas", Long.class);
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        String bearer = "Bearer " + jwt.generar(usuarios.buscarPorUsername("admin").orElseThrow());
        String body = json.writeValueAsString(Map.of("cedula", cedula, "nombres", "Ana", "apellidos", "Pérez", "email", email));
        var respuesta = mvc.perform(post("/api/admin/clientes").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.result.creado").isNotEmpty())
            .andReturn().getResponse().getContentAsString();
        long id = json.readTree(respuesta).get("result").get("id").asLong();
        mvc.perform(put("/api/admin/clientes/" + id).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content(body.replace("Ana", "María")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.result.nombres").value("María"));
        mvc.perform(get("/api/admin/clientes/" + id).header("Authorization", bearer))
            .andExpect(status().isOk()).andExpect(jsonPath("$.result.email").value(email));
        mvc.perform(delete("/api/admin/clientes/" + id).header("Authorization", bearer)).andExpect(status().isOk());
        assertFalse(repository.existsById(id));
        assertEquals(usuariosAntes, jdbc.queryForObject("select count(*) from usuarios", Long.class));
        assertEquals(reservasAntes, jdbc.queryForObject("select count(*) from reservas", Long.class));
    }

    @Test void actualizarPreservaCreadoYActualizaModificado() {
        Cliente creado = clientes.crear(datos());
        assertNotNull(creado.getCreado());
        assertEquals(creado.getCreado(), creado.getModificado());
        var antiguo = LocalDateTime.of(2000, 1, 1, 0, 0);
        jdbc.update("update cliente set creado=?, modificado=? where id=?", antiguo, antiguo, creado.getId());
        em.clear();
        Cliente actualizado = clientes.actualizar(creado.getId(), Cliente.nuevo(cedula, "María", "Pérez", email, "0991234567"));
        em.clear();
        var recargado = clientes.buscarPorId(actualizado.getId());
        assertEquals(antiguo, recargado.getCreado());
        assertTrue(recargado.getModificado().isAfter(antiguo));
        assertEquals(actualizado.getModificado(), recargado.getModificado());
    }

    @Test void duplicadosSeRechazanYActualizarElMismoClienteEsValido() {
        Cliente creado = clientes.crear(datos());
        assertThrows(ReglaNegocioException.class, () -> clientes.crear(datos()));
        assertThrows(ReglaNegocioException.class, () -> clientes.crear(
                Cliente.nuevo(nueva(), "Otra", "Persona", email.toUpperCase(), null)));
        assertEquals(creado.getId(), clientes.actualizar(creado.getId(), datos()).getId());
    }

    @Test void indiceUnicoProtegeDuplicadosAunqueNoPasenPorElServicio() {
        clientes.crear(datos());
        var duplicado = new ClienteEntity();
        duplicado.setCedula(cedula);
        duplicado.setNombres("Otra");
        duplicado.setApellidos("Persona");
        duplicado.setEmail("otro." + email);
        assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(duplicado));
    }

    @Test void entidadRechazaCedulaInvalidaAunqueNoPasePorElServicio() {
        var invalido = new ClienteEntity();
        invalido.setCedula("1750397803");
        invalido.setNombres("Persona"); invalido.setApellidos("Prueba"); invalido.setEmail(email);
        assertThrows(jakarta.validation.ConstraintViolationException.class, () -> repository.saveAndFlush(invalido));
    }
}
