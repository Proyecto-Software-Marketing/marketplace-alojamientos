package co.edu.unbosque.marketplace.busqueda.exception;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ControlErrores {

    //fechas que no tienen sentido
    @ExceptionHandler(BusquedaException.class)
    public ResponseEntity<Map<String, List<String>>> errorBusqueda(BusquedaException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("mensajes", List.of(e.getMessage())));
    }

    // Campos vacios o con formato malo
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, List<String>>> errorCampos(BindException e) {
        List<String> mensajes = new ArrayList<>();
        for (FieldError error : e.getFieldErrors()) {
            if (error.isBindingFailure()) {
                mensajes.add("El valor de " + error.getField() + " no es válido");
            } else {
                mensajes.add(error.getDefaultMessage());
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("mensajes", mensajes));
    }
}