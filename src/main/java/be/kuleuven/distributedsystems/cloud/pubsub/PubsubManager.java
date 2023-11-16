package be.kuleuven.distributedsystems.cloud.pubsub;

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.TopicName;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PubsubManager {
    private FixedTransportChannelProvider channelProvider;
    private NoCredentialsProvider credentialsProvider;
    private String projectId = "distri-405018";
    private String topicId = "bookings";
    private String subscriptionId = "subID";

    public void startEmulator() {
        ManagedChannel emuChannel =  ManagedChannelBuilder.forTarget("localhost:8083").usePlaintext().build();
        try {


        } finally {
            emuChannel.shutdown();
        }
    }
    @Bean
    public void createTopic() throws IOException {
        try (TopicAdminClient topicAdminClient = TopicAdminClient
                .create(TopicAdminSettings.newBuilder()
                .setTransportChannelProvider(channelProvider)
                .setCredentialsProvider(credentialsProvider)
                .build())) {
            TopicName topicName = TopicName.of(projectId, topicId);
            topicAdminClient.createTopic(topicName);
        }
    }

    @Bean
    public Publisher createPublisher() throws IOException {
        TopicName topicName = TopicName.of(projectId, topicId);
        return Publisher
                .newBuilder(topicName)
                .setChannelProvider(channelProvider)
                .setCredentialsProvider(credentialsProvider)
                .build();
    }

    public void createSubscription() throws IOException {
        try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create()) {
            PushConfig pushConfig = PushConfig
                    .newBuilder()
                    .setPushEndpoint("TODO")
                    .build();
            subscriptionAdminClient.createSubscription(subscriptionId, topicId, pushConfig, 60);
        };
    }

    public PubsubManager() {
        this.channelProvider = FixedTransportChannelProvider.create(
                GrpcTransportChannel.create(
                        ManagedChannelBuilder.forTarget("localhost:8083").usePlaintext().build()));
        this.credentialsProvider = NoCredentialsProvider.create();
    }
}
