package src;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.swing.*;

// ตัวสลับหน้าจอทั้งเกม (หน้าหลัก / Lobby / Join Room / หน้าเล่นเกม) และเป็นตัวรับข้อความจาก Server
public class SencePanel extends JPanel implements ActionListener, GameClient.Listener {
    private ImageIcon bg   = new ImageIcon("src/images/background.jpg");
    private ImageIcon bglobby   = new ImageIcon("src/images/lobbyroom.png");
    private ImageIcon bgJoin   = new ImageIcon("src/images/๋JoinRoom.jpg");
    private ImageIcon bgGame   = new ImageIcon("src/images/bgLevel1.jpg");
    private ImageIcon CBtn = new ImageIcon(
    new ImageIcon("src/images/btn1.jpg")
        .getImage()
        .getScaledInstance(350, 250, Image.SCALE_SMOOTH)
    );

    private ImageIcon JBtn = new ImageIcon(
    new ImageIcon("src/images/btn2.jpg")
        .getImage()
        .getScaledInstance(350, 250, Image.SCALE_SMOOTH)
    );

    private JButton CreateBtn;
    private JButton JoinBtn;
    private JButton backlobbyButton;
    private JButton backJoinButton;
    private JButton StartBtn;
    private JButton JoinRoomBtn;
    private JLabel lobbyStatusLabel;   // ข้อความในกล่อง Lobby (IP ห้อง / จำนวนคน)
    private JTextField ipField;        // ช่องกรอก IP ของห้องในหน้า Join Room
    private GamePlayPanel gamePlayPanel; // หน้าเล่นเกม
    private JFrame frame;
    private CardLayout cardLayout;
    private String currentPanel = "mainPanel"; //เช็คว่าตอนนี้อยู่ที่ panel ไหน

    // ระบบเครือข่าย
    private GameServer server;   // มีเฉพาะเครื่อง Host
    private GameClient client;   // ทุกเครื่องมี (Host ก็ต่อเข้า Server ตัวเอง)
    private List<String> roomIp;  // IP ห้องที่แสดงใน Lobby

