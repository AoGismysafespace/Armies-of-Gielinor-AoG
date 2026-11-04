/*
 * Decompiled with CFR 0.152.
 */
package net.alterorb.server;

import com.codedisaster.steamworks.SteamID;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.alterorb.server.BattleEngine;
import net.alterorb.server.IsaacCipher;
import net.alterorb.steam.SteamManager;
import net.alterorb.steam.SteamP2PTunnel;

public class AoGServer {
    public static final BigInteger RSA_MODULUS = new BigInteger("9502234814386934206236586670021593446078800264605631156447462595930865222500086384781785588329727379330742229955687690286837333342097904999277355733542633");
    public static final BigInteger RSA_PRIV_EXP = new BigInteger("1247352275938337045886344433254432891597355977179337547323153649278929970996025784537839180435226953142731788905056108920324547337421857120032020102316125");
    private static final Map<Long, byte[]> JS5_ARCHIVES = new ConcurrentHashMap<Long, byte[]>();
    private static AoGServer instance;
    private final int port;
    private ServerSocket serverSocket;
    private Thread serverThread;
    private ScheduledExecutorService keepaliveExecutor;
    private volatile boolean running = false;
    private final AtomicLong nextPlayerId = new AtomicLong(1L);
    private final AtomicInteger nextRoomId = new AtomicInteger(1);
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<ClientHandler>();
    private final Map<Integer, LobbyRoom> rooms = new LinkedHashMap<Integer, LobbyRoom>();
    private final List<ClientHandler> activeMatchPlayers = new ArrayList<ClientHandler>();
    private BattleEngine engine;
    private int lastUnpausedTurn = -1;
    private String hostUsername = "Host";
    public static final Map<Integer, Map<Integer, int[]>> campaignProgress;
    public static volatile byte[] hostCoatOfArms;

