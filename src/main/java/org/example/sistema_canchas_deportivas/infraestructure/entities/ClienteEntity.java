package org.example.sistema_canchas_deportivas.infraestructure.entities;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.sistema_canchas_deportivas.infraestructure.auditoria.*;

@Entity
@Table(name = "cliente", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cliente_cedula", columnNames = "cedula"),
        @UniqueConstraint(name = "uk_cliente_email", columnNames = "email")})
@EntityListeners(AuditoriaListener.class)
@Getter
@NoArgsConstructor
public class ClienteEntity implements RegistroAuditable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @org.example.sistema_canchas_deportivas.infraestructure.validation.CedulaValida
    @Setter @Column(nullable = false, length = 10)
    private String cedula;
    @Setter @Column(nullable = false, length = 100)
    private String nombres;
    @Setter @Column(nullable = false, length = 100)
    private String apellidos;
    @Setter @Column(nullable = false, length = 100)
    private String email;
    @Setter @Column(length = 20)
    private String telefono;
    @Column(nullable = false, updatable = false)
    private LocalDateTime creado;
    @Column(nullable = false)
    private LocalDateTime modificado;

    // Los mismos eventos de clase, centralizados en el listener con la zona de Guayaquil.
    @Override public void alCrear(LocalDateTime ahora) { creado = ahora; modificado = ahora; }
    @Override public void alModificar(LocalDateTime ahora) { modificado = ahora; }
    @Override public void conservarAuditoriaDe(RegistroAuditable existente) {
        creado = existente.getCreado();
        modificado = existente.getModificado();
    }
}
