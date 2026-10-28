<<<<<<< HEAD
package src;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import javax.swing.SwingUtilities;

/**
 * Client ของเกม (ทุกคนรวมถึง Host มีตัวนี้ 1 ตัว)
 * - เชื่อมต่อไปที่ GameServer
 * - มี thread คอยอ่านข้อความจาก Server แล้วส่งต่อให้ Listener บน Swing thread (ปลอดภัยต่อการแก้ UI)
 */
public class GameClient {

    /** ผู้รับข้อความจาก Server (SencePanel เป็นคนรับ) */
    public interface Listener {
        void onMessage(String msg);
        void onDisconnected();
    }

    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    private final Listener listener;
    private volatile boolean closed = false; // ปิดเองหรือเปล่า (ปิดเองไม่ต้องแจ้งว่าหลุด)

    // เชื่อมต่อ Server (รอสูงสุด 3 วินาที) แล้วเริ่มอ่านข้อความ
    public GameClient(String host, int port, Listener listener) throws IOException {
        this.listener = listener;
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 3000);
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

        Thread reader = new Thread(this::readLoop, "Client-reader");
        reader.setDaemon(true);
        reader.start();
    }

    // อ่านข้อความทีละบรรทัดจนกว่าจะหลุด
    private void readLoop() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                String msg = line;
                SwingUtilities.invokeLater(() -> listener.onMessage(msg));
            }
        } catch (IOException ignored) {
            // หลุดการเชื่อมต่อ
        }
        if (!closed) SwingUtilities.invokeLater(listener::onDisconnected);
    }

    // ส่งข้อความไป Server
    public synchronized void send(String msg) {
        out.println(msg);
    }

    public void close() {
        closed = true;
        try { socket.close(); } catch (IOException ignored) {}
    }
}
=======
package src;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import javax.swing.SwingUtilities;

/**
 * Client ของเกม (ทุกคนรวมถึง Host มีตัวนี้ 1 ตัว)
 * - เชื่อมต่อไปที่ GameServer
 * - มี thread คอยอ่านข้อความจาก Server แล้วส่งต่อให้ Listener บน Swing thread (ปลอดภัยต่อการแก้ UI)
 */
public class GameClient {

    /** ผู้รับข้อความจาก Server (SencePanel เป็นคนรับ) */
    public interface Listener {
        void onMessage(String msg);
        void onDisconnected();
    }

    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    private final Listener listener;
    private volatile boolean closed = false; // ปิดเองหรือเปล่า (ปิดเองไม่ต้องแจ้งว่าหลุด)

    // เชื่อมต่อ Server (รอสูงสุด 3 วินาที) แล้วเริ่มอ่านข้อความ
    public GameClient(String host, int port, Listener listener) throws IOException {
        this.listener = listener;
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 3000);
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

        Thread reader = new Thread(this::readLoop, "Client-reader");
        reader.setDaemon(true);
        reader.start();
    }

    // อ่านข้อความทีละบรรทัดจนกว่าจะหลุด
    private void readLoop() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                String msg = line;
                SwingUtilities.invokeLater(() -> listener.onMessage(msg));
            }
        } catch (IOException ignored) {
            // หลุดการเชื่อมต่อ
        }
        if (!closed) SwingUtilities.invokeLater(listener::onDisconnected);
    }

    // ส่งข้อความไป Server
    public synchronized void send(String msg) {
        out.println(msg);
    }

    public void close() {
        closed = true;
        try { socket.close(); } catch (IOException ignored) {}
    }
}
>>>>>>> c5947919378dd743a9f8be9b9dd4ac26e9d396d5
