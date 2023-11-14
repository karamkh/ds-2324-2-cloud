package be.kuleuven.distributedsystems.cloud;

import be.kuleuven.distributedsystems.cloud.entities.*;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;


import java.time.LocalDateTime;
import java.util.*;

public class TrainRepository {
    List<Train> trains  = new ArrayList<>();
    List<Seat> seats = new ArrayList<>();
    Map<String, List<Booking>> bookingMap = new HashMap<>();
    FirestoreOptions firestoreOptions =
            FirestoreOptions.getDefaultInstance().toBuilder()
                    .setProjectId("demo-distributed-systems-kul")
                    .setCredentials(new FirestoreOptions.EmulatorCredentials())
                    .setEmulatorHost("localhost:8084")
                    .build();
    Firestore db = firestoreOptions.getService();

    public TrainRepository(){
    }

    public void setTrains(List<Train> trains){
        this.trains = trains;
    }

    public void setSeats(List<Seat> seats){
        this.seats = seats;
    }

    public void setBookingMap(Map<String, List<Booking>> bookingMap ){
        this.bookingMap = bookingMap;
    }

    public List<Train> getTrains(){
        return trains;
    }

    public List<Seat> getSeats(){
        return seats;
    }

    public Map<String, List<Booking>> getBookingMap(){
        return bookingMap;
    }

    public Map<String, List<Seat>> sortSeat(){
        ArrayList<Seat> firstClass = new ArrayList<>();
        ArrayList<Seat> secondClass = new ArrayList<>();
        for(Seat s : seats){
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
        Map<String, List<Seat>> body = new HashMap<>();
        body.put("1st class:", firstClass);
        body.put("2nd class:", secondClass);

        return body;
    }

    public void createBooking(User user, Quote[] quotes, UUID bookingId){
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
            List<Booking> bookings = bookingMap.get(user.getEmail());
            bookings.add(booking);
            bookingMap.put(user.getEmail(), bookings);
        }

    }
}
