
import com.google.cloud.storage.*;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import servicestubs.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class CN_Client {
    // generic ClientApp for Calling a grpc Service
    private static String svcIP = "0.0.0.0";//"34.175.151.247";
    private static int svcPort = 8000;
    private static ManagedChannel channel;
    private static cn2425tfGrpc.cn2425tfBlockingStub blockingStub;
    private static cn2425tfGrpc.cn2425tfStub noBlockStub;
    private static Storage storage;
    private static String username = "empty-user";

    public static void main(String[] args) {
        try {
            if (args.length == 2) {
                svcIP = args[0]; svcPort = Integer.parseInt(args[1]);
            }
            System.out.println("connect to " + svcIP + ":" + svcPort);
            channel = ManagedChannelBuilder.forAddress(svcIP, svcPort)
                    // Channels are secure by default (via SSL/TLS).
                    // For the example we disable TLS to avoid
                    // needing certificates.
                    .usePlaintext()
                    .build();
            blockingStub = cn2425tfGrpc.newBlockingStub(channel);
            noBlockStub = cn2425tfGrpc.newStub(channel);

            // Obtain current user
            //Scanner scanner = new Scanner(System.in);
            //System.out.print("Enter your username: ");
            //username = scanner.nextLine();

            // Call service operations for example ping server

            boolean end = false;
            while (!end) {
                try {
                    int option = Menu();
                    switch (option) {
                        case 1:
                            uploadImage();
                            break;
                        case 99:  System.exit(0);
                    }
                } catch (Exception ex) {
                    System.out.println("Execution call Error  !");
                    ex.printStackTrace();
                }
            }
            read("prima enter to end", new Scanner(System.in));
        } catch (Exception ex) {
            System.out.println("Unhandled exception");
            ex.printStackTrace();
        }
    }

    static void uploadImage() {
        Scanner scanner = new Scanner(System.in);


        System.out.print("Enter image path to upload: ");
        String imagePath = scanner.nextLine().trim();
        String imageName = imagePath;

        if(!imagePath.contains("./")){
            imagePath = "./" + imagePath;
        }

        System.out.println("\nUploading image...");

        Path uploadPath = Paths.get(imagePath);

        //File big
        StreamObserver<ImageBlock> streamBlocks = noBlockStub.uploadImage(
                new StreamObserver<ReturnFile>() {
                    @Override
                    public void onNext(ReturnFile value) {
                        System.out.println("Block sent");
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

        // When Blob size is big or unknown use the blob's channel reader.
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

    private static int Menu() {
        int op;
        Scanner scan = new Scanner(System.in);
        do {
            System.out.println();
            System.out.println("    MENU");
            System.out.println(" 1 - Upload Image");
            System.out.println("99 - Exit");
            System.out.println();
            System.out.println("Choose an Option?");
            op = scan.nextInt();
        } while (!((op >= 1 && op <= 1) || op == 99));
        return op;
    }

    private static String read(String msg, Scanner input) {
        System.out.println(msg);
        return input.nextLine();
    }

}

