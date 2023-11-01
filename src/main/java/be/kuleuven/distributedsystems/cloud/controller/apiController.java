package be.kuleuven.distributedsystems.cloud.controller;


import be.kuleuven.distributedsystems.cloud.auth.SecurityFilter;
import be.kuleuven.distributedsystems.cloud.entities.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;

import java.awt.print.Book;
import java.time.LocalDateTime;
import java.util.*;


@Controller
public class apiController {
    public final WebClient.Builder webClientBuilder;
    public WebClient client;
    public final ObjectMapper mapper = new ObjectMapper();
    public Map<String, List<Booking>> bookingMap = new HashMap<>();


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
        User user  = SecurityFilter.getUser();
        UUID bookingId = UUID.randomUUID();
        List<Ticket> tickets = new ArrayList<>();
        for(Quote q : quotes){
            tickets.add(new Ticket(q.getTrainCompany(), q.getTrainId(), q.getSeatId(), UUID.randomUUID(), user.getEmail(), bookingId.toString()));
        }
        Booking booking = new Booking(UUID.randomUUID(), LocalDateTime.now(), tickets, user.getEmail());

        if(bookingMap.get(user.getEmail()) == null){
            List<Booking> bookings = new ArrayList<>();
            bookings.add(booking);
            bookingMap.put(user.getEmail(), bookings);
        }else {
            List<Booking> bookings = bookingMap.get(user);
            bookings.add(booking);
            bookingMap.put(user.getEmail(), bookings);
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/getBookings")
    public ResponseEntity<List<Booking>> getCustomerBookings(){
        List<Booking> bookings = bookingMap.get(SecurityFilter.getUser().getEmail());
        return ResponseEntity.ok(bookings);
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
