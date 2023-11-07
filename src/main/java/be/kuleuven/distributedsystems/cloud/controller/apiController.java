package be.kuleuven.distributedsystems.cloud.controller;


import be.kuleuven.distributedsystems.cloud.TrainRepository;
import be.kuleuven.distributedsystems.cloud.auth.SecurityFilter;
import be.kuleuven.distributedsystems.cloud.entities.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.*;


@Controller
public class apiController {
    public final WebClient.Builder webClientBuilder;
    public WebClient reliableClient;
    public WebClient unReliableClient;
    public final ObjectMapper mapper = new ObjectMapper();
    public final TrainRepository reliable = new TrainRepository();
    public Map<String, List<Booking>> bookingMap = new HashMap<>();


    @Autowired
    public apiController(WebClient.Builder webClientBuilder) throws IOException {
        this.webClientBuilder = webClientBuilder;
        reliableClient = webClientBuilder
                .baseUrl("https://reliabletrains.com")
                .build();
        unReliableClient = webClientBuilder
                .baseUrl("https://unreliabletrains.com")
                .build();
    }

    public Train[] parseTrains(String body) throws JsonProcessingException {
            JsonNode node = mapper.readTree(body).get("_embedded").get("trains");
            Train[] trains = mapper.treeToValue(node, Train[].class);
            reliable.setTrains(Arrays.stream(trains).toList());

            return trains;
    }

    public static <T> T[] concatenateArrays(T[] first, T[] second) {
        int totalLength = first.length + second.length;
        T[] result = Arrays.copyOf(first, totalLength);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }


    @GetMapping("/api/getTrains")
    public ResponseEntity<Train[]> getTrains(){

        String resp = reliableClient.get()
                .uri("/trains?key=JViZPgNadspVcHsMbDFrdGg0XXxyiE")
                .retrieve()
                .toEntity(String.class)
                .block().getBody();

        String resp1 = unReliableClient.get()
                .uri("/trains?key=JViZPgNadspVcHsMbDFrdGg0XXxyiE")
                .retrieve()
                .toEntity(String.class)
                .onErrorReturn(ResponseEntity.ok("{\"_embedded\":{\"trains\": []}}"))
                .block()
                .getBody();


        try {
            Train[] reliableTrains = parseTrains(resp);
            Train[] unreliableTrains = parseTrains(resp1);
            Train[] trains = concatenateArrays(reliableTrains, unreliableTrains);
            return ResponseEntity.ok(trains);

        } catch (JsonProcessingException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/api/getTrain")
    public ResponseEntity<Train> getTrain(@RequestParam String trainCompany, @RequestParam String trainId){
        return reliableClient.get()
                .uri("/trains/{trainId}?key=JViZPgNadspVcHsMbDFrdGg0XXxyiE", trainId)
                .retrieve()
                .toEntity(Train.class).block();
    }

    @GetMapping("/api/getTrainTimes")
    public ResponseEntity<?> getTrainTimes(@RequestParam String trainCompany, @RequestParam String trainId){
        String resp =  reliableClient.get()
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

    @GetMapping("/api/getAvailableSeats")
    public ResponseEntity<?> getAvailableSeats(@RequestParam String trainCompany, @RequestParam String trainId, @RequestParam String time){
        String resp = reliableClient.get()
                .uri("/trains/{trainId}/seats?time={time}&available=true&key=JViZPgNadspVcHsMbDFrdGg0XXxyiE", trainId, time)
                .retrieve()
                .toEntity(String.class)
                .block().getBody();

        try{
            JsonNode node = mapper.readTree(resp).get("_embedded").get("seats");
            Seat[] availableSeats = mapper.treeToValue(node, Seat[].class);
            reliable.setSeats(Arrays.stream(availableSeats).toList());
            return ResponseEntity.ok(reliable.sortSeat());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    @GetMapping("/api/getSeat")
    public ResponseEntity<Seat> getSeat(@RequestParam String trainCompany, @RequestParam String trainId, @RequestParam String seatId){
        return reliableClient.get()
                .uri("/trains/{trainId}/seats/{seatId}?key=JViZPgNadspVcHsMbDFrdGg0XXxyiE", trainId, seatId)
                .retrieve()
                .toEntity(Seat.class)
                .block();

    }

    @PostMapping("/api/confirmQuotes")
    public ResponseEntity<?> createBooking(@RequestBody Quote[] quotes){
        User user  = SecurityFilter.getUser();
        reliable.createBooking(user, quotes);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/getBookings")
    public ResponseEntity<List<Booking>> getCustomerBookings(){
        List<Booking> bookings = bookingMap.get(SecurityFilter.getUser().getEmail());
        if (bookings != null){
            return ResponseEntity.ok(bookings);
        }else {
            return ResponseEntity.noContent().build();
        }

    }

    @GetMapping("/api/getAllBookings")
    public ResponseEntity<?> getAllBookings(){
        if (SecurityFilter.getUser().isManager()){
            return ResponseEntity.ok(bookingMap.values());
        }else {
            return getCustomerBookings();
        }
    }

    @GetMapping("/api/getBestCustomers")
    public ResponseEntity<?> getBestCustomers(){
        if(SecurityFilter.getUser().isManager()){
            List<String> bestUsers = null;
            int maxTickets = -1;
            for (Map.Entry<String, List<Booking>> entry : bookingMap.entrySet()){
                int totalTickets = 0;
                for(Booking booking : entry.getValue()){
                    totalTickets = totalTickets + booking.getTickets().size();
                }
                if(totalTickets > maxTickets){
                    bestUsers = new ArrayList<>();
                    bestUsers.add(entry.getKey());
                    maxTickets = totalTickets;
                } else if (totalTickets == maxTickets){
                    bestUsers.add(entry.getKey());
                }
            }
            return ResponseEntity.ok(bestUsers);

        }else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
