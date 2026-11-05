package edu.example.kinesislab;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkException;

@RestControllerAdvice
public class ApiErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiErrorHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", "sensorId o temperatura inválidos"));
    }

    @ExceptionHandler(SdkException.class)
    ResponseEntity<Map<String, String>> aws(SdkException ex) {
        if (ex instanceof AwsServiceException serviceException) {
            String code = serviceException.awsErrorDetails() == null
                    ? serviceException.getClass().getSimpleName()
                    : serviceException.awsErrorDetails().errorCode();
            if (code == null || code.isBlank()) {
                code = serviceException.getClass().getSimpleName();
            }
            log.warn("Kinesis rechazó PutRecord: código={}, requestId={}", code, serviceException.requestId());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", messageFor(code) + " (AWS: " + code + ")"));
        }
        log.warn("No se pudo contactar Kinesis o cargar credenciales: {}", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "No se pudo conectar a AWS. Revisa el perfil local de credenciales y la conexión a internet."));
    }

    private String messageFor(String code) {
        return switch (code) {
            case "ExpiredTokenException", "ExpiredToken", "InvalidClientTokenId", "UnrecognizedClientException" ->
                    "Las credenciales temporales de AWS Academy vencieron o son inválidas. Cópialas de nuevo desde AWS Details.";
            case "AccessDeniedException", "AccessDenied" ->
                    "La identidad del laboratorio no tiene permiso para escribir en este stream.";
            case "ResourceNotFoundException" ->
                    "No se encontró el stream. Revisa su nombre, región y estado Active.";
            case "ProvisionedThroughputExceededException" ->
                    "Se superó la capacidad del stream. Espera unos segundos y vuelve a intentar.";
            default -> "AWS rechazó la publicación. Revisa el código de error y la salida del backend.";
        };
    }
}
