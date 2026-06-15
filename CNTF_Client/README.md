# CNTF_Client

Interactive command-line client for the Landmark Detection System. Connects to a gRPC server, uploads images, retrieves landmark information, and downloads static map images.

## Key Class

`CN_Client` — single entry-point class containing all logic: server discovery, gRPC stub management, and the three operation handlers.

Two stubs are kept for the lifetime of the session:

| Stub | Type | Used for |
|---|---|---|
| `blockingStub` | `cn2425tfGrpc.cn2425tfBlockingStub` | `obtainImageInformation` (waits synchronously) |
| `noBlockStub` | `cn2425tfGrpc.cn2425tfStub` | `uploadImage`, `retrieveImageMapLocation` (async streaming) |

## Startup — Server Discovery

Before showing the main menu, the client must connect to a server. It queries the `CNTF_IPLookup` Cloud Function to get the list of live server IPs:

```
https://europe-west1-cn2425-t4-g06.cloudfunctions.net/funcHttpIPlookup
  ?projectID=cn2425-t4-g06
  &zone=europe-southwest1-a
  &instanceGroup=group-grpc-server
```

The response is a JSON array of IP strings. The user then chooses:

| Option | Behaviour |
|---|---|
| 1 — Select from list | Displays all available IPs; user picks one by number |
| 2 — Random IP | Picks one at random from the list |

Once an IP is chosen, a plaintext gRPC `ManagedChannel` is opened to `<ip>:<port>`.

## Main Menu Operations

### 1 — Upload Image

- Prompts for a local file path.
- Opens the file and reads it in **8 KB chunks**.
- Sends each chunk as an `ImageBlock` message via the async stub's client-streaming call `uploadImage`.
- On completion, the server responds with a `RequestInformation` containing the `requestId` — print this and keep it for the next two operations.

### 2 — Obtain Image Information

- Prompts for a `requestId` (integer printed after upload).
- Calls `obtainImageInformation` (blocking unary RPC).
- Prints the `ImageInformation` proto: landmark name, latitude, longitude, and confidence score.
- Returns an error if the document is not yet in Firestore (detection may still be running — retry after a few seconds).

### 3 — Retrieve Map Image

- Prompts for a `requestId`.
- Calls `retrieveImageMapLocation` (async server-streaming RPC).
- Reassembles the incoming `ImageBlock` chunks into a local PNG file named `<landmarkDescription>.png` in the working directory.
- Prints `Download completed!` when the stream ends.

## Sample Images

Pre-loaded landmark images are available in [CNTF_Client/images/](images/) (Eiffel Tower, Taj Mahal, etc.) and can be used directly as upload paths.

## Prerequisites

- Java 21, Maven 3.x
- `CNTF_Contract` installed in the local Maven cache (`mvn install`)
- Network access to the GCP project (`cn2425-t4-g06`) — the Cloud Function endpoint and the gRPC server must be reachable

No GCP credentials are required by the client itself; all cloud access is handled server-side.

## Build

```bash
mvn clean package -f CNTF_Client/pom.xml
```

Produces `target/cn2425tf_client-1.0-jar-with-dependencies.jar`.

## Run

```bash
java -jar CNTF_Client/target/cn2425tf_client-1.0-jar-with-dependencies.jar [PORT]
```

| Argument | Required | Default | Description |
|---|---|---|---|
| `PORT` | No | `8000` | gRPC server port |

### Typical Session

```
1 - Select IP from the list of available instances
2 - Select a random IP
Choose an Option? 2
Selected IP: 34.175.240.184
Conectando a 34.175.240.184:8000

    MENU
 1 - Upload Image
 2 - Obtain image information
 3 - Retrieve map image
99 - Exit
Choose an Option? 1
Enter image path to upload: images/eiffel_tower.jpg
Uploading image...
Request Id: -1203948201
Image sent successfully

Choose an Option? 2
Enter request id: -1203948201
description: "Eiffel Tower"
latitude: 48.858372
longitude: 2.294481
score: 0.9021
```
