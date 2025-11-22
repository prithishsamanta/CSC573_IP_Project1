# P2P-CI: Peer-to-Peer with Centralized Index

## Team Members
- **Prithish Samanta** - Unity ID: `psamant2`
- **Nayan Taori** - Unity ID: `ntaori`

## System Overview

This project implements a hybrid Peer-to-Peer (P2P) system with a centralized index server for RFC document sharing. The system allows multiple peers to register, discover, and exchange RFC documents directly with each other while using a central server to maintain an index of available resources.

### Architecture

The system consists of two main components:

1. **Central Index Server**: Maintains a registry of active peers and their available RFC documents. Handles peer registration, RFC lookups, and provides a directory of all available RFCs across the network.

2. **Peers**: Independent nodes that can both upload and download RFC documents. Each peer:
   - Registers with the central server upon startup
   - Advertises its available RFCs to the server
   - Queries the server to discover RFCs available on other peers
   - Transfers RFC documents directly with other peers (P2P transfer)
   - Automatically deregisters when disconnecting

## Server-Peer Interaction

### Registration Phase
1. Peer connects to the central server via TCP
2. Peer sends `ADD` requests to register each available RFC
3. Server maintains a list of active peers and their RFCs

### Discovery Phase
1. Peer queries server using `LOOKUP` (for specific RFC) or `LIST ALL` (for all RFCs)
2. Server responds with peer information (hostname, port) hosting the requested RFC(s)

### Transfer Phase
1. Peer establishes direct P2P connection with another peer (bypassing server)
2. Peer sends `GET` request for specific RFC
3. Hosting peer responds with the RFC content or error code
4. Connection closes after transfer

### Cleanup Phase
- When a peer disconnects, the server automatically removes all its RFC entries
- Ensures registry stays current and accurate

## Key Features (Grading Rubric)

- **Server Registry**: Central server maintains index of all peers and their RFCs  
- **P2P File Transfer**: Direct peer-to-peer GET requests for RFC documents  
- **HTTP-style Response Codes**:
   - `200 OK` - Successful request
   - `400 Bad Request` - Malformed request syntax
   - `404 Not Found` - RFC not available
   - `505 P2P-CI Version Not Supported` - Version mismatch

- **Automatic Cleanup**: Server removes peer entries on disconnect  
- **Concurrency**: Multiple peers can operate simultaneously  
- **RFC 2616-style Protocol**: Custom application-layer protocol based on HTTP message format

## Protocol Specification

The system uses a custom `P2P-CI/1.0` protocol with HTTP-style message formatting:

### Request Format
```
METHOD RFC_NUMBER P2P-CI/1.0
Header-Field: Value
...
<blank line>
```

### Response Format
```
P2P-CI/1.0 STATUS_CODE STATUS_PHRASE
Header-Field: Value
...
<blank line>
[optional data]
```

### Supported Methods
- `ADD RFC [number] P2P-CI/1.0` - Register RFC with server
- `LOOKUP RFC [number] P2P-CI/1.0` - Query server for RFC location
- `LIST ALL P2P-CI/1.0` - List all available RFCs
- `GET RFC [number] P2P-CI/1.0` - Retrieve RFC from peer (P2P)

## Project Structure

```
.
├── README.md                          # This file
├── RUN.md                            # Compilation and execution instructions
├── pom.xml                           # Maven build configuration
└── src/
    └── main/
        └── java/
            └── org/
                └── p2p/
                    ├── server/       # Server implementation
                    ├── peer/         # Peer implementation
                    └── common/       # Shared protocol definitions
```

## Prerequisites

Before running the project, ensure the following are installed and configured:

### 1. Java Development Kit (JDK)
- **Version Required**: JDK 17 or higher
- **Check Installation**:
  ```bash
  java -version
  javac -version
  ```
