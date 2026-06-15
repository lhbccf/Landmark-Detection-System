# LandmarkDetector

Long-running worker service that subscribes to a Google Cloud Pub/Sub topic, runs landmark detection on uploaded images using the Cloud Vision API, and persists results to Firestore.

## How It Fits In

The server publishes a Pub/Sub message after each successful image upload. This worker receives those messages, processes images asynchronously, and writes the detection result back to Firestore so the server can serve it to the client.

```
Pub/Sub (cn2425tf_t4_g6_topic-sub)
        │
        ▼
  LandmarkWorker
  ├── createNewDocFirestore()   ← placeholder doc (score=0) written immediately
  └── detectLandmarksSaveFirestore()
            │
            ▼
      Cloud Vision API
            │
            ▼
      Firestore (landmarks-info / <requestId>)
```

## Key Classes

| Class | Role |
|---|---|
| `trabfinal.LandmarkWorker` | Entry point; sets up the Pub/Sub subscriber and processes each message |
| `trabfinal.LandmarkDetector` | Core detection logic; calls Vision API and writes to Firestore |
| `trabfinal.LandmarksInfo` | Plain data model (`description`, `score`, `latitude`, `longitude`) |

## Message Processing Flow (`LandmarkWorker`)

Each Pub/Sub message carries three attributes published by the server:

| Attribute | Example value | Description |
|---|---|---|
| `bucket_id` | `cn2425tf_g06` | Cloud Storage bucket containing the image |
| `blob_id` | `eiffel_tower.jpg` | Blob name (original filename) |
| `request_id` | `-1203948201` | Integer hash used as the Firestore document key |

Processing steps per message:

1. **Placeholder write** — `createNewDocFirestore()` immediately creates a Firestore document with `score=0` and `description=<blobName>`. This lets the server return a "not ready" response rather than a "not found" error if the client queries too soon.
2. **Detection** — `detectLandmarksSaveFirestore()` calls the Vision API with the GCS path (`gs://bucket/blob`).
3. **Result write** — for each detected landmark, overwrites the Firestore document with the real `description`, `score`, `latitude`, and `longitude`.
4. **Ack** — `consumer.ack()` is called unconditionally after processing so the message is not re-delivered.

## Detection Logic (`LandmarkDetector`)

- Builds an `AnnotateImageRequest` with feature type `LANDMARK_DETECTION` pointing at the GCS URI.
- Sends it to `ImageAnnotatorClient` (Vision API).
- Iterates the response's `landmarkAnnotationsList`; takes the **first location** from each annotation's location list.
- If multiple landmarks are returned, each one overwrites the Firestore document in turn (last write wins).
- If the Vision API returns an error for the image, logs it and returns without writing — the placeholder document with `score=0` remains.

## GCP Resources

| Resource | Value |
|---|---|
| Pub/Sub subscription | `cn2425tf_t4_g6_topic-sub` |
| Cloud Storage (read) | `cn2425tf_g06` |
| Firestore database | `cn2425-t4-g06` |
| Firestore collection | `landmarks-info` |
| GCP project | `cn2425-t4-g06` |

## Prerequisites

- Java 21, Maven 3.x
- Service account with roles: **Storage Object Viewer**, **Pub/Sub Subscriber**, **Cloud Datastore User**, **Cloud Vision API User**
- `GOOGLE_APPLICATION_CREDENTIALS` pointing to the service account JSON
- The Pub/Sub subscription `cn2425tf_t4_g6_topic-sub` must exist and be attached to topic `cn2425tf_t4_g6_topic`

## Build

```bash
mvn clean package -f LandmarkDetector/pom.xml
```

Produces `target/LandmarkDetector-1.0-jar-with-dependencies.jar`.

## Run

```bash
java -jar LandmarkDetector/target/LandmarkDetector-1.0-jar-with-dependencies.jar
```

No command-line arguments are needed. The worker blocks indefinitely, printing each received message:

```
Mensagem recebida: bucket=cn2425tf_g06, blob=eiffel_tower.jpg, requestId=-1203948201
Update time : 2025-01-15T10:23:44Z
Landmarks list size: 1
Landmark: Eiffel Tower(0.902148)
 latitude: 48.858372
 longitude: 2.294481
Update time : 2025-01-15T10:23:45Z
```

Stop with `Ctrl+C`.
