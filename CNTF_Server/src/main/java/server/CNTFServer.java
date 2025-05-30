package server;

// generic ServerApp for hosting grpcService

import io.grpc.ServerBuilder;

public class CNTFServer {

    public static void main(String[] args) {
        try {
            String apiKey = "";
            int svcPort = 8000; // porta padrão

            if (args.length > 0) apiKey = args[0];

            if (args.length > 1) svcPort = Integer.parseInt(args[1]);

            io.grpc.Server svc = ServerBuilder.forPort(svcPort)
                    // Add one or more services.
                    // The Server can host many services in same TCP/IP port
                    .addService(new CNTFService(apiKey, svcPort))
                    .build();
            svc.start();
            System.out.println("Server started on port " + svcPort);
            // Java virtual machine shutdown hook
            // to capture normal or abnormal exits
            Runtime.getRuntime().addShutdownHook(new server.ShutdownHook(svc));
            // Waits for the server to become terminated
            svc.awaitTermination();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
