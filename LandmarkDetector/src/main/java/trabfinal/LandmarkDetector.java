package trabfinal;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import com.google.cloud.firestore.*;
import com.google.api.core.ApiFuture;

public class LandmarkDetector {
    static Firestore db;
    static String currentCollection;

    public static void init(String pathFileKeyJson, String currentDatabase, String collectionName) throws Exception {
        GoogleCredentials credentials = null;
        if (pathFileKeyJson != null) {
            InputStream serviceAccount = new FileInputStream(pathFileKeyJson);
            credentials = GoogleCredentials.fromStream(serviceAccount);
        } else {
            // use GOOGLE_APPLICATION_CREDENTIALS environment variable
            credentials = GoogleCredentials.getApplicationDefault();
        }
        FirestoreOptions options = FirestoreOptions
                //.newBuilder().setCredentials(credentials).build();
                .newBuilder().setCredentials(credentials).setDatabaseId(currentDatabase).build();
        db = options.getService();
        currentCollection = collectionName;
    }

    public static void createNewDocFirestore(LandmarksInfo info, String requestId) throws Exception {
        CollectionReference colRef = db.collection(currentCollection);
        DocumentReference docRef = colRef.document(requestId);
        ApiFuture<WriteResult> resultFut = docRef.set(info);
        WriteResult result = resultFut.get();
        System.out.println("Update time : " + result.getUpdateTime());
    }

    public  static void detectLandmarksSaveFirestore (String blobGsPath, String requestId) throws IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();

        ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(blobGsPath).build();
        Image img = Image.newBuilder().setSource(imgSource).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.LANDMARK_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    System.out.format("Error: %s%n", res.getError().getMessage());
                    return;
                }

                System.out.println("Landmarks list size: " + res.getLandmarkAnnotationsList().size());
                for (EntityAnnotation annotation : res.getLandmarkAnnotationsList()) {
                    LocationInfo info = annotation.getLocationsList().listIterator().next();

                    LandmarksInfo landmarkInfo = new LandmarksInfo();
                    landmarkInfo.description = annotation.getDescription();
                    landmarkInfo.score = annotation.getScore();
                    landmarkInfo.latitude = info.getLatLng().getLatitude();
                    landmarkInfo.longitude = info.getLatLng().getLongitude();

                    try {
                        createNewDocFirestore(landmarkInfo, requestId);
                    } catch (Exception e) {
                        System.err.println("Error saving to Firestore: " + e.getMessage());

                    }

                    System.out.format("Landmark: %s(%f)%n %s%n",
                            annotation.getDescription(),
                            annotation.getScore(),
                            info.getLatLng());
                }
            }
        }
    }
}