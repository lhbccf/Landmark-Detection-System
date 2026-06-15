# CNTF_Contract

Shared gRPC service contract for the Landmark Detection System. This module compiles a single `.proto` file into Java stubs that are consumed by both **CNTF_Server** and **CNTF_Client**. It must be built and installed into the local Maven repository before any other module.

## Service Definition

**Package / Java package:** `cn2425tf` / `servicestubs`

### RPCs

| Method | Type | Description |
|---|---|---|
| `uploadImage` | Client-streaming | Client streams image chunks; server returns a single request ID |
| `obtainImageInformation` | Unary | Client sends a request ID; server returns landmark metadata from Firestore |
| `retrieveImageMapLocation` | Server-streaming | Client sends a request ID; server streams a PNG map image in chunks |

### Messages

**`ImageBlock`** — used to stream binary image data in both directions.

| Field | Type | Description |
|---|---|---|
| `imageName` | string | Original filename |
| `imageType` | string | MIME type (e.g. `image/jpeg`) |
| `dataBlock` | bytes | Raw image chunk (8 KB by default) |

**`RequestInformation`** — a handle that ties an upload to its detection result.

| Field | Type | Description |
|---|---|---|
| `requestId` | int32 | Hash code of the Cloud Storage blob ID, used as Firestore document key |

**`ImageInformation`** — landmark detection result returned to the client.

| Field | Type | Description |
|---|---|---|
| `description` | string | Human-readable landmark name |
| `latitude` | double | Geographic latitude |
| `longitude` | double | Geographic longitude |
| `score` | double | Detection confidence (0.0 – 1.0) |

## Build

```bash
mvn clean install -f CNTF_Contract/pom.xml
```

`install` (not just `package`) is required so the artifact `isel.grpc:cn2425tf:1.0` is placed in the local Maven cache and picked up by the server and client builds.

## How Stubs Are Generated

The build uses `protobuf-maven-plugin` (0.6.1) with:
- **protoc** `3.25.5` — compiles `.proto` to Java message classes
- **protoc-gen-grpc-java** `1.70.0` — generates the blocking and async stub classes (`cn2425tfGrpc`)

Generated sources are written to `target/generated-sources/protobuf/java/` and `target/generated-sources/protobuf/grpc-java/` and compiled into the final JAR automatically.

## Proto File Location

[CNTF_Contract/src/main/proto/CNTF_Contract.proto](src/main/proto/CNTF_Contract.proto)
