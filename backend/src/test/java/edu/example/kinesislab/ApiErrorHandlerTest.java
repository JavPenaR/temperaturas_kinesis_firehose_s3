package edu.example.kinesislab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;

class ApiErrorHandlerTest {
    @Test
    void explicaTokenVencidoSinExponerCredenciales() {
        AwsServiceException exception = AwsServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("ExpiredTokenException").build())
                .build();

        var response = new ApiErrorHandler().aws(exception);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertTrue(response.getBody().get("error").contains("AWS Details"));
        assertTrue(response.getBody().get("error").contains("ExpiredTokenException"));
    }
}
