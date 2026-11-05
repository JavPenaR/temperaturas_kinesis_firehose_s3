package edu.example.kinesislab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;

class LecturaServiceTest {
    @Test
    void publicaRegistroConClaveDelSensorYDevuelveAcuse() throws Exception {
        KinesisClient client = Mockito.mock(KinesisClient.class);
        when(client.putRecord(any(PutRecordRequest.class))).thenReturn(PutRecordResponse.builder()
                .shardId("shard-1").sequenceNumber("123").build());
        ObjectMapper mapper = new ObjectMapper();
        LecturaService service = new LecturaService(client, mapper, "lab-lecturas");

        LecturaResponse response = service.publicar(new LecturaRequest("sala-1", 22.5));

        ArgumentCaptor<PutRecordRequest> captor = ArgumentCaptor.forClass(PutRecordRequest.class);
        verify(client).putRecord(captor.capture());
        PutRecordRequest request = captor.getValue();
        JsonNode payload = mapper.readTree(request.data().asUtf8String());
        assertEquals("lab-lecturas", request.streamName());
        assertEquals("sala-1", request.partitionKey());
        assertEquals("sala-1", payload.get("sensorId").asText());
        assertEquals(22.5, payload.get("temperatura").asDouble());
        assertEquals(response.eventoId(), payload.get("eventoId").asText());
        assertNotNull(payload.get("registradoEn"));
        assertEquals("shard-1", response.shardId());
        assertEquals("123", response.sequenceNumber());
    }
}
