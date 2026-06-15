# CN2425TF_T4_G06 — Landmark Detection System

A distributed image analysis system built on gRPC and Google Cloud Platform. Clients stream images to the server, which stores them in Cloud Storage and triggers asynchronous landmark detection via Cloud Vision API. Results are persisted in Firestore and returned to the client along with a map image generated from the Google Maps Static API.

## Architecture

```
Client ──gRPC stream──► Server ──► Cloud Storage
                           │              │
                           └──► Pub/Sub ──► LandmarkDetector (worker)
                                                    │
                                              Cloud Vision API
                                                    │
                                                Firestore
                           ▲
                    Client queries
                   landmark info / map
```

- **CNTF_Contract** — Protobuf service definition shared by client and server
- **CNTF_Server** — gRPC server; stores uploads and serves landmark results
- **CNTF_Client** — Interactive CLI client
- **LandmarkDetector** — Pub/Sub worker that runs Vision API and writes to Firestore
- **CNTF_IPLookup** — Google Cloud Function that returns live server IPs from an instance group

## Prerequisites

| Requirement | Notes |
|---|---|
| Java 21 | All modules target Java 21 |
| Maven 3.x | Build tool |
| Google Cloud credentials | Service account JSON with Storage, Pub/Sub, Firestore, Vision, Compute permissions |
| Google Maps API key | Required by the server to generate static map images |
| GCP project | `cn2425-t4-g06` (europe-southwest1-a) |

Set your service account credentials before running anything:

```bash
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json
```

## Build

Build **CNTF_Contract first** — it is a local dependency for the other modules.

```bash
mvn clean install -f CNTF_Contract/pom.xml
mvn clean package -f CNTF_Server/pom.xml
mvn clean package -f CNTF_Client/pom.xml
mvn clean package -f LandmarkDetector/pom.xml
mvn clean package -f CNTF_IPLookup/pom.xml   # only needed for Cloud Functions deployment
```

## Run

### Server

```bash
java -jar CNTF_Server/target/cn2425tf_server-1.0-jar-with-dependencies.jar <MAPS_API_KEY> [PORT]
```

- `MAPS_API_KEY` — Google Maps Static API key (required)
- `PORT` — defaults to `8000`

### Landmark Worker

Run on any machine (or GCP VM) with access to the GCP project:

```bash
java -jar LandmarkDetector/target/LandmarkDetector-1.0-jar-with-dependencies.jar
```

Subscribes to Pub/Sub topic `cn2425tf_t4_g6_topic-sub` and processes images continuously.

### Client

```bash
java -jar CNTF_Client/target/cn2425tf_client-1.0-jar-with-dependencies.jar [PORT]
```

- `PORT` — defaults to `8000`; server address is resolved via the `CNTF_IPLookup` Cloud Function

The interactive menu lets you:
1. Upload an image (streamed in 8 KB chunks)
2. Query landmark information by request ID
3. Download a map image of the detected landmark location

Sample images are provided in [CNTF_Client/images/](CNTF_Client/images/).