- **Expected Output**: Should show version 17 or higher
- **Download**: [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://adoptium.net/)

### 2. Apache Maven
- **Version Required**: Maven 3.6 or higher
- **Check Installation**:
  ```bash
  mvn -version
  ```
- **Expected Output**: Should show Maven version 3.6+ and JDK 17+
- **Download**: [Maven Download](https://maven.apache.org/download.cgi)
- **Installation Guide**: [Maven Installation](https://maven.apache.org/install.html)

### 3. Environment Variables (if not already set)
- **JAVA_HOME**: Should point to JDK installation directory
  ```bash
  # Check on macOS/Linux
  echo $JAVA_HOME
  
  # Check on Windows
  echo %JAVA_HOME%
  ```
- **PATH**: Should include Maven's `bin` directory

### 4. Network Configuration
- Ensure ports **7734** (server) and **5001-5010** (peers) are available
- No firewall blocking localhost connections

## Compilation

Navigate to the project directory and compile:

```bash
cd /path/to/p2p-psamant2-ntaori
mvn clean package -DskipTests
```

**Expected Output**: `BUILD SUCCESS`

## Running the System

### Step 1: Prepare RFC Directories

Create directories for peers and add RFC files:

```bash
# Create peer directories
mkdir -p peer1 peer2 peer3

# Add RFC files (format: RFC_<number>_<title>.txt)
echo "TCP Protocol Specification" > peer1/RFC_1001_TCP.txt
echo "UDP Protocol Specification" > peer1/RFC_1002_UDP.txt
echo "ICMP Protocol Specification" > peer2/RFC_1003_ICMP.txt
echo "ARP Protocol Specification" > peer2/RFC_1004_ARP.txt
echo "DHCP Protocol Specification" > peer3/RFC_1005_DHCP.txt
```

### Step 2: Start the Server

Open a new terminal and run:

```bash
java -cp target/classes org.p2p.server.ServerMain
```

**Expected Output**:
```
P2P-CI Server listening on port 7734
```

**Note**: Keep this terminal open. The server runs until you press `Ctrl+C`.

### Step 3: Start Peer 1

Open a new terminal (separate from server) and run:

```bash
java -cp target/classes org.p2p.peer.PeerMain \
  --serverHost localhost \
  --serverPort 7734 \
  --uploadPort 5001 \
  --rfcDir ./peer1 \
  --os "Ubuntu 22.04"
```

**Parameters Explained**:
- `--serverHost localhost`: Central server hostname (use IP address for remote server)
- `--serverPort 7734`: Central server port (default: 7734)
- `--uploadPort 5001`: This peer's upload server port (must be unique per peer)
- `--rfcDir ./peer1`: Directory containing this peer's RFC files
- `--os "Ubuntu 22.04"`: Operating system name (any string)

**Expected Output**:
```
Connected to server at localhost:7734
Peer hostname: <your-hostname>
Starting upload server on port 5001
Auto-registered 2 RFCs from ./peer1

Enter command (ADD, LIST ALL, LOOKUP, GET, EXIT):
>
```

### Step 4: Start Peer 2

Open another new terminal and run:

```bash
java -cp target/classes org.p2p.peer.PeerMain \
  --serverHost localhost \
  --serverPort 7734 \
  --uploadPort 5002 \
  --rfcDir ./peer2 \
  --os "Windows 11"
```

### Step 5: Start Peer 3 (Optional)

Open another terminal:

```bash
java -cp target/classes org.p2p.peer.PeerMain \
  --serverHost localhost \
  --serverPort 7734 \
  --uploadPort 5003 \
  --rfcDir ./peer3 \
  --os "MacOS 14"
```

## Interactive Commands

Once a peer is running, you can enter the following commands at the prompt:

### 1. LIST ALL - View all RFCs in the network

```
LIST ALL P2P-CI/1.0
Host: <your-hostname>
Port: 5001
```

**Example Output**:
```
P2P-CI/1.0 200 OK

All RFCs in network (5 total):
  RFC 1001 TCP <hostname> 5001
  RFC 1002 UDP <hostname> 5001
  RFC 1003 ICMP <hostname> 5002
  RFC 1004 ARP <hostname> 5002
  RFC 1005 DHCP <hostname> 5003
```

### 2. LOOKUP - Find a specific RFC

```
LOOKUP RFC 1003 P2P-CI/1.0
Host: <your-hostname>
Port: 5001
Title: ICMP
```

**Example Output**:
```
P2P-CI/1.0 200 OK

Found 1 peer(s) with RFC 1003:
  RFC 1003 ICMP Protocol <hostname> 5002
```

### 3. ADD - Register a new RFC

```
ADD RFC 1006 NewProtocol P2P-CI/1.0
Host: <your-hostname>
Port: 5001
Title: New Protocol Specification
```

**Note**: The RFC file must exist in your peer's directory.

### 4. GET - Download RFC from another peer

```
GET RFC 1003 P2P-CI/1.0
Host: <peer-hostname>
OS: Ubuntu 22.04
```

**Example Output**:
```
P2P-CI/1.0 200 OK

Downloaded RFC 1003 to: ./peer1/RFC_1003_ICMP.txt
```

### 5. EXIT - Disconnect peer

```
EXIT
```

This will:
- Send EXIT request to server
- Remove all this peer's RFCs from server registry
- Close upload server
- Terminate the peer process

## Example Session

**Terminal 1 (Server)**:
```bash
java -cp target/classes org.p2p.server.ServerMain
# Server listening on port 7734
```

**Terminal 2 (Peer 1)**:
```bash
java -cp target/classes org.p2p.peer.PeerMain \
  --serverHost localhost --serverPort 7734 \
  --uploadPort 5001 --rfcDir ./peer1 --os "Ubuntu"

> LIST ALL P2P-CI/1.0
Host: Mac.lan
Port: 5001

# Shows all RFCs from all peers
```

**Terminal 3 (Peer 2)**:
```bash
java -cp target/classes org.p2p.peer.PeerMain \
  --serverHost localhost --serverPort 7734 \
  --uploadPort 5002 --rfcDir ./peer2 --os "Windows"

> GET RFC 1001 P2P-CI/1.0
Host: Mac.lan
OS: Ubuntu

# Downloads RFC 1001 from Peer 1
```

## Troubleshooting

### Port Already in Use
```bash
# Check what's using the port
lsof -ti:7734
lsof -ti:5001

# Kill process using the port
lsof -ti:7734 | xargs kill -9
```

### Java Version Issues
```bash
# Ensure JDK 17+ is active
java -version

# If multiple Java versions installed, set JAVA_HOME
export JAVA_HOME=/path/to/jdk-17
```

### Maven Build Failures
```bash
# Clean and rebuild
mvn clean
mvn clean package -DskipTests
```

## Additional Documentation

See [RUN.md](RUN.md) for more detailed compilation and execution instructions.

## License

Academic project for CSC 573 - Internet Protocols

