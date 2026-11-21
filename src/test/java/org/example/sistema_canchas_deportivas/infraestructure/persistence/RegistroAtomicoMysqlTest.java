package org.example.sistema_canchas_deportivas.infraestructure.persistence;

import static org.example.sistema_canchas_deportivas.CedulasPrueba.nueva;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.example.sistema_canchas_deportivas.application.ports.in.UsuarioIn;
import org.example.sistema_canchas_deportivas.application.ports.out.*;
import org.example.sistema_canchas_deportivas.domain.models.Cliente;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Una transacción fallida real demuestra que no queda un perfil sin su cuenta. */
@SpringBootTest(properties = "spring.jpa.show-sql=false")
@ActiveProfiles({"local", "test"})
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_TESTS", matches = "true")
class RegistroAtomicoMysqlTest {
    @Autowired UsuarioIn servicio;
    @Autowired UsuarioOut usuarios;
    @Autowired ClienteOut clientes;
    @MockitoSpyBean PasswordOut passwords;

    @Test void errorDespuesDeInsertarPerfilRevierteLasDosEntidades() {
        String nombre = "atomic_" + UUID.randomUUID().toString().replace("-", "");
        String email = nombre + "@example.test";
        String password = "ClaveSoloPruebas2026!";
        doThrow(new IllegalStateException("Fallo controlado de prueba")).when(passwords).encriptar(password);
        assertThrows(IllegalStateException.class, () -> servicio.registrarCliente(nombre, password,
                Cliente.nuevo(nueva(), "Registro", "Atomico", email, "0999999999")));
        assertTrue(usuarios.buscarPorUsername(nombre).isEmpty());
        assertFalse(clientes.existePorEmail(email, null));
    }
}
