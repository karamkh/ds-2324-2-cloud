package be.kuleuven.distributedsystems.cloud.controller;


import be.kuleuven.distributedsystems.cloud.domain.LocalRepository;
import be.kuleuven.distributedsystems.cloud.entities.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.*;


@Controller
public class apiController {
    private final LocalRepository localRepository;

    @Autowired
    public apiController(WebClient.Builder webClientBuilder) {
        localRepository = new LocalRepository(webClientBuilder);
    }

    @GetMapping("/api/getTrains")
    public ResponseEntity<Train[]> getTrains(){
        return localRepository.getTrains();
    }

    @GetMapping("/api/getTrain")
    public ResponseEntity<Train> getTrain(@RequestParam String trainCompany, @RequestParam String trainId){
        return localRepository.getTrain(trainCompany, trainId);
    }

    @GetMapping("/api/getTrainTimes")
    public ResponseEntity<?> getTrainTimes(@RequestParam String trainCompany, @RequestParam String trainId){
        return localRepository.getTrainTimes(trainCompany, trainId);
    }

    @GetMapping("/api/getAvailableSeats")
    public ResponseEntity<?> getAvailableSeats(@RequestParam String trainCompany, @RequestParam String trainId, @RequestParam String time){
            return localRepository.getAvailableSeats(trainCompany, trainId, time);
    }

    @GetMapping("/api/getSeat")
    public ResponseEntity<Seat> getSeat(@RequestParam String trainCompany, @RequestParam String trainId, @RequestParam String seatId){
        return localRepository.getSeat(trainCompany, trainId, seatId);
    }

    @PostMapping("/api/confirmQuotes")
    public ResponseEntity<?> createBooking(@RequestBody Quote[] quotes){
        return localRepository.createBooking(quotes);
    }

    @GetMapping("/api/getBookings")
    public ResponseEntity<List<Booking>> getCustomerBookings(){
        return localRepository.getCustomerBookings();
    }

    @GetMapping("/api/getAllBookings")
    public ResponseEntity<?> getAllBookings(){
        return localRepository.getAllBookings();
    }

    @GetMapping("/api/getBestCustomers")
    public ResponseEntity<?> getBestCustomers(){
        return localRepository.getBestCustomers();
    }
}
