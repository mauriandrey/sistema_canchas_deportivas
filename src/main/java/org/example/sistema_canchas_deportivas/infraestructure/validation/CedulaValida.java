package org.example.sistema_canchas_deportivas.infraestructure.validation;

import java.lang.annotation.*;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.example.sistema_canchas_deportivas.domain.models.CedulaEcuatoriana;

@Documented
@Constraint(validatedBy = CedulaValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CedulaValida {
    String message() default CedulaEcuatoriana.MENSAJE;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
