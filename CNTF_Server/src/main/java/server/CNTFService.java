package server;

import com.google.cloud.ByteArray;
import com.google.cloud.storage.*;
import io.grpc.stub.StreamObserver;
import servicestubs.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class CNTFService extends cn2425tfGrpc.cn2425tfImplBase {

    public CNTFService(int svcPort) {
        System.out.println("Service is available on port:" + svcPort);
    }

    @Override
    public StreamObserver<ImageBlock> uploadImage(StreamObserver<ReturnFile> responseObserver){
        StorageOptions storageOptions = StorageOptions.getDefaultInstance();
        Storage storage = storageOptions.getService();
        ByteArrayOutputStream bytesReceived = new ByteArrayOutputStream();
        final String[] imageName = {""};
        final String[] imageType = {""};
        return new StreamObserver<ImageBlock>() {
            @Override
            public void onNext(ImageBlock value) {
                imageName[0] = value.getImageName();
                imageType[0] = value.getImageType();
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

                if (blob == null) {
                    try {
                        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                                .setContentType(imageType[0])
                                .build();
                        blob = storage.create(blobInfo, bytesReceived.toByteArray());

                        ReturnFile response = ReturnFile.newBuilder()
                                .setRequestId(blob.asBlobInfo().getBlobId().toString())
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
        };
    }

}