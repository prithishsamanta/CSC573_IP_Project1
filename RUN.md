# Native Compilation and Execution Guide

This guide is for running the P2P-CI system natively without Docker.

**Note:** For easier setup, see [QUICKSTART.md](QUICKSTART.md) for Docker instructions.

## Prerequisites

- Java Development Kit (JDK) 17 or higher
- Apache Maven 3.6 or higher
- Network connectivity

## Installation

### Check Java Version

```bash
java -version
# Should show version 17 or higher
```

### Check Maven Version

```bash
mvn -version
# Should show version 3.6 or higher
```

## Compilation

```bash
# Clean and compile the project
mvn clean compile

# Or compile and package
mvn clean package
```

## Running the System

### Step 1: Prepare RFC Directories

Create directories for each peer and add RFC files:

```bash
mkdir -p peer1_rfc peer2_rfc

# Add RFC files in format: RFC_<number>_<title>.txt
echo "TCP Protocol Specification" > peer1_rfc/RFC_1001_TCP.txt
echo "UDP Protocol Specification" > peer1_rfc/RFC_1002_UDP.txt
echo "ICMP Protocol Specification" > peer2_rfc/RFC_1003_ICMP.txt
```

### Step 2: Start the Server

Open a terminal window:

```bash
java -cp target/classes org.p2p.server.ServerMain
```

The server will start listening on port `7734`.

### Step 3: Start Peers

Open additional terminal windows for each peer.

**Peer 1:**

```bash
java -cp target/classes org.p2p.peer.PeerMain \
  --serverHost 127.0.0.1 \
  --serverPort 7734 \
  --uploadPort 5001 \
  --rfcDir ./peer1_rfc \
  --os "Mac OS 14.5"
```

**Peer 2:**

```bash
java -cp target/classes org.p2p.peer.PeerMain \
  --serverHost 127.0.0.1 \
  --serverPort 7734 \
  --uploadPort 5002 \
  --rfcDir ./peer2_rfc \
  --os "Linux Ubuntu 22.04"
```

**Peer 3 (optional):**

```bash
java -cp target/classes org.p2p.peer.PeerMain \
  --serverHost 127.0.0.1 \
  --serverPort 7734 \
  --uploadPort 5003 \
  --rfcDir ./peer3_rfc \
  --os "Windows 11"
```

## Command Line Arguments

### Peer Arguments

- `--serverHost <hostname>` - Server hostname (default: 127.0.0.1)
- `--serverPort <port>` - Server port (default: 7734)
- `--uploadPort <port>` - Peer's upload server port (required)
- `--rfcDir <directory>` - Directory containing RFC files (required)
- `--os <os_name>` - Operating system name (default: system OS)

## Interactive Commands

Once a peer is running, you can use these commands:

```
> ADD RFC 1234 P2P-CI/1.0
Host: localhost
Port: 5001
Title: New RFC Title

> LOOKUP RFC 1234 P2P-CI/1.0
Host: localhost
Port: 5001
Title: New RFC Title

> LIST ALL P2P-CI/1.0
Host: localhost
Port: 5001

> GET RFC 1234 P2P-CI/1.0
Host: localhost
OS: Mac OS 14.5

> EXIT
```

## Example Session

### Terminal 1: Server

```bash
java -cp target/classes org.p2p.server.ServerMain

[Server] P2P-CI Server started on port 7734
[Server] Waiting for peer connections...
```

### Terminal 2: Peer 1

```bash
java -cp target/classes org.p2p.peer.PeerMain \
  --uploadPort 5001 --rfcDir ./peer1_rfc

Starting peer with config:
  Server host   : 127.0.0.1
  Server port   : 7734
  RFC directory : ./peer1_rfc
  Upload port   : 5001

Registering 2 RFC(s) with server...
  Registered RFC 1001: TCP
  Registered RFC 1002: UDP

=== P2P-CI Peer Ready ===
Commands:
  ADD RFC <num> P2P-CI/1.0       - Register an RFC
  LIST ALL P2P-CI/1.0            - List all RFCs
  LOOKUP RFC <num> P2P-CI/1.0    - Find peers with RFC
  GET RFC <num> P2P-CI/1.0       - Download RFC
  EXIT                           - Exit the peer

> LIST ALL P2P-CI/1.0
Host: localhost
Port: 5001

All RFCs in network (2 total):
  127.0.0.1 5001 RFC1001 TCP
  127.0.0.1 5001 RFC1002 UDP
```

## Troubleshooting

### Port Already in Use

```bash
# Find process using the port
lsof -ti:7734

# Kill the process
lsof -ti:7734 | xargs kill -9

# Or for peer ports
lsof -ti:5001 | xargs kill -9
```

### Compilation Errors

```bash
# Clean and rebuild
mvn clean compile

# If Maven dependencies fail
mvn dependency:purge-local-repository
mvn clean compile
```

### Cannot Connect to Server

1. Verify server is running on port 7734
2. Check firewall settings
3. Use `localhost` or `127.0.0.1` for local testing
4. Ensure no other process is using port 7734

### RFC Files Not Found

1. Check RFC file naming: `RFC_<number>_<title>.txt`
2. Verify directory path is correct
3. Ensure files have read permissions

## Testing Protocol Features

### Test 200 OK

```bash
> GET RFC 1001 P2P-CI/1.0
# Should succeed if RFC exists
```

### Test 404 Not Found

```bash
> GET RFC 9999 P2P-CI/1.0
# Should fail with 404 if RFC doesn't exist
```

### Test 505 Version Not Supported

```bash
> ADD RFC 1001 P2P-CI/2.0
# Should fail with 505 for wrong version
```

### Test 400 Bad Request

```bash
# Try to add duplicate RFC
> ADD RFC 1001 P2P-CI/1.0
(first time succeeds)
> ADD RFC 1001 P2P-CI/1.0
(second time fails with 400)
```

## Cleanup

To stop the system:

1. Type `EXIT` in each peer terminal
2. Press `Ctrl+C` in the server terminal

## Next Steps

- See [README.md](README.md) for system architecture
- See [QUICKSTART.md](QUICKSTART.md) for Docker setup
- See [DOCKER.md](DOCKER.md) for advanced Docker usage

