package server;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.storage.*;
import com.google.cloud.storage.Blob;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import io.grpc.stub.StreamObserver;
import model.Location;
import servicestubs.*;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

public class CNTFService extends cn2425tfGrpc.cn2425tfImplBase {
    static String PROJECT_ID = "cn2425-t4-g06";
    static String TOPIC_NAME = "cn2425tf_t4_g6_topic";
    static String SUBSCRIPTION_NAME = "cn2425tf_t4_g6_topic-sub";
    static Firestore db;
    static String currentDatabase = "cn2425-t4-g06";
    static String currentCollection = "landmarks-info";
    ConcurrentHashMap<Integer, Location> requestMap = new ConcurrentHashMap<>();

    public CNTFService(int svcPort) throws IOException {
        init(null, currentDatabase, currentCollection);
        System.out.println("Service is available on port:" + svcPort);
    }

    public static void init(String pathFileKeyJson, String currentDatabase, String collectionName) throws IOException {
        GoogleCredentials credentials = null;
        if (pathFileKeyJson != null) {
            InputStream serviceAccount = new FileInputStream(pathFileKeyJson);
            credentials = GoogleCredentials.fromStream(serviceAccount);
        } else {
            // use GOOGLE_APPLICATION_CREDENTIALS environment variable
            credentials = GoogleCredentials.getApplicationDefault();
        }
        FirestoreOptions options = FirestoreOptions
                .newBuilder().setCredentials(credentials).setDatabaseId(currentDatabase).build();
        db = options.getService();
        currentCollection = collectionName;
    }

    @Override
    public StreamObserver<ImageBlock> uploadImage(StreamObserver<RequestInformation> responseObserver){
        StorageOptions storageOptions = StorageOptions.getDefaultInstance();
        Storage storage = storageOptions.getService();
        ByteArrayOutputStream bytesReceived = new ByteArrayOutputStream();
        final String[] imageType = {""};
        final String[] imageName = {""};
        final boolean[] isFirst = {false};


        return new StreamObserver<ImageBlock>() {
            @Override
            public void onNext(ImageBlock value) {
                if(!isFirst[0]) {
                    imageType[0] = value.getImageType();
                    imageName[0] = value.getImageName();
                    isFirst[0] = true;
                }
                bytesReceived.writeBytes(value.getDataBlock().toByteArray());
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onCompleted() {
                bytesReceived.toByteArray();
                BlobId blobId = BlobId.of("cn2425tf_g06", imageName[0]);
                Blob blob = storage.get(blobId);
                int blobHashCode = 0;
                if (blob == null) {
                    try {
                        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                                .setContentType(imageType[0])
                                .build();
                        blob = storage.create(blobInfo, bytesReceived.toByteArray());
                        blobHashCode = blob.asBlobInfo().getBlobId().toString().hashCode();

                        RequestInformation response = RequestInformation.newBuilder()
                                .setRequestId(blobHashCode)
                                .build();

                        responseObserver.onNext(response);
                        responseObserver.onCompleted();

                        String pubSubMsg = "Message for Request: " + blobHashCode;

                        TopicName topicName = TopicName.ofProjectTopicName(PROJECT_ID, TOPIC_NAME);
                        Publisher publisher = Publisher.newBuilder(topicName).build();
                        ByteString msgData = ByteString.copyFromUtf8(pubSubMsg);
                        PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                                .setData(msgData)
                                .putAttributes("request_id", String.valueOf(blobHashCode))
                                .putAttributes("bucket_id", "cn2425tf_g06")
                                .putAttributes("blob_id", blobInfo.getName())
                                .build();
                        ApiFuture<String> future = publisher.publish(pubsubMessage);
                        String msgID = future.get();
                        System.out.println("Message Published with ID =" + msgID);
                        publisher.shutdown();




                    } catch(Exception e){
                        System.out.println("File not found");
                        responseObserver.onError(e);
                    }
                } else {
                    System.out.println("Blob name already exists");
                }

            }
        };
    }

    @Override
    public void obtainImageInformation(RequestInformation requestId, StreamObserver<ImageInformation> responseObserver) {
        Location location = null;
        try {
            DocumentReference docRef = db.collection(currentCollection).document(String.valueOf(requestId.getRequestId()));
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                System.out.println(document.getData().toString());
                location = document.toObject(Location.class);
                requestMap.put(requestId.getRequestId(), location);
                responseObserver.onNext(ImageInformation.newBuilder()
                        .setDescription(document.get("description").toString())
                        .setLatitude(Double.parseDouble(document.get("latitude").toString()))
                        .setLongitude(Double.parseDouble(document.get("latitude").toString()))
                        .setScore(Double.parseDouble(document.get("score").toString()))
                        .build()
                );
                responseObserver.onCompleted();
            } else {
                responseObserver.onError(new Exception("Document does not exist"));
                System.out.println("Document not exists");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