    //Constructor
public SencePanel(JFrame frame) {
        this.frame = frame;
       cardLayout = new CardLayout();
        setLayout(cardLayout);

    add(createMainPanel(), "mainPanel");
    add(createLobbyPanel(), "lobbyPanel");
    add(createJoinRoomPanel(), "joinRoomPanel");
    add(createGamePanel(), "GamePanel");

    // จัดตำแหน่งปุ่ม/ช่องกรอก ให้ตรงกับรูปพื้นหลัง ทุกครั้งที่ขนาดหน้าต่างเปลี่ยน
    addComponentListener(new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) { layoutOnImage(); }
    });
    }

    // วางปุ่มตามสัดส่วนของรูปพื้นหลัง (รูปถูกยืดเต็มจอ ตำแหน่งจึงคิดเป็น % ของจอ)
    private void layoutOnImage() {
        placeRel(backlobbyButton, 0.015, 0.021, 0.113, 0.053);
        placeRel(backJoinButton, 0.015, 0.021, 0.113, 0.053);
        placeRel(lobbyStatusLabel, 0.19, 0.245, 0.62, 0.485);
        placeRel(StartBtn, 0.4225, 0.796, 0.155, 0.074);
        placeRel(ipField, 0.345, 0.47, 0.31, 0.06);
        placeRel(JoinRoomBtn, 0.4175, 0.792, 0.1625, 0.078);
    }

    private void placeRel(JComponent c, double fx, double fy, double fw, double fh) {
        int w = getWidth(), h = getHeight();
        c.setBounds((int) (fx * w), (int) (fy * h), (int) (fw * w), (int) (fh * h));
    }

    // ทำปุ่มให้โปร่งใส (ใช้รูปปุ่มที่อยู่บนพื้นหลังแทน)
    private void makeInvisibleButton(JButton b) {
        b.setContentAreaFilled(false); //ทำให้ปุ่มไม่แสดงพื้นหลัง
        b.setBorderPainted(false); //ทำให้ปุ่มไม่แสดงขอบ
        b.setFocusPainted(false); //ทำให้ปุ่มไม่แสดงขอบตอนโฟกัส
        b.setOpaque(false); //ทำให้ปุ่มโปร่งใส
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addActionListener(this);
    }

    //หน้าหลัก
    private JPanel createMainPanel() {

        JPanel mainPanel = new JPanel();
        mainPanel.setOpaque(false);
        mainPanel.setLayout(null);

        //ปุ่ม Create
    CreateBtn = new JButton(CBtn);
        CreateBtn.setBounds(frame.getWidth() / 2 - 300, frame.getHeight() / 2 - 150 , 200, 100);
        makeInvisibleButton(CreateBtn);

       //ปุ่ม Join
    JoinBtn = new JButton(JBtn);
        JoinBtn.setBounds(frame.getWidth() / 2 -300, frame.getHeight() / 2  , 200, 100);
        makeInvisibleButton(JoinBtn);

        mainPanel.add(CreateBtn);
        mainPanel.add(JoinBtn);
        return mainPanel;
    }

    //หน้าห้อง lobby
    private JPanel createLobbyPanel() {
        JPanel lobbyPanel = new JPanel();
        lobbyPanel.setOpaque(false);
        lobbyPanel.setLayout(null);

        //ปุ่มback
        backlobbyButton= new JButton();
        makeInvisibleButton(backlobbyButton);

        //ปุ่ม Start (กดได้เฉพาะ Host)
        StartBtn = new JButton();
        makeInvisibleButton(StartBtn);

        //ข้อความสถานะห้อง
        lobbyStatusLabel = new JLabel("", SwingConstants.CENTER);
        lobbyStatusLabel.setFont(new Font("Tahoma", Font.BOLD, 32));
        lobbyStatusLabel.setForeground(new Color(70, 40, 10));

        lobbyPanel.add(backlobbyButton);
        lobbyPanel.add(StartBtn);
        lobbyPanel.add(lobbyStatusLabel);

        return lobbyPanel;
    }

    //หน้าห้อง join room
    private JPanel createJoinRoomPanel() {
        JPanel joinRoomPanel = new JPanel();
        joinRoomPanel.setOpaque(false);
        joinRoomPanel.setLayout(null);

        //ปุ่ม Back
        backJoinButton= new JButton();
        makeInvisibleButton(backJoinButton);

        //ช่องกรอก IP ของห้อง (Room ID = IP เครื่อง Host)
        ipField = new JTextField("127.0.0.1");
        ipField.setFont(new Font("Tahoma", Font.BOLD, 32));
        ipField.setHorizontalAlignment(JTextField.CENTER);
        ipField.setOpaque(false);
        ipField.setBorder(null);
        ipField.addActionListener(this); // กด Enter = Join

        //ปุ่ม Join Room
        JoinRoomBtn = new JButton();
        makeInvisibleButton(JoinRoomBtn);

        joinRoomPanel.add(backJoinButton);
        joinRoomPanel.add(ipField);
        joinRoomPanel.add(JoinRoomBtn);
        return joinRoomPanel;
    }

    //หน้าด่านเกม
    private JPanel createGamePanel() {
        gamePlayPanel = new GamePlayPanel();
        return gamePlayPanel;
    }

    // เปลี่ยนหน้าจอ
    private void showPanel(String name) {
        currentPanel = name;
        cardLayout.show(this, name);
        repaint();
    }

    // กด Create: เปิด Server บนเครื่องนี้ แล้วต่อเข้าไปเป็นผู้เล่นคนแรก (Host)
    private void hostRoom() {
        try {
            server = new GameServer();
            server.start();
            client = new GameClient("127.0.0.1", GameServer.PORT, this);
            roomIp = GameServer.getLocalIps();
            lobbyStatusLabel.setText("<html><center>Room ID (IP): " + roomIp + "<br>กำลังรอผู้เล่น...</center></html>");
            showPanel("lobbyPanel");
        } catch (IOException ex) {
            leaveRoom();
            JOptionPane.showMessageDialog(this, "เปิดห้องไม่ได้ (พอร์ต " + GameServer.PORT + " อาจถูกใช้อยู่)\n" + ex.getMessage());
        }
    }
    
    // กด Join Room: ต่อเข้า Server ตาม IP ที่กรอก
    private void joinRoom() {
        List<String> ip = Arrays.asList(ipField.getText().trim().split(","));
        if (ip.isEmpty()) return;
        try {
            client = new GameClient(ip.get(0), GameServer.PORT, this);
            roomIp = ip;
            lobbyStatusLabel.setText("กำลังเข้าห้อง...");
            showPanel("lobbyPanel");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "เชื่อมต่อห้อง " + ip + " ไม่ได้");
        }
    }

    // ออกจากห้อง: ตัดการเชื่อมต่อ และปิด Server ถ้าเราเป็น Host
    private void leaveRoom() {
        gamePlayPanel.stopGameLoop();
        if (client != null) { client.close(); client = null; }
        if (server != null) { server.stop(); server = null; }
    }

    // รับข้อความจาก Server (ถูกเรียกบน Swing thread โดย GameClient)
    @Override
    public void onMessage(String msg) {
        String[] p = msg.split("\\|", -1);
        switch (p[0]) {
            case "LOBBY" -> { // LOBBY|จำนวนคน|isHost
                boolean isHost = Boolean.parseBoolean(p[2]);
                StartBtn.setEnabled(isHost);
                lobbyStatusLabel.setText("<html><center>Room ID (IP): " + roomIp
                        + "<br>ผู้เล่นในห้อง: " + p[1] + " / " + GameServer.MAX_PLAYERS
                        + "<br>" + (isHost ? "คุณคือหัวห้อง กด START เพื่อเริ่ม" : "รอหัวห้องกด START") + "</center></html>");
            }
            case "START" -> { // START|เลขผู้เล่นของเรา
                gamePlayPanel.startGame(client, Integer.parseInt(p[1]));
                showPanel("GamePanel");
            }
            case "FULL" -> {
                leaveRoom();
                showPanel("joinRoomPanel");
                JOptionPane.showMessageDialog(this, "ห้องเต็มหรือเกมเริ่มไปแล้ว");
            }
            default -> gamePlayPanel.handleServerMessage(msg); // STATE / RESULT / GAMEOVER
        }
    }

    // หลุดจาก Server (เช่น Host ปิดเกม)
    @Override
    public void onDisconnected() {
        leaveRoom();
        showPanel("mainPanel");
        JOptionPane.showMessageDialog(this, "หลุดการเชื่อมต่อกับห้อง");
    }

    @Override
        public void actionPerformed(ActionEvent e) {
            if(e.getSource() == CreateBtn) {      //สร้างห้อง แล้วเข้าสู่ lobbyPanel
                hostRoom();

            } else if(e.getSource() == JoinBtn) { //เข้าสู่ joinRoomPanel
                showPanel("joinRoomPanel");

            } else if(e.getSource() == backlobbyButton || e.getSource() == backJoinButton) { //ออกจากห้อง กลับสู่ mainPanel
                leaveRoom();
                showPanel("mainPanel");

            }
            else if(e.getSource() == StartBtn) { //Host สั่งเริ่มเกม (หน้าเกมจะเปิดเมื่อ Server ตอบ START)
                if (client != null) client.send("START");

            } else if(e.getSource() == JoinRoomBtn || e.getSource() == ipField) { //เข้าห้อง
                joinRoom();
            }
        }

         @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                if(currentPanel.equals("mainPanel")){
                    g.drawImage(bg.getImage(), 0, 0, getWidth(), getHeight(), this);
                }
                else if(currentPanel.equals("lobbyPanel")){
                    g.drawImage(bglobby.getImage(), 0, 0, getWidth(), getHeight(), this);
                }
                else if(currentPanel.equals("joinRoomPanel")){
                    g.drawImage(bgJoin.getImage(), 0, 0, getWidth(), getHeight(), this);
                }
                else if(currentPanel.equals("GamePanel")){
                    g.drawImage(bgGame.getImage(), 0, 0, getWidth(), getHeight(), this);
                }
            }
}
