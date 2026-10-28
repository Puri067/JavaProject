package src;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server ของเกม (รันบนเครื่องของคนที่กด Create)
 * - รับผู้เล่นได้สูงสุด 4 คน คนแรกที่ต่อเข้ามา = หัวห้อง (Host) มีสิทธิ์กด Start
 * - ถือ GameLogic ตัวจริง อัปเดตเกมทุก tick แล้วส่งสถานะไปทุก Client
 *
 * โปรโตคอล (ข้อความ 1 บรรทัด คั่นด้วย |)
 *   Server -> Client : LOBBY|จำนวนคน|isHost, START|เลขผู้เล่นของเรา, STATE|..., RESULT|true/false, GAMEOVER|winner, FULL
 *   Client -> Server : START, ANSWER|ตัวเลข
 */
public class GameServer {

    public static final int PORT = 5000;
    public static final int MAX_PLAYERS = 4;

    private ServerSocket serverSocket;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>(); // ผู้เล่นที่ต่ออยู่ (ลำดับ = ลำดับเข้าห้อง)
    private volatile GamePlayPanel.GameLogic logic;  // null = ยังอยู่ใน Lobby
    private volatile boolean running = false;

    // เปิด Server และเริ่มรอผู้เล่นเชื่อมต่อ
    public void start() throws IOException {
        serverSocket = new ServerSocket(PORT);
        running = true;
        Thread t = new Thread(this::acceptLoop, "Server-accept");
        t.setDaemon(true);
        t.start();
    }

    // ปิด Server และตัดการเชื่อมต่อทุกคน
    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
        for (ClientHandler c : clients) c.close();
        clients.clear();
    }

    // รอรับผู้เล่นใหม่ไปเรื่อย ๆ
    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                synchronized (this) {
                    if (logic != null || clients.size() >= MAX_PLAYERS) { // ห้องเต็มหรือเกมเริ่มแล้ว
                        handler.send("FULL");
                        handler.close();
                        continue;
                    }
                    clients.add(handler);
                }
                handler.start();
                broadcastLobby();
            } catch (IOException e) {
                if (running) e.printStackTrace(); // ถ้าปิด Server เองจะเกิด exception ตรงนี้ ไม่ต้องสนใจ
            }
        }
    }

    // แจ้งจำนวนคนในห้องให้ทุกคน (คนแรกในลิสต์ = Host)
    private void broadcastLobby() {
        for (int i = 0; i < clients.size(); i++) {
            clients.get(i).send("LOBBY|" + clients.size() + "|" + (i == 0));
        }
    }

    private void broadcast(String msg) {
        for (ClientHandler c : clients) c.send(msg);
    }

    // Host กด Start: กำหนดเลขผู้เล่นตามลำดับ แล้วเริ่ม Game Loop
    private synchronized void startGame(ClientHandler from) {
        if (logic != null || clients.isEmpty() || clients.get(0) != from) return; // เริ่มได้ครั้งเดียว และเฉพาะ Host
        for (int i = 0; i < clients.size(); i++) {
            clients.get(i).playerIdx = i;
            clients.get(i).send("START|" + i);
        }
        logic = new GamePlayPanel.GameLogic(clients.size());
        Thread t = new Thread(this::gameLoop, "Server-gameLoop");
        t.setDaemon(true);
        t.start();
    }

    // อัปเดตเกมทุก TICK_MS แล้วส่งสถานะให้ทุกคน จนกว่าเกมจะจบ
    private void gameLoop() {
        while (running) {
            logic.update();
            broadcast(logic.getStateString()); // ถ้าเกมจบ ข้อความนี้จะเป็น GAMEOVER
            if (logic.isGameOver()) break;
            try {
                Thread.sleep(GamePlayPanel.GameLogic.TICK_MS);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    /** ดูแลผู้เล่น 1 คน: อ่านข้อความจาก Client ใน thread ของตัวเอง */
    private class ClientHandler extends Thread {
        private final Socket socket;
        private final PrintWriter out;
        private final BufferedReader in;
        private volatile int playerIdx = -1; // เลขผู้เล่น (กำหนดตอนเริ่มเกม)

        ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    handle(line);
                }
            } catch (IOException ignored) {
                // หลุดการเชื่อมต่อ
            }
            onDisconnect();
        }

        // จัดการคำสั่งจาก Client
        private void handle(String line) {
            String[] p = line.split("\\|");
            if (p[0].equals("START")) {
                startGame(this);
            } else if (p[0].equals("ANSWER") && p.length > 1 && logic != null && playerIdx >= 0) {
                try {
                    boolean correct = logic.checkAnswer(playerIdx, Integer.parseInt(p[1].trim()));
                    send("RESULT|" + correct);
                    broadcast(logic.getStateString()); // ส่งสถานะใหม่ทันที ไม่ต้องรอ tick
                } catch (NumberFormatException ignored) {
                    // ไม่ใช่ตัวเลข ไม่สนใจ
                }
            }
        }

        // ผู้เล่นออก: ถ้าอยู่ Lobby อัปเดตจำนวนคน, ถ้าอยู่ในเกมให้ถือว่าตกรอบ
        private void onDisconnect() {
            clients.remove(this);
            close();
            if (!running) return;
            if (logic == null) {
                broadcastLobby();
            } else {
                logic.removePlayer(playerIdx);
                broadcast(logic.getStateString());
            }
        }

        synchronized void send(String msg) {
            out.println(msg);
        }

        void close() {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    /// คืนรายการ IP แบบ IPv4 ทุกตัวของเครื่อง (WiFi / LAN / VPN) ไม่ต้องเดาว่าเครื่องไหนใช้การ์ดอะไร
    public static List<String> getLocalIps() {
        List<String> ips = new ArrayList<>();                      // รายการเก็บ IP ที่หาเจอ
        try {
            // วนดูการ์ดเครือข่ายทุกใบในเครื่อง
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nic.isUp() || nic.isLoopback()) continue;     // ข้ามการ์ดที่ปิดอยู่ และ loopback (127.0.0.1)
                for (InetAddress addr : Collections.list(nic.getInetAddresses())) { // วน IP ทุกตัวของการ์ดนั้น
                    if (addr instanceof Inet4Address) {            // เอาเฉพาะ IPv4 (instanceof = เช็คชนิดของ object)
                        ips.add(addr.getHostAddress());            // เก็บ IP เป็นข้อความ
                    }
                }
            }
        } catch (SocketException e) {
            // อ่านรายการการ์ดไม่ได้ ก็ปล่อยให้ลิสต์ว่างไว้
        }
        if (ips.isEmpty()) ips.add("127.0.0.1");                   // ไม่เจอเลย ใช้เครื่องตัวเอง (เล่นคนเดียวทดสอบ)
        return ips;                                                // ส่งรายการ IP กลับไป
    }
}
