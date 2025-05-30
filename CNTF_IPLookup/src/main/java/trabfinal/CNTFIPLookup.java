package trabfinal;

import com.google.cloud.compute.v1.Instance;
import com.google.cloud.compute.v1.InstancesClient;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;

import java.io.BufferedWriter;

import java.util.logging.Logger;

public class CNTFIPLookup implements HttpFunction{
    private static final Logger logger = Logger.getLogger(CNTFIPLookup.class.getName());

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        BufferedWriter writer = response.getWriter();
        String projectID = "cn2425-t4-g06";
        String zone = request.getFirstQueryParameter("zone").orElse("europe-southwest1-a");
        //TODO: String instanceGroup =
        logger.info("Zone: " + zone);
        try (InstancesClient client = InstancesClient.create()) {
            for (Instance instance : client.list(projectID, zone).iterateAll()) {
                logger.info("instance: " + instance.getName());
                if (instance.getStatus().compareTo("RUNNING") == 0 ) {
                    if(instance.getName().contains("group-grpc-server")) {

                        logger.info("Response instance: " + instance.getName());
                        String ip = instance.getNetworkInterfaces(0).getAccessConfigs(0).getNatIP();
                        writer.write(ip+"\n");

                        //writer.write(" Last Start time: " + instance.getLastStartTimestamp()+"\n");
                        //writer.write(" IP: " + ip+"\n");
                    }
                }
            }
        }
    }
}
