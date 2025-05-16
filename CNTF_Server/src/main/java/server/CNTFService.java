package server;

import com.google.cloud.storage.*;
import io.grpc.stub.StreamObserver;
import servicestubs.*;

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
        } else {
            System.out.println("Blob name already exists");
        }
    }

}