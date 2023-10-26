package be.kuleuven.distributedsystems.cloud.controller;


import be.kuleuven.distributedsystems.cloud.entities.Train;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Controller
public class apiController {
    public final WebClient.Builder webClientBuilder;

    @Autowired
    public apiController(WebClient.Builder webClientBuilder){
        this.webClientBuilder = webClientBuilder;
    }

    @GetMapping("/api/getTrains")
    public ResponseEntity<?> getTrains(){
        WebClient client = webClientBuilder
                .baseUrl("https://reliabletrains.com")
                .build();

        String resp = client.get()
                .uri("/trains?key=JViZPgNadspVcHsMbDFrdGg0XXxyiE")
                .retrieve()
                .toEntity(String.class)
                .block().getBody();

        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode node = mapper.readTree(resp);
            return ResponseEntity.ok(node.get("_embedded"));
        } catch (JsonProcessingException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return ResponseEntity.badRequest().build();
    }

}
