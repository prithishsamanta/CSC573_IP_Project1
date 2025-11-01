# Build and Run Instructions

This document provides exact commands to compile, run, and test the P2P-CI system.

## Prerequisites

- Java 11 or higher
- Apache Maven 3.6+
- Multiple terminal windows (for testing concurrent peers)

## Compilation

Clean and build the project using Maven:

```bash
mvn clean package
```

This will:
- Compile all Java source files
- Run unit tests (if any)
- Create compiled classes in `target/classes/`

## Running the System

### Step 1: Start the Central Index Server

Open a terminal window and run:

```bash
# Terminal 1 - Server
java -cp target/classes org.p2p.server.ServerMain
```

**Expected Output:**
```
Server started on port 7734
Waiting for peer connections...
```

The server will:
- Listen on port `7734` for peer registrations
- Accept multiple concurrent peer connections
- Log all peer activities (ADD, LOOKUP, LIST ALL)

---

### Step 2: Start First Peer

Open a new terminal window and run:

```bash
# Terminal 2 - Peer 1
java -cp target/classes org.p2p.peer.PeerMain \
  --serverHost 127.0.0.1 \
  --serverPort 7734 \
  --uploadPort 5001 \
  --rfcDir ./peer1_rfc \
  --os "Mac OS 14.5"
```

**Parameters:**
- `--serverHost`: Central server hostname/IP
- `--serverPort`: Server's listening port (default: 7734)
- `--uploadPort`: Port for this peer to accept P2P upload requests
- `--rfcDir`: Directory containing this peer's RFC files
- `--os`: Operating system string (included in ADD requests)

---

### Step 3: Start Second Peer

Open another terminal window:

```bash
# Terminal 3 - Peer 2
java -cp target/classes org.p2p.peer.PeerMain \
  --serverHost 127.0.0.1 \
  --serverPort 7734 \
  --uploadPort 5002 \
  --rfcDir ./peer2_rfc \
  --os "Ubuntu 22.04"
```

**Note:** Use a different `--uploadPort` for each peer on the same machine.

---

### Step 4: Start Third Peer (Optional - for Concurrency Demo)

```bash
# Terminal 4 - Peer 3
java -cp target/classes org.p2p.peer.PeerMain \
  --serverHost 127.0.0.1 \
  --serverPort 7734 \
  --uploadPort 5003 \
  --rfcDir ./peer3_rfc \
  --os "Windows 11"
```

---

## Example Interactive Session

Once peers are running, you can interact with them via their command-line interface:

### Example 1: LIST ALL RFCs

**Command (in any peer terminal):**
```
LIST ALL
```

**Server Response:**
```
P2P-CI/1.0 200 OK
RFC 1234 InternetDrafts.pdf peer1.example.com 5001
RFC 1234 InternetDrafts.pdf peer2.example.com 5002
RFC 5678 HTTPProtocol.pdf peer1.example.com 5001
RFC 9012 SecurityBasics.pdf peer3.example.com 5003
```

**Explanation:**
- Status: `200 OK` (successful)
- Shows all RFCs available across all peers
- Format: `RFC [number] [title] [hostname] [port]`

---

### Example 2: LOOKUP Specific RFC

**Command (in Peer 1):**
```
LOOKUP RFC 1234
```

**Server Response:**
```
P2P-CI/1.0 200 OK
RFC 1234 InternetDrafts.pdf peer1.example.com 5001
RFC 1234 InternetDrafts.pdf peer2.example.com 5002
```

**Explanation:**
- Status: `200 OK`
- RFC 1234 is available on both Peer 1 and Peer 2
- Peer can choose which source to download from

---

### Example 3: LOOKUP Non-Existent RFC

**Command:**
```
LOOKUP RFC 9999
```

**Server Response:**
```
P2P-CI/1.0 404 Not Found
```

**Explanation:**
- Status: `404 Not Found`
- No peer in the network has RFC 9999

---

### Example 4: GET RFC from Another Peer (P2P Transfer)

**Command (in Peer 3, requesting from Peer 1):**
```
GET RFC 1234 peer1.example.com 5001
```

