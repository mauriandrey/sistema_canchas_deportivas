package org.example.sistema_canchas_deportivas.infraestructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.example.sistema_canchas_deportivas.application.services.UsuarioService;
import org.example.sistema_canchas_deportivas.domain.models.Rol;
import org.example.sistema_canchas_deportivas.infraestructure.adapters.PasswordAdapter;
import org.example.sistema_canchas_deportivas.infraestructure.adapters.UsuarioAdapter;
import org.example.sistema_canchas_deportivas.infraestructure.entities.UsuarioEntity;
import org.example.sistema_canchas_deportivas.infraestructure.mappers.UsuarioMapper;
import org.example.sistema_canchas_deportivas.infraestructure.repositories.UsuarioRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Flujo real hasta el repositorio simulado; estas pruebas no verifican una inserción en MySQL. */
class UsuarioInicialTest {
    private UsuarioRepository repository;
    private PasswordEncoder encoder;
    private MockEnvironment environment;
    private CommandLineRunner runner;

    @BeforeEach
    void preparar() {
        repository = mock(UsuarioRepository.class);
        encoder = spy(new SecurityConfig().passwordEncoder());
        environment = new MockEnvironment();
        environment.setActiveProfiles("local");
        environment.setProperty("app.admin.initial-password", "admin123");
        UsuarioAdapter adapter = new UsuarioAdapter(repository, new UsuarioMapper(), mock(org.example.sistema_canchas_deportivas.infraestructure.repositories.ClienteRepository.class));
        UsuarioService service = new UsuarioService(adapter, new PasswordAdapter(encoder), mock(org.example.sistema_canchas_deportivas.application.ports.in.ClienteIn.class), mock(org.example.sistema_canchas_deportivas.application.ports.out.ClienteOut.class), mock(org.example.sistema_canchas_deportivas.application.ports.out.ReservaOut.class));
        runner = new UsuarioInicial(service, adapter, environment).inicializarAdministrador();
    }

    @Test
    void ejecutarDosVecesCreaUnSoloAdministradorSinRecalcularElHash() throws Exception {
        // Primera ejecución: consultas del runner y del servicio. Segunda: admin ya existe.
        when(repository.existsByUsername("admin")).thenReturn(false, false, true);
        runner.run();
        runner.run();
        ArgumentCaptor<UsuarioEntity> captor = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(repository, times(1)).saveAndFlush(captor.capture());
        verify(encoder, times(1)).encode("admin123");
        UsuarioEntity admin = captor.getValue();
        assertNull(admin.getId());
        assertEquals("admin", admin.getUsername());
        assertEquals(Rol.ADMIN, admin.getRol());
        assertTrue(admin.isActivo());
        assertNotEquals("admin123", admin.getPasswordHash());
        assertTrue(encoder.matches("admin123", admin.getPasswordHash()));
    }

    @Test
    void administradorExistenteNoSeActualizaNiRequiereOtraClave() throws Exception {
        when(repository.existsByUsername("admin")).thenReturn(true);
        environment.setActiveProfiles("mysql");
        environment.setProperty("app.admin.initial-password", "");
        runner.run();
        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(encoder);
    }

    @Test
    void clienteExistenteNoImpideCrearElAdministrador() throws Exception {
        when(repository.existsByUsername("cliente")).thenReturn(true);
        when(repository.existsByUsername("admin")).thenReturn(false);
        assertTrue(repository.existsByUsername("cliente"));
        runner.run();
        verify(repository, times(2)).existsByUsername("admin");
        verify(repository).saveAndFlush(argThat(usuario -> usuario.getRol() == Rol.ADMIN));
        verify(repository, never()).count();
    }

    @Test
    void fueraDeLocalRechazaLaClaveDeReferencia() {
        environment.setActiveProfiles("mysql");
        assertThrows(IllegalStateException.class, () -> runner.run());
        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(encoder);
    }

    @Test
    void fueraDeLocalExigeUnaClaveConfigurada() {
        environment.setActiveProfiles("mysql");
        environment.setProperty("app.admin.initial-password", "");
        assertThrows(IllegalStateException.class, () -> runner.run());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void fueraDeLocalPermiteUnaClaveExplicitaLarga() throws Exception {
        environment.setActiveProfiles("mysql");
        environment.setProperty("app.admin.initial-password", "SoloPruebas-2026!");
        runner.run();
        ArgumentCaptor<UsuarioEntity> captor = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(repository).saveAndFlush(captor.capture());
        assertTrue(encoder.matches("SoloPruebas-2026!", captor.getValue().getPasswordHash()));
    }

    @Test
    void falloDePersistenciaSePropagaSinFingirUnaCreacion() {
        when(repository.saveAndFlush(any())).thenThrow(
                new DataAccessResourceFailureException("Base de datos no disponible"));
        assertThrows(DataAccessResourceFailureException.class, () -> runner.run());
    }
}

