package be.kuleuven.distributedsystems.cloud.pubsub;

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.SubscriptionName;
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
    private static final String projectId = "demo-distributed-systems-kul";
    private static final String topicId = "bookings";

    public static void startEmulator() {
        ManagedChannel emulator =  ManagedChannelBuilder.forTarget("localhost:8083").usePlaintext().build();
        try {
            createSubscription();
        } finally {
            emulator.shutdown();
        }
    }

    private boolean topicExists() {
        try {
            TopicAdminClient topicClient = TopicAdminClient.create(
                    TopicAdminSettings.newBuilder()
                            .setTransportChannelProvider(channelProvider)
                            .setCredentialsProvider(NoCredentialsProvider.create())
                            .build());
            TopicName topicName = TopicName.of(projectId, topicId);
            topicClient.getTopic(topicName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void createTopic() {
        try (TopicAdminClient topicAdminClient = TopicAdminClient
                .create(TopicAdminSettings.newBuilder()
                .setTransportChannelProvider(channelProvider)
                .setCredentialsProvider(credentialsProvider)
                .build())) {
            TopicName topicName = TopicName.of(projectId, topicId);
            topicAdminClient.createTopic(topicName);
        } catch (IOException e) {
            // topic already exists;
        }
    }

    @Bean
    public Publisher createPublisher() {
        if (!topicExists()) createTopic();
        TopicName topicName = TopicName.of(projectId, topicId);
        try {
            return Publisher
                    .newBuilder(topicName)
                    .setChannelProvider(channelProvider)
                    .setCredentialsProvider(credentialsProvider)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void createSubscription() {
        try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create()) {
            PushConfig pushConfig = PushConfig
                    .newBuilder()
                    .setPushEndpoint("http://localhost:8080/sub/subscription")
                    .build();
            SubscriptionName subscriptionName = SubscriptionName.of(projectId, "sub-id");
            TopicName topicName = TopicName.of(projectId, topicId);
            subscriptionAdminClient.createSubscription(subscriptionName, topicName, pushConfig, 60);
        } catch (IOException e) {
            // already exists;
        }
        ;
    }

    public PubsubManager() {
        this.channelProvider = FixedTransportChannelProvider.create(
                GrpcTransportChannel.create(
                        ManagedChannelBuilder.forTarget("localhost:8083").usePlaintext().build()));
        this.credentialsProvider = NoCredentialsProvider.create();
    }
}
