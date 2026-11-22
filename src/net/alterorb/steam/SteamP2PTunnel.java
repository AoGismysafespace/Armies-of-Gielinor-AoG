package net.alterorb.steam;

import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamNativeHandle;
import com.codedisaster.steamworks.SteamNetworking;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.alterorb.steam.SteamManager;

/**
 * ============================================================================
 * SteamP2PTunnel: Virtual TCP-over-Steam-P2P Multiplexing Layer
 * ============================================================================
 * 
 * ARCHITECTURE OVERVIEW:
 * Original Jagex FunOrb game clients communicate exclusively over raw TCP
 * sockets connected to port 43594. To allow modern multiplayer without requiring
 * port forwarding, public static IP addresses, or dedicated servers, this class
 * creates a transparent P2P network bridge over Valve's Steam Datagram Relay (SDR).
 * 
 * ROLE OF GuestProxy (Runs on Joining Player's Client):
 * 1. Binds a local loopback ServerSocket on 127.0.0.1 (default port 43594).
 * 2. When the local FunOrb Applet connects to 127.0.0.1:43594, GuestProxy accepts
 *    the TCP socket and assigns it a unique integer `streamId`.
 * 3. Sends an OP_OPEN packet over Steam P2P to the Host's SteamID.
 * 4. Reads binary TCP stream data from the local game and encapsulates it into
 *    OP_DATA packets sent reliably over Steam P2P.
 * 5. Receives incoming OP_DATA packets from Steam and writes them back into the
 *    local game socket.
 * 
 * ROLE OF HostBridge (Runs on Hosting Player's Client):
 * 1. Listens for incoming Steam P2P packets from any authenticated guest.
 * 2. On OP_OPEN, opens a real local TCP socket to the Host's internal AoGServer
 *    (127.0.0.1:43594) and maps it to a unique `StreamKey(GuestSteamID, streamId)`.
 * 3. Relays game packets bidirectionally between Steam P2P and the local server.
 * 
 * PACKET PROTOCOL DEFINITION (Channel 0):
 * ----------------------------------------------------------------------------
 * | Opcode | Name    | Byte Layout                                            |
 * |--------|---------|--------------------------------------------------------|
 * | 0x01   | OP_OPEN | [byte: 1][int: streamId][byte: nameLen][byte[]: name]  |
 * | 0x02   | OP_DATA | [byte: 2][int: streamId][byte[]: rawTCPPayload]        |
 * | 0x03   | OP_CLOSE| [byte: 3][int: streamId]                               |
 * | 0x04   | OP_PING | [byte: 4]                                              |
 * ----------------------------------------------------------------------------
 */
public class SteamP2PTunnel {

    /** Default Steam P2P communication channel. */
    public static final int CHANNEL = 0;

    /** Opcode 1: Request opening a new virtual TCP stream across P2P. */
    public static final byte OP_OPEN = 1;

    /** Opcode 2: Transmit raw TCP byte payload for an active stream. */
    public static final byte OP_DATA = 2;

    /** Opcode 3: Signal closure/teardown of a virtual stream. */
    public static final byte OP_CLOSE = 3;

    /** Opcode 4: Heartbeat / ping packet to keep NAT hole-punch active. */
    public static final byte OP_PING = 4;

    /**
     * Maps host-side local socket ports to guest identity info.
     * Used by AoGServer to identify which Steam persona belongs to which TCP connection.
     */
    private static final Map<Integer, TunnelConnectionInfo> tunnelConnections = new ConcurrentHashMap<Integer, TunnelConnectionInfo>();

    /**
     * Registers a local ephemeral port mapping to a remote guest's SteamID.
     * 
     * @param localPort The local port allocated on 127.0.0.1
     * @param steamID   The 64-bit SteamID of the guest
     * @param username  The Steam display name of the guest
     */
    /**
     * Legacy 3-argument overload kept for compatibility with callers/tests that
     * do not supply a 12-byte Coat of Arms payload.
     */
    public static void registerTunnelConnection(int localPort, SteamID steamID, String username) {
        registerTunnelConnection(localPort, steamID, username, null);
    }

    /**
     * Registers a Steam P2P tunnel mapping from the host's local loopback TCP
     * port to the connecting guest's Steam identity, lobby username, and
     * optional 12-byte serialized Coat of Arms.
     */
    public static void registerTunnelConnection(int localPort, SteamID steamID, String username, byte[] coatOfArms) {
        if (localPort > 0) {
            tunnelConnections.put(localPort, new TunnelConnectionInfo(steamID, username, coatOfArms));
        }
    }

