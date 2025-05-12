package client;

import io.grpc.stub.StreamObserver;
import servicestubs.ReturnFile;


public class ObserverUploadImage implements StreamObserver<ReturnFile> {

    @Override
    public void onNext(ReturnFile value) {
        System.out.println("\nBucket/Blob IDs: " + value.getFileId());
    }

    @Override
    public void onError(Throwable throwable) {
        System.err.println("\nError in subscription: " + throwable.getMessage());
    }

    @Override
    public void onCompleted() {
        System.out.println("\nRequest finished");
    }
}

