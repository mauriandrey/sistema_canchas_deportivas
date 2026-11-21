package org.example.sistema_canchas_deportivas;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.example.sistema_canchas_deportivas.domain.models.Rol;
import org.example.sistema_canchas_deportivas.infraestructure.entities.UsuarioEntity;
import org.example.sistema_canchas_deportivas.infraestructure.repositories.UsuarioRepository;
import org.example.sistema_canchas_deportivas.infraestructure.repositories.CanchaRepository;
import org.example.sistema_canchas_deportivas.infraestructure.repositories.HorarioAtencionRepository;
import org.example.sistema_canchas_deportivas.infraestructure.repositories.UtileriaRepository;
import org.example.sistema_canchas_deportivas.infraestructure.repositories.ReservaRepository;
import org.example.sistema_canchas_deportivas.application.ports.in.DatosInicialesIn;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration",
        "app.admin.initial-password=SoloPruebas-2026!"
})
@ActiveProfiles("test")
class SistemaCanchasDeportivasApplicationTests {
    // Solo esta prueba sustituye MySQL; producción mantiene JPA y su DataSource.
    @MockitoBean
    private UsuarioRepository repository;

    @MockitoBean
    private CanchaRepository canchaRepository;
    @MockitoBean
    private HorarioAtencionRepository horarioRepository;
    @MockitoBean
    private UtileriaRepository utileriaRepository;
    @MockitoBean
    private ReservaRepository reservaRepository;
    @MockitoBean
    private org.example.sistema_canchas_deportivas.infraestructure.repositories.ClienteRepository clienteRepository;
    @MockitoBean
    private DatosInicialesIn datosInicialesIn;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private WebApplicationContext context;

    @Test
    void contextLoadsConInicializadorYSeguridadActivos() throws Exception {
        ArgumentCaptor<UsuarioEntity> captor = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(repository).saveAndFlush(captor.capture());
        assertEquals(Rol.ADMIN, captor.getValue().getRol());
        assertTrue(encoder.matches("SoloPruebas-2026!", captor.getValue().getPasswordHash()));
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        mvc.perform(get("/api/admin/canchas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/login")).andExpect(status().isUnauthorized());
    }
}
