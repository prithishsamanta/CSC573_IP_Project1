# Quick Submission Checklist

## ✅ Pre-Submission Checks

### 1. Build Verification
- [ ] Run: `.\mvnw.cmd clean package` (creates peer.jar)
- [ ] Run: `.\mvnw.cmd package -Pserver` (creates server.jar)
- [ ] Verify both JARs exist in `target/` directory

### 2. Basic Functionality (5 minutes)

**Terminal 1 - Start Server:**
```powershell
java -jar target/server.jar
```
- [ ] Server starts on port 7734
- [ ] Shows: "P2P-CI Server listening on port 7734"

**Terminal 2 - Start Peer 1:**
```powershell
java -jar target/peer.jar --serverHost 127.0.0.1 --serverPort 7734 --uploadPort 5001 --rfcDir ./peer1_rfc --os "Windows 11"
```
- [ ] Peer connects to server
- [ ] Shows: "[P2SClient] Connected to server"
- [ ] Automatically registers RFCs
- [ ] Shows command prompt: `>`

**Terminal 3 - Start Peer 2:**
```powershell
java -jar target/peer.jar --serverHost 127.0.0.1 --serverPort 7734 --uploadPort 5002 --rfcDir ./peer2_rfc --os "Ubuntu 22.04"
```

### 3. Test Commands (In Peer terminals)

**Test LIST ALL:**
- [ ] Type: `LIST ALL`
- [ ] Shows all RFCs from all peers
- [ ] Format: `P2P-CI/1.0 200 OK` followed by RFC lines

**Test LOOKUP:**
- [ ] Type: `LOOKUP RFC 1234`
- [ ] Shows peers with RFC 1234
- [ ] Type: `LOOKUP RFC 9999`
- [ ] Shows: "404 Not Found"

**Test GET (P2P Transfer):**
- [ ] Type: `GET RFC 1234`
- [ ] Downloads file successfully
- [ ] Shows: "200 OK" with all headers
- [ ] File saved in peer's rfc directory
- [ ] RFC automatically registered after download

**Test Exit:**
- [ ] Type: `EXIT`
- [ ] Peer disconnects cleanly
- [ ] Server removes peer's RFCs

### 4. Error Cases

**Test 404:**
- [ ] GET non-existent RFC returns 404

**Test 400:**
- [ ] Malformed requests return 400

**Test 505:**
- [ ] Wrong protocol version returns 505

### 5. Protocol Compliance

**Check Message Format:**
- [ ] All messages use `\r\n` line endings
- [ ] Headers properly formatted
- [ ] Blank line after headers
- [ ] Status codes match spec (200, 400, 404, 505)

**P2P Response Headers:**
- [ ] Date header present
- [ ] OS header present
- [ ] Last-Modified header present
- [ ] Content-Length header present
- [ ] Content-Type: text/plain header present

### 6. Concurrency Test

- [ ] Start 3+ peers simultaneously
- [ ] Execute commands in parallel
- [ ] All work without blocking
- [ ] No errors or deadlocks

---

## 🎯 Critical Requirements (Must Work)

1. ✅ **Peer connects to server on startup**
2. ✅ **ADD requests sent automatically for local RFCs**
3. ✅ **LOOKUP returns all peers with specific RFC**
4. ✅ **LIST ALL returns complete index**
5. ✅ **GET downloads RFC via P2P (200 OK)**
6. ✅ **GET handles errors (400, 404, 505)**
7. ✅ **All P2P response headers present**
8. ✅ **Server removes peer entries on disconnect**
9. ✅ **Multiple peers work concurrently**
10. ✅ **Message format matches specification**

---

## 📝 Submission Files

Make sure you have:
- [ ] `pom.xml` (with build configuration)
- [ ] `src/` directory (all source code)
- [ ] `README.md` (project description)
- [ ] `RUN.md` (build/run instructions)
- [ ] `TESTING_GUIDE.md` (this file)
- [ ] JARs can be built successfully

---

## 🚀 Quick Test (2 minutes)

```powershell
# Terminal 1
java -jar target/server.jar

# Terminal 2
java -jar target/peer.jar --serverHost 127.0.0.1 --serverPort 7734 --uploadPort 5001 --rfcDir ./peer1_rfc --os "Windows 11"
# Type: LIST ALL

# Terminal 3
java -jar target/peer.jar --serverHost 127.0.0.1 --serverPort 7734 --uploadPort 5002 --rfcDir ./peer2_rfc --os "Ubuntu 22.04"
# Type: GET RFC 1234
```

If all three work, you're good! ✅

