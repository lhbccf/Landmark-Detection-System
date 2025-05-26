package trabfinal;

import com.google.cloud.pubsub.v1.*;
import com.google.pubsub.v1.*;
import java.io.IOException;

import static trabfinal.LandmarkDetector.init;

public class LandmarkWorker {

    public static void main(String[] args) throws Exception {
        String projectId = "cn2425-t4-g06";
        String subscriptionId = "cn2425tf_t4_g6_topic-sub";
        init("cn2425-t4-g06-8d582a570f0f.json","cn2425-t4-g06","landmarks-info");
        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId);

        Subscriber subscriber = Subscriber.newBuilder(subscriptionName, new MessageReceiver() {
            @Override
            public void receiveMessage(PubsubMessage message, AckReplyConsumer consumer) {
                String bucket = message.getAttributesOrDefault("bucket_id", null);
                String blob = message.getAttributesOrDefault("blob_id", null);
                String requestId = message.getAttributesOrDefault("request_id", null);

                if (bucket == null || blob == null) {
                    //TODO: retornar mensagem de erros
                    consumer.ack();

                    return;
                }

                System.out.println("Mensagem recebida: bucket=" + bucket + ", blob=" + blob + ", requestId=" + requestId);
                String gsPath = "gs://" + bucket + "/" + blob;

                try {
                    LandmarkDetector.detectLandmarksSaveFirestore(gsPath, requestId);
                } catch (IOException e) {
                    System.err.println("Erro ao processar imagem: " + e.getMessage());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                //consumer.ack();
            }
        }).build();

        subscriber.startAsync().awaitRunning();
        subscriber.awaitTerminated();
    }
}
