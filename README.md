# P2P-CI: Peer-to-Peer with Centralized Index

## Team Members
- **[Prithish Samanta]** - Unity ID: `psamant2`
- **[Nayan Taori]** - Unity ID: `ntaori`

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

✅ **Server Registry**: Central server maintains index of all peers and their RFCs  
✅ **P2P File Transfer**: Direct peer-to-peer GET requests for RFC documents  
✅ **HTTP-style Response Codes**:
   - `200 OK` - Successful request
   - `400 Bad Request` - Malformed request syntax
   - `404 Not Found` - RFC not available
   - `505 P2P-CI Version Not Supported` - Version mismatch

✅ **Automatic Cleanup**: Server removes peer entries on disconnect  
✅ **Concurrency**: Multiple peers can operate simultaneously  
✅ **RFC 2616-style Protocol**: Custom application-layer protocol based on HTTP message format

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

## Requirements

- Java 11 or higher
- Apache Maven 3.6+
- Network connectivity (for P2P transfers)

## Quick Start

See [RUN.md](RUN.md) for detailed compilation and execution instructions.

## License

Academic project for CSC 573 - Internet Protocols

