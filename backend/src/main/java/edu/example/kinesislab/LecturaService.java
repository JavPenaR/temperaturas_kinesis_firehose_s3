package edu.example.kinesislab;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;

@Service
public class LecturaService {
    private final KinesisClient client;
    private final ObjectMapper mapper;
    private final String streamName;

    public LecturaService(KinesisClient client, ObjectMapper mapper,
                          @Value("${app.kinesis.stream-name}") String streamName) {
        this.client = client;
        this.mapper = mapper;
        this.streamName = streamName;
    }

    public LecturaResponse publicar(LecturaRequest lectura) throws JsonProcessingException {
        String eventoId = UUID.randomUUID().toString();
        Map<String, Object> evento = new LinkedHashMap<>();
        evento.put("eventoId", eventoId);
        evento.put("sensorId", lectura.sensorId());
        evento.put("temperatura", lectura.temperatura());
        evento.put("registradoEn", Instant.now().toString());

        PutRecordResponse resultado = client.putRecord(PutRecordRequest.builder()
                .streamName(streamName)
                .partitionKey(lectura.sensorId())
                .data(SdkBytes.fromUtf8String(mapper.writeValueAsString(evento)))
                .build());
        return new LecturaResponse(eventoId, resultado.shardId(), resultado.sequenceNumber());
    }
}
