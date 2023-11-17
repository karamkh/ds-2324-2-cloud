package be.kuleuven.distributedsystems.cloud.controller;


import be.kuleuven.distributedsystems.cloud.domain.Repository;
import be.kuleuven.distributedsystems.cloud.entities.*;
import com.google.cloud.pubsub.v1.Publisher;
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
    private final Repository repository;
    private final Publisher publisher;

    @Autowired
    public apiController(WebClient.Builder webClientBuilder, Publisher publisher) {
        repository = new Repository(webClientBuilder);
        this.publisher = publisher;
    }

    @GetMapping("/api/getTrains")
    public ResponseEntity<Train[]> getTrains(){
        return repository.getTrains();
    }

    @GetMapping("/api/getTrain")
    public ResponseEntity<Train> getTrain(@RequestParam String trainCompany, @RequestParam String trainId){
        return repository.getTrain(trainCompany, trainId);
    }

    @GetMapping("/api/getTrainTimes")
    public ResponseEntity<?> getTrainTimes(@RequestParam String trainCompany, @RequestParam String trainId){
        return repository.getTrainTimes(trainCompany, trainId);
    }

    @GetMapping("/api/getAvailableSeats")
    public ResponseEntity<?> getAvailableSeats(@RequestParam String trainCompany, @RequestParam String trainId, @RequestParam String time){
            return repository.getAvailableSeats(trainCompany, trainId, time);
    }

    @GetMapping("/api/getSeat")
    public ResponseEntity<Seat> getSeat(@RequestParam String trainCompany, @RequestParam String trainId, @RequestParam String seatId){
        return repository.getSeat(trainCompany, trainId, seatId);
    }

    @PostMapping("/api/confirmQuotes")
    public ResponseEntity<?> createBooking(@RequestBody Quote[] quotes){
        return repository.createBooking(quotes);
        /* TODO: dit veranderen zodra pub sup connectie is gemaakt
        User user = SecurityFilter.getUser();
        String jsonStringUser = new Gson().toJson(user);

        String jsonStringQuotes = new Gson().toJson(quotes);
        String jsonString = jsonStringQuotes + "+++" + jsonStringUser;

        ByteString data = ByteString.copyFromUtf8(jsonString);
        PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();
        publisher.publish(pubsubMessage);

        // TODO: is dit zelfs nodig ipv het in void functie te verandere?
        return ResponseEntity.noContent().build(); */
    }

    @GetMapping("/api/getBookings")
    public ResponseEntity<List<Booking>> getCustomerBookings(){
        return repository.getCustomerBookings();
    }

    @GetMapping("/api/getAllBookings")
    public ResponseEntity<?> getAllBookings(){
        return repository.getAllBookings();
    }

    @GetMapping("/api/getBestCustomers")
    public ResponseEntity<?> getBestCustomers(){
        return repository.getBestCustomers();
    }
}
