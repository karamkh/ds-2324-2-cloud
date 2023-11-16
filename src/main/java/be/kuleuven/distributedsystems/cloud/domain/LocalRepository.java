package be.kuleuven.distributedsystems.cloud.domain;

import be.kuleuven.distributedsystems.cloud.TrainRepository;
import be.kuleuven.distributedsystems.cloud.auth.SecurityFilter;
import be.kuleuven.distributedsystems.cloud.entities.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
// TODO: delete component when coudrep is made
@Component
public class LocalRepository {
    private final Firestore db;
    private final WebClient reliableClient;
    private final WebClient unReliableClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final TrainRepository reliable = new TrainRepository();
    private final TrainRepository unReliable = new TrainRepository();
    // TODO: delete this when implemented with firestore
    public Map<String, List<Booking>> bookingMap = new HashMap<>();

    public LocalRepository(WebClient.Builder webClientBuilder) {
        reliableClient = webClientBuilder
                .baseUrl("https://reliabletrains.com")
                .build();
        unReliableClient = webClientBuilder
                .baseUrl("https://unreliabletrains.com")
                .build();

        FirestoreOptions firestoreOptions =
                FirestoreOptions.getDefaultInstance().toBuilder()
                        .setProjectId("demo-distributed-systems-kul")
                        .setCredentials(new FirestoreOptions.EmulatorCredentials())
                        .setEmulatorHost("localhost:8084")
                        .build();
        db = firestoreOptions.getService();
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

    public ResponseEntity<Train> getTrain(@RequestParam String trainCompany, @RequestParam String trainId){
        if(trainCompany.equals("reliabletrains.com")){
            return reliableClient.get()
                    .uri("/trains/{trainId}?key=JViZPgNadspVcHsMbDFrdGg0XXxyiE", trainId)
                    .retrieve()
                    .toEntity(Train.class).block();
        }else {
            return unReliableClient.get()
                    .uri("/trains/{trainId}?key=JViZPgNadspVcHsMbDFrdGg0XXxyiE", trainId)
                    .retrieve()
                    .toEntity(Train.class)
                    .onErrorReturn(ResponseEntity.notFound().build())
                    .block();
        }

    }

    public String[] times(WebClient client, String trainId){
        String resp =  client.get()
                .uri("/trains/{trainId}/times?key=JViZPgNadspVcHsMbDFrdGg0XXxyiE", trainId)
                .retrieve()
                .toEntity(String.class)
                .onErrorReturn(ResponseEntity.ok("{\"_embedded\":{\"stringList\":[]}}"))
                .block().getBody();
        try {
            JsonNode node = mapper.readTree(resp).get("_embedded").get("stringList");
            return mapper.treeToValue(node, String[].class);
        } catch (JsonProcessingException e) {
            return new String[0];
        }
    }

    public ResponseEntity<?> getTrainTimes(@RequestParam String trainCompany, @RequestParam String trainId){
        String[] times;
        if(trainCompany.equals("reliabletrains.com")){
            times = times(reliableClient, trainId);
        }else {
            times = times(unReliableClient, trainId);
        }
        return ResponseEntity.ok(Arrays.stream(times).sorted());
    }

    public Seat[] seats(WebClient client, String trainId, String time){
        String resp = client.get()
                .uri("/trains/{trainId}/seats?time={time}&available=true&key=JViZPgNadspVcHsMbDFrdGg0XXxyiE", trainId, time)
                .retrieve()
                .toEntity(String.class)
                .onErrorReturn(ResponseEntity.ok("{\"_embedded\":{\"seats\":[]}}"))
                .block().getBody();
        try{
            JsonNode node = mapper.readTree(resp).get("_embedded").get("seats");
            return mapper.treeToValue(node, Seat[].class);
        } catch (JsonProcessingException e) {
            return new Seat[0];
        }

    }

    public ResponseEntity<?> getAvailableSeats(@RequestParam String trainCompany, @RequestParam String trainId, @RequestParam String time){
        Seat[] seats;
        if(trainCompany.equals("reliabletrains.com")){
            seats = seats(reliableClient, trainId, time);
            reliable.setSeats(Arrays.stream(seats).toList());
            return ResponseEntity.ok(reliable.sortSeat());
        }else {
            seats = seats(unReliableClient, trainId, time);
            unReliable.setSeats(Arrays.stream(seats).toList());
            return ResponseEntity.ok(unReliable.sortSeat());
        }
    }

    public ResponseEntity<Seat> getSeat(@RequestParam String trainCompany, @RequestParam String trainId, @RequestParam String seatId){
        if(trainCompany.equals("reliabletrains.com")){
            return reliableClient.get()
                    .uri("/trains/{trainId}/seats/{seatId}?key=JViZPgNadspVcHsMbDFrdGg0XXxyiE", trainId, seatId)
                    .retrieve()
                    .toEntity(Seat.class)
                    .block();
        }else {
            Seat emptySeat = unReliable.getSeats().stream()
                    .filter(seat -> (Objects.equals(seat.getSeatId().toString(), seatId)) && (Objects.equals(seat.getTrainId().toString(), trainId)))
                    .findAny().orElse(new Seat());
            return unReliableClient.get()
                    .uri("/trains/{trainId}/seats/{seatId}?key=JViZPgNadspVcHsMbDFrdGg0XXxyiE", trainId, seatId)
                    .retrieve()
                    .toEntity(Seat.class)
                    .onErrorReturn(ResponseEntity.ok(emptySeat))
                    .block();
        }


    }

    public ResponseEntity<List<Booking>> getCustomerBookings(){
        //List<Booking> bookings = bookingMap.get(SecurityFilter.getUser().getEmail());
        List<Booking> bookings = new ArrayList<>();
        try{
            bookings = getAllCustomerBooking(SecurityFilter.getUser().getEmail());
        } catch (ExecutionException | InterruptedException ignored) {

        }

        if (bookings != null){
            return ResponseEntity.ok(bookings);
        }else {
            return ResponseEntity.noContent().build();
        }

    }

    // TODO: werkt niet ATM
    public ResponseEntity<?> getAllBookings() {
        if (SecurityFilter.getUser().isManager()){
            return ResponseEntity.ok(bookingMap.values());
        }else {
            return getCustomerBookings();
        }
    }

    // TODO: werkt niet ATM
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

    public void releaseTicket(Ticket ticket){
        String trainId = ticket.getTrainId().toString();
        String seatId = ticket.getSeatId().toString();
        String ticketId = ticket.getTicketId().toString();
        if(ticket.getTrainCompany().equals("reliabletrains.com")){
            ResponseEntity<?> response = reliableClient.delete()
                    .uri("/trains/{trainId}/seats/{seatId}/ticket/{ticketId}?key=JViZPgNadspVcHsMbDFrdGg0XXxyiE", trainId, seatId, ticketId)
                    .retrieve()
                    .toEntity(String.class)
                    .onErrorReturn(ResponseEntity.ok().build())
                    .block();
        }else {
            ResponseEntity<?> response = unReliableClient.delete()
                    .uri("/trains/{trainId}/seats/{seatId}/ticket/{ticketId}?key=JViZPgNadspVcHsMbDFrdGg0XXxyiE", trainId, seatId, ticketId)
                    .retrieve()
                    .toEntity(String.class)
                    .onErrorReturn(ResponseEntity.ok().build())
                    .block();
        }
    }

    public List<Booking> getAllCustomerBooking(String customer) throws ExecutionException, InterruptedException {
        List<Booking> bookings = new ArrayList<>();
        ApiFuture<QuerySnapshot> query = db.collection(customer).get();
        QuerySnapshot querySnapshot = query.get();
        List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
        for (QueryDocumentSnapshot document : documents){
            UUID bookingId = UUID.fromString(Objects.requireNonNull(document.getString("bookingId")));
            LocalDateTime time = LocalDateTime.parse(document.getString("time"));

            List<Map<String, Object>> ticketList = (List<Map<String, Object>>) document.get("tickets");
            List<Ticket> tickets = new ArrayList<>();
            for(Map<String, Object> ticketMap : ticketList){
                Ticket ticket = new Ticket((String) ticketMap.get("trainCompany"), UUID.fromString((String) ticketMap.get("trainId")), UUID.fromString((String) ticketMap.get("seatId")), UUID.fromString((String) ticketMap.get("ticketId")), (String) ticketMap.get("customer"), (String) ticketMap.get("bookingReference"));
                tickets.add(ticket);
            }
            Booking booking = new Booking(bookingId, time, tickets, customer);
            bookings.add(booking);
        }
        return bookings;
    }



    // TODO: delete wanneer implemented met firebase
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

    // TODO: delete wanneer implemented met firebase
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

    // TODO: delete wanneer implemented met firebase
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
    }
}
