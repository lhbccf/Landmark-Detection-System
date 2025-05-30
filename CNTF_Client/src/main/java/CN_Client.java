
import com.google.cloud.storage.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.StreamObserver;
import servicestubs.*;

import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class CN_Client {
    private static String svcIP = "0.0.0.0";
    private static int svcPort = 8000;
    private static ManagedChannel channel;
    private static cn2425tfGrpc.cn2425tfBlockingStub blockingStub;
    private static cn2425tfGrpc.cn2425tfStub noBlockStub;

    public static void main(String[] args) {
        try {
            if (args.length == 2)
                svcPort = Integer.parseInt(args[0]);

            if (!connectToServer()) {
                System.out.println("Could not establish a connection!");
                return;
            }

            boolean end = false;
            while (!end) {
                try {
                    int option = Menu();
                    switch (option) {
                        case 1:
                            uploadImage();
                            break;
                        case 2:
                            obtainImageInformation();
                            break;
                        case 3:
                            retrieveMapImage();
                            break;
                        case 99:  System.exit(0);
                    }
                } catch (Exception ex) {
                    System.out.println("Execution call Error!");
                    ex.printStackTrace();
                }
            }
            read("prima enter to end", new Scanner(System.in));
        } catch (Exception ex) {
            System.out.println("Unhandled exception");
        }
    }

    private static boolean connectToServer() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println();
            System.out.println("    MENU");
            System.out.println("1 - Select IP from the list of available instances");
            System.out.println("2 - Select a random IP");
            System.out.println();
            System.out.println("Choose an Option?");

            try {
                int choice = scanner.nextInt();
                List<String> availableIPs = null;

                switch (choice) {
                    case 1:
                        availableIPs = getInstanceIp();
                        if (availableIPs == null || availableIPs.isEmpty()) {
                            System.out.println("Could not retrieve IP list or the list is empty.");
                            continue;
                        }
                        svcIP = selectIPFromList(availableIPs, scanner);
                        break;

                    case 2:
                        availableIPs = getInstanceIp();
                        if (availableIPs == null || availableIPs.isEmpty()) {
                            System.out.println("Could not retrieve IP list or the list is empty.");
                            continue;
                        }
                        svcIP = selectRandomIP(availableIPs);
                        break;

                    default:
                        System.out.println("Invalid option!");
                        continue;
                }

                if (establishConnection(svcIP)) {
                    return true;
                }

                return false;

            } catch (Exception ex) {
                System.out.println("Unhandled exception");
            }
        }
    }

    private static String selectIPFromList(List<String> availableIPs, Scanner scanner) {
        System.out.println();

        for (int i = 0; i < availableIPs.size(); i++) {
            System.out.println((i + 1) + " - " + availableIPs.get(i));
        }

        while (true) {
            System.out.println();
            System.out.println("Select the IP: ");
            try {
                int selection = scanner.nextInt();
                if (selection >= 1 && selection <= availableIPs.size()) {
                    return availableIPs.get(selection - 1);
                } else {
                    System.out.println("Invalid selection!");
                }
            } catch (Exception e) {
                System.out.println("Invalid input!");
                scanner.nextLine();
            }
        }
    }

    private static String selectRandomIP(List<String> availableIPs) {
        Random random = new Random();
        String selectedIP = availableIPs.get(random.nextInt(availableIPs.size()));
        System.out.println("Selected IP: " + selectedIP);
        return selectedIP;
    }

    private static boolean establishConnection(String ip) {
        try {
            System.out.println("Conectando a " + ip + ":" + svcPort);

            channel = ManagedChannelBuilder.forAddress(ip, svcPort)
                    .usePlaintext()
                    .build();

            blockingStub = cn2425tfGrpc.newBlockingStub(channel);
            noBlockStub = cn2425tfGrpc.newStub(channel);

            return true;

        } catch (Exception e) {
            System.out.println("Erro ao estabelecer conexão: " + e.getMessage());
            return false;
        }
    }

    static void uploadImage() {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter image path to upload: ");
        String imagePath = scanner.nextLine().trim();
        String[] imagePathSplit = imagePath.split("/");
        String imageName = imagePathSplit[imagePathSplit.length - 1];

        if(imagePath.startsWith("/")){
            imagePath = "." + imagePath;
        } else if(!imagePath.startsWith("./")){
            imagePath = "./" + imagePath;
        }
        System.out.println("Path: " + imagePath + "\nName: " + imageName);


        System.out.println("\nUploading image...");

        Path uploadPath = Paths.get(imagePath);

        StreamObserver<ImageBlock> streamBlocks = noBlockStub.uploadImage(
                new StreamObserver<>() {
                    @Override
                    public void onNext(RequestInformation value) {
                        System.out.println("\nRequest Id: " + value.getRequestId());
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println("Error on upload");
                    }

                    @Override
                    public void onCompleted() {
                        System.out.println("Image sent successfully");
                    }
                }
        );

        try (FileInputStream inStream = new FileInputStream(uploadPath.toFile())){
            byte[] buffer = new byte[8192];
            while(inStream.read(buffer) > 0){
                streamBlocks.onNext(
                        ImageBlock.newBuilder()
                                .setImageName(imageName)
                                .setImageType(Files.probeContentType(uploadPath))
                                .setDataBlock(ByteString.copyFrom(buffer))
                                .build()
                );
                buffer = new byte[8192];
            }
            streamBlocks.onCompleted();
        } catch (Exception e) {
            streamBlocks.onError(e);
            System.out.println(e.getMessage());
        }

    }

    static void obtainImageInformation(){
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter request id: ");
        String requestId = scanner.nextLine().trim();

        try {
            int documentId = Integer.parseInt(requestId);
            ImageInformation imageInformation = blockingStub.obtainImageInformation(
                    RequestInformation.newBuilder()
                            .setRequestId(documentId)
                            .build()
            );

            System.out.println("\n" + imageInformation.toString() + "\n");

        } catch (Exception e){

            System.out.println("Id not found/incorrect");
        }
    }

    static void retrieveMapImage() {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter request id to get map image: ");
        String requestId = scanner.nextLine().trim();
        final String[] imageType = {""};
        final String[] imageName = {""};
        final boolean[] isFirst = {false};


        try{
            final FileOutputStream[] os = {null};
            int documentId = Integer.parseInt(requestId);

            System.out.println("Requesting map image...");
            noBlockStub.retrieveImageMapLocation(
                    RequestInformation.newBuilder()
                            .setRequestId(documentId)
                            .build(), new StreamObserver<>() {
                        @Override
                        public void onNext(ImageBlock value) {
                            if (!isFirst[0]) {
                                imageType[0] = value.getImageType();
                                imageName[0] = value.getImageName();
                                isFirst[0] = true;
                                try {
                                    os[0] = new FileOutputStream(value.getImageName() + ".png");
                                } catch (FileNotFoundException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            try {
                                os[0].write(value.getDataBlock().toByteArray());
                            } catch (IOException e) {
                                System.out.println("Erro");
                            }
                        }

                        @Override
                        public void onError(Throwable t) {
                            System.out.println("Erro");
                        }

                        @Override
                        public void onCompleted() {
                            try {
                                os[0].close();
                                System.out.println("Download completed!");
                            } catch (Exception e) {
                                System.out.println("Erro");
                            }
                        }

                    }
            );

        } catch (NumberFormatException e) {
            System.out.println("Invalid request ID format");
        } catch (Exception e){
            System.out.println("Error retrieving map image: " + e.getMessage());
        }
    }

    public static List<String> getInstanceIp() {
        List<String> ipList = new ArrayList<>();

        try {
            String projectID = "cn2425-t4-g06";
            String zone = "europe-southwest1-a";
            String instanceGroup = "group-grpc-server";

            String cfURL = "https://europe-west1-cn2425-t4-g06.cloudfunctions.net/funcHttpIPlookup?" +
                            "projectID=" + projectID +
                            "&zone=" + zone +
                            "&instanceGroup=" + instanceGroup;

            HttpClient client = HttpClient.newBuilder().build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(cfURL))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String result = response.body();

                if (result.isEmpty()) {
                    return new ArrayList<>();
                } else {
                    Type listType = new TypeToken<List<String>>() {}.getType();
                    return new Gson().fromJson(result, listType);
                }
            } else {
                System.out.println("Error with: " + response.statusCode());
                return null;
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Error while retrieving IP list: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static int Menu() {
        int op;
        Scanner scan = new Scanner(System.in);
        do {
            System.out.println();
            System.out.println("    MENU");
            System.out.println(" 1 - Upload Image");
            System.out.println(" 2 - Obtain image information");
            System.out.println(" 3 - Retrieve map image");
            System.out.println("99 - Exit");
            System.out.println();
            System.out.println("Choose an Option?");
            op = scan.nextInt();
        } while (!((op >= 1 && op <= 3) || op == 99));
        return op;
    }

    private static String read(String msg, Scanner input) {
        System.out.println(msg);
        return input.nextLine();
    }

}

