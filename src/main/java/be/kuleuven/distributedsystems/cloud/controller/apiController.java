package be.kuleuven.distributedsystems.cloud.controller;


import be.kuleuven.distributedsystems.cloud.entities.Train;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;


@Controller
public class apiController {
    public final WebClient.Builder webClientBuilder;


    @Autowired
    public apiController(WebClient.Builder webClientBuilder){
        this.webClientBuilder = webClientBuilder;
    }

    @GetMapping("/api/getTrains")
    public ResponseEntity<Train[]> getTrains(){
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
            JsonNode node = mapper.readTree(resp).get("_embedded").get("trains");
            Train[] trains = mapper.treeToValue(node, Train[].class);

            return ResponseEntity.ok(trains);

        } catch (JsonProcessingException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return ResponseEntity.badRequest().build();
    }

}
