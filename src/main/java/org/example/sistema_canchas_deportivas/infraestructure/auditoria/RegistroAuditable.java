package org.example.sistema_canchas_deportivas.infraestructure.auditoria;

import java.time.LocalDateTime;

/** Contrato técnico de las entidades JPA; no pertenece al dominio. */
public interface RegistroAuditable {
    LocalDateTime getCreado();
    LocalDateTime getModificado();
    void alCrear(LocalDateTime ahora);
    void alModificar(LocalDateTime ahora);
    void conservarAuditoriaDe(RegistroAuditable existente);
}
