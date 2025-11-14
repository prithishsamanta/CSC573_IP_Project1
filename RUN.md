# Build and Run Instructions

This document provides exact commands to compile, run, and test the P2P-CI system.

## Prerequisites

- Java 17 or higher (JDK)
- Apache Maven 3.6+
- Multiple terminal windows (for testing concurrent peers)

## Installing Maven (Windows)

If you don't have Maven installed, follow these steps:

### Option 1: Using Chocolatey (Recommended)

1. Open PowerShell as Administrator
2. Install Chocolatey (if not already installed):
   ```powershell
   Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
   ```
3. Install Maven:
   ```powershell
   choco install maven
   ```
4. Verify installation:
   ```powershell
   mvn -version
   ```

### Option 2: Manual Installation

1. Download Maven from: https://maven.apache.org/download.cgi
   - Download the `apache-maven-3.9.x-bin.zip` file
2. Extract the zip file to a location like `C:\Program Files\Apache\maven`
3. Add Maven to your PATH:
   - Open System Properties → Environment Variables
   - Under System Variables, find `Path` and click Edit
   - Click New and add: `C:\Program Files\Apache\maven\bin`
   - Click OK to save
4. Verify installation:
   ```powershell
   mvn -version
   ```

### Option 3: Using Maven Wrapper (No Installation Required)

This project includes Maven Wrapper scripts. You can use them directly:

**Windows:**
```powershell
.\mvnw.cmd clean package
```

**Linux/Mac:**
```bash
./mvnw clean package
```

## Compilation

### Option 1: Using Build Script (Recommended for Windows)

**Windows:**
```powershell
.\build.bat
```

This script will:
- Clean and compile the project
- Create both `target/server.jar` and `target/peer.jar`

### Option 2: Using Maven Directly

Clean and build the project using Maven:

```bash
mvn clean package
```

This will create `target/peer.jar` by default. To create both JARs, use the build script or run:

```powershell
# Create peer.jar (default)
mvn clean package

# Create server.jar
mvn org.apache.maven.plugins:maven-shade-plugin:3.5.0:shade -Dshade.finalName=server -Dshade.mainClass=org.p2p.server.ServerMain -Dshade.outputDirectory=target
```

### Option 3: Using Maven Wrapper

**Windows:**
```powershell
.\mvnw.cmd clean package
```

**Linux/Mac:**
```bash
./mvnw clean package
```

### Build Output

The build will:
- Compile all Java source files
- Create runnable JAR files: `target/server.jar` and `target/peer.jar`
- Create compiled classes in `target/classes/`

**Note:** The build creates two executable JAR files:
- `target/server.jar` - Central index server
- `target/peer.jar` - Peer application

## Running the System

### Step 1: Start the Central Index Server

Open a terminal/PowerShell window and run:

**Using JAR file (Recommended):**
```powershell
# Terminal 1 - Server
java -jar target/server.jar
```

**Or using classpath:**
```powershell
java -cp target/classes org.p2p.server.ServerMain
```

**Expected Output:**
```
P2P-CI Server listening on port 7734
```

The server will:
- Listen on port `7734` for peer registrations
- Accept multiple concurrent peer connections
- Log all peer activities (ADD, LOOKUP, LIST ALL)

---

### Step 2: Prepare RFC Directories

Create directories for your peers and add some RFC files:

**Windows PowerShell:**
```powershell
# Create directories
New-Item -ItemType Directory -Force -Path peer1_rfc
New-Item -ItemType Directory -Force -Path peer2_rfc

# Create sample RFC files (or copy real RFC files)
# Example: Create rfc1234.txt in peer1_rfc directory
```

**Note:** RFC files should be named as `rfc<number>.txt` (e.g., `rfc1234.txt`, `rfc5678.txt`)

---

### Step 3: Start First Peer

Open a new terminal/PowerShell window and run:

**Using JAR file (Recommended):**
```powershell
# Terminal 2 - Peer 1
java -jar target/peer.jar --serverHost 127.0.0.1 --serverPort 7734 --uploadPort 5001 --rfcDir ./peer1_rfc --os "Windows 11"
```

**Or using classpath:**
```powershell
java -cp target/classes org.p2p.peer.PeerMain --serverHost 127.0.0.1 --serverPort 7734 --uploadPort 5001 --rfcDir ./peer1_rfc --os "Windows 11"
```

**Parameters:**
- `--serverHost`: Central server hostname/IP (default: localhost)
- `--serverPort`: Server's listening port (default: 7734)
- `--uploadPort`: Port for this peer to accept P2P upload requests (use 0 for auto-assign)
- `--rfcDir`: Directory containing this peer's RFC files (default: ./rfc)
- `--os`: Operating system string (default: system OS name)

**Expected Output:**
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
=== P2P-CI Peer Commands ===
...
>
```

---

### Step 4: Start Second Peer

Open another terminal/PowerShell window:

**Using JAR file:**
```powershell
# Terminal 3 - Peer 2
java -jar target/peer.jar --serverHost 127.0.0.1 --serverPort 7734 --uploadPort 5002 --rfcDir ./peer2_rfc --os "Ubuntu 22.04"
```

**Note:** Use a different `--uploadPort` for each peer on the same machine.

---

### Step 5: Start Third Peer (Optional - for Concurrency Demo)

```powershell
# Terminal 4 - Peer 3
java -jar target/peer.jar --serverHost 127.0.0.1 --serverPort 7734 --uploadPort 5003 --rfcDir ./peer3_rfc --os "Mac OS 14.5"
```

---

## Example Interactive Session

Once peers are running, you can interact with them via their command-line interface. The peer will show a prompt `>` where you can enter commands.

### Example 1: LIST ALL RFCs

**Command (in any peer terminal):**
```
LIST ALL
```

**Peer Output:**
```
[P2SClient] LIST response: P2P-CI/1.0 200 OK
All RFCs in network (3 total):
  RFC 1234 - Internet Drafts @ 127.0.0.1:5001
  RFC 1234 - Internet Drafts @ 127.0.0.1:5002
  RFC 5678 - HTTP Protocol @ 127.0.0.1:5001
  RFC 9012 - Security Basics @ 127.0.0.1:5003
