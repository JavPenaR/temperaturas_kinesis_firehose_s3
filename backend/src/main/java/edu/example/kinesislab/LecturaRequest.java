package edu.example.kinesislab;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record LecturaRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String sensorId,
        @NotNull @DecimalMin("-100.0") @DecimalMax("200.0") Double temperatura
) {
}
