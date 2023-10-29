package be.kuleuven.distributedsystems.cloud.controller;


import be.kuleuven.distributedsystems.cloud.entities.Seat;
import be.kuleuven.distributedsystems.cloud.entities.Train;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Arrays;


@Controller
public class apiController {
    public final WebClient.Builder webClientBuilder;
    public WebClient client;
    public final ObjectMapper mapper = new ObjectMapper();


    @Autowired
    public apiController(WebClient.Builder webClientBuilder){
        this.webClientBuilder = webClientBuilder;
        client = webClientBuilder
                .baseUrl("https://reliabletrains.com")
                .build();
    }

    @GetMapping("/api/getTrains")
    public ResponseEntity<Train[]> getTrains(){

        String resp = client.get()
                .uri("/trains?key=JViZPgNadspVcHsMbDFrdGg0XXxyiE")
                .retrieve()
                .toEntity(String.class)
                .block().getBody();

        try {
            JsonNode node = mapper.readTree(resp).get("_embedded").get("trains");
            Train[] trains = mapper.treeToValue(node, Train[].class);

            return ResponseEntity.ok(trains);

        } catch (JsonProcessingException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/api/getTrain")
    public ResponseEntity<Train> getTrain(@RequestParam String trainCompany, @RequestParam String trainId){
        return client.get()
                .uri("/trains/{trainId}?key=JViZPgNadspVcHsMbDFrdGg0XXxyiE", trainId)
                .retrieve()
                .toEntity(Train.class).block();
    }

    @GetMapping("/api/getTrainTimes")
    public ResponseEntity<?> getTrainTimes(@RequestParam String trainCompany, @RequestParam String trainId){
        String resp =  client.get()
                .uri("/trains/{trainId}/times?key=JViZPgNadspVcHsMbDFrdGg0XXxyiE", trainId)
                .retrieve()
                .toEntity(String.class)
                .block().getBody();


        try {
            JsonNode node = mapper.readTree(resp).get("_embedded").get("stringList");
            String[] times = mapper.treeToValue(node, String[].class);
            return ResponseEntity.ok(Arrays.stream(times).sorted());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    //TODO: sort seats based on seat number and group them into classes
    @GetMapping("/api/getAvailableSeats")
    public ResponseEntity<Seat[]> getAvailableSeats(@RequestParam String trainCompany, @RequestParam String trainId, @RequestParam String time){
        String resp = client.get()
                .uri("/trains/{trainId}/seats?time={time}&available=true&key=JViZPgNadspVcHsMbDFrdGg0XXxyiE", trainId, time)
                .retrieve()
                .toEntity(String.class)
                .block().getBody();

        try{
            JsonNode node = mapper.readTree(resp).get("_embedded").get("seats");
            Seat[] availableSeats = mapper.treeToValue(node, Seat[].class);
            return ResponseEntity.ok(availableSeats);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    @GetMapping("/api/getSeat")
    public ResponseEntity<Seat> getSeat(@RequestParam String trainCompany, @RequestParam String trainId, @RequestParam String seatId){
        return client.get()
                .uri("/trains/{trainId}/seats/{seatId}?key=JViZPgNadspVcHsMbDFrdGg0XXxyiE", trainId, seatId)
                .retrieve()
                .toEntity(Seat.class)
                .block();

    }

}