    /**
     * Retrieves guest connection details for a specific local server socket.
     */
    public static TunnelConnectionInfo getTunnelConnection(int localPort) {
        return tunnelConnections.get(localPort);
    }

    /**
     * Resolves the guest's Steam persona name for an active local socket port.
     */
    public static String getTunnelGuestName(int localPort) {
        TunnelConnectionInfo info = tunnelConnections.get(localPort);
        return info != null ? info.username : null;
    }

    /**
     * Unregisters a closed local port from the active tunnel registry.
     */
    public static void unregisterTunnelConnection(int localPort) {
        if (localPort > 0) {
            tunnelConnections.remove(localPort);
        }
    }

    /**
     * Immutable data record pairing a guest's SteamID with their sanitized username.
     */
    /**
     * Metadata captured for an incoming Steam P2P stream mapped onto a local
     * loopback TCP socket on the host.
     */
    public static class TunnelConnectionInfo {
        public final SteamID steamId;
        public final String username;
        /** Optional 12-byte serialized Coat of Arms sent by the Steam guest in OP_OPEN, or null if omitted. */
        public final byte[] coatOfArms;

        public TunnelConnectionInfo(SteamID steamID, String username) {
            this(steamID, username, null);
        }

        public TunnelConnectionInfo(SteamID steamID, String username, byte[] coatOfArms) {
            this.steamId = steamID;
            this.username = username;
            if (coatOfArms != null && coatOfArms.length >= 12) {
                this.coatOfArms = new byte[12];
                System.arraycopy(coatOfArms, 0, this.coatOfArms, 0, 12);
            } else {
                this.coatOfArms = null;
            }
        }
    }

    /**
     * ========================================================================
     * GuestProxy: Local loopback server for joining players.
     * ========================================================================
     * Listens on 127.0.0.1, intercepts game client traffic, and relays it
     * across Steam P2P to the Host.
     */
    public static class GuestProxy {
        private final SteamNetworking networking;
        private final SteamID hostSteamID;
        private final String guestUsername;
        private ServerSocket proxyServer;
        
        /** Maps active streamId -> local Client Socket. */
        private final Map<Integer, Socket> streams = new ConcurrentHashMap<Integer, Socket>();
        private final AtomicInteger streamCounter = new AtomicInteger(1);
        private volatile boolean running = false;
        private Thread acceptThread;
        private Thread receiveThread;

        public GuestProxy(SteamNetworking steamNetworking, SteamID hostSteamID) {
            this(steamNetworking, hostSteamID, null);
        }

        public GuestProxy(SteamNetworking steamNetworking, SteamID hostSteamID, String guestUsername) {
            this.networking = steamNetworking;
            this.hostSteamID = hostSteamID;
            this.guestUsername = guestUsername;
        }

        /**
         * Starts the proxy on an automatically assigned ephemeral port.
         */
        public synchronized int start() throws Exception {
            return this.start(0);
        }

