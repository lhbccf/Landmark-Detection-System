package trabfinal;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.type.LatLng;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.google.cloud.firestore.*;
import com.google.api.core.ApiFuture;

public class LandmarkDetector {
    static Firestore db;
    static String currentCollection;
    final static int ZOOM = 15; // Streets
    final static String SIZE = "600x300";
    // Considera-se que os nomes de imagens correspondem aos nomes de BLOB
    // existentes num bucket de nome BUCKET_NAME no Storage do Projeto
    final static String BUCKET_NAME="cn2425tf_g06";
    static String[] images = {
            "CristoRei-Almada.jpg",
    };

    // A variável de ambiente GOOGLE_APPLICATION_CREDENTIALS
    // deve ter conta de serviço com as roles: Storage Admin + VisionAI Admin

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("API Key missing");
            System.out.println("Usage: java -jar LandmarkDetector.jar <API_KEY>");
            System.exit(1);
        }

        init("cn2425-t4-g06-8d582a570f0f.json","cn2425-t4-g06","landmarks-info");

        detectAllLandmarksGcs(args[0]);
    }

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

    public static void detectAllLandmarksGcs(String apiKey) throws IOException {
        for (String name : images) {
            String blobGsPath = "gs://"+BUCKET_NAME+"/" + name;
            detectLandmarksGcs(blobGsPath, apiKey, "");
        }
    }

    public static void createNewDocFirestore(LandmarksInfo info, String requestId) throws Exception {
        CollectionReference colRef = db.collection(currentCollection);
        DocumentReference docRef = colRef.document(requestId);
        ApiFuture<WriteResult> resultFut = docRef.set(info);
        WriteResult result = resultFut.get();
        System.out.println("Update time : " + result.getUpdateTime());
    }

    // Detects landmarks in the specified remote image on Google Cloud Storage.
    public static void detectLandmarksGcs(String blobGsPath, String apiKey, String requestId) throws IOException {
        System.out.println("Detecting landmarks for: " + blobGsPath);
        List<AnnotateImageRequest> requests = new ArrayList<>();

        ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(blobGsPath).build();
        Image img = Image.newBuilder().setSource(imgSource).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.LANDMARK_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);

        // Initialize client that will be used to send requests. This client only needs to be created
        // once, and can be reused for multiple requests. After completing all of your requests, call
        // the "close" method on the client to safely clean up any remaining background resources.
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    System.out.format("Error: %s%n", res.getError().getMessage());
                    return;
                }

                System.out.println("Landmarks list size: " + res.getLandmarkAnnotationsList().size());
                // For the full list of available annotations, see http://g.co/cloud/vision/docs
                boolean first = true; // Only get a map for the first annotation
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

                    if (first) {
                        getStaticMapSaveImage(info.getLatLng(), apiKey);
                        first = false;
                    }
                }
            }
        }
    }

    private static void getStaticMapSaveImage(LatLng latLng, String apiKey) {
        String mapUrl = "https://maps.googleapis.com/maps/api/staticmap?"
                + "center=" + latLng.getLatitude() + "," + latLng.getLongitude()
                + "&zoom=" + ZOOM
                + "&size=" + SIZE
                + "&key=" + apiKey;
        System.out.println(mapUrl);
        try {
            URL url = new URL(mapUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            InputStream in = conn.getInputStream();
            BufferedInputStream bufIn = new BufferedInputStream(in);
            FileOutputStream out = new FileOutputStream("static_map_"+ UUID.randomUUID() +".png");
            byte[] buffer = new byte[8*1024];
            int bytesRead;
            while ((bytesRead = bufIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            bufIn.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}