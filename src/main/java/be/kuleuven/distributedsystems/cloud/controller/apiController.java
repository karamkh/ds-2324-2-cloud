package be.kuleuven.distributedsystems.cloud.controller;


import be.kuleuven.distributedsystems.cloud.entities.Quote;
import be.kuleuven.distributedsystems.cloud.entities.Seat;
import be.kuleuven.distributedsystems.cloud.entities.Train;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;


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
    public ResponseEntity<?> getAvailableSeats(@RequestParam String trainCompany, @RequestParam String trainId, @RequestParam String time){
        String resp = client.get()
                .uri("/trains/{trainId}/seats?time={time}&available=true&key=JViZPgNadspVcHsMbDFrdGg0XXxyiE", trainId, time)
                .retrieve()
                .toEntity(String.class)
                .block().getBody();

        try{
            JsonNode node = mapper.readTree(resp).get("_embedded").get("seats");
            Seat[] availableSeats = mapper.treeToValue(node, Seat[].class);
            ArrayList<Seat> firstClass = new ArrayList<>();
            ArrayList<Seat> secondClass = new ArrayList<>();
            for(Seat s : availableSeats){
                if (s.getType().equals("1st class")){
                    firstClass.add(s);
                }else {
                    secondClass.add(s);
                }
            }
            Comparator<Seat> stringComparator = new Comparator<Seat>() {
                @Override
                public int compare(Seat o1, Seat o2) {
                    String s1 = o1.getName();
                    String s2 = o2.getName();
                    // Extract the numeric part of the strings
                    int num1 = Integer.parseInt(s1.replaceAll("[\\D]", ""));
                    int num2 = Integer.parseInt(s2.replaceAll("[\\D]", ""));

                    // Compare the numeric parts
                    int numComparison = Integer.compare(num1, num2);

                    // If the numeric parts are equal, compare the alphabetic part
                    if (numComparison == 0) {
                        String alpha1 = s1.replaceAll("[\\d]", "");
                        String alpha2 = s2.replaceAll("[\\d]", "");
                        return alpha1.compareTo(alpha2);
                    }

                    return numComparison;
                }

            };
            firstClass.sort(stringComparator);
            secondClass.sort(stringComparator);
            Map<String, ArrayList<Seat>> body = new HashMap<>();
            body.put("1st class:", firstClass);
            body.put("2nd class:", secondClass);
            return ResponseEntity.ok(body);
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

    @PostMapping("/api/confirmQuotes")
    public ResponseEntity<?> createBooking(@RequestBody Quote[] quotes){
        return ResponseEntity.ok().build();
    }

}
