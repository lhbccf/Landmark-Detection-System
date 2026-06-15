# CNTF_Server

gRPC server that accepts image uploads, stores them in Google Cloud Storage, triggers asynchronous landmark detection via Pub/Sub, and serves detection results and map images back to clients.

## Responsibilities

1. Receive streamed image uploads from clients and write them to Cloud Storage.
2. After a successful upload, publish a Pub/Sub message so the `LandmarkDetector` worker can process the image asynchronously.
3. Serve landmark metadata (read from Firestore) on demand.
4. Fetch a static map image from the Google Maps API and stream it back to the client.

## Key Classes

| Class | Role |
|---|---|
| `server.CNTFServer` | Entry point; builds and starts the gRPC server on the configured port |
| `server.CNTFService` | Implements all three gRPC service methods |
| `server.ShutdownHook` | Registered as a JVM shutdown hook; calls `server.shutdown()` for a graceful stop |
| `model.LandmarksInfo` | Plain data model used when deserialising Firestore documents |

## gRPC Methods — Implementation Detail

### `uploadImage` (client-streaming)

- Opens a Cloud Storage `WriteChannel` on the first received `ImageBlock` into bucket `cn2425tf_g06`.
- Appends each subsequent chunk to the same channel.
- On stream completion, closes the channel and computes `requestId` as `BlobId.toString().hashCode()`.
- Returns `RequestInformation(requestId)` to the client.
- Publishes a Pub/Sub message to topic `cn2425tf_t4_g6_topic` with attributes:
  - `request_id` — the integer hash used as the Firestore document key
  - `bucket_id` — `cn2425tf_g06`
  - `blob_id` — the blob name (original filename)

### `obtainImageInformation` (unary)

- Reads the Firestore document at collection `landmarks-info`, document ID = `requestId`.
- Returns `ImageInformation` with `description`, `latitude`, `longitude`, and `score` fields.
- Errors if the document does not exist (detection may not have finished yet).

### `retrieveImageMapLocation` (server-streaming)

- Reads the same Firestore document to get coordinates.
- Aborts with an error if `score == 0` (no landmark detected).
- Calls the Google Maps Static API (`zoom=15`, `size=600x300`) and streams the response PNG back as `ImageBlock` chunks of 8 KB.

## GCP Resources

| Resource | Value |
|---|---|
| Cloud Storage bucket | `cn2425tf_g06` |
| Pub/Sub topic | `cn2425tf_t4_g6_topic` |
| Firestore database | `cn2425-t4-g06` |
| Firestore collection | `landmarks-info` |
| GCP project | `cn2425-t4-g06` |

## Prerequisites

- Java 21, Maven 3.x
- `CNTF_Contract` installed in the local Maven cache (`mvn install`)
- Service account with roles: **Storage Object Admin**, **Pub/Sub Publisher**, **Cloud Datastore User**
- `GOOGLE_APPLICATION_CREDENTIALS` pointing to the service account JSON
- Google Maps Static API key

## Build

```bash
mvn clean package -f CNTF_Server/pom.xml
```

Produces `target/cn2425tf_server-1.0-jar-with-dependencies.jar` (fat JAR via Maven Assembly Plugin).

## Run

```bash
java -jar CNTF_Server/target/cn2425tf_server-1.0-jar-with-dependencies.jar <MAPS_API_KEY> [PORT]
```

| Argument | Required | Default | Description |
|---|---|---|---|
| `MAPS_API_KEY` | Yes | — | Google Maps Static API key |
| `PORT` | No | `8000` | TCP port the gRPC server listens on |

Expected startup output:
```
Service is available on port:8000
Server started on port 8000
```
