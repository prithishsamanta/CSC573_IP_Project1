# Project Testing Guide - Part B (Peer Implementation)

This guide will help you verify that your project meets all the requirements for Part B.

## Prerequisites

1. **Build the project:**
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
   .\mvnw.cmd clean package
   .\mvnw.cmd package -Pserver
   ```

2. **Verify JARs exist:**
   ```powershell
   Get-ChildItem target\*.jar
   ```
   Should show: `server.jar` and `peer.jar`

3. **Prepare test RFC files:**
   ```powershell
   # Create test directories
   New-Item -ItemType Directory -Force -Path peer1_rfc
   New-Item -ItemType Directory -Force -Path peer2_rfc
   New-Item -ItemType Directory -Force -Path peer3_rfc
   
   # Create sample RFC files (or copy real ones)
   # Example: Create rfc1234.txt in peer1_rfc
   "RFC 1234 - Internet Drafts Specification`nThis is a test RFC file." | Out-File -FilePath peer1_rfc\rfc1234.txt -Encoding UTF8
   "RFC 5678 - HTTP Protocol Specification`nThis is another test RFC file." | Out-File -FilePath peer1_rfc\rfc5678.txt -Encoding UTF8
   "RFC 1234 - Internet Drafts Specification`nThis is a test RFC file." | Out-File -FilePath peer2_rfc\rfc1234.txt -Encoding UTF8
   ```

---

## Test 1: Server Startup and Basic Connection

**Goal:** Verify server starts and accepts peer connections.

**Steps:**
1. Open Terminal 1 (Server):
   ```powershell
   java -jar target/server.jar
   ```
   
2. **Expected Output:**
   ```
   P2P-CI Server listening on port 7734
   ```

3. Open Terminal 2 (Peer 1):
   ```powershell
   java -jar target/peer.jar --serverHost 127.0.0.1 --serverPort 7734 --uploadPort 5001 --rfcDir ./peer1_rfc --os "Windows 11"
   ```

4. **Expected Output:**
   ```
   Starting peer with config:
     Server host   : 127.0.0.1
     Server port   : 7734
     RFC directory : C:\...\peer1_rfc
     Upload port   : 5001
     OS            : Windows 11
   Upload server listening on port 5001
   [P2SClient] Connected to server at 127.0.0.1:7734
   [PeerMain] Found X RFC file(s), registering with server...
   [PeerMain] Successfully registered RFC 1234: ...
   === P2P-CI Peer Commands ===
   >
   ```

5. **Check Server Terminal:**
   - Should show: `New peer connected: /127.0.0.1:xxxxx`
   - Should show: `Received from ...: ADD RFC ...`

**✅ PASS if:** Peer connects and registers RFCs automatically.

---

## Test 2: ADD Functionality (P2S Protocol)

**Goal:** Verify peers can register RFCs with the server.

**Steps:**
1. Server and Peer 1 should be running from Test 1.

2. **Check Server Terminal:**
   - Should show ADD requests for each RFC file
   - Should show: `Received from ...: ADD RFC X P2P-CI/1.0`

3. **Verify Registration:**
   - In Peer 1 terminal, type: `LIST ALL`
   - Should show all registered RFCs

**✅ PASS if:** 
- Server receives ADD requests
- RFCs appear in LIST ALL response
- Response format: `P2P-CI/1.0 200 OK` followed by RFC lines

---

## Test 3: LIST ALL Functionality

**Goal:** Verify LIST ALL returns all RFCs in the network.

**Steps:**
1. Start Peer 2 in Terminal 3:
   ```powershell
   java -jar target/peer.jar --serverHost 127.0.0.1 --serverPort 7734 --uploadPort 5002 --rfcDir ./peer2_rfc --os "Ubuntu 22.04"
   ```

2. In Peer 1 terminal, type:
   ```
   LIST ALL
   ```

3. **Expected Output:**
   ```
   [P2SClient] LIST response: P2P-CI/1.0 200 OK
   All RFCs in network (X total):
     RFC 1234 - ... @ 127.0.0.1:5001
     RFC 1234 - ... @ 127.0.0.1:5002
     RFC 5678 - ... @ 127.0.0.1:5001
   ```

4. **Verify Message Format:**
   - Status line: `P2P-CI/1.0 200 OK`
   - Data lines: `RFC <number> <title> <host> <port>`
   - Blank line at end

**✅ PASS if:**
- Shows all RFCs from all peers
- Correct message format
- Status code 200 OK

---

## Test 4: LOOKUP Functionality

**Goal:** Verify LOOKUP finds peers with specific RFC.

**Steps:**
1. In Peer 1 terminal, type:
   ```
   LOOKUP RFC 1234
   ```

2. **Expected Output:**
   ```
   [P2SClient] LOOKUP response: P2P-CI/1.0 200 OK
   Found 2 peer(s) with RFC 1234:
     RFC 1234 - ... @ 127.0.0.1:5001
     RFC 1234 - ... @ 127.0.0.1:5002
   ```

3. Test non-existent RFC:
   ```
   LOOKUP RFC 9999
   ```

4. **Expected Output:**
   ```
   [P2SClient] LOOKUP response: P2P-CI/1.0 404 Not Found
   [P2SClient] RFC 9999 not found
   No peers found with RFC 9999
   ```

**✅ PASS if:**
- Returns all peers with the RFC
- Returns 404 for non-existent RFCs
- Correct message format

---

## Test 5: GET (P2P Transfer) - Success Case (200 OK)

**Goal:** Verify P2P file transfer works correctly.

**Steps:**
1. Start Peer 3 in Terminal 4 (with empty or different RFC directory):
   ```powershell
   java -jar target/peer.jar --serverHost 127.0.0.1 --serverPort 7734 --uploadPort 5003 --rfcDir ./peer3_rfc --os "Mac OS 14.5"
   ```

2. In Peer 3 terminal, type:
   ```
   GET RFC 1234
   ```

3. **Expected Output:**
   ```
   Looking up RFC 1234...
   [P2SClient] LOOKUP response: P2P-CI/1.0 200 OK
   Found 2 peer(s) with RFC 1234:
     RFC 1234 - ... @ 127.0.0.1:5001
     RFC 1234 - ... @ 127.0.0.1:5002
   Downloading RFC 1234 from 127.0.0.1:5001...
   [P2PClient] Status: P2P-CI/1.0 200 OK
   [P2PClient] Header: Date: ...
   [P2PClient] Header: OS: Windows 11
   [P2PClient] Header: Last-Modified: ...
   [P2PClient] Header: Content-Length: ...
   [P2PClient] Header: Content-Type: text/plain
   [P2PClient] Saved RFC 1234 to C:\...\peer3_rfc\rfc1234.txt
   Successfully downloaded RFC 1234
   Registered RFC 1234 with server
   ```

4. **Verify:**
   - File exists in `peer3_rfc\rfc1234.txt`
   - File content matches original
   - Peer 3 now shows RFC 1234 in LIST ALL

**✅ PASS if:**
- File downloaded successfully
- All required headers present (Date, OS, Last-Modified, Content-Length, Content-Type)
- Status 200 OK
- File saved correctly
- RFC automatically registered after download

---

## Test 6: GET - Error Cases

### Test 6a: 404 Not Found

**Steps:**
1. In Peer 3, try to GET an RFC that doesn't exist on any peer:
   ```
   GET RFC 9999
   ```

2. **Expected Output:**
   ```
   Looking up RFC 9999...
   [P2SClient] LOOKUP response: P2P-CI/1.0 404 Not Found
   No peers found with RFC 9999
   ```

3. Or try GET from a peer that doesn't have the file (if you can manually specify peer):
   - This should return 404 from the peer's UploadServer

**✅ PASS if:** Returns 404 with proper message format.

### Test 6b: 400 Bad Request

**Steps:**
1. You can test this by sending malformed requests. The UploadServer should handle:
   - Invalid request format
   - Missing headers
   - Malformed request line

**Expected:** Server/Peer returns `P2P-CI/1.0 400 Bad Request`

**✅ PASS if:** Returns 400 for malformed requests.

### Test 6c: 505 Version Not Supported

**Steps:**
1. This requires sending a request with wrong protocol version (e.g., P2P-CI/2.0)
2. The UploadServer should check version and return 505

**Expected:** Returns `P2P-CI/1.0 505 P2P-CI Version Not Supported`

**✅ PASS if:** Returns 505 for unsupported versions.

---

## Test 7: Concurrency

**Goal:** Verify multiple peers can operate simultaneously.

**Steps:**
1. Start Server (Terminal 1)
2. Start Peer 1 (Terminal 2)
3. Start Peer 2 (Terminal 3)
4. Start Peer 3 (Terminal 4)

5. **Simultaneously execute:**
   - Peer 1: `LIST ALL`
   - Peer 2: `LOOKUP RFC 1234`
   - Peer 3: `GET RFC 5678`

6. **Expected:**
   - All commands execute without blocking
   - Server handles all requests concurrently
   - No errors or deadlocks

**✅ PASS if:**
- All peers operate independently
- No blocking or errors
- Server handles concurrent connections

---

## Test 8: Automatic Cleanup on Disconnect

**Goal:** Verify server removes peer entries when peer disconnects.

**Steps:**
1. With Server, Peer 1, and Peer 2 running:
   - In Peer 2: `LIST ALL` - should show Peer 1's RFCs

2. **Disconnect Peer 1:**
   - In Peer 1 terminal: Type `EXIT` or press `Ctrl+C`

3. **Check Server Terminal:**
   - Should show: `Peer ... disconnected: ...`
   - Should remove peer entries

4. **In Peer 2 terminal:**
   - Type: `LIST ALL`
   - Peer 1's RFCs should be gone

**✅ PASS if:**
- Server detects disconnection
- Peer 1's RFCs removed from index
- LIST ALL no longer shows Peer 1's resources

---

## Test 9: Message Format Compliance

**Goal:** Verify all messages follow P2P-CI/1.0 protocol specification.

### P2S Protocol (Peer to Server):

**ADD Request Format:**
```
ADD RFC <number> P2P-CI/1.0\r\n
Host: <hostname>\r\n
Port: <port>\r\n
Title: <title>\r\n
\r\n
```

**LOOKUP Request Format:**
```
LOOKUP RFC <number> P2P-CI/1.0\r\n
Host: <hostname>\r\n
Port: <port>\r\n
Title: <title>\r\n
\r\n
```

**LIST Request Format:**
```
LIST ALL P2P-CI/1.0\r\n
Host: <hostname>\r\n
Port: <port>\r\n
\r\n
```

**Response Format:**
```
P2P-CI/1.0 <code> <phrase>\r\n
[data lines]\r\n
\r\n
```

### P2P Protocol (Peer to Peer):

**GET Request Format:**
```
GET RFC <number> P2P-CI/1.0\r\n
Host: <hostname>\r\n
OS: <os>\r\n
\r\n
```

**Response Format (200 OK):**
```
P2P-CI/1.0 200 OK\r\n
Date: <RFC1123 date>\r\n
OS: <os>\r\n
Last-Modified: <RFC1123 date>\r\n
Content-Length: <size>\r\n
Content-Type: text/plain\r\n
\r\n
[file content as bytes]
```

**✅ PASS if:**
- All messages use `\r\n` line endings
- Headers are properly formatted
- Blank line separates headers from body
- Status codes match specification

---

## Test 10: End-to-End Workflow

**Goal:** Complete workflow from registration to download.

**Steps:**
1. Start Server
2. Start Peer 1 with RFC files
3. Start Peer 2 (empty directory)
4. Peer 2: `LIST ALL` - sees Peer 1's RFCs
5. Peer 2: `LOOKUP RFC 1234` - finds Peer 1
6. Peer 2: `GET RFC 1234` - downloads from Peer 1
7. Peer 2: `LIST ALL` - now shows RFC 1234 (registered after download)
8. Start Peer 3
9. Peer 3: `GET RFC 1234` - can download from either Peer 1 or Peer 2

**✅ PASS if:**
- Complete workflow executes successfully
- All steps work as expected
- RFCs propagate through the network

---

## Grading Checklist

Use this checklist to verify all requirements:

### Part B Requirements:

- [ ] **Peer connects to central server** (P2SClient implemented)
- [ ] **UploadServer runs on specified/random port**
- [ ] **ADD requests sent for all local RFCs on startup**
- [ ] **LOOKUP functionality works**
- [ ] **LIST ALL functionality works**
- [ ] **GET (P2P transfer) works with 200 OK**
- [ ] **GET returns 400 for bad requests**
- [ ] **GET returns 404 for missing files**
- [ ] **GET returns 505 for wrong protocol version**
- [ ] **All P2P response headers present** (Date, OS, Last-Modified, Content-Length, Content-Type)
- [ ] **Control connection stays open until peer exit**
- [ ] **CLI menu works** (LOOKUP, LIST, GET, HELP, EXIT)

### Protocol Compliance:

- [ ] **P2S messages match spec** (exact format, CRLF)
- [ ] **P2P messages match spec** (exact format, CRLF)
- [ ] **Response codes correct** (200, 400, 404, 505)

### System Requirements:

- [ ] **Concurrency works** (multiple peers simultaneously)
- [ ] **Automatic cleanup** (server removes entries on disconnect)
- [ ] **JAR files build correctly** (server.jar and peer.jar)
- [ ] **CLI parameters work** (--serverHost, --serverPort, --uploadPort, --rfcDir, --os)

---

## Troubleshooting

### Peer won't connect to server:
- Check server is running
- Verify `--serverHost` and `--serverPort` are correct
- Check firewall settings

### GET fails:
- Verify target peer's UploadServer is running
- Check upload port is correct
- Verify RFC file exists on target peer

### Messages not formatted correctly:
- Check for `\r\n` line endings (not just `\n`)
- Verify blank line after headers
- Check header format matches spec exactly

### Build fails:
- Verify JAVA_HOME is set correctly
- Check Java version is 17+
- Try: `.\mvnw.cmd clean package`

---

## Quick Test Script

Run this to quickly verify basic functionality:

```powershell
# Terminal 1 - Server
java -jar target/server.jar

# Terminal 2 - Peer 1
java -jar target/peer.jar --serverHost 127.0.0.1 --serverPort 7734 --uploadPort 5001 --rfcDir ./peer1_rfc --os "Windows 11"
# Then type: LIST ALL

# Terminal 3 - Peer 2  
java -jar target/peer.jar --serverHost 127.0.0.1 --serverPort 7734 --uploadPort 5002 --rfcDir ./peer2_rfc --os "Ubuntu 22.04"
# Then type: GET RFC 1234
```

Good luck with your submission! 🚀

