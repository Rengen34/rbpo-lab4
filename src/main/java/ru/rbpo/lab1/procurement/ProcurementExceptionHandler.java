package ru.rbpo.lab1.procurement;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@RestControllerAdvice
public final class ProcurementExceptionHandler {
    @ExceptionHandler(ProcurementException.class)
    public ResponseEntity<Map<String, String>> procurementError(ProcurementException error) {
        HttpStatus status = switch (error.kind()) {
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
        };
        return response(status, error.kind().name(), error.getMessage());
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> unreadableBody(HttpMessageNotReadableException error) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "Некорректное тело JSON");
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> invalidPathValue(MethodArgumentTypeMismatchException error) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "Неверный формат идентификатора");
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> databaseConflict(DataIntegrityViolationException error) {
        return response(HttpStatus.CONFLICT, "CONFLICT", "Операция нарушает целостность данных");
    }
    private ResponseEntity<Map<String, String>> response(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of("error", code, "message", message));
    }
}