```

**Explanation:**
- Shows all RFCs available across all peers
- Format: `RFC [number] - [title] @ [hostname]:[port]`

---

### Example 2: LOOKUP Specific RFC

**Command (in Peer 1):**
```
LOOKUP RFC 1234
```

**Peer Output:**
```
[P2SClient] LOOKUP response: P2P-CI/1.0 200 OK
Found 2 peer(s) with RFC 1234:
  RFC 1234 - Internet Drafts @ 127.0.0.1:5001
  RFC 1234 - Internet Drafts @ 127.0.0.1:5002
```

**Explanation:**
- RFC 1234 is available on both Peer 1 and Peer 2
- Peer can choose which source to download from

---

### Example 3: LOOKUP Non-Existent RFC

**Command:**
```
LOOKUP RFC 9999
```

**Peer Output:**
```
[P2SClient] LOOKUP response: P2P-CI/1.0 404 Not Found
[P2SClient] RFC 9999 not found
No peers found with RFC 9999
```

**Explanation:**
- Status: `404 Not Found`
- No peer in the network has RFC 9999

---

### Example 4: GET RFC from Another Peer (P2P Transfer)

**Command (in Peer 3):**
```
GET RFC 1234
```

**Peer Output:**
```
Looking up RFC 1234...
[P2SClient] LOOKUP response: P2P-CI/1.0 200 OK
Found 2 peer(s) with RFC 1234:
  RFC 1234 - Internet Drafts @ 127.0.0.1:5001
  RFC 1234 - Internet Drafts @ 127.0.0.1:5002
Downloading RFC 1234 from 127.0.0.1:5001...
[P2PClient] Status: P2P-CI/1.0 200 OK
[P2PClient] Header: Date: Mon, 01 Nov 2025 12:34:56 GMT
[P2PClient] Header: OS: Windows 11
[P2PClient] Header: Last-Modified: Fri, 15 Oct 2025 10:20:00 GMT
[P2PClient] Header: Content-Length: 245678
[P2PClient] Header: Content-Type: text/plain
[P2PClient] Saved RFC 1234 to C:\...\peer3_rfc\rfc1234.txt
Successfully downloaded RFC 1234
Registered RFC 1234 with server
```

**Explanation:**
- Peer automatically looks up which peers have the RFC
- Direct P2P connection established between peers
- Status: `200 OK` (successful transfer)
- RFC content transferred directly (server not involved)
- After download, peer automatically registers the RFC with the server

---

### Example 5: GET RFC Not Available on Target Peer

If you try to GET an RFC that doesn't exist on the target peer, you'll see:

**Peer Output:**
```
[P2PClient] Status: P2P-CI/1.0 404 Not Found
[P2PClient] RFC 9012 not found on peer
Failed to download RFC 9012
```

**Explanation:**
- Status: `404 Not Found`
- Target peer doesn't have the requested RFC locally

---

### Example 6: Help Command

**Command:**
```
HELP
```

**Output:**
```
=== P2P-CI Peer Commands ===
LOOKUP RFC <number>  - Find peers that have a specific RFC
LIST ALL             - List all RFCs available in the network
GET RFC <number>     - Download an RFC from a peer (will lookup first)
HELP                 - Show this menu
EXIT / QUIT          - Exit the peer
```

---

### Example 7: Exit Command

**Command:**
```
EXIT
```

**Output:**
```
[P2SClient] Disconnected from server
Peer shutting down...
```

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

### Port Already in Use (Windows)
If you see "Address already in use":

**PowerShell:**
```powershell
# Find process using the port
netstat -ano | findstr :7734

# Kill the process (replace PID with actual process ID)
taskkill /PID <PID> /F
```

**Or use:**
```powershell
# For server port 7734
Get-NetTCPConnection -LocalPort 7734 | Select-Object -ExpandProperty OwningProcess | ForEach-Object { Stop-Process -Id $_ -Force }

# For peer upload port 5001
Get-NetTCPConnection -LocalPort 5001 | Select-Object -ExpandProperty OwningProcess | ForEach-Object { Stop-Process -Id $_ -Force }
```

### Connection Refused
- Ensure server is running before starting peers
- Check Windows Firewall settings allow connections on specified ports
- Verify `--serverHost` points to correct IP/hostname (use `127.0.0.1` for localhost)

### Classes Not Found / JAR Not Found
- Run `mvn clean package` to recompile and create JARs
- Ensure you're in the project root directory when running commands
- Verify JAR files exist: `target/server.jar` and `target/peer.jar`

### Maven Not Found
- Verify Maven is installed: `mvn -version`
- If not installed, follow the Maven installation steps above
- Or use Maven Wrapper: `.\mvnw.cmd clean package`

### Java Version Issues
- Verify Java version: `java -version` (should be Java 17 or higher)
- If using older Java, update to Java 17+ or modify `pom.xml` to use your Java version

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

