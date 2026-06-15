# CNTF_IPLookup

Google Cloud Function that returns the external IP addresses of all running gRPC server instances in a managed instance group. Used by the client at startup to discover available servers dynamically without hardcoding IPs.

## How It Fits In

```
Client startup
    │
    ▼
HTTP GET → funcHttpIPlookup (Cloud Function)
    │
    ▼
Compute Engine API — list instances in zone
    │
    ▼
Filter: status == RUNNING && name contains instanceGroup
    │
    ▼
JSON array of external IPs → Client
```

## Implementation

**Class:** `trabfinal.CNTFIPLookup` implements `com.google.cloud.functions.HttpFunction`

The function:
1. Reads query parameters from the incoming HTTP GET request (with defaults for all three).
2. Creates an `InstancesClient` using Application Default Credentials.
3. Lists all instances in the given project and zone.
4. Keeps only instances whose status is `"RUNNING"` and whose name contains the `instanceGroup` string.
5. Extracts the NAT (external) IP from `networkInterfaces[0].accessConfigs[0].natIP`.
6. Returns the IP list serialised as a JSON array (via Gson).

## HTTP Interface

**Method:** `GET`

**Query Parameters:**

| Parameter | Default | Description |
|---|---|---|
| `projectID` | `cn2425-t4-g06` | GCP project ID |
| `zone` | `europe-southwest1-a` | Compute Engine zone to search |
| `instanceGroup` | `group-grpc-server` | Substring that instance names must contain |

**Response:** `application/json` — a JSON array of IP strings, or an empty array `[]` if no matching running instances are found.

**Example response:**
```json
["34.175.240.184", "34.175.112.93"]
```

## Deployed Endpoint

```
https://europe-west1-cn2425-t4-g06.cloudfunctions.net/funcHttpIPlookup
```

Called by the client with the full parameter set:
```
?projectID=cn2425-t4-g06&zone=europe-southwest1-a&instanceGroup=group-grpc-server
```

## Prerequisites

- Java 21, Maven 3.x
- GCP project `cn2425-t4-g06` with the Cloud Functions API and Compute Engine API enabled
- Service account attached to the Cloud Function with role: **Compute Viewer**

## Build

```bash
mvn clean package -f CNTF_IPLookup/pom.xml
```

Produces `target/CNTFIPLookup-1.0.jar` plus a dependency JAR, ready for Cloud Functions deployment.

## Deploy to Google Cloud Functions

```bash
gcloud functions deploy funcHttpIPlookup \
  --entry-point trabfinal.CNTFIPLookup \
  --runtime java21 \
  --trigger-http \
  --allow-unauthenticated \
  --region europe-west1 \
  --source CNTF_IPLookup/target/deployment
```

> The Maven build (using `maven-dependency-plugin`) copies all dependency JARs into `target/deployment/` alongside the function JAR, which is the directory structure expected by the Cloud Functions runtime.

## Local Testing

You can invoke the deployed function with `curl`:

```bash
curl "https://europe-west1-cn2425-t4-g06.cloudfunctions.net/funcHttpIPlookup\
?projectID=cn2425-t4-g06&zone=europe-southwest1-a&instanceGroup=group-grpc-server"
```

Expected output when servers are running:
```json
["34.175.240.184"]
```

Expected output when no instances are running:
```json
[]
```
