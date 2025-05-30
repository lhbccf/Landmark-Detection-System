package trabfinal;

import com.google.cloud.compute.v1.Instance;
import com.google.cloud.compute.v1.InstancesClient;
import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;
import com.google.gson.Gson;

import java.io.BufferedWriter;

import java.util.ArrayList;
import java.util.List;

public class CNTFIPLookup implements HttpFunction{

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {

        BufferedWriter writer = response.getWriter();
        String projectID = request.getFirstQueryParameter("projectID").orElse("cn2425-t4-g06");
        String zone = request.getFirstQueryParameter("zone").orElse("europe-southwest1-a");
        String instanceGroup = request.getFirstQueryParameter("instanceGroup").orElse("group-grpc-server");

        try (InstancesClient client = InstancesClient.create()) {

            List<String> ipList = new ArrayList<>();

            for (Instance instance : client.list(projectID, zone).iterateAll()) {
                if (instance.getStatus().compareTo("RUNNING") == 0 ) {
                    if(instance.getName().contains(instanceGroup)) {

                        String ip = instance.getNetworkInterfaces(0).getAccessConfigs(0).getNatIP();
                        ipList.add(ip);
                    }
                }
            }

            String json = new Gson().toJson(ipList);
            writer.write(json);
        }
    }
}