    private static void loadJs5Cache() {
        Object object;
        InputStream inputStream = AoGServer.class.getResourceAsStream("/net/alterorb/server/js5_cache.dat");
        if (inputStream == null) {
            inputStream = AoGServer.class.getResourceAsStream("js5_cache.dat");
        }
        if (inputStream == null) {
            object = new File("cache/js5_cache.dat");
            if (!((File)object).exists()) {
                object = new File("src/net/alterorb/server/js5_cache.dat");
            }
            if (!((File)object).exists()) {
                object = new File("standalone/src/net/alterorb/server/js5_cache.dat");
            }
            if (((File)object).exists()) {
                try {
                    inputStream = new FileInputStream((File)object);
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
        }
        if (inputStream != null) {
            try {
                object = new DataInputStream(new BufferedInputStream(inputStream));
                try {
                    int n = ((DataInputStream)object).readInt();
                    for (int i = 0; i < n; ++i) {
                        int n2 = ((DataInputStream)object).readInt();
                        int n3 = ((DataInputStream)object).readInt();
                        int n4 = ((DataInputStream)object).readInt();
                        byte[] byArray = ((DataInputStream)object).readNBytes(n4);
                        long l = (long)n2 << 32 | (long)n3 & 0xFFFFFFFFL;
                        JS5_ARCHIVES.put(l, byArray);
                    }
                    System.out.println("[AoGServer] Successfully loaded " + JS5_ARCHIVES.size() + " JS5 cache archives into memory.");
                }
                finally {
                    ((FilterInputStream)object).close();
                }
            }
            catch (Exception exception) {
                System.err.println("[AoGServer] Failed to load JS5 cache: " + exception.getMessage());
            }
        } else {
            System.err.println("[AoGServer] WARNING: js5_cache.dat not found in resources or fallback paths!");
        }
    }

    public AoGServer(int n) {
        this.port = n;
    }

    public void setHostUsername(String string) {
        this.hostUsername = string;
    }

    public synchronized BattleEngine getEngine() {
        if (this.engine == null) {
            try {
                this.engine = Class.forName("AoGBattleBridge").asSubclass(BattleEngine.class).getDeclaredConstructor(new Class[0]).newInstance(new Object[0]);
            }
            catch (Exception exception) {
                throw new RuntimeException("Failed to initialize AoGBattleBridge", exception);
            }
        }
        return this.engine;
    }

    public static Map<Integer, Map<Integer, int[]>> getCampaignProgress() {
        return campaignProgress;
    }

    public static byte[] getHostCoatOfArms() {
        return hostCoatOfArms;
    }

    public static void setHostCoatOfArms(byte[] byArray) {
        if (byArray != null && byArray.length >= 12) {
            hostCoatOfArms = (byte[])byArray.clone();
            AoGServer.savePersistedData();
        }
    }

    public static byte[] coatStringToBytes(String string) {
        int n;
        byte[] byArray = new byte[12];
        if (string == null || string.isBlank()) {
            return byArray;
        }
        String[] stringArray = string.split(":");
        if (stringArray.length < 2) {
            return byArray;
        }
        String[] stringArray2 = stringArray[0].split(",");
        String[] stringArray3 = stringArray[1].split(",");
        for (n = 0; n < 6 && n < stringArray2.length; ++n) {
            try {
                int n2 = Integer.parseInt(stringArray2[n].trim());
                byArray[n * 2] = (byte)(n2 & 0xFF);
                continue;
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        for (n = 0; n < 6 && n < stringArray3.length; ++n) {
            try {
                int n3 = Integer.parseInt(stringArray3[n].trim());
                byArray[n * 2 + 1] = (byte)(n3 & 0xFF);
                continue;
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        return byArray;
    }

    public static String coatBytesToString(byte[] byArray) {
        if (byArray == null || byArray.length < 12) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder stringBuilder2 = new StringBuilder();
        for (int i = 0; i < 6; ++i) {
            int n;
            int n2;
            if (i > 0) {
                stringBuilder.append(',');
                stringBuilder2.append(',');
            }
            if ((n2 = byArray[i * 2] & 0xFF) == 255) {
                n2 = -1;
            }
            if ((n = byArray[i * 2 + 1] & 0xFF) == 255) {
                n = -1;
            }
            stringBuilder.append(n2);
            stringBuilder2.append(n);
        }
        return stringBuilder.toString() + ":" + stringBuilder2.toString();
    }

    public static Map<Integer, Map<Integer, int[]>> parseCampaignProgress(String string) {
        String[] stringArray;
        ConcurrentHashMap<Integer, Map<Integer, int[]>> concurrentHashMap = new ConcurrentHashMap<Integer, Map<Integer, int[]>>();
        if (string == null || string.isBlank()) {
            return concurrentHashMap;
        }
        for (String string2 : stringArray = string.split(";")) {
            String[] stringArray2 = string2.split(":");
            if (stringArray2.length < 4) continue;
            try {
                int n2 = Integer.parseInt(stringArray2[0].trim());
                int n3 = Integer.parseInt(stringArray2[1].trim());
                int n4 = Integer.parseInt(stringArray2[2].trim());
                int n5 = Integer.parseInt(stringArray2[3].trim());
                concurrentHashMap.computeIfAbsent(n2, n -> new ConcurrentHashMap()).put(n3, new int[]{n4, n5});
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        return concurrentHashMap;
    }

    public static String serializeCampaignProgress(Map<Integer, Map<Integer, int[]>> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 3; ++i) {
            Map<Integer, int[]> map2 = map.get(i);
            if (map2 == null) continue;
            for (Map.Entry<Integer, int[]> entry : map2.entrySet()) {
                if (stringBuilder.length() > 0) {
                    stringBuilder.append(';');
                }
                stringBuilder.append(i).append(':').append(entry.getKey()).append(':').append(entry.getValue()[0]).append(':').append(entry.getValue()[1]);
            }
        }
        return stringBuilder.toString();
    }

    private static Path findPropertiesPath() {
        Path[] pathArray;
        for (Path path : pathArray = new Path[]{Paths.get("player.properties", new String[0]), Paths.get("..", "player.properties"), Paths.get(System.getProperty("user.home"), ".armiesofgielinor", "player.properties"), Paths.get(System.getProperty("user.home"), ".alterorb", "player.properties")}) {
            if (!Files.exists(path, new LinkOption[0])) continue;
            return path;
        }
        return Paths.get("player.properties", new String[0]);
    }

    public static synchronized void loadPersistedData() {
        Path path = AoGServer.findPropertiesPath();
        if (!Files.exists(path, new LinkOption[0])) {
            System.out.println("[AoGServer] No existing player.properties found at " + String.valueOf(path));
            return;
        }
        try (InputStream inputStream = Files.newInputStream(path, new OpenOption[0]);){
            String string;
            Properties properties = new Properties();
            properties.load(inputStream);
            String string2 = properties.getProperty("coat_of_arms");
            if (string2 != null && !string2.isBlank()) {
                hostCoatOfArms = AoGServer.coatStringToBytes(string2);
                System.out.println("[AoGServer] Loaded coat_of_arms: " + string2);
            }
            if ((string = properties.getProperty("campaign_progress")) != null && !string.isBlank()) {
                Map<Integer, Map<Integer, int[]>> map = AoGServer.parseCampaignProgress(string);
                campaignProgress.clear();
                campaignProgress.putAll(map);
                System.out.println("[AoGServer] Loaded campaign_progress: " + string);
            }
        }
        catch (Exception exception) {
            System.err.println("[AoGServer] Error loading player.properties: " + exception.getMessage());
        }
    }

    public static synchronized void savePersistedData() {
        try {
            Object object;
            Path path = null;
            Path path2 = Paths.get("player.properties", new String[0]);
            Path path3 = Paths.get(System.getProperty("user.home"), ".armiesofgielinor", "player.properties");
            Path path4 = Paths.get("..", "player.properties");
            Properties properties = new Properties();
            Path path5 = Files.exists(path2, new LinkOption[0]) ? path2 : (Files.exists(path3, new LinkOption[0]) ? path3 : (path = Files.exists(path4, new LinkOption[0]) ? path4 : null));
            if (path != null) {
                try {
                    object = Files.newInputStream(path, new OpenOption[0]);
                    try {
                        properties.load((InputStream)object);
                    }
                    finally {
                        if (object != null) {
                            ((InputStream)object).close();
                        }
                    }
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            if (hostCoatOfArms != null && (object = AoGServer.coatBytesToString(hostCoatOfArms)) != null) {
                properties.setProperty("coat_of_arms", (String)object);
            }
            if ((object = AoGServer.serializeCampaignProgress(campaignProgress)) != null && !((String)object).isBlank()) {
                properties.setProperty("campaign_progress", (String)object);
            }
            try (OutputStream outputStream = Files.newOutputStream(path2, new OpenOption[0])) {
                properties.store(outputStream, "Armies of Gielinor Configuration");
            }
            Files.createDirectories(path3.getParent(), new FileAttribute[0]);
            try (OutputStream outputStream = Files.newOutputStream(path3, new OpenOption[0])) {
                properties.store(outputStream, "Armies of Gielinor Configuration");
            }
            if (Files.exists(path4, new LinkOption[0])) {
                try (OutputStream outputStream = Files.newOutputStream(path4, new OpenOption[0])) {
                    properties.store(outputStream, "Armies of Gielinor Configuration");
                }
            }
            System.out.println("[AoGServer] Successfully saved player.properties (coat_of_arms & campaign_progress)");
        }
        catch (Exception exception) {
            System.err.println("[AoGServer] Error saving player.properties: " + exception.getMessage());
        }
    }

    public static synchronized void recordCampaignMission(int n2, int n3, int n4, int n5) {
        Map map = campaignProgress.computeIfAbsent(n2, n -> new ConcurrentHashMap());
        int[] nArray = (int[])map.get(n3);
        if (nArray == null || n4 < nArray[0] || n4 == nArray[0] && n5 < nArray[1]) {
            map.put(n3, new int[]{n4, n5});
            System.out.printf("[AoGServer] Recorded campaign mission: campaign=%d, mission=%d, turns=%d, damage=%d\n", n2, n3, n4, n5);
            AoGServer.savePersistedData();
        }
    }

    public static synchronized AoGServer start(int n) {
        if (instance != null && AoGServer.instance.running) {
            return instance;
        }
        AoGServer.loadPersistedData();
        instance = new AoGServer(n);
        instance.startServer();
        return instance;
    }

    public static synchronized AoGServer getInstance() {
        return instance;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void broadcastFriendUpdate(String string, String string2) {
        byte[] byArray = AoGServer.buildFriendEntry(string, string2, "");
        AoGServer aoGServer = this;
        synchronized (aoGServer) {
            for (ClientHandler clientHandler : this.clients) {
                if (!clientHandler.connected || !clientHandler.inLobby) continue;
                clientHandler.sendVarBytePacket(13, byArray);
            }
        }
    }

    public static synchronized AoGServer start(int n, String string) {
        AoGServer aoGServer = AoGServer.start(n);
        if (string != null && !string.isBlank()) {
            aoGServer.setHostUsername(string);
        }
        return aoGServer;
    }

    public static synchronized void stop() {
        if (instance != null) {
            instance.stopServer();
            instance = null;
        }
    }

    private void startServer() {
        this.running = true;
        this.keepaliveExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "AoGServer-Keepalive");
            thread.setDaemon(true);
            return thread;
        });
        this.keepaliveExecutor.scheduleAtFixedRate(() -> {
            for (ClientHandler clientHandler : this.clients) {
                if (!clientHandler.connected || !clientHandler.loggedIn) continue;
                clientHandler.sendKeepalive();
            }
        }, 10L, 10L, TimeUnit.SECONDS);
        SteamManager.getInstance().setPersonaStateChangeListener(steamID -> {
            block6: {
                try {
                    SteamManager.SteamFriendInfo steamFriendInfo = SteamManager.getInstance().getFriendInfo((SteamID)steamID);
                    if (steamFriendInfo == null) break block6;
                    byte[] byArray = AoGServer.buildFriendEntry(steamFriendInfo.status, steamFriendInfo.name, "");
                    AoGServer aoGServer = this;
                    synchronized (aoGServer) {
                        for (ClientHandler clientHandler : this.clients) {
                            if (!clientHandler.connected || !clientHandler.inLobby) continue;
                            clientHandler.sendVarBytePacket(13, byArray);
                        }
                    }
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            }
        });
        this.serverThread = new Thread(() -> {
            try {
                this.serverSocket = new ServerSocket();
                this.serverSocket.setReuseAddress(true);
                this.serverSocket.bind(new InetSocketAddress(this.port));
                System.out.println("[AoGServer] Listening for players on 0.0.0.0:" + this.port);
                while (this.running) {
                    try {
                        Socket socket = this.serverSocket.accept();
                        socket.setTcpNoDelay(true);
                        System.out.println("[AoGServer] Incoming connection from " + String.valueOf(socket.getRemoteSocketAddress()));
                        ClientHandler clientHandler = new ClientHandler(this, socket);
                        new Thread((Runnable)clientHandler, "AoGClient-" + String.valueOf(socket.getRemoteSocketAddress())).start();
                    }
                    catch (IOException iOException) {
                        if (!this.running) return;
                        System.err.println("[AoGServer] Error accepting client: " + iOException.getMessage());
                    }
                }
                return;
            }
            catch (IOException iOException) {
                System.err.println("[AoGServer] Could not bind to port " + this.port + ": " + iOException.getMessage());
            }
        }, "AoGServer-Listener");
        this.serverThread.setDaemon(true);
        this.serverThread.start();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void stopServer() {
        this.running = false;
        if (this.keepaliveExecutor != null) {
            this.keepaliveExecutor.shutdownNow();
            this.keepaliveExecutor = null;
        }
        try {
            if (this.serverSocket != null) {
                this.serverSocket.close();
            }
        }
        catch (IOException iOException) {
            // empty catch block
        }
        for (ClientHandler clientHandler : this.clients) {
            clientHandler.disconnect();
        }
        this.clients.clear();
        AoGServer aoGServer = this;
        synchronized (aoGServer) {
            this.rooms.clear();
            this.activeMatchPlayers.clear();
        }
    }

    synchronized String allocateUniqueUsername(String string) {
        String string2 = string == null || string.isBlank() ? "Commander" : string.trim();
        if ((string2 = string2.replaceAll("[^a-zA-Z0-9_\\- ]", "").trim()).isEmpty()) {
            string2 = "Commander";
        }
        if (string2.length() > 12) {
            string2 = string2.substring(0, 12).trim();
        }
        String result = string2;
        int n = 2;
        while (this.isUsernameTaken(result)) {
            String string3 = "(" + n + ")";
            int n2 = Math.max(1, 12 - string3.length());
            String string4 = string2.length() > n2 ? string2.substring(0, n2) : string2;
            result = string4 + string3;
            ++n;
        }
        return result;
    }

    private boolean isUsernameTaken(String string) {
        for (ClientHandler clientHandler : this.clients) {
            if (!clientHandler.connected || !string.equalsIgnoreCase(clientHandler.username)) continue;
            return true;
        }
        return false;
    }

    synchronized void addClient(ClientHandler clientHandler) {
        this.clients.add(clientHandler);
        System.out.println("[AoGServer] Player registered: " + clientHandler.username + " [id=" + clientHandler.playerId + "] (total players: " + this.clients.size() + ")");
    }

    synchronized void removeClient(ClientHandler clientHandler) {
        boolean bl = this.clients.remove(clientHandler);
        if (!bl) {
            return;
        }
        System.out.println("[AoGServer] Client disconnected: " + clientHandler.username + " (remaining: " + this.clients.size() + ")");
        if (clientHandler.currentRoom != null) {
            this.leaveRoom(clientHandler, clientHandler.currentRoom.roomId);
        }
        if (clientHandler.inLobby) {
            clientHandler.inLobby = false;
            byte[] byArray = AoGServer.buildSubOp6RemoveLobbyPlayer(clientHandler.playerId, 0);
            for (ClientHandler clientHandler2 : this.clients) {
                if (!clientHandler2.inLobby) continue;
                clientHandler2.sendLobbySubPacket(byArray);
            }
        }
        if (this.activeMatchPlayers.contains(clientHandler)) {
            this.handlePlayerMatchExit(clientHandler, true);
        }
    }

    public List<ClientHandler> getClients() {
        return this.clients;
    }

    synchronized void handleEnterLobby(ClientHandler clientHandler) {
        if (clientHandler.currentRoom != null) {
            this.leaveRoom(clientHandler, clientHandler.currentRoom.roomId);
        }
        clientHandler.inLobby = true;
        System.out.println("[AoGServer] Player " + clientHandler.username + " entered Multiplayer Lobby (Opcode 9).");
        clientHandler.sendZeroBytePacket(14);
        this.syncLobbyStateToClient(clientHandler, true);
        this.syncFriendsToClient(clientHandler);
        if (!this.rooms.isEmpty()) {
            for (LobbyRoom lobbyRoom : this.rooms.values()) {
                if (clientHandler == lobbyRoom.host || lobbyRoom.members.size() >= lobbyRoom.maxPlayers) continue;
                System.out.println("[AoGServer] Auto-joining invited guest " + clientHandler.username + " into Room #" + lobbyRoom.roomId);
                this.joinRoom(clientHandler, lobbyRoom.roomId);
                break;
            }
        }
    }

    public void syncFriendsToClient(ClientHandler clientHandler) {
        try {
            clientHandler.sendVarBytePacket(13, AoGServer.buildFriendInit("AoG"));
            List<SteamManager.SteamFriendInfo> list = SteamManager.getInstance().getFriendsList();
            System.out.println("[AoGServer] Syncing " + list.size() + " Steam friends to " + clientHandler.username);
            for (SteamManager.SteamFriendInfo steamFriendInfo : list) {
                clientHandler.sendVarBytePacket(13, AoGServer.buildFriendEntry(steamFriendInfo.status, steamFriendInfo.name, ""));
            }
            clientHandler.sendVarBytePacket(13, AoGServer.buildFriendDone());
        }
        catch (Throwable throwable) {
            System.err.println("[AoGServer] Error syncing friends to " + clientHandler.username + ": " + throwable.getMessage());
        }
    }

    private void syncLobbyStateToClient(ClientHandler clientHandler, boolean bl) {
        clientHandler.sendLobbySubPacket(AoGServer.buildSubOp23SelfId(clientHandler.playerId));
        clientHandler.sendLobbySubPacket(AoGServer.buildSubOp22Stats(1500, 50));
        for (ClientHandler object : this.clients) {
            if (!object.inLobby) continue;
            clientHandler.sendLobbySubPacket(AoGServer.buildSubOp5LobbyPlayer(object));
        }
        if (bl) {
            byte[] subOp5 = AoGServer.buildSubOp5LobbyPlayer(clientHandler);
            for (ClientHandler clientHandler2 : this.clients) {
                if (clientHandler2 == clientHandler || !clientHandler2.inLobby) continue;
                clientHandler2.sendLobbySubPacket(subOp5);
            }
        }
        for (LobbyRoom lobbyRoom : this.rooms.values()) {
            clientHandler.sendLobbySubPacket(AoGServer.buildSubOp8RoomListUpdate(lobbyRoom));
        }
    }

    synchronized void handleLeaveLobby(ClientHandler clientHandler) {
        System.out.println("[AoGServer] Player " + clientHandler.username + " leaving Lobby -> Main Menu (Opcode 10).");
        if (clientHandler.currentRoom != null) {
            this.leaveRoom(clientHandler, clientHandler.currentRoom.roomId);
        }
        if (clientHandler.inLobby) {
            clientHandler.inLobby = false;
            byte[] byArray = AoGServer.buildSubOp6RemoveLobbyPlayer(clientHandler.playerId, 0);
            for (ClientHandler clientHandler2 : this.clients) {
                if (clientHandler2 == clientHandler || !clientHandler2.inLobby) continue;
                clientHandler2.sendLobbySubPacket(byArray);
            }
        }
        clientHandler.sendZeroBytePacket(15);
    }

    synchronized void handleLobbySubCommand(ClientHandler clientHandler, byte[] byArray) {
        if (byArray == null || byArray.length == 0) {
            return;
        }
        int n = byArray[0] & 0xFF;
        System.out.println("[AoGServer] Lobby Opcode 11 subOp=" + n + " (len=" + byArray.length + ") from " + clientHandler.username);
        if (!clientHandler.inLobby || this.activeMatchPlayers.contains(clientHandler) || clientHandler.matchPlayerIdx >= 0) {
            System.out.println("[AoGServer] Player " + clientHandler.username + " returning from match to Lobby via Lobby Opcode 11 (subOp=" + n + ")");
            this.handleReturnToLobbyFromMatch(clientHandler);
            return;
        }
        switch (n) {
            case 0: {
                if (clientHandler.currentRoom != null) break;
                LobbyRoom lobbyRoom = null;
                for (LobbyRoom lobbyRoom2 : this.rooms.values()) {
                    if (lobbyRoom2.members.size() >= lobbyRoom2.maxPlayers) continue;
                    lobbyRoom = lobbyRoom2;
                    break;
                }
                if (lobbyRoom != null) {
                    this.joinRoom(clientHandler, lobbyRoom.roomId);
                    break;
                }
                int n2 = byArray.length >= 2 ? byArray[1] & 0xFF : 0;
                this.createRoom(clientHandler, n2, 0, this.getEngine().getDefaultRoomOptions());
                break;
            }
            case 1: 
            case 2: 
            case 3: {
                clientHandler.sendLobbySubPacket(new byte[]{0});
                break;
            }
            case 4: {
                if (clientHandler.currentRoom != null) {
                    this.leaveRoom(clientHandler, clientHandler.currentRoom.roomId);
                }
                int n3 = byArray.length >= 2 ? byArray[1] & 0xFF : 0;
                int n4 = byArray.length >= 3 ? byArray[2] & 0xFF : 0;
                int n5 = this.getEngine().getRoomOptionsLength();
                byte[] byArray2 = this.getEngine().getDefaultRoomOptions();
                if (byArray.length >= 3 + n5) {
                    byArray2 = Arrays.copyOfRange(byArray, 3, 3 + n5);
                }
                this.createRoom(clientHandler, n3, n4, byArray2);
                break;
            }
            case 5: {
                LobbyRoom lobbyRoom = clientHandler.currentRoom;
                if (lobbyRoom == null || lobbyRoom.host != clientHandler || byArray.length < 3) break;
                lobbyRoom.maxPlayers = Math.max(2, Math.min(8, byArray[1] & 0xFF));
                int n6 = byArray[2] & 0xFF;
                int n7 = n6 >>> 6 & 3;
                lobbyRoom.ac = n7 == 2 ? 2 : 1;
                lobbyRoom.gameType = n6 & 0x3F;
                int n8 = lobbyRoom.options.length;
                if (byArray.length >= 3 + n8) {
                    System.arraycopy(byArray, 3, lobbyRoom.options, 0, n8);
                }
                byte[] byArray3 = AoGServer.buildSubOp10CurrentRoomUpdate(lobbyRoom);
                for (ClientHandler clientHandler2 : lobbyRoom.members) {
                    clientHandler2.sendLobbySubPacket(byArray3);
                }
                this.broadcastLobbyRoomUpdate(lobbyRoom);
                break;
            }
            case 6: {
                long l;
                ClientHandler clientHandler3;
                LobbyRoom lobbyRoom = clientHandler.currentRoom;
                if (lobbyRoom == null || lobbyRoom.host != clientHandler || byArray.length < 9 || (clientHandler3 = this.findClientById(l = AoGServer.readLongBE(byArray, 1))) == null || clientHandler3 == clientHandler || !lobbyRoom.members.contains(clientHandler3)) break;
                lobbyRoom.members.remove(clientHandler3);
                clientHandler3.currentRoom = null;
                clientHandler3.sendLobbySubPacket(new byte[]{0});
                byte[] byArray4 = AoGServer.buildSubOp19RemoveRoomMember(clientHandler3.playerId, 1);
                for (ClientHandler clientHandler4 : lobbyRoom.members) {
                    clientHandler4.sendLobbySubPacket(byArray4);
                }
                this.broadcastLobbyRoomUpdate(lobbyRoom);
                break;
            }
            case 7: {
                long l;
                ClientHandler clientHandler5;
                LobbyRoom lobbyRoom = clientHandler.currentRoom;
                if (lobbyRoom == null || byArray.length < 9 || (clientHandler5 = this.findClientById(l = AoGServer.readLongBE(byArray, 1))) == null || !clientHandler5.inLobby) break;
                clientHandler5.sendLobbySubPacket(AoGServer.buildSubOp11RoomInvite(lobbyRoom.roomId));
                clientHandler5.sendLobbySubPacket(AoGServer.buildSubOp14or16PlayerFlag(14, clientHandler.playerId));
                clientHandler.sendLobbySubPacket(AoGServer.buildSubOp14or16PlayerFlag(16, clientHandler5.playerId));
                break;
            }
            case 8: 
            case 10: {
                if (byArray.length < 3) break;
                int n9 = (byArray[1] & 0xFF) << 8 | byArray[2] & 0xFF;
                if (n9 == 0) {
                    LobbyRoom lobbyRoom = null;
                    for (LobbyRoom lobbyRoom3 : this.rooms.values()) {
                        if (lobbyRoom3.members.size() >= lobbyRoom3.maxPlayers) continue;
                        lobbyRoom = lobbyRoom3;
                        break;
                    }
                    if (lobbyRoom != null) {
                        this.joinRoom(clientHandler, lobbyRoom.roomId);
                        break;
                    }
                    this.createRoom(clientHandler, 0, 0, this.getEngine().getDefaultRoomOptions());
                    break;
                }
                this.joinRoom(clientHandler, n9);
                break;
            }
            case 9: {
                int n10 = byArray.length >= 3 ? (byArray[1] & 0xFF) << 8 | byArray[2] & 0xFF : (clientHandler.currentRoom != null ? clientHandler.currentRoom.roomId : 0);
                this.leaveRoom(clientHandler, n10);
                break;
            }
            default: {
                System.out.println("[AoGServer] Unhandled Lobby Opcode 11 subOp=" + n);
            }
        }
    }

    synchronized void handleFriendClientPacket(ClientHandler clientHandler, byte[] byArray) {
        if (byArray == null || byArray.length == 0) {
            return;
        }
        int n = byArray[0] & 0xFF;
        String string = "";
        if (byArray.length > 1) {
            int n2;
            for (n2 = 1; n2 < byArray.length && byArray[n2] != 0; ++n2) {
            }
            string = new String(byArray, 1, n2 - 1, StandardCharsets.ISO_8859_1).trim();
        }
        System.out.println("[AoGServer] Friend client packet subOp=" + n + " target='" + string + "' from " + clientHandler.username);
        if (n == 0) {
            SteamManager.getInstance().openFriendsOverlay();
            this.syncFriendsToClient(clientHandler);
        } else if (n == 1) {
            this.syncFriendsToClient(clientHandler);
        }
    }

    private void createRoom(ClientHandler clientHandler, int n, int n2, byte[] byArray) {
        int n3 = this.nextRoomId.getAndIncrement() & Short.MAX_VALUE;
        if (n3 == 0) {
            n3 = this.nextRoomId.getAndIncrement() & Short.MAX_VALUE;
        }
        LobbyRoom lobbyRoom = new LobbyRoom(n3, clientHandler, byArray);
        lobbyRoom.gameType = n;
        lobbyRoom.maxPlayers = 4;
        this.rooms.put(n3, lobbyRoom);
        clientHandler.currentRoom = lobbyRoom;
        System.out.println("[AoGServer] Created LobbyRoom #" + n3 + " hosted by " + clientHandler.username);
        clientHandler.sendLobbySubPacket(AoGServer.buildSubOp4JoinStagingRoom(lobbyRoom));
        clientHandler.sendLobbySubPacket(AoGServer.buildSubOp18AddRoomMember(clientHandler));
        this.broadcastLobbyRoomUpdate(lobbyRoom);
    }

    private void joinRoom(ClientHandler clientHandler, int n) {
        LobbyRoom lobbyRoom;
        if (clientHandler.currentRoom != null) {
            if (clientHandler.currentRoom.roomId == n) {
                return;
            }
            this.leaveRoom(clientHandler, clientHandler.currentRoom.roomId);
        }
        if ((lobbyRoom = this.rooms.get(n)) == null || lobbyRoom.members.size() >= lobbyRoom.maxPlayers) {
            System.out.println("[AoGServer] Room #" + n + " unavailable or full for " + clientHandler.username);
            return;
        }
        lobbyRoom.members.add(clientHandler);
        clientHandler.currentRoom = lobbyRoom;
        System.out.println("[AoGServer] Player " + clientHandler.username + " joined LobbyRoom #" + n + " (" + lobbyRoom.members.size() + "/" + lobbyRoom.maxPlayers + ")");
        clientHandler.sendLobbySubPacket(AoGServer.buildSubOp4JoinStagingRoom(lobbyRoom));
        for (ClientHandler object : lobbyRoom.members) {
            clientHandler.sendLobbySubPacket(AoGServer.buildSubOp18AddRoomMember(object));
        }
        Object object = AoGServer.buildSubOp18AddRoomMember(clientHandler);
        for (ClientHandler clientHandler2 : lobbyRoom.members) {
            if (clientHandler2 == clientHandler) continue;
            clientHandler2.sendLobbySubPacket((byte[])object);
        }
        this.broadcastLobbyRoomUpdate(lobbyRoom);
    }

    private void leaveRoom(ClientHandler clientHandler, int n) {
        LobbyRoom lobbyRoom = clientHandler.currentRoom;
        if (lobbyRoom == null) {
            lobbyRoom = this.rooms.get(n);
        }
        if (lobbyRoom == null) {
            clientHandler.currentRoom = null;
            clientHandler.sendLobbySubPacket(new byte[]{0});
            return;
        }
        lobbyRoom.members.remove(clientHandler);
        clientHandler.currentRoom = null;
        clientHandler.sendLobbySubPacket(new byte[]{0});
        if (clientHandler == lobbyRoom.host || lobbyRoom.members.isEmpty()) {
            this.rooms.remove(lobbyRoom.roomId);
            for (ClientHandler object : new ArrayList<ClientHandler>(lobbyRoom.members)) {
                object.currentRoom = null;
                object.sendLobbySubPacket(new byte[]{0});
            }
            lobbyRoom.members.clear();
            Object object = AoGServer.buildSubOp9RemoveRoom(lobbyRoom.roomId, 0);
            for (ClientHandler clientHandler3 : this.clients) {
                if (!clientHandler3.inLobby) continue;
                clientHandler3.sendLobbySubPacket((byte[])object);
            }
            System.out.println("[AoGServer] Closed LobbyRoom #" + lobbyRoom.roomId);
        } else {
            byte[] byArray = AoGServer.buildSubOp19RemoveRoomMember(clientHandler.playerId, 0);
            for (ClientHandler clientHandler2 : lobbyRoom.members) {
                clientHandler2.sendLobbySubPacket(byArray);
            }
            this.broadcastLobbyRoomUpdate(lobbyRoom);
        }
    }

    private void broadcastLobbyRoomUpdate(LobbyRoom lobbyRoom) {
        byte[] byArray = AoGServer.buildSubOp8RoomListUpdate(lobbyRoom);
        for (ClientHandler clientHandler : this.clients) {
            if (!clientHandler.inLobby) continue;
            clientHandler.sendLobbySubPacket(byArray);
        }
    }

    synchronized void handleStartMatchRequest(ClientHandler clientHandler) {
        LobbyRoom lobbyRoom = clientHandler.currentRoom;
        if (lobbyRoom == null || lobbyRoom.host != clientHandler) {
            System.out.println("[AoGServer] Ignored Start Match (Opcode 64): " + clientHandler.username + " is not room host.");
            return;
        }
        if (lobbyRoom.members.size() < 2) {
            System.out.println("[AoGServer] Ignored Start Match (Opcode 64): Room #" + lobbyRoom.roomId + " has fewer than 2 players.");
            return;
        }
        ArrayList<ClientHandler> arrayList = new ArrayList<ClientHandler>(lobbyRoom.members);
        int n = arrayList.size();
        Object[] objectArray = new String[n];
        for (int i = 0; i < n; ++i) {
            objectArray[i] = ((ClientHandler)arrayList.get((int)i)).username;
        }
        System.out.println("[AoGServer] Launching authoritative multiplayer battle for Room #" + lobbyRoom.roomId + ": " + Arrays.toString(objectArray));
        this.rooms.remove(lobbyRoom.roomId);
        byte[] byArray = AoGServer.buildSubOp9RemoveRoom(lobbyRoom.roomId, 0);
        for (ClientHandler clientHandler2 : arrayList) {
            clientHandler2.currentRoom = null;
            clientHandler2.inLobby = false;
            clientHandler2.matchPlayerIdx = arrayList.indexOf(clientHandler2);
        }
        for (ClientHandler clientHandler2 : this.clients) {
            if (!clientHandler2.inLobby) continue;
            clientHandler2.sendLobbySubPacket(byArray);
            for (ClientHandler clientHandler3 : arrayList) {
                clientHandler2.sendLobbySubPacket(AoGServer.buildSubOp6RemoveLobbyPlayer(clientHandler3.playerId, 0));
            }
        }
        this.activeMatchPlayers.clear();
        this.activeMatchPlayers.addAll(arrayList);
        BattleEngine.MatchLaunchData matchLaunchData = this.getEngine().startMatch((String[])objectArray, lobbyRoom.options, lobbyRoom.gameType, lobbyRoom.isTeams);
        for (int i = 0; i < n; ++i) {
            ClientHandler clientHandler4 = (ClientHandler)arrayList.get(i);
            clientHandler4.sendVarShortPacket(64, matchLaunchData.playerPacket64[i]);
            clientHandler4.sendVarShortPacket(77, matchLaunchData.playerPacket77[i]);
            clientHandler4.sendFixedPacket(67, matchLaunchData.initialPacket67);
        }
        System.out.println("[AoGServer] Sent Packet 64 + Packet 77 + Packet 67 to all " + n + " match players!");
        this.lastUnpausedTurn = -1;
        int initialTurn = this.getEngine().getCurrentTurn();
        this.schedulePacket76Fallback(initialTurn);
    }

    synchronized void handleBattleAction(ClientHandler clientHandler, byte[] byArray) {
        int n = clientHandler.matchPlayerIdx;
        if (n < 0 || !this.activeMatchPlayers.contains(clientHandler)) {
            return;
        }
        BattleEngine.ActionResult actionResult = this.getEngine().handleAction(n, byArray);
        this.broadcastActionResult(actionResult);
    }

    synchronized void handleTurnTimeout(ClientHandler clientHandler) {
        int n = clientHandler.matchPlayerIdx;
        if (n < 0 || !this.activeMatchPlayers.contains(clientHandler)) {
            return;
        }
        System.out.println("[AoGServer] Turn timer expired (Opcode 74) from " + clientHandler.username + " - advancing turn.");
        BattleEngine.ActionResult actionResult = this.getEngine().forceAdvanceTurn();
        this.broadcastActionResult(actionResult);
    }

    /**
     * Schedules a fallback broadcast of Packet 76 if the active player has not transmitted
     * Opcode 73 within 2.5 seconds (125 engine ticks) following a turn change.
     *
     * Architectural Rationale:
     *   Ensures that spectators are never left with a frozen stone banner and locked map scrolling
     *   even if the active player experiences a network hiccup or client freeze during transition.
     *
     * @param targetTurn The turn round index to unpause.
     */
    private void schedulePacket76Fallback(int targetTurn) {
        if (this.keepaliveExecutor != null && !this.keepaliveExecutor.isShutdown()) {
            this.keepaliveExecutor.schedule(() -> {
                synchronized (this) {
                    this.broadcastPacket76IfPending(targetTurn);
                }
            }, 2500L, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Broadcasts Packet 76 (Server Turn Unpause & Acknowledgment) to all active match players.
     *
     * Rosetta Stone / Obfuscated FunOrb Protocol Mappings:
     *   - ha.Gb       : Match round counter.
     *   - ha.gb       : Active turn player slot index.
     *   - ic.q        : Local client player slot index.
     *   - ic.j        : Turn transition banner animation timer (starts at 100 on Packet 67).
     *   - ic.Vb       : Turn round index acknowledged by server via Packet 76.
     *   - ic.Tb       : Timer pause flag; cleared (set to false) when Packet 76 byte 0 is 0.
     *
     * FunOrb Client Banner Synchronization Lifecycle:
     *   When a turn begins (Packet 67), clients animate the stone banner into the screen center (ticks 100 -> 50).
     *   Active players continue through ticks 50 -> 0 and transmit Opcode 73 to the server.
     *   Spectators pause at tick 50 ('if (50 == this.j && this.Vb < this.xb.Gb && this.xb.gb != this.q) break;')
     *   which suppresses map scrolling ('if (0 < this.j) return -1') until Packet 76 arrives.
     *
     * Packet 76 Structure (sh.a[76] = 3, ArmiesOfGielinor.java:364-374):
     *   - Byte 0: 0 -> unpauses timer ('ic.Tb = false')
     *   - Bytes 1-2: unsigned 16-bit short -> sets 'ic.Vb = currentTurn'
     *
     * Result on Spectator Clients:
     *   'ic.Vb >= this.xb.Gb' becomes true, unfreezing 'ic.j' from 50 to 0. The banner slides off-screen,
     *   the battlefield dimming clears, and free map scrolling and clicking are completely unlocked.
     */
    synchronized void broadcastPacket76IfPending(int targetTurn) {
        if (!this.getEngine().isMatchActive()) {
            return;
        }
        int currentTurn = this.getEngine().getCurrentTurn();
        if (targetTurn != currentTurn || this.lastUnpausedTurn == currentTurn) {
            return;
        }
        this.lastUnpausedTurn = currentTurn;
        byte[] packet76 = new byte[]{
                (byte) 0,
                (byte) ((currentTurn >>> 8) & 0xFF),
                (byte) (currentTurn & 0xFF)
        };
        for (ClientHandler player : this.activeMatchPlayers) {
            player.sendFixedPacket(76, packet76);
        }
        System.out.printf("[AoGServer] Broadcasted Packet 76 (turn=%d) to unpause spectator banner across all %d players\n",
                currentTurn, this.activeMatchPlayers.size());
    }

    /**
     * Handles client turn acknowledgment signal (Opcode 73).
     * Transmitted automatically by the active client when its banner countdown reaches tick 0.
     *
     * @param clientHandler The client sending Opcode 73.
     * @param payload       The 3-byte payload [subOpcode (1 byte), elapsedTicks (2 bytes)].
     */
    synchronized void handleClientTurnReady(ClientHandler clientHandler, byte[] payload) {
        int n = clientHandler.matchPlayerIdx;
        if (n < 0 || !this.activeMatchPlayers.contains(clientHandler) || !this.getEngine().isMatchActive()) {
            return;
        }
        int currentTurn = this.getEngine().getCurrentTurn();
        int activePlayer = this.getEngine().getActivePlayer();
        System.out.printf("[AoGServer] Client %s (idx=%d) signaled turn ready (Opcode 73) for turn=%d, activePlayer=%d\n",
                clientHandler.username, n, currentTurn, activePlayer);
        this.broadcastPacket76IfPending(currentTurn);
    }

    synchronized void handleSurrenderRequest(ClientHandler clientHandler) {
        int n = clientHandler.matchPlayerIdx;
        if (n < 0 || !this.activeMatchPlayers.contains(clientHandler)) {
            return;
        }
        System.out.println("[AoGServer] Player " + clientHandler.username + " (idx=" + n + ") surrendered (Opcode 67)!");
        if (this.getEngine().isMatchActive()) {
            byte[] byArray = new byte[]{(byte)n};
            for (ClientHandler clientHandler2 : this.activeMatchPlayers) {
                clientHandler2.sendFixedPacket(71, byArray);
            }
            BattleEngine.ActionResult actionResult = this.getEngine().handleSurrender(n);
            this.broadcastActionResult(actionResult);
        }
    }

    synchronized void handleReturnToLobbyFromMatch(ClientHandler clientHandler) {
        System.out.println("[AoGServer] Player " + clientHandler.username + " returning from match to Lobby (Opcode 66).");
        this.handlePlayerMatchExit(clientHandler, false);
        clientHandler.inLobby = true;
        clientHandler.sendZeroBytePacket(66);
        this.syncLobbyStateToClient(clientHandler, true);
    }

    private void handlePlayerMatchExit(ClientHandler clientHandler, boolean bl) {
        int n = clientHandler.matchPlayerIdx;
        if (n >= 0 && this.activeMatchPlayers.contains(clientHandler) && this.getEngine().isMatchActive()) {
            byte[] byArray = new byte[]{(byte)n};
            for (ClientHandler object : this.activeMatchPlayers) {
                if (object == clientHandler && bl) continue;
                object.sendFixedPacket(71, byArray);
            }
            BattleEngine.ActionResult actionResult = this.getEngine().handleSurrender(n);
            for (ClientHandler clientHandler2 : this.activeMatchPlayers) {
                if (clientHandler2 == clientHandler) continue;
                this.sendActionResultTo(clientHandler2, actionResult);
            }
        }
        this.activeMatchPlayers.remove(clientHandler);
        clientHandler.matchPlayerIdx = -1;
    }

    private void broadcastActionResult(BattleEngine.ActionResult actionResult) {
        if (actionResult == null) {
            return;
        }
        for (ClientHandler clientHandler : this.activeMatchPlayers) {
            this.sendActionResultTo(clientHandler, actionResult);
        }
        if (actionResult.packet67 != null && this.getEngine().isMatchActive()) {
            int newTurn = this.getEngine().getCurrentTurn();
            this.schedulePacket76Fallback(newTurn);
        }
    }

    private void sendActionResultTo(ClientHandler clientHandler, BattleEngine.ActionResult actionResult) {
        for (byte[] byArray : actionResult.eventPackets68) {
            clientHandler.sendVarBytePacket(68, byArray);
        }
        Iterator<Integer> iterator = actionResult.newlyEliminatedPlayers.iterator();
        while (iterator.hasNext()) {
            int n = iterator.next();
            clientHandler.sendFixedPacket(73, new byte[]{(byte)n});
        }
        if (actionResult.packet67 != null) {
            clientHandler.sendFixedPacket(67, actionResult.packet67);
        }
        for (byte[] byArray : actionResult.postTurnEventPackets68) {
            clientHandler.sendVarBytePacket(68, byArray);
        }
        if (actionResult.packet69MatchEnd != null) {
            clientHandler.sendVarBytePacket(69, actionResult.packet69MatchEnd);
        }
    }

    private ClientHandler findClientById(long l) {
        for (ClientHandler clientHandler : this.clients) {
            if (clientHandler.playerId != l) continue;
            return clientHandler;
        }
        return null;
    }

    private static void writeStr0(DataOutputStream dataOutputStream, String string) throws IOException {
        if (string != null && !string.isEmpty()) {
            dataOutputStream.write(string.getBytes(StandardCharsets.ISO_8859_1));
        }
        dataOutputStream.writeByte(0);
    }

    private static void writeVarInt(DataOutputStream dataOutputStream, int n) throws IOException {
        if ((n & 0xFFFFFF80) != 0) {
            if ((n & 0xFFFFC000) != 0) {
                if ((n & 0xFFE00000) != 0) {
                    if ((n & 0xF0000000) != 0) {
                        dataOutputStream.writeByte(n >>> 28 & 0x7F | 0x80);
                    }
                    dataOutputStream.writeByte(n >>> 21 & 0x7F | 0x80);
                }
                dataOutputStream.writeByte(n >>> 14 & 0x7F | 0x80);
            }
            dataOutputStream.writeByte(n >>> 7 & 0x7F | 0x80);
        }
        dataOutputStream.writeByte(n & 0x7F);
    }

    private static long readLongBE(byte[] byArray, int n) {
        long l = 0L;
        for (int i = 0; i < 8; ++i) {
            l = l << 8 | (long)byArray[n + i] & 0xFFL;
        }
        return l;
    }

    private static byte[] buildSubOp23SelfId(long l) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(9);
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeByte(23);
            dataOutputStream.writeLong(l);
            dataOutputStream.flush();
            return byteArrayOutputStream.toByteArray();
        }
        catch (IOException iOException) {
            throw new RuntimeException(iOException);
        }
    }

    private static byte[] buildSubOp22Stats(int n, int n2) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(8);
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeByte(22);
            dataOutputStream.writeShort(n);
            AoGServer.writeVarInt(dataOutputStream, n2);
            dataOutputStream.flush();
            return byteArrayOutputStream.toByteArray();
        }
        catch (IOException iOException) {
            throw new RuntimeException(iOException);
        }
    }

    private static byte[] buildSubOp5LobbyPlayer(ClientHandler clientHandler) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(64);
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeByte(5);
            dataOutputStream.writeLong(clientHandler.playerId);
            AoGServer.writeStr0(dataOutputStream, clientHandler.username);
            AoGServer.writeStr0(dataOutputStream, clientHandler.username);
            AoGServer.writeStr0(dataOutputStream, clientHandler.username);
            dataOutputStream.writeInt(0);
            dataOutputStream.writeShort(1500);
            AoGServer.writeVarInt(dataOutputStream, 101);
            dataOutputStream.writeByte(0);
            dataOutputStream.writeByte(255);
            dataOutputStream.flush();
            return byteArrayOutputStream.toByteArray();
        }
        catch (IOException iOException) {
            throw new RuntimeException(iOException);
        }
    }

    private static byte[] buildSubOp6RemoveLobbyPlayer(long l, int n) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(10);
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeByte(6);
            dataOutputStream.writeLong(l);
            dataOutputStream.writeByte(n);
            dataOutputStream.flush();
            return byteArrayOutputStream.toByteArray();
        }
        catch (IOException iOException) {
            throw new RuntimeException(iOException);
        }
    }

    private static byte[] buildSubOp18AddRoomMember(ClientHandler clientHandler) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(64);
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeByte(18);
            dataOutputStream.writeLong(clientHandler.playerId);
            AoGServer.writeStr0(dataOutputStream, clientHandler.username);
            AoGServer.writeStr0(dataOutputStream, clientHandler.username);
            dataOutputStream.writeShort(1500);
            AoGServer.writeVarInt(dataOutputStream, 101);
            dataOutputStream.writeByte(0);
            dataOutputStream.writeByte(255);
            dataOutputStream.flush();
            return byteArrayOutputStream.toByteArray();
        }
        catch (IOException iOException) {
            throw new RuntimeException(iOException);
        }
    }

    private static byte[] buildSubOp19RemoveRoomMember(long l, int n) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(10);
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeByte(19);
            dataOutputStream.writeLong(l);
            dataOutputStream.writeByte(n);
            dataOutputStream.flush();
            return byteArrayOutputStream.toByteArray();
        }
        catch (IOException iOException) {
            throw new RuntimeException(iOException);
        }
    }