**Peer 1 Response:**
```
P2P-CI/1.0 200 OK
Date: Mon, 01 Nov 2025 12:34:56 GMT
OS: Mac OS 14.5
Last-Modified: Fri, 15 Oct 2025 10:20:00 GMT
Content-Length: 245678
Content-Type: text/plain

[RFC 1234 content follows...]
```

**Explanation:**
- Direct P2P connection established between Peer 3 and Peer 1
- Status: `200 OK` (successful transfer)
- RFC content transferred directly (server not involved)
- Headers include metadata about the file

---

### Example 5: GET RFC Not Available on Target Peer

**Command (in Peer 3, requesting RFC that Peer 2 doesn't have):**
```
GET RFC 9012 peer2.example.com 5002
```

**Peer 2 Response:**
```
P2P-CI/1.0 404 Not Found
Date: Mon, 01 Nov 2025 12:35:10 GMT
OS: Ubuntu 22.04
```

**Explanation:**
- Status: `404 Not Found`
- Peer 2 doesn't have RFC 9012 locally

---

### Example 6: Malformed Request (400 Bad Request)

**Command:**
```
INVALID COMMAND
```

**Response:**
```
P2P-CI/1.0 400 Bad Request
```

**Explanation:**
- Status: `400 Bad Request`
- Invalid protocol syntax or unrecognized method

---

### Example 7: Version Not Supported (505)

**Command:**
```
GET RFC 1234 P2P-CI/2.0
```

**Response:**
```
P2P-CI/1.0 505 P2P-CI Version Not Supported
```

**Explanation:**
- Status: `505 Version Not Supported`
- System only supports P2P-CI/1.0

---

## Response Code Summary

| Code | Status Phrase | Meaning |
|------|---------------|---------|
| `200` | OK | Request successful |
| `400` | Bad Request | Malformed request syntax |
| `404` | Not Found | RFC or resource not found |
| `505` | P2P-CI Version Not Supported | Unsupported protocol version |

---

## Testing Concurrency

To demonstrate concurrent operation (grading requirement):

1. **Start server** (Terminal 1)
2. **Start 3+ peers** simultaneously (Terminals 2, 3, 4...)
3. **Execute commands in parallel:**
   - Peer 1: `LIST ALL`
   - Peer 2: `LOOKUP RFC 1234`
   - Peer 3: `GET RFC 5678 peer1.example.com 5001`

All operations should execute without blocking, demonstrating:
- Server handles multiple concurrent peer connections
- Peers can query and transfer simultaneously
- No race conditions or deadlocks

---

## Automatic Cleanup Demo

1. **Start server and 2 peers**
2. **Peer 1 adds RFCs** to server registry
3. **Peer 2 does LIST ALL** - sees Peer 1's RFCs
4. **Terminate Peer 1** (Ctrl+C or close terminal)
5. **Peer 2 does LIST ALL again** - Peer 1's RFCs should be gone

**Expected Behavior:**
- Server detects Peer 1 disconnection
- Server removes all Peer 1 entries from registry
- LIST ALL no longer shows Peer 1's resources

This demonstrates automatic cleanup on disconnect (grading requirement).

---

## Stopping the System

- **Stop Peers**: Press `Ctrl+C` or type `EXIT` command
- **Stop Server**: Press `Ctrl+C` in the server terminal

---

## Troubleshooting

### Port Already in Use
If you see "Address already in use":
```bash
# Find and kill process using the port
lsof -ti:7734 | xargs kill -9  # For server
lsof -ti:5001 | xargs kill -9  # For peer upload port
```

### Connection Refused
- Ensure server is running before starting peers
- Check firewall settings allow connections on specified ports
- Verify `--serverHost` points to correct IP/hostname

### Classes Not Found
- Run `mvn clean package` to recompile
- Ensure you're in the project root directory when running commands

---

## Grading Checklist

This system demonstrates all required features:

- ✅ **Server behavior**: Central registry, concurrent connections
- ✅ **Peer behavior**: Registration, discovery, P2P transfer
- ✅ **Concurrency**: Multiple peers operate simultaneously
- ✅ **Message formatting**: RFC 2616-style protocol
- ✅ **Response codes**: 200, 400, 404, 505
- ✅ **Automatic cleanup**: Disconnect handling
- ✅ **P2P transfer**: Direct GET between peers

Follow the examples above to demonstrate each feature during grading.

