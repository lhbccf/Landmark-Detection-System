package client;

import io.grpc.stub.StreamObserver;
import servicestubs.ReturnFile;


public class ObserverUploadImage implements StreamObserver<ReturnFile> {

    /*@Override
    public void onNext() {

        StorageOptions    storageOptions = StorageOptions.getDefaultInstance();
        Storage storage = storageOptions.getService();
        if(forumMessage.getTxtMsg().contains(";")){
            String[] downloadContent = forumMessage.getTxtMsg().split(";");
            System.out.println("\n[" + forumMessage.getTopicName() + "] " +
                    forumMessage.getFromUser() + ": " + downloadContent[0] +
                    "--> downloading " + downloadContent[2]);

            BlobId blobId = BlobId.of(downloadContent[1], downloadContent[2]);
            Blob blob = storage.get(blobId);
            Path downloadTo = Paths.get("." + downloadContent[2]);

            if (blob == null) {
                System.out.println("No such Blob exists !");
                return;
            }
            try  (PrintStream writeTo = new PrintStream(Files.newOutputStream(downloadTo))) {
                if (blob.getSize() < 1_000_000) {
                    // Blob is small read all its content in one request
                    byte[] content = blob.getContent();
                    writeTo.write(content);
                } else {
                    // When Blob size is big or unknown use the blob's channel reader.
                    try (ReadChannel reader = blob.reader()) {
                        WritableByteChannel channel = Channels.newChannel(writeTo);
                        ByteBuffer bytes = ByteBuffer.allocate(64 * 1024);
                        while (reader.read(bytes) > 0) {
                            bytes.flip();
                            channel.write(bytes);
                            bytes.clear();
                        }
                    }
                }
            } catch (Exception e){
                System.out.println(e);
            }

        } else {
            System.out.println("\n[" + forumMessage.getTopicName() + "] " +
                    forumMessage.getFromUser() + ": " + forumMessage.getTxtMsg());
        }
    }*/

    @Override
    public void onNext(ReturnFile value) {
        System.out.println("Bucket/Blob IDs: " + value.getFileId());
    }

    @Override
    public void onError(Throwable throwable) {
        System.err.println("Error in subscription: " + throwable.getMessage());
    }

    @Override
    public void onCompleted() {
        System.out.println("Subscription ended");
    }
}

