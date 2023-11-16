package be.kuleuven.distributedsystems.cloud.domain;

import be.kuleuven.distributedsystems.cloud.auth.SecurityFilter;
import be.kuleuven.distributedsystems.cloud.entities.Booking;
import be.kuleuven.distributedsystems.cloud.entities.Quote;
import be.kuleuven.distributedsystems.cloud.entities.Ticket;
import be.kuleuven.distributedsystems.cloud.entities.User;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.WriteResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;
import java.util.*;

public class CloudRepository {
    FirestoreOptions firestoreOptions =
            FirestoreOptions.getDefaultInstance().toBuilder()
                    .setProjectId("demo-distributed-systems-kul")
                    .setCredentials(new FirestoreOptions.EmulatorCredentials())
                    .setEmulatorHost("localhost:8084")
                    .build();
    Firestore db = firestoreOptions.getService();
    //adds new booking to the cloud
    // TODO: change to pub/sub

    /*
    public ResponseEntity<?> createBooking(@RequestBody Quote[] quotes){
        User user  = SecurityFilter.getUser();
        UUID bookingId = UUID.randomUUID();
        List<Ticket> tickets = new ArrayList<>();
        boolean booked = true;
        for(Quote q : quotes){
            Ticket ticket = putTicket(q.getTrainCompany(), q.getTrainId().toString(), q.getSeatId().toString(), user.getEmail(), bookingId.toString());
            if(ticket.getTicketId() == null){
                booked = false;
                break;
            }else {
                tickets.add(ticket);
            }
        }
        if(booked){
            Booking booking = new Booking(UUID.randomUUID(), LocalDateTime.now(), tickets, user.getEmail());

            if(bookingMap.get(user.getEmail()) == null){
                List<Booking> bookings = new ArrayList<>();
                bookings.add(booking);
                bookingMap.put(user.getEmail(), bookings);
            }else {
                List<Booking> bookings = bookingMap.get(user.getEmail());
                bookings.add(booking);
                bookingMap.put(user.getEmail(), bookings);
            }
            addBooking(booking);
            return ResponseEntity.noContent().build();
        }else {
            for(Ticket ticket : tickets){
                releaseTicket(ticket);
            }
            return ResponseEntity.notFound().build();
        }
    }
    public void addBooking(Booking booking){
        DocumentReference docRef = db.collection(booking.getCustomer()).document(booking.getId().toString());
        Map<String, Object> data = new HashMap<>();
        data.put("bookingId", booking.getId().toString());
        data.put("time", booking.getTime().toString());
        List<Map<String, Object>> ticketList = new ArrayList<>();
        for (Ticket ticket : booking.getTickets()) {
            Map<String, Object> ticketData = new HashMap<>();
            ticketData.put("trainCompany", ticket.getTrainCompany());
            ticketData.put("trainId", ticket.getTrainId().toString());
            ticketData.put("seatId", ticket.getSeatId().toString());
            ticketData.put("ticketId", ticket.getTicketId().toString());
            ticketData.put("customer", ticket.getCustomer());
            ticketData.put("bookingReference", ticket.getBookingReference());

            ticketList.add(ticketData);
        }
        data.put("tickets", ticketList);
        data.put("customer", booking.getCustomer());
        ApiFuture<WriteResult> result = docRef.set(data);
    }

    public Ticket putTicket(String trainCompany, String trainId, String seatId, String customerId, String bookingReference){
        ResponseEntity<Ticket> response;


        if(trainCompany.equals("reliabletrains.com")){
            response = reliableClient.put()
                    .uri("/trains/{trainId}/seats/{seatId}/ticket?customer={customerId}&bookingReference={bookingReference}&key=JViZPgNadspVcHsMbDFrdGg0XXxyiE", trainId, seatId, customerId, bookingReference)
                    .retrieve()
                    .toEntity(Ticket.class)
                    .onErrorReturn(ResponseEntity.ok(new Ticket(null, null, null, null, null, null)))
                    .block();
        }else {
            response = unReliableClient.put()
                    .uri("/trains/{trainId}/seats/{seatId}/ticket?customer={customerId}&bookingReference={bookingReference}&key=JViZPgNadspVcHsMbDFrdGg0XXxyiE", trainId, seatId, customerId, bookingReference)
                    .retrieve()
                    .toEntity(Ticket.class)
                    .onErrorReturn(ResponseEntity.ok(new Ticket(null, null, null, null, null, null)))
                    .block();
        }
        assert response != null;
        return response.getBody();
    } */
}
