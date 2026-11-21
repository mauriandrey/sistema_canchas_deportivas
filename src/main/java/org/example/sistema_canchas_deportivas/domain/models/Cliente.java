package org.example.sistema_canchas_deportivas.domain.models;

import java.time.LocalDateTime;
import java.util.Locale;
import org.example.sistema_canchas_deportivas.domain.exception.DatoInvalidoException;

/** Datos personales del cliente; la cuenta Usuario contiene solo sus credenciales y permisos. */
public class Cliente {
    private Long id;
    private String cedula;
    private String nombres;
    private String apellidos;
    private String email;
    private String telefono;
    private LocalDateTime creado;
    private LocalDateTime modificado;

    public Cliente(Long id, String cedula, String nombres, String apellidos, String email, String telefono,
                   LocalDateTime creado, LocalDateTime modificado) {
        this.id = id;
        this.cedula = cedula;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.email = email;
        this.telefono = telefono;
        this.creado = creado;
        this.modificado = modificado;
    }

    public static Cliente nuevo(String cedula, String nombres, String apellidos, String email, String telefono) {
        Cliente cliente = new Cliente(null, null, null, null, null, null, null, null);
        cliente.actualizar(cedula, nombres, apellidos, email, telefono);
        return cliente;
    }

    public void actualizar(String cedula, String nombres, String apellidos, String email, String telefono) {
        CedulaEcuatoriana.validar(cedula);
        validarTexto(nombres, 100, "Los nombres");
        validarTexto(apellidos, 100, "Los apellidos");
        validarTexto(email, 100, "El email");
        if (!email.strip().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new DatoInvalidoException("El email no tiene un formato válido.");
        }
        String telefonoNormalizado = telefono == null || telefono.isBlank() ? null : telefono.strip();
        if (telefonoNormalizado != null && !telefonoNormalizado.matches("[0-9]{7,10}")) {
            throw new DatoInvalidoException("El teléfono debe tener entre 7 y 10 dígitos.");
        }
        this.cedula = cedula;
        this.nombres = nombres.strip();
        this.apellidos = apellidos.strip();
        this.email = email.strip().toLowerCase(Locale.ROOT);
        this.telefono = telefonoNormalizado;
    }

    private static void validarTexto(String valor, int maximo, String campo) {
        if (valor == null || valor.isBlank() || valor.strip().length() > maximo) {
            throw new DatoInvalidoException(campo + " es obligatorio y admite hasta " + maximo + " caracteres.");
        }
    }

    public Long getId() { return id; }
    public String getCedula() { return cedula; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getEmail() { return email; }
    public String getTelefono() { return telefono; }
    public LocalDateTime getCreado() { return creado; }
    public LocalDateTime getModificado() { return modificado; }
}