        /**
         * Starts the local proxy server socket and launches network worker threads.
         * 
         * @param preferredPort Port number to bind (0 for dynamic ephemeral port, or 43594)
         * @return The actual bound port number on 127.0.0.1
         */
        public synchronized int start(int preferredPort) throws Exception {
            if (this.running) {
                return this.proxyServer.getLocalPort();
            }
            this.running = true;

            // Send an initial OP_PING to warm up the Steam P2P / SDR route to the host
            try {
                ByteBuffer pingBuffer = ByteBuffer.allocateDirect(1);
                pingBuffer.put(OP_PING);
                pingBuffer.flip();
                synchronized (this.networking) {
                    this.networking.sendP2PPacket(this.hostSteamID, pingBuffer, SteamNetworking.P2PSend.Reliable, CHANNEL);
                }
            } catch (Throwable ignored) {
                // Initial ping failure is non-fatal; SDR will retry during actual packet transfer
            }

            this.proxyServer = new ServerSocket();
            this.proxyServer.setReuseAddress(true);

            // Bind loopback socket with retry loop (handles quick client restarts)
            if (preferredPort > 0) {
                IOException lastEx = null;
                boolean bound = false;
                for (int attempt = 0; attempt < 25; ++attempt) {
                    try {
                        this.proxyServer.bind(new InetSocketAddress(preferredPort));
                        bound = true;
                        break;
                    } catch (IOException e) {
                        lastEx = e;
                        try {
                            Thread.sleep(100L);
                        } catch (InterruptedException ie) {
                            break;
                        }
                    }
                }
                if (!bound) {
                    if (lastEx != null) throw lastEx;
                    throw new IOException("Failed to bind proxy server to port " + preferredPort + " after retries");
                }
            } else {
                this.proxyServer.bind(null);
            }

            int boundPort = this.proxyServer.getLocalPort();
            System.out.println("[SteamP2P-GuestProxy] Proxy listening on 127.0.0.1:" + boundPort 
                + " -> Tunneling to Host " + this.hostSteamID 
                + (this.guestUsername != null ? " [User: " + this.guestUsername + "]" : ""));

            // ----------------------------------------------------------------
            // Accept Thread: Accepts local game connections and spawns stream relays
            // ----------------------------------------------------------------
            this.acceptThread = new Thread(() -> {
                while (this.running) {
                    try {
                        Socket clientSocket = this.proxyServer.accept();
                        clientSocket.setTcpNoDelay(true); // Disable Nagle's algorithm for instant game packet delivery
                        int streamId = this.streamCounter.getAndIncrement();
                        this.streams.put(streamId, clientSocket);

                        System.out.println("[SteamP2P-GuestProxy] Accepted local client connection -> Assigned streamId=" + streamId);

                        // Frame OP_OPEN: [byte: OP_OPEN][int: streamId][byte: nameLen][byte[]: name][optional 12 bytes: coatOfArms]
                        // Appending the local guest's saved 12-byte Coat of Arms here avoids asking the
                        // client applet to encode or push Packet 71 out-of-band before login, fixing Bug 2A
                        // safely without touching original 2009 Jagex client bytecode.
                        byte[] nameBytes = (this.guestUsername != null ? this.guestUsername.trim() : "").getBytes(StandardCharsets.UTF_8);
                        if (nameBytes.length > 64) {
                            nameBytes = Arrays.copyOf(nameBytes, 64);
                        }
                        byte[] coatBytes = net.alterorb.launcher.CoatManager.getLocalCoatBytes();
                        boolean hasCoat = (coatBytes != null && coatBytes.length >= 12);
                        ByteBuffer openBuf = ByteBuffer.allocateDirect(6 + nameBytes.length + (hasCoat ? 12 : 0));
                        openBuf.put(OP_OPEN);
                        openBuf.putInt(streamId);
                        openBuf.put((byte) nameBytes.length);
                        openBuf.put(nameBytes);
                        if (hasCoat) {
                            openBuf.put(coatBytes, 0, 12);
                        }
                        openBuf.flip();

                        synchronized (this.networking) {
                            this.networking.sendP2PPacket(this.hostSteamID, openBuf, SteamNetworking.P2PSend.Reliable, CHANNEL);
                        }

                        // Worker Thread: Reads data from local FunOrb client socket -> Sends OP_DATA to Steam
                        Thread clientReader = new Thread(() -> {
                            byte[] readBuf = new byte[8192];
                            ByteBuffer sendBuf = ByteBuffer.allocateDirect(16384);
                            try (InputStream in = clientSocket.getInputStream()) {
                                int readBytes;
                                while ((readBytes = in.read(readBuf)) != -1) {
                                    sendBuf.clear();
                                    sendBuf.put(OP_DATA);
                                    sendBuf.putInt(streamId);
                                    sendBuf.put(readBuf, 0, readBytes);
                                    sendBuf.flip();

                                    synchronized (this.networking) {
                                        this.networking.sendP2PPacket(this.hostSteamID, sendBuf, SteamNetworking.P2PSend.Reliable, CHANNEL);
                                    }
                                }
                            } catch (Throwable ignored) {
                                // Stream disconnect or socket close
                            } finally {
                                this.streams.remove(streamId);
                                // Send OP_CLOSE to inform the host this stream has closed
                                try {
                                    sendBuf.clear();
                                    sendBuf.put(OP_CLOSE);
                                    sendBuf.putInt(streamId);
                                    sendBuf.flip();
                                    synchronized (this.networking) {
                                        this.networking.sendP2PPacket(this.hostSteamID, sendBuf, SteamNetworking.P2PSend.Reliable, CHANNEL);
                                    }
                                } catch (Throwable ignored) {}
                                try {
                                    clientSocket.close();
                                } catch (Throwable ignored) {}
                                System.out.println("[SteamP2P-GuestProxy] Local client stream " + streamId + " closed.");
                            }
                        }, "SteamGuestProxy-Client-" + streamId);
                        clientReader.setDaemon(true);
                        clientReader.start();

                    } catch (Throwable t) {
                        if (this.running) continue;
                        break;
                    }
                }
            }, "SteamGuestProxy-Acceptor");
            this.acceptThread.setDaemon(true);
            this.acceptThread.start();

            // ----------------------------------------------------------------
            // Receiver Thread: Polls Steam P2P for responses from the Host
            // ----------------------------------------------------------------
            this.receiveThread = new Thread(() -> {
                int[] msgSize = new int[1];
                ByteBuffer packetBuf = ByteBuffer.allocateDirect(32768);
                SteamID remoteSender = new SteamID();

                while (this.running) {
                    boolean hadPacket = false;
                    try {
                        while (this.networking.isP2PPacketAvailable(CHANNEL, msgSize)) {
                            hadPacket = true;
                            packetBuf.clear();
                            int bytesRead = this.networking.readP2PPacket(remoteSender, packetBuf, CHANNEL);
                            if (bytesRead < 5) continue; // Minimum valid packet has 1 byte opcode + 4 bytes streamId

                            packetBuf.position(0);
                            packetBuf.limit(bytesRead);
                            byte opcode = packetBuf.get();
                            int streamId = packetBuf.getInt();

                            if (opcode == OP_DATA) {
                                byte[] payload = new byte[packetBuf.remaining()];
                                packetBuf.get(payload);
                                Socket targetSocket = this.streams.get(streamId);
                                if (targetSocket != null && !targetSocket.isClosed()) {
                                    try {
                                        OutputStream out = targetSocket.getOutputStream();
                                        out.write(payload);
                                        out.flush();
                                    } catch (Throwable err) {
                                        this.streams.remove(streamId);
                                        try { targetSocket.close(); } catch (Throwable ignored) {}
                                    }
                                }
                            } else if (opcode == OP_CLOSE) {
                                Socket closedSocket = this.streams.remove(streamId);
                                if (closedSocket != null) {
                                    try { closedSocket.close(); } catch (Throwable ignored) {}
                                }
                            }
                        }
                    } catch (Throwable ignored) {
                        if (!this.running) break;
                    }

                    // Adaptive polling: if no packets were queued, yield CPU for 2ms
                    if (!hadPacket) {
                        try {
                            Thread.sleep(2L);
                        } catch (InterruptedException ie) {
                            break;
                        }
                    }
                }
            }, "SteamGuestProxy-Receiver");
            this.receiveThread.setDaemon(true);
            this.receiveThread.start();

            return boundPort;
        }

