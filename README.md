# P2P-CI: Peer-to-Peer with Centralized Index

## Team Members
- **Prithish Samanta** - Unity ID: `psamant2`
- **Nayan Taori** - Unity ID: `ntaori`

## System Overview

Hybrid P2P system with centralized index server for RFC document sharing.

## HTTP-style Response Codes

- **200 OK** - Request was successful
- **400 Bad Request** - Request has invalid syntax or missing required fields
- **404 Not Found** - Requested RFC is not available in the network
- **505 P2P-CI Version Not Supported** - Protocol version in request does not match P2P-CI/1.0

## Requirements

- Java Development Kit (JDK) version 17 or higher

## Compilation

```bash
mvn clean compile
```

Or using javac:

```bash
javac -d target/classes -sourcepath src/main/java src/main/java/org/p2p/**/*.java
```

## Running the System

### Prepare RFC Directories

```bash
mkdir -p peer1 peer2 peer3
echo "RFC 123 - Test Content" > peer1/RFC_123_Test.txt
```

### Start the Server

```bash
java -cp target/classes org.p2p.server.ServerMain
```

### Start Peers

```bash
java -cp target/classes org.p2p.peer.PeerMain --serverHost localhost --serverPort 7734 --uploadPort 5001 --rfcDir ./peer1 --os "Windows 10"
java -cp target/classes org.p2p.peer.PeerMain --serverHost localhost --serverPort 7734 --uploadPort 5002 --rfcDir ./peer2 --os "Mac OS 10.4.1"
java -cp target/classes org.p2p.peer.PeerMain --serverHost localhost --serverPort 7734 --uploadPort 5003 --rfcDir ./peer3 --os "Linux Ubuntu 22.04"
```

## Interactive Commands

### ADD - Register an RFC with the server

```
ADD RFC 123
Host: localhost
Port: 5001
Title: A Proferred Official ICP
```

### LIST ALL - List all RFCs from a specific peer

```
LIST ALL P2P-CI/1.0
Host: localhost
Port: 5001
```

### LOOKUP - Find peers with a specific RFC

```
LOOKUP RFC 123
Host: localhost
Port: 5001
Title: A Proferred Official ICP
```

### GET - Download an RFC from a peer

```
GET RFC 123 P2P-CI/1.0
Host: localhost
OS: Windows 10
```

### EXIT - Disconnect from server

```
EXIT
```

## Protocol Format

**Request:**
```
METHOD RFC_NUMBER P2P-CI/1.0
Header-Field: Value
<blank line>
```

**Response:**
```
P2P-CI/1.0 STATUS_CODE STATUS_PHRASE
Header-Field: Value
<blank line>
[optional data]
```

## License

Academic project for CSC 573 - Internet Protocols
