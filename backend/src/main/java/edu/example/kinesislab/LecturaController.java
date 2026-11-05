package edu.example.kinesislab;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lecturas")
public class LecturaController {
    private final LecturaService service;

    public LecturaController(LecturaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LecturaResponse crear(@Valid @RequestBody LecturaRequest lectura) throws JsonProcessingException {
        return service.publicar(lectura);
    }
}