        /**
         * Shuts down all active proxy threads and disconnects open streams.
         */
        public synchronized void stop() {
            this.running = false;
            try {
                if (this.proxyServer != null) this.proxyServer.close();
            } catch (Throwable ignored) {}

            if (this.acceptThread != null) this.acceptThread.interrupt();
            if (this.receiveThread != null) this.receiveThread.interrupt();

            for (Socket s : this.streams.values()) {
                try { s.close(); } catch (Throwable ignored) {}
            }
            this.streams.clear();
        }
    }

    /**
     * ========================================================================
     * HostBridge: Server relay for the hosting player.
     * ========================================================================
     * Receives Steam P2P packets from guests and bridges them to the local
     * AoGServer running on 127.0.0.1:43594.
     */
    public static class HostBridge {
        private final SteamNetworking networking;
        
        /** Maps StreamKey(GuestSteamID, streamId) -> Local Socket connected to AoGServer. */
        private final Map<StreamKey, Socket> streams = new ConcurrentHashMap<StreamKey, Socket>();
        private volatile boolean running = false;
        private Thread receiveThread;

        public HostBridge(SteamNetworking steamNetworking) {
            this.networking = steamNetworking;
        }

        /**
         * Starts the Host P2P receiving thread.
         */
        public synchronized void start() {
            if (this.running) return;
            this.running = true;
            System.out.println("[SteamP2P-HostBridge] Starting Host P2P Bridge...");

            this.receiveThread = new Thread(() -> {
                int[] msgSize = new int[1];
                ByteBuffer packetBuf = ByteBuffer.allocateDirect(32768);
                SteamID remoteSender = new SteamID();

                while (this.running) {
                    boolean hadPacket = false;
                    try {
                        while (this.networking.isP2PPacketAvailable(CHANNEL, msgSize)) {
                            hadPacket = true;
                            packetBuf.clear();
                            int bytesRead = this.networking.readP2PPacket(remoteSender, packetBuf, CHANNEL);
                            if (bytesRead < 1) continue;

                            packetBuf.position(0);
                            packetBuf.limit(bytesRead);
                            byte opcode = packetBuf.get();

                            // Heartbeat / ping packet
                            if (opcode == OP_PING || bytesRead < 5) continue;

                            int streamId = packetBuf.getInt();
                            SteamID guestSteamID = new SteamID(remoteSender);
                            StreamKey key = new StreamKey(guestSteamID, streamId);

                            if (opcode == OP_OPEN) {
                                // Extract optional guest username string followed by optional 12-byte Coat of Arms
                                String guestName = null;
                                int nameLen;
                                if (packetBuf.hasRemaining() && (nameLen = packetBuf.get() & 0xFF) > 0 && packetBuf.remaining() >= nameLen) {
                                    byte[] nameBytes = new byte[nameLen];
                                    packetBuf.get(nameBytes);
                                    guestName = new String(nameBytes, StandardCharsets.UTF_8).trim();
                                }
                                byte[] guestCoat = null;
                                if (packetBuf.remaining() >= 12) {
                                    guestCoat = new byte[12];
                                    packetBuf.get(guestCoat);
                                }
                                // Fallback resolution for guest persona name
                                if (guestName == null || guestName.isBlank()) {
                                    guestName = SteamManager.getInstance().getPersonaName(guestSteamID);
                                }
                                if (guestName == null || guestName.isBlank()) {
                                    guestName = SteamManager.getInstance().getLobbyMemberPersonaName(guestSteamID);
                                }
                                if (guestName == null || guestName.isBlank()) {
                                    SteamManager.SteamFriendInfo friend = SteamManager.getInstance().getFriendInfo(guestSteamID);
                                    if (friend != null && friend.name != null && !friend.name.isBlank()) {
                                        guestName = friend.name.trim();
                                    }
                                }
                                if (guestName == null || guestName.isBlank()) {
                                    guestName = SteamManager.getInstance().getFirstGuestLobbyMemberName();
                                }
                                this.handleOpen(key, guestSteamID, guestName, guestCoat);

                            } else if (opcode == OP_DATA) {
                                byte[] payload = new byte[packetBuf.remaining()];
                                packetBuf.get(payload);
                                this.handleData(key, payload);

                            } else if (opcode == OP_CLOSE) {
                                this.handleClose(key);
                            }
                        }
                    } catch (Throwable ignored) {
                        if (!this.running) break;
                    }

                    if (!hadPacket) {
                        try {
                            Thread.sleep(2L);
                        } catch (InterruptedException ie) {
                            break;
                        }
                    }
                }
            }, "SteamHostBridge-Receiver");
            this.receiveThread.setDaemon(true);
            this.receiveThread.start();
        }

