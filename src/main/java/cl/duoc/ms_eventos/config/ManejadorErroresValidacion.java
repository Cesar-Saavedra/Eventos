package cl.duoc.ms_eventos.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Convierte los errores de @Valid en una respuesta 400 con el detalle por campo
@RestControllerAdvice
public class ManejadorErroresValidacion {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> manejarValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> detalles = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            detalles.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("error", "Datos invalidos en la solicitud.");
        respuesta.put("detalles", detalles);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);
    }
}
