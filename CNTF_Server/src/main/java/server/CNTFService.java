package server;

import com.google.cloud.storage.*;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import servicestubs.*;

import java.nio.file.Files;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CNTFService extends cn2425tfGrpc.cn2425tfImplBase {

    public CNTFService(int svcPort) {
        System.out.println("Service is available on port:" + svcPort);
    }

    @Override
    public void uploadImage(ImageFile image, StreamObserver<ReturnFile> responseObserver){
        StorageOptions storageOptions = StorageOptions.getDefaultInstance();
        Storage storage = storageOptions.getService();

        BlobId blobId = BlobId.of("cn2425tf_g06", image.getImageName());
        Blob blob = storage.get(blobId);

        if (blob == null) {
            try {
                BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                        .setContentType(image.getImageType())
                        .build();
                blob = storage.create(blobInfo, image.getImageData().toByteArray());

                ReturnFile response = ReturnFile.newBuilder()
                        .setFileId(blob.asBlobInfo().getBlobId().toString())
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } catch(Exception e){
                System.out.println("File not found");
                responseObserver.onError(e);
            }
        }
    }

    /*
    @Override
    public void isAlive(ProtoVoid request, StreamObserver<TextMessage> responseObserver) {
        System.out.println("isAlive called!");
        responseObserver.onNext(TextMessage.newBuilder().setTxt("Service is alive").build());
        responseObserver.onCompleted();
    }

    @Override
    public void topicSubscribe(SubscribeUnSubscribe request, StreamObserver<ForumMessage> responseObserver) {
        String topicName = request.getTopicName();
        String usrName = request.getUsrName();

        System.out.println("User '" + usrName + "' subscribing to topic: " + topicName);

        ConcurrentMap<String, StreamObserver<ForumMessage>> usersInTopic =
                topicsMap.computeIfAbsent(topicName, k -> new ConcurrentHashMap<>());

        usersInTopic.put(usrName, responseObserver);

        System.out.println("User '" + usrName + "' successfully subscribed to topic: " + topicName);
    }

    @Override
    public void topicUnSubscribe(SubscribeUnSubscribe request, StreamObserver<Empty> responseObserver) {
        String topicName = request.getTopicName();
        String usrName = request.getUsrName();

        System.out.println("User '" + usrName + "' unsubscribing from topic: " + topicName);

        ConcurrentMap<String, StreamObserver<ForumMessage>> usersInTopic = topicsMap.get(topicName);

        if (usersInTopic != null) {
            StreamObserver<ForumMessage> observer = usersInTopic.remove(usrName);

            if (observer != null) {
                observer.onCompleted();
                System.out.println("User '" + usrName + "' unsubscribed from topic: " + topicName);

                if (usersInTopic.isEmpty()) {
                    topicsMap.remove(topicName);
                    System.out.println("Topic '" + topicName + "' removed as it has no subscribers");
                }
            } else {
                System.out.println("User '" + usrName + "' was not subscribed to topic: " + topicName);
            }
        } else {
            System.out.println("Topic '" + topicName + "' does not exist");
        }

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void getAllTopics(Empty request, StreamObserver<ExistingTopics> responseObserver) {
        System.out.println("Getting all topics");

        ExistingTopics topics = ExistingTopics.newBuilder()
                .addAllTopicName(topicsMap.keySet())
                .build();

        System.out.println("Returning " + topicsMap.size() + " topics");

        responseObserver.onNext(topics);
        responseObserver.onCompleted();
    }

    @Override
    public void publishMessage(ForumMessage request, StreamObserver<Empty> responseObserver) {
        String topicName = request.getTopicName();
        String fromUser = request.getFromUser();

        System.out.println("User '" + fromUser + "' publishing to topic: " + topicName);

        ConcurrentMap<String, StreamObserver<ForumMessage>> usersInTopic = topicsMap.get(topicName);

        if (usersInTopic == null) {
            responseObserver.onError(
                    Status.FAILED_PRECONDITION
                            .withDescription("Topic '" + topicName + "' does not exist")
                            .asRuntimeException()
            );
            return;
        }

        if (!usersInTopic.containsKey(fromUser)) {
            responseObserver.onError(
                    Status.PERMISSION_DENIED
                            .withDescription("User '" + fromUser + "' must subscribe to topic '" + topicName + "' before publishing messages")
                            .asRuntimeException()
            );
            return;
        }

        System.out.println("Publishing message from '" + fromUser + "' to topic '" + topicName +
                "' with " + usersInTopic.size() + " subscribers");

        for (StreamObserver<ForumMessage> observer : usersInTopic.values()) {
            try {
                observer.onNext(request);
            } catch (Exception e) {
                System.err.println("Error sending message to subscriber: " + e.getMessage());
            }
        }

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
    */
}