        /**
         * Connects to the local AoGServer on behalf of an incoming guest stream.
         */
        private void handleOpen(StreamKey streamKey, SteamID guestSteamID, String guestUsername, byte[] guestCoat) {
            int streamId = streamKey.getStreamId();
            System.out.println("[SteamP2P-HostBridge] Opening stream " + streamKey 
                + " to local AoGServer (127.0.0.1:43594) for guest " + guestUsername 
                + " (" + guestSteamID + ")...");

            int localPort = -1;
            try {
                Socket serverSocket = new Socket();
                serverSocket.bind(new InetSocketAddress("127.0.0.1", 0)); // Dynamic local client port
                localPort = serverSocket.getLocalPort();

                // Register mapping so AoGServer knows who this socket belongs to (including their OP_OPEN Coat of Arms)
                SteamP2PTunnel.registerTunnelConnection(localPort, guestSteamID, guestUsername, guestCoat);

                serverSocket.connect(new InetSocketAddress("127.0.0.1", 43594), 5000);
                serverSocket.setTcpNoDelay(true);
                this.streams.put(streamKey, serverSocket);

                final int registeredPort = localPort;

                // Worker: Reads responses from AoGServer -> Sends OP_DATA back to Guest over Steam P2P
                Thread serverReader = new Thread(() -> {
                    byte[] readBuf = new byte[8192];
                    ByteBuffer sendBuf = ByteBuffer.allocateDirect(16384);
                    try (InputStream in = serverSocket.getInputStream()) {
                        int readBytes;
                        while ((readBytes = in.read(readBuf)) != -1) {
                            sendBuf.clear();
                            sendBuf.put(OP_DATA);
                            sendBuf.putInt(streamId);
                            sendBuf.put(readBuf, 0, readBytes);
                            sendBuf.flip();

                            synchronized (this.networking) {
                                this.networking.sendP2PPacket(guestSteamID, sendBuf, SteamNetworking.P2PSend.Reliable, CHANNEL);
                            }
                        }
                    } catch (Throwable ignored) {
                    } finally {
                        this.streams.remove(streamKey);
                        if (registeredPort > 0) {
                            SteamP2PTunnel.unregisterTunnelConnection(registeredPort);
                        }
                        // Send OP_CLOSE to Guest
                        try {
                            sendBuf.clear();
                            sendBuf.put(OP_CLOSE);
                            sendBuf.putInt(streamId);
                            sendBuf.flip();
                            synchronized (this.networking) {
                                this.networking.sendP2PPacket(guestSteamID, sendBuf, SteamNetworking.P2PSend.Reliable, CHANNEL);
                            }
                        } catch (Throwable ignored) {}
                        try {
                            serverSocket.close();
                        } catch (Throwable ignored) {}
                        System.out.println("[SteamP2P-HostBridge] Stream " + streamKey + " closed.");
                    }
                }, "SteamHostBridge-Stream-" + streamKey.getSteamID64() + "-" + streamId);
                serverReader.setDaemon(true);
                serverReader.start();

            } catch (Throwable t) {
                if (localPort > 0) {
                    SteamP2PTunnel.unregisterTunnelConnection(localPort);
                }
                System.err.println("[SteamP2P-HostBridge] Failed to connect to local AoGServer: " + t.getMessage());
            }
        }

        /**
         * Relays incoming guest payload to the local AoGServer socket.
         */
        private void handleData(StreamKey streamKey, byte[] payload) {
            Socket socket = this.streams.get(streamKey);
            if (socket != null && !socket.isClosed()) {
                try {
                    OutputStream out = socket.getOutputStream();
                    out.write(payload);
                    out.flush();
                } catch (Throwable t) {
                    this.handleClose(streamKey);
                }
            }
        }

        /**
         * Closes local AoGServer socket when guest disconnects.
         */
        private void handleClose(StreamKey streamKey) {
            Socket socket = this.streams.remove(streamKey);
            if (socket != null) {
                SteamP2PTunnel.unregisterTunnelConnection(socket.getLocalPort());
                try {
                    socket.close();
                } catch (Throwable ignored) {}
            }
        }

        /**
         * Shuts down the HostBridge and disconnects all guest sockets.
         */
        public synchronized void stop() {
            this.running = false;
            if (this.receiveThread != null) {
                this.receiveThread.interrupt();
            }
            for (Socket s : this.streams.values()) {
                SteamP2PTunnel.unregisterTunnelConnection(s.getLocalPort());
                try {
                    s.close();
                } catch (Throwable ignored) {}
            }
            this.streams.clear();
        }
    }

    /**
     * Unique composite key identifying an individual stream multiplexed over P2P.
     * Combines the guest's 64-bit SteamID with the connection's stream integer ID.
     */
    public static final class StreamKey {
        private final long steamID64;
        private final int streamId;

        public StreamKey(SteamID steamID, int streamId) {
            this.steamID64 = steamID != null ? SteamNativeHandle.getNativeHandle(steamID) : 0L;
            this.streamId = streamId;
        }

        public long getSteamID64() {
            return this.steamID64;
        }

        public int getStreamId() {
            return this.streamId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || this.getClass() != o.getClass()) return false;
            StreamKey other = (StreamKey) o;
            return this.steamID64 == other.steamID64 && this.streamId == other.streamId;
        }

        @Override
        public int hashCode() {
            int hash = (int)(this.steamID64 ^ (this.steamID64 >>> 32));
            return 31 * hash + this.streamId;
        }

        @Override
        public String toString() {
            return "StreamKey[" + this.steamID64 + ":" + this.streamId + "]";
        }
    }
}
