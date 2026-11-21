package org.example.sistema_canchas_deportivas.infraestructure.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.example.sistema_canchas_deportivas.domain.models.CedulaEcuatoriana;

public class CedulaValidator implements ConstraintValidator<CedulaValida, String> {
    @Override public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        return CedulaEcuatoriana.esValida(valor);
    }
}