    private static void writeRoomInfo(DataOutputStream dataOutputStream, LobbyRoom lobbyRoom, boolean bl, boolean bl2) throws IOException {
        if (bl) {
            dataOutputStream.writeByte(lobbyRoom.members.size());
        }
        dataOutputStream.writeByte(lobbyRoom.maxPlayers);
        dataOutputStream.writeByte(lobbyRoom.gameType);
        int n = 128;
        if (lobbyRoom.ac == 2) {
            n |= 0x10;
        }
        if (bl2) {
            n |= 2;
        }
        dataOutputStream.writeByte(n);
        dataOutputStream.write(lobbyRoom.options);
        dataOutputStream.writeShort(0);
        dataOutputStream.writeInt(0);
        dataOutputStream.writeLong(lobbyRoom.host.playerId);
        if (!bl2) {
            AoGServer.writeStr0(dataOutputStream, lobbyRoom.host.username);
        } else {
            for (ClientHandler clientHandler : lobbyRoom.members) {
                AoGServer.writeStr0(dataOutputStream, clientHandler.username);
            }
        }
    }

    private static byte[] buildSubOp4JoinStagingRoom(LobbyRoom lobbyRoom) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(96);
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeByte(4);
            dataOutputStream.writeShort(lobbyRoom.roomId);
            AoGServer.writeRoomInfo(dataOutputStream, lobbyRoom, false, false);
            dataOutputStream.flush();
            return byteArrayOutputStream.toByteArray();
        }
        catch (IOException iOException) {
            throw new RuntimeException(iOException);
        }
    }

    private static byte[] buildSubOp10CurrentRoomUpdate(LobbyRoom lobbyRoom) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(96);
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeByte(10);
            AoGServer.writeRoomInfo(dataOutputStream, lobbyRoom, false, false);
            dataOutputStream.flush();
            return byteArrayOutputStream.toByteArray();
        }
        catch (IOException iOException) {
            throw new RuntimeException(iOException);
        }
    }

    private static byte[] buildSubOp8RoomListUpdate(LobbyRoom lobbyRoom) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(128);
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeByte(8);
            dataOutputStream.writeShort(lobbyRoom.roomId);
            AoGServer.writeRoomInfo(dataOutputStream, lobbyRoom, true, true);
            dataOutputStream.flush();
            return byteArrayOutputStream.toByteArray();
        }
        catch (IOException iOException) {
            throw new RuntimeException(iOException);
        }
    }

    private static byte[] buildSubOp9RemoveRoom(int n, int n2) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(4);
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeByte(9);
            dataOutputStream.writeShort(n);
            dataOutputStream.writeByte(n2);
            dataOutputStream.flush();
            return byteArrayOutputStream.toByteArray();
        }
        catch (IOException iOException) {
            throw new RuntimeException(iOException);
        }
    }

    private static byte[] buildSubOp11RoomInvite(int n) {
        return new byte[]{11, (byte)(n >>> 8 & 0xFF), (byte)(n & 0xFF)};
    }

    private static byte[] buildSubOp14or16PlayerFlag(int n, long l) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(9);
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeByte(n);
            dataOutputStream.writeLong(l);
            dataOutputStream.flush();
            return byteArrayOutputStream.toByteArray();
        }
        catch (IOException iOException) {
            throw new RuntimeException(iOException);
        }
    }

    private static byte[] buildFriendInit(String string) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeByte(4);
            AoGServer.writeStr0(dataOutputStream, string);
            dataOutputStream.writeByte(0);
            dataOutputStream.flush();
            return byteArrayOutputStream.toByteArray();
        }
        catch (IOException iOException) {
            return new byte[]{4, 0, 0};
        }
    }

    private static byte[] buildFriendEntry(String string, String string2, String string3) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            dataOutputStream.writeByte(1);
            AoGServer.writeStr0(dataOutputStream, string != null ? string : "");
            AoGServer.writeStr0(dataOutputStream, string2);
            AoGServer.writeStr0(dataOutputStream, string3 != null ? string3 : "");
            dataOutputStream.flush();
            return byteArrayOutputStream.toByteArray();
        }
        catch (IOException iOException) {
            return new byte[]{1, 0, 0, 0};
        }
    }

    private static byte[] buildFriendDone() {
        return new byte[]{2};
    }

    public static void xteaDecrypt(byte[] byArray, int n, int n2, int[] nArray) {
        int n3 = n2 / 8;
        for (int i = 0; i < n3; ++i) {
            int n4 = n + i * 8;
            int n5 = (byArray[n4] & 0xFF) << 24 | (byArray[n4 + 1] & 0xFF) << 16 | (byArray[n4 + 2] & 0xFF) << 8 | byArray[n4 + 3] & 0xFF;
            int n6 = (byArray[n4 + 4] & 0xFF) << 24 | (byArray[n4 + 5] & 0xFF) << 16 | (byArray[n4 + 6] & 0xFF) << 8 | byArray[n4 + 7] & 0xFF;
            int n7 = -1640531527;
            int n8 = n7 * 32;
            for (int j = 0; j < 32; ++j) {
                n5 -= ((n6 -= (n5 << 4 ^ n5 >>> 5) + n5 ^ n8 + nArray[n8 >>> 11 & 3]) << 4 ^ n6 >>> 5) + n6 ^ (n8 -= n7) + nArray[n8 & 3];
            }
            byArray[n4] = (byte)(n5 >>> 24);
            byArray[n4 + 1] = (byte)(n5 >>> 16);
            byArray[n4 + 2] = (byte)(n5 >>> 8);
            byArray[n4 + 3] = (byte)n5;
            byArray[n4 + 4] = (byte)(n6 >>> 24);
            byArray[n4 + 5] = (byte)(n6 >>> 16);
            byArray[n4 + 6] = (byte)(n6 >>> 8);
            byArray[n4 + 7] = (byte)n6;
        }
    }

    private static String extractTaggedUsername(byte[] byArray) {
        if (byArray == null) {
            return null;
        }
        int n = 0;
        while (n + 3 < byArray.length) {
            if (byArray[n] == 112 && byArray[n + 1] == 119 && (byArray[n + 2] == 64 || byArray[n + 2] == 58)) {
                String string;
                int n2;
                int n3;
                for (n3 = n2 = n + 3; n3 < byArray.length && byArray[n3] != 0; ++n3) {
                }
                if (n3 > n2 && !(string = new String(byArray, n2, n3 - n2, StandardCharsets.ISO_8859_1).trim()).isEmpty()) {
                    return string;
                }
            }
            ++n;
        }
        return null;
    }

    private static String extractBase37Username(byte[] byArray) {
        if (byArray == null || byArray.length < 50) {
            return null;
        }
        long l = 0L;
        for (int i = 0; i < 8; ++i) {
            l = l << 8 | (long)byArray[42 + i] & 0xFFL;
        }
        String string = AoGServer.decodeBase37(l);
        if (string != null && !string.isBlank()) {
            return string;
        }
        return null;
    }

    public static String decodeBase37(long l) {
        if (l <= 0L || l >= 6582952005840035281L || l % 37L == 0L) {
            return null;
        }
        char[] cArray = new char[]{'_', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
        int n = 0;
        for (long i = l; i != 0L; i /= 37L) {
            ++n;
        }
        StringBuilder stringBuilder = new StringBuilder(n);
        while (l != 0L) {
            long l2 = l;
            char c = cArray[(int)(l2 - (l /= 37L) * 37L)];
            stringBuilder.append(c);
        }
        stringBuilder.reverse();
        stringBuilder.setCharAt(0, Character.toUpperCase(stringBuilder.charAt(0)));
        return stringBuilder.toString();
    }

    static {
        AoGServer.loadJs5Cache();
        campaignProgress = new ConcurrentHashMap<Integer, Map<Integer, int[]>>();
        hostCoatOfArms = null;
    }

    public static class ClientHandler
    implements Runnable {
        private final AoGServer server;
        private final Socket socket;
        public final long playerId;
        private DataInputStream in;
        private DataOutputStream out;
        private IsaacCipher inCipher;
        private IsaacCipher outCipher;
        private String username = "Commander";
        private volatile boolean connected = true;
        private volatile boolean loggedIn = false;
        private volatile boolean inLobby = false;
        private volatile LobbyRoom currentRoom = null;
        private volatile int matchPlayerIdx = -1;
        public byte[] coatOfArms = null;

        public ClientHandler(AoGServer aoGServer, Socket socket) {
            this.server = aoGServer;
            this.socket = socket;
            this.playerId = aoGServer.nextPlayerId.getAndIncrement();
        }

        public String getUsername() {
            return this.username;
        }

        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public void run() {
            try {
                int n;
                int n2;
                int n3;
                this.in = new DataInputStream(new BufferedInputStream(this.socket.getInputStream()));
                this.out = new DataOutputStream(new BufferedOutputStream(this.socket.getOutputStream()));
                int n4 = this.in.readUnsignedByte();
                if (n4 == 12) {
                    this.in.readNBytes(7);
                    n4 = this.in.readUnsignedByte();
                }
                if (n4 == 15) {
                    int n5 = this.in.readInt();
                    System.out.println("[AoGServer] JS5 handshake received, client revision=" + n5);
                    this.out.writeByte(0);
                    this.out.flush();
                    while (this.connected) {
                        int n6 = this.in.read();
                        if (n6 == -1) {
                            break;
                        }
                        if (n6 == 0 || n6 == 1) {
                            int n7;
                            int n8 = this.in.readUnsignedByte();
                            long l = (long)n8 << 32 | (long)(n7 = this.in.readInt()) & 0xFFFFFFFFL;
                            byte[] byArray = JS5_ARCHIVES.get(l);
                            if (byArray != null) {
                                int n9;
                                int n10 = byArray[0] & 0xFF;
                                int n11 = (byArray[1] & 0xFF) << 24 | (byArray[2] & 0xFF) << 16 | (byArray[3] & 0xFF) << 8 | byArray[4] & 0xFF;
                                byte[] byArray2 = Arrays.copyOfRange(byArray, 5, byArray.length);
                                int n12 = n11 + (n10 != 0 ? 4 : 0);
                                this.out.writeByte(n8);
                                this.out.writeInt(n7);
                                this.out.writeByte(n10);
                                this.out.writeInt(n11);
                                int n13 = Math.min(n12, 502);
                                this.out.write(byArray2, 0, n13);
                                for (int i = n13; i < n12; i += n9) {
                                    this.out.writeByte(255);
                                    n9 = Math.min(n12 - i, 511);
                                    this.out.write(byArray2, i, n9);
                                }
                                this.out.flush();
                                continue;
                            }
                            System.err.printf("[AoGServer] Unhandled JS5 request: (%d, %d)\n", n8, n7);
                            continue;
                        }
                        if (n6 == 2 || n6 == 3 || n6 == 4 || n6 == 6) {
                            this.in.readNBytes(5);
                            continue;
                        }
                        System.err.println("[AoGServer] Unknown JS5 opcode: " + n6);
                    }
                    return;
                }
                if (n4 != 14) {
                    System.err.println("[AoGServer] Expected login handshake opcode 14, got " + n4);
                    this.disconnect();
                    return;
                }
                this.in.readUnsignedByte();
                this.out.writeByte(0);
                this.out.writeLong(0L);
                this.out.flush();
                int n14 = this.in.readUnsignedByte();
                int n15 = this.in.readUnsignedShort();
                byte[] byArray = this.in.readNBytes(n15);
                System.out.println("[AoGServer] Login opcode=" + n14 + ", payloadLength=" + n15);
                for (n3 = 13; n3 < byArray.length && byArray[n3] != 0; ++n3) {
                }
                int n16 = ++n3;
                int n17 = ++n3;
                int n18 = (byArray[n16] & 0xFF) << 8 | byArray[n17] & 0xFF;
                byte[] byArray3 = Arrays.copyOfRange(byArray, ++n3, n3 + n18);
                byte[] byArray4 = Arrays.copyOfRange(byArray, n3 += n18, byArray.length);
                BigInteger bigInteger = new BigInteger(1, byArray3);
                BigInteger bigInteger2 = bigInteger.modPow(RSA_PRIV_EXP, RSA_MODULUS);
                byte[] byArray5 = bigInteger2.toByteArray();
                int n19 = 0;
                if (byArray5.length > 0 && byArray5[0] == 0) {
                    ++n19;
                }
                ++n19;
                int[] nArray = new int[4];
                for (n2 = 0; n2 < 4; ++n2) {
                    nArray[n2] = (byArray5[n19++] & 0xFF) << 24 | (byArray5[n19++] & 0xFF) << 16 | (byArray5[n19++] & 0xFF) << 8 | byArray5[n19++] & 0xFF;
                }
                AoGServer.xteaDecrypt(byArray4, 0, byArray4.length, nArray);
                n2 = 0;
                int[] nArray2 = new int[4];
                for (n = 0; n < 4; ++n) {
                    nArray2[n] = (byArray4[n2++] & 0xFF) << 24 | (byArray4[n2++] & 0xFF) << 16 | (byArray4[n2++] & 0xFF) << 8 | byArray4[n2++] & 0xFF;
                }
                n = this.socket.getPort();
                SteamP2PTunnel.TunnelConnectionInfo tunnelConnectionInfo = SteamP2PTunnel.getTunnelConnection(n);
                String string = tunnelConnectionInfo != null ? tunnelConnectionInfo.username : null;
                String object = string;
                if (object == null || object.isBlank()) {
                    object = AoGServer.extractTaggedUsername(byArray4);
                }
                if (object == null || object.isBlank()) {
                    object = AoGServer.extractTaggedUsername(byArray5);
                }
                if (object == null || object.isBlank()) {
                    object = AoGServer.extractBase37Username(byArray4);
                }
                if ((object == null || object.isBlank()) && tunnelConnectionInfo != null && tunnelConnectionInfo.steamId != null) {
                    object = SteamManager.getInstance().getPersonaName(tunnelConnectionInfo.steamId);
                }
                if ((object == null || object.isBlank()) && tunnelConnectionInfo != null && tunnelConnectionInfo.steamId != null) {
                    object = SteamManager.getInstance().getLobbyMemberPersonaName(tunnelConnectionInfo.steamId);
                }
                if (object == null || object.isBlank()) {
                    object = SteamManager.getInstance().getFirstGuestLobbyMemberName();
                }
                if (object == null || object.isBlank()) {
                    object = this.server.clients.isEmpty() ? this.server.hostUsername : "Player " + this.playerId;
                }
                this.username = this.server.allocateUniqueUsername(object);
                int[] nArray3 = new int[4];
                int[] nArray4 = new int[4];
                for (int i = 0; i < 4; ++i) {
                    nArray3[i] = nArray2[i];
                    nArray4[i] = nArray2[i] + 50;
                }
                this.inCipher = new IsaacCipher(nArray3);
                this.outCipher = new IsaacCipher(nArray4);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
                dataOutputStream.writeLong(this.playerId);
                dataOutputStream.writeByte(0);
                dataOutputStream.writeByte(0);
                dataOutputStream.writeShort(365);
                dataOutputStream.writeByte(0);
                dataOutputStream.writeByte(0);
                dataOutputStream.write((this.username + "\u0000").getBytes(StandardCharsets.ISO_8859_1));
                dataOutputStream.writeByte(0);
                dataOutputStream.flush();
                byte[] byArray6 = byteArrayOutputStream.toByteArray();
                ClientHandler clientHandler = this;
                synchronized (clientHandler) {
                    this.out.writeByte(0);
                    this.out.writeByte(byArray6.length);
                    this.out.write(byArray6);
                    this.out.flush();
                }
                this.loggedIn = true;
                this.server.addClient(this);
                if ((this.username.equalsIgnoreCase(this.server.hostUsername) || this.server.clients.size() <= 1) && hostCoatOfArms != null) {
                    this.coatOfArms = (byte[])hostCoatOfArms.clone();
                }
                this.sendUnlockedProfileAndReadiness();
                System.out.println("[AoGServer] Login SUCCESS sent to " + this.username + " [id=" + this.playerId + "]");
                while (this.connected) {
                    int n20 = this.in.read();
                    if (n20 == -1) {
                        System.out.println("[AoGServer] Socket EOF received from " + this.username);
                        break;
                    }
                    int n21 = n20 - this.inCipher.getNextValue() & 0xFF;
                    System.out.println("[AoGServer] << Packet received: opcode=" + n21 + " (raw=" + n20 + ") from " + this.username);
                    this.handlePacket(n21);
                }
            }
            catch (Exception exception) {
                if (this.connected) {
                    System.err.println("[AoGServer] Client connection error (" + this.username + "): " + String.valueOf(exception));
                    exception.printStackTrace();
                }
            }
            finally {
                System.out.println("[AoGServer] Client loop terminating for " + this.username);
                this.disconnect();
            }
        }

        private void handlePacket(int n) throws IOException {
            switch (n) {
                case 0: 
                case 17: 
                case 68: 
                case 81: {
                    break;
                }
                case 75: {
                    this.sendUnlockedProfileAndReadiness();
                    break;
                }
                case 1: {
                    this.disconnect();
                    break;
                }
                case 8: 
                case 14: 
                case 15: 
                case 16: 
                case 18: {
                    int n2 = this.in.readUnsignedByte();
                    this.in.readNBytes(n2);
                    break;
                }
                case 13: {
                    int n3 = this.in.readUnsignedByte();
                    byte[] byArray = this.in.readNBytes(n3);
                    this.server.handleFriendClientPacket(this, byArray);
                    break;
                }
                case 9: {
                    this.server.handleEnterLobby(this);
                    break;
                }
                case 10: {
                    this.server.handleLeaveLobby(this);
                    break;
                }
                case 11: {
                    int n4 = this.in.readUnsignedByte();
                    byte[] byArray = this.in.readNBytes(n4);
                    this.server.handleLobbySubCommand(this, byArray);
                    break;
                }
                case 64: {
                    this.server.handleStartMatchRequest(this);
                    break;
                }
                case 65: {
                    int n5 = this.in.readUnsignedByte();
                    byte[] byArray = this.in.readNBytes(n5);
                    this.server.handleBattleAction(this, byArray);
                    break;
                }
                case 66: {
                    this.server.handleReturnToLobbyFromMatch(this);
                    break;
                }
                case 67: {
                    this.server.handleSurrenderRequest(this);
                    break;
                }
                case 69: {
                    this.sendUnlockedProfileAndReadiness();
                    break;
                }
                case 70: {
                    this.in.readUnsignedShort();
                    break;
                }
                case 71: {
                    this.coatOfArms = this.in.readNBytes(12);
                    System.out.println("[AoGServer] Stored coat of arms customization for " + this.username);
                    if (!this.username.equalsIgnoreCase(this.server.hostUsername) && this.server.clients.size() > 1) break;
                    AoGServer.setHostCoatOfArms(this.coatOfArms);
                    break;
                }
                case 72: {
                    this.in.readNBytes(8);
                    break;
                }
                case 73: {
                    byte[] payload73 = this.in.readNBytes(3);
                    this.server.handleClientTurnReady(this, payload73);
                    break;
                }
                case 74: {
                    this.server.handleTurnTimeout(this);
                    break;
                }
                case 76: {
                    this.in.readNBytes(10);
                    break;
                }
                case 79: {
                    int n6 = this.in.readUnsignedByte();
                    int n7 = this.in.readUnsignedByte();
                    int n8 = this.in.readUnsignedShort();
                    int n9 = this.in.readUnsignedShort();
                    int n10 = this.in.readUnsignedShort();
                    int n11 = this.in.readUnsignedByte();
                    this.in.readNBytes(16);
                    System.out.printf("[AoGServer] Received campaign mission completion from %s: campaign=%d, mission=%d, rank=%d, turns=%d, damage=%d\n", this.username, n6, n7, n8, n9, n10);
                    if (n6 < 0 || n6 >= 3 || n7 < 0 || n7 >= 64) break;
                    AoGServer.recordCampaignMission(n6, n7, n9, n10);
                    break;
                }
                case 80: {
                    int n12 = this.in.readUnsignedShort();
                    this.in.readNBytes(n12);
                    break;
                }
                default: {
                    System.out.println("[AoGServer] Received unhandled opcode " + n + " from " + this.username);
                }
            }
        }

        public void sendUnlockedProfileAndReadiness() {
            byte[] byArray = this.server.getEngine().buildUnlockedProfilePacket();
            this.sendVarShortPacket(80, byArray);
            this.sendFixedPacket(75, this.coatOfArms != null ? this.coatOfArms : new byte[12]);
            this.sendVarBytePacket(81, new byte[]{0});
        }

        public synchronized void sendKeepalive() {
            this.sendZeroBytePacket(0);
        }

        public synchronized void sendZeroBytePacket(int n) {
            if (!this.connected || this.outCipher == null) {
                return;
            }
            try {
                int n2 = n + this.outCipher.getNextValue() & 0xFF;
                this.out.writeByte(n2);
                this.out.flush();
                System.out.println("[AoGServer] >> Packet sent: opcode=" + n + " (0 bytes) to " + this.username);
            }
            catch (IOException iOException) {
                this.disconnect();
            }
        }

        public synchronized void sendFixedPacket(int n, byte[] byArray) {
            if (!this.connected || this.outCipher == null) {
                return;
            }
            try {
                int n2 = n + this.outCipher.getNextValue() & 0xFF;
                this.out.writeByte(n2);
                this.out.write(byArray);
                this.out.flush();
                System.out.println("[AoGServer] >> Packet sent: opcode=" + n + " (fixed " + byArray.length + " bytes) to " + this.username);
            }
            catch (IOException iOException) {
                this.disconnect();
            }
        }

        public synchronized void sendVarBytePacket(int n, byte[] byArray) {
            if (!this.connected || this.outCipher == null) {
                return;
            }
            try {
                int n2 = n + this.outCipher.getNextValue() & 0xFF;
                this.out.writeByte(n2);
                this.out.writeByte(byArray.length);
                this.out.write(byArray);
                this.out.flush();
                System.out.println("[AoGServer] >> Packet sent: opcode=" + n + " (varByte " + byArray.length + " bytes) to " + this.username);
            }
            catch (IOException iOException) {
                this.disconnect();
            }
        }

        public synchronized void sendVarShortPacket(int n, byte[] byArray) {
            if (!this.connected || this.outCipher == null) {
                return;
            }
            try {
                int n2 = n + this.outCipher.getNextValue() & 0xFF;
                this.out.writeByte(n2);
                this.out.writeShort(byArray.length);
                this.out.write(byArray);
                this.out.flush();
                System.out.println("[AoGServer] >> Packet sent: opcode=" + n + " (varShort " + byArray.length + " bytes) to " + this.username);
            }
            catch (IOException iOException) {
                this.disconnect();
            }
        }

        public synchronized void sendLobbySubPacket(byte[] byArray) {
            this.sendVarBytePacket(10, byArray);
        }

        public void disconnect() {
            if (!this.connected) {
                return;
            }
            this.connected = false;
            this.loggedIn = false;
            try {
                if (this.socket != null) {
                    this.socket.close();
                }
            }
            catch (IOException iOException) {
                // empty catch block
            }
            this.server.removeClient(this);
        }
    }

    public static final class LobbyRoom {
        public final int roomId;
        public final ClientHandler host;
        public final List<ClientHandler> members = new ArrayList<ClientHandler>();
        public int maxPlayers = 4;
        public int gameType = 0;
        public int ac = 1;
        public boolean isTeams = false;
        public byte[] options;

        public LobbyRoom(int n, ClientHandler clientHandler, byte[] byArray) {
            this.roomId = n;
            this.host = clientHandler;
            this.members.add(clientHandler);
            this.options = byArray;
        }
    }
}
