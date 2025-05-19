package trabfinal;

import com.google.cloud.vision.v1.*;
import com.google.type.LatLng;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;




public class LandmarkDetector {
    final static int ZOOM = 15; // Streets
    final static String SIZE = "600x300";
    // Considera-se que os nomes de imagens correspondem aos nomes de BLOB
    // existentes num bucket de nome BUCKET_NAME no Storage do Projeto
    final static String BUCKET_NAME="cn2425tf_g06";
    static String[] images = {
            "TorreBelem.jpg", "CristoRei-Almada.jpg",
            "TajMahal.jpg",
            "Machu_Picchu_Peru.jpg",
            "Eiffel-tower.jpg",
            "Petra.jpg",
            "Sidney_Opera.jpg",
            "TorreBelem-v2.jpg",
            "torre-pizza-3.jpg"
    };

    // A variável de ambiente GOOGLE_APPLICATION_CREDENTIALS
    // deve ter conta de serviço com as roles: Storage Admin + VisionAI Admin

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("API Key missing");
            System.out.println("Usage: java -jar LandmarkDetector.jar <API_KEY>");
            System.exit(1);
        }
        detectAllLandmarksGcs(args[0]);
    }

    public static void detectAllLandmarksGcs(String apiKey) throws IOException {
        for (String name : images) {
            String blobGsPath = "gs://"+BUCKET_NAME+"/" + name;
            detectLandmarksGcs(blobGsPath, apiKey);
        }
    }

    // Detects landmarks in the specified remote image on Google Cloud Storage.
    public static void detectLandmarksGcs(String blobGsPath, String apiKey) throws IOException {
        System.out.println("Detecting landmarks for: " + blobGsPath);
        List<AnnotateImageRequest> requests = new ArrayList<>();

        ImageSource imgSource = ImageSource.newBuilder().setGcsImageUri(blobGsPath).build();
        Image img = Image.newBuilder().setSource(imgSource).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.LANDMARK_DETECTION).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
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