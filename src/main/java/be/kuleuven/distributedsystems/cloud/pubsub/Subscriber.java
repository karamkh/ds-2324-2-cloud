package be.kuleuven.distributedsystems.cloud.pubsub;

import be.kuleuven.distributedsystems.cloud.domain.LocalRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Base64;
import java.util.concurrent.ExecutionException;

@org.springframework.web.bind.annotation.RestController
@RequestMapping("/sub")
public class Subscriber {

    // TODO: change to cloudrepository when done
    private final LocalRepository localRepository;

    @Autowired
    Subscriber(LocalRepository localRepository) {
        this.localRepository = localRepository;
    }

    @PostMapping("/subscription")
    public void subscribe(@RequestBody String body) throws ExecutionException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode jsonNode = objectMapper.readTree(body);
            String data = jsonNode.path("message").path("data").asText();

            localRepository.createBooking(null);
        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

