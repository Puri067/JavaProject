<<<<<<< HEAD
package src;

import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Random;
import javax.swing.*;


public class GamePlayPanel extends JPanel implements Runnable {

    public static class GameLogic {

        // พิกัดทั้งหมดอยู่ใน "โลกเกม" สูง 600 หน่วย แล้วหน้าจอค่อยย่อ/ขยายตามขนาดจริง
        public static final int WORLD_HEIGHT = 600;
        public static final int LAVA_START_Y = 550;    // ตำแหน่ง Y เริ่มต้นของลาวา (ล่างจอ)
        public static final int PILLAR_START_Y = 450;  // ตำแหน่ง Y เริ่มต้นของยอดเสา
        public static final int PILLAR_STEP = 10;      // เสาขึ้นกี่หน่วยต่อ 1 คะแนน
        public static final int PILLAR_MIN_Y = 50;     // เสาสูงได้มากสุดแค่นี้ (กันทะลุขอบบน)
        public static final int PILLAR_GOAL_Y = 120;   // ยอดเสาถึง Y นี้ก่อน = ชนะทันที
        public static final int LAVA_STEP = 1;         // ลาวาขึ้นกี่หน่วยต่อ 1 tick
        public static final long TICK_MS = 500;        // Server อัปเดตเกมทุก ๆ กี่มิลลิวินาที
        public static final long ROUND_TIME_MS = 10000; // เวลาต่อข้อ

        private final int[] pillarY;       // Y ของยอดเสาแต่ละคน (index = เลขผู้เล่น)
        private final boolean[] alive;     // ผู้เล่นแต่ละคนยังรอดไหม
        private final int[] score;         // คะแนนสะสมของแต่ละคน
        private final boolean[] answered;  // แต่ละคนส่งคำตอบในข้อนี้แล้วหรือยัง
        private int lavaY = LAVA_START_Y;  // Y ปัจจุบันของลาวา
        private boolean gameOver = false;
        private int winnerIdx = -1;        // index ผู้ชนะ (-1 = ยังไม่มี)

        // สถานะของข้อปัจจุบัน
        private int roundNo = 0;            // เลขข้อ (Client ใช้รู้ว่าขึ้นข้อใหม่แล้ว)
        private int correctCount = 0;       // ข้อนี้มีคนตอบถูกไปแล้วกี่คน (ใช้เป็นลำดับคิดคะแนน)
        private int aliveAtRoundStart = 0;  // จำนวนคนที่รอดตอนเริ่มข้อ
        private long roundStartMs = 0;      // เวลาที่เริ่มข้อนี้
        private String questionText = "";   // ข้อความโจทย์
        private int answer;                 // คำตอบของโจทย์
        private final Random random = new Random();

        public GameLogic(int playerCount) {
            pillarY = new int[playerCount];
            alive = new boolean[playerCount];
            score = new int[playerCount];
            answered = new boolean[playerCount];
            for (int i = 0; i < playerCount; i++) {
                pillarY[i] = PILLAR_START_Y;  // ทุกคนเริ่มที่ความสูงเท่ากัน
                alive[i] = true;
            }
            nextQuestion();
        }

        // สุ่มโจทย์ใหม่ (บวก / ลบ / คูณ) แทนคลังโจทย์แบบตายตัว เล่นได้ไม่จำกัดข้อ
        private void nextQuestion() {
            String old = questionText;
            do {
                int a = random.nextInt(50) + 1;
                int b = random.nextInt(50) + 1;
                switch (random.nextInt(3)) {
                    case 0 -> { questionText = a + " + " + b; answer = a + b; }
                    case 1 -> { // เอาเลขมากลบเลขน้อย คำตอบจะไม่ติดลบ
                        questionText = Math.max(a, b) + " - " + Math.min(a, b);
                        answer = Math.abs(a - b);
                    }
                    default -> { // สูตรคูณ 1-12
                        a = random.nextInt(12) + 1;
                        b = random.nextInt(12) + 1;
                        questionText = a + " x " + b;
                        answer = a * b;
                    }
                }
            } while (questionText.equals(old)); // กันโจทย์ซ้ำกับข้อที่เพิ่งจบ

            // รีเซ็ตสถานะของข้อใหม่
            roundNo++;
            roundStartMs = System.currentTimeMillis();
            correctCount = 0;
            aliveAtRoundStart = 0;
            for (int i = 0; i < answered.length; i++) {
                answered[i] = false;
                if (alive[i]) aliveAtRoundStart++;
            }
        }

        // ข้อจะจบก่อนหมดเวลา ถ้าทุกคนที่ยังรอดส่งคำตอบครบแล้ว
        private void checkRoundComplete() {
            if (gameOver) return;
            for (int i = 0; i < alive.length; i++) {
                if (alive[i] && !answered[i]) return; // ยังมีคนไม่ส่ง
            }
            nextQuestion();
        }

        // ตรวจคำตอบของผู้เล่น คืนค่า true ถ้าตอบถูก
        public synchronized boolean checkAnswer(int idx, int ans) {
            if (gameOver || !isValid(idx) || !alive[idx] || answered[idx]) return false; // ตกรอบ/ตอบซ้ำ = ไม่รับ
            answered[idx] = true;

            boolean correct = (ans == answer);
            if (correct) {
                correctCount++;
                int points = Math.max(1, aliveAtRoundStart - correctCount + 1); // ตอบถูกก่อนได้มากกว่า แต่ได้อย่างน้อย 1
                score[idx] += points;
                raisePillar(idx, points);
            }
            checkRoundComplete();
            return correct;
        }

        // ดันเสาขึ้นตามคะแนน ถ้าถึงเส้นชัยก่อน = ชนะ
        private void raisePillar(int idx, int points) {
            pillarY[idx] = Math.max(PILLAR_MIN_Y, pillarY[idx] - points * PILLAR_STEP); // Y ลด = เสาสูงขึ้น
            if (!gameOver && pillarY[idx] <= PILLAR_GOAL_Y) {
                gameOver = true;
                winnerIdx = idx;
            }
        }

        // Server เรียกทุก tick: ลาวาขึ้น เช็คคนจม และเช็คหมดเวลาข้อ
        public synchronized void update() {
            if (gameOver) return;
            lavaY -= LAVA_STEP;
            checkElimination();
            if (!gameOver && System.currentTimeMillis() - roundStartMs >= ROUND_TIME_MS) {
                nextQuestion(); // หมดเวลา ขึ้นข้อใหม่
            }
        }

        // ผู้เล่นหลุดการเชื่อมต่อระหว่างเกม = ถือว่าตกรอบ
        public synchronized void removePlayer(int idx) {
            if (gameOver || !isValid(idx) || !alive[idx]) return;
            alive[idx] = false;
            checkAllOut();
            checkRoundComplete();
        }

        // ตรวจผู้เล่นที่โดนลาวา: แมวยืนอยู่บนยอดเสา ลาวาถึงยอดเสา = โดนแมว = แพ้ออกจากเกม
        private void checkElimination() {
            boolean someoneOut = false;
            for (int i = 0; i < pillarY.length; i++) {
                if (alive[i] && lavaY <= pillarY[i]) {
                    alive[i] = false;
                    someoneOut = true;
                }
            }
            if (!someoneOut) return;
            checkAllOut();
            checkRoundComplete(); // ถ้าคนที่ยังไม่ตอบโดนลาวาหมดแล้ว ข้อนี้ก็จบ
        }

        // ชนะได้ทางเดียวคือเสาถึงเส้นชัย ถ้าทุกคนโดนลาวาหมดก่อน = เกมจบแบบไม่มีผู้ชนะ
        private void checkAllOut() {
            if (gameOver) return;
            for (boolean a : alive) {
                if (a) return; // ยังมีคนเล่นอยู่
            }
            gameOver = true;
            winnerIdx = -1;
        }

        private boolean isValid(int idx) { 
            return idx >= 0 && idx < pillarY.length;// ตรวจ index ของผู้เล่นให้ถูกต้อง
        }

        public synchronized boolean isGameOver() { return gameOver; } 

        
        public synchronized String getStateString() {
            if (gameOver) return "GAMEOVER|" + winnerIdx;
            long timeLeft = Math.max(0, ROUND_TIME_MS - (System.currentTimeMillis() - roundStartMs));
            StringBuilder sb = new StringBuilder("STATE|");
            sb.append(roundNo).append('|').append(lavaY).append('|')
              .append(timeLeft).append('|').append(questionText).append('|');
            for (int i = 0; i < pillarY.length; i++) {
                if (i > 0) sb.append(';');//
                sb.append(pillarY[i]).append(',').append(alive[i]).append(',').append(score[i]);
            }
            return sb.toString();
        }
    }

    // =====================================================================
    //  GamePlayPanel : หน้าจอเกมฝั่ง Client
    // =====================================================================

    // Game Loop: สั่งวาดจอใหม่ 60 ครั้ง/วินาที
    private Thread gameThread;
    private volatile boolean isRunning = false;

    // สถานะเกมล่าสุดที่ได้รับจาก Server (อัปเดตบน Swing thread เท่านั้น)
    private int[] pillarY;            // Y ยอดเสาของทุกคน (null = ยังไม่ได้รับสถานะ)
    private boolean[] alive;          // ใครยังรอดบ้าง
    private int[] scores;             // คะแนนทุกคน
    private int lavaY = -1;           // Y ของลาวา (-1 = ยังไม่มีข้อมูล)
    private String currentQuestion = "";
    private int timeLeft = 0;         // เวลาที่เหลือ (วินาที)
    private int roundNo = 0;          // เลขข้อปัจจุบัน
    private int answeredRound = -1;   // ข้อล่าสุดที่เราส่งคำตอบไปแล้ว (กันส่งซ้ำ)
    private String feedback = "";     // ข้อความ "ถูก!/ผิด!" หลังส่งคำตอบ
    private long feedbackUntil = 0;   // แสดง feedback ถึงเวลานี้ (ms)
    private int winnerIdx = -1;       // ผู้ชนะ (ใช้ตอนจบเกม)
    private boolean gameOver = false;

    private int myIdx = -1;           // เราคือผู้เล่นคนที่เท่าไร (0-3)
    private GameClient client;        // ใช้ส่งคำตอบไป Server

    // ช่องกรอกคำตอบด้านล่าง
    private final JPanel bottomPanel;
    private final JTextField txtAnswer;
    private final JButton btnSubmit;

    // รูปภาพ: ตัวละครแมว 4 สี + พื้นผิวเสา (ไม่มีรูปลาวา จึงวาดด้วยสีแทน)
    private final Image[] playerImages = new Image[4];
    private final Image platformImage = new ImageIcon("src/images/plantform.png").getImage();
    private final Image dirtImage = new ImageIcon("src/images/dirt.png").getImage();
    

    public GamePlayPanel() {
        setLayout(new BorderLayout());
        setOpaque(false); // โปร่งใส ให้เห็นฉากหลัง bgLevel1 ที่ SencePanel วาดไว้

        for (int i = 0; i < playerImages.length; i++) {
            playerImages[i] = new ImageIcon("src/images/cat" + (i + 1) + ".png").getImage();
        }

        // แผงกรอกคำตอบด้านล่าง
        bottomPanel = new JPanel();
        bottomPanel.setBackground(new Color(40, 40, 40));

        JLabel lblAnswer = new JLabel("คำตอบ: ");
        lblAnswer.setForeground(Color.WHITE);
        lblAnswer.setFont(new Font("Tahoma", Font.BOLD, 20));

        txtAnswer = new JTextField(12);
        txtAnswer.setFont(new Font("Tahoma", Font.PLAIN, 20));

        btnSubmit = new JButton("ส่งคำตอบ");
        btnSubmit.setFont(new Font("Tahoma", Font.BOLD, 18));

        bottomPanel.add(lblAnswer);
        bottomPanel.add(txtAnswer);
        bottomPanel.add(btnSubmit);
        add(bottomPanel, BorderLayout.SOUTH);

        // กด Enter หรือกดปุ่ม = ส่งคำตอบ
        ActionListener submitAction = e -> submitAnswer();
        txtAnswer.addActionListener(submitAction);
        btnSubmit.addActionListener(submitAction);
    }

    // ส่งคำตอบไป Server (ต้องเป็นตัวเลข และส่งได้ข้อละครั้ง)
    private void submitAnswer() {
        String text = txtAnswer.getText().trim();
        txtAnswer.setText("");
        if (text.isEmpty() || client == null || gameOver) return;
        if (answeredRound == roundNo) { showFeedback("ข้อนี้ตอบไปแล้ว รอข้อถัดไป"); return; }
        try {
            client.send("ANSWER|" + Integer.parseInt(text));
            answeredRound = roundNo;
        } catch (NumberFormatException ex) {
            showFeedback("กรอกเป็นตัวเลขเท่านั้น");
        }
    }

    // แสดงข้อความสั้น ๆ 1.5 วินาที
    private void showFeedback(String text) {
        feedback = text;
        feedbackUntil = System.currentTimeMillis() + 1500;
    }

    // เริ่มเกมใหม่ (SencePanel เรียกเมื่อได้รับ START จาก Server)
    public void startGame(GameClient client, int myIdx) {
        this.client = client;
        this.myIdx = myIdx;
        pillarY = null;
        lavaY = -1;
        currentQuestion = "";
        feedback = "";
        roundNo = 0;
        answeredRound = -1;
        gameOver = false;
        winnerIdx = -1;
        txtAnswer.setEnabled(true);
        btnSubmit.setEnabled(true);
        startGameLoop();
        SwingUtilities.invokeLater(txtAnswer::requestFocusInWindow);
    }

    // รับข้อความจาก Server แล้วอัปเดตสถานะที่ใช้วาด
    public void handleServerMessage(String msg) {
        String[] p = msg.split("\\|", -1); //
        switch (p[0]) {
            case "STATE" -> {
                roundNo = Integer.parseInt(p[1]);
                lavaY = Integer.parseInt(p[2]);
                timeLeft = (int) Math.ceil(Long.parseLong(p[3]) / 1000.0);
                currentQuestion = p[4];

                String[] players = p[5].split(";");
                pillarY = new int[players.length];
                alive = new boolean[players.length];
                scores = new int[players.length];
                for (int i = 0; i < players.length; i++) { //
                    String[] f = players[i].split(","); //
                    pillarY[i] = Integer.parseInt(f[0]);
                    alive[i] = Boolean.parseBoolean(f[1]);
                    scores[i] = Integer.parseInt(f[2]);
                }
                // ถ้าเราตกรอบแล้ว ปิดช่องกรอกคำตอบ
                boolean meAlive = myIdx >= 0 && myIdx < alive.length && alive[myIdx];
                txtAnswer.setEnabled(meAlive);
                btnSubmit.setEnabled(meAlive);
            }
            case "RESULT" -> showFeedback(Boolean.parseBoolean(p[1]) ? "ถูกต้อง!" : "ผิด!");
            case "GAMEOVER" -> {
                gameOver = true;
                winnerIdx = Integer.parseInt(p[1]);
                txtAnswer.setEnabled(false);
                btnSubmit.setEnabled(false);
            }
        }
    }

    // เริ่ม Game Loop (Thread แยก) สั่ง repaint ทุกเฟรม
    public void startGameLoop() {
        if (!isRunning) {
            isRunning = true;
            gameThread = new Thread(this, "GameLoop");
            gameThread.setDaemon(true); // ปิดโปรแกรมแล้ว thread ตายตาม
            gameThread.start();
        }
    }

    public void stopGameLoop() {
        isRunning = false;
    }

    @Override
    public void run() {
        long delay = 1000 / 60; // 60 FPS
        while (isRunning) {
            repaint();
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    // วาดทุกอย่างบนหน้าจอ (ฉากหลังถูกวาดโดย SencePanel แล้ว)
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight() - bottomPanel.getHeight();   // พื้นที่วาดไม่รวมแผงคำตอบ
        double scale = (double) h / GameLogic.WORLD_HEIGHT; // แปลงพิกัดโลกเกม -> พิกเซลจริง

        // 1. เส้นชัย (เสาถึงเส้นนี้ก่อน = ชนะ)
        int goalY = (int) (GameLogic.PILLAR_GOAL_Y * scale);
        g2d.setColor(new Color(255, 215, 0));
        g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{15, 10}, 0));
        g2d.drawLine(0, goalY, w, goalY);
        g2d.setStroke(new BasicStroke(1));
        g2d.setFont(new Font("Tahoma", Font.BOLD, 16));
        g2d.drawString("เส้นชัย", 10, goalY - 6);

        // 2. เสาและตัวละครของผู้เล่นทุกคน
// 2. เสาและตัวละครของผู้เล่นทุกคน
if (pillarY != null) {
    int n = pillarY.length;
    int slot = w / n;
    int pillarW = Math.min(160, slot / 2);
    int catSize = (int) (90 * scale);

    // ดึงขนาดจริงของรูปภาพมาคำนวณ Aspect Ratio
    int rawPlatformW = platformImage.getWidth(this);
    int rawPlatformH = platformImage.getHeight(this);
    int rawDirtW = dirtImage.getWidth(this);
    int rawDirtH = dirtImage.getHeight(this);

    // คำนวณความสูงแบบปรับตามสัดส่วนจริงอัตโนมัติ
    int topH = (int) (pillarW * ((double) rawPlatformH / rawPlatformW));
    int dirtH = (int) (pillarW * ((double) rawDirtH / rawDirtW));

    // 💡 ปรับค่า OVERLAP เพิ่มขึ้น (เช่น 5 - 15) หากรูปยังมีช่องว่างโปร่งใสเหลืออยู่
    int OVERLAP = 15; 
    int stepY = Math.max(1, dirtH - OVERLAP);

    for (int i = 0; i < n; i++) {
        int x = slot * i + (slot - pillarW) / 2;
        int y = (int) (pillarY[i] * scale);
        int pillarHeight = h - y;

        if (pillarHeight > 0) {
            Shape oldClip = g2d.getClip();
            g2d.clipRect(x, y, pillarW, pillarHeight);

            // A. วาดลำเสา (dirt.png) วนลูปต่อกันลงมาโดยขยับเหลื่อมกันเล็กน้อย
            int dirtStartY = y + topH - OVERLAP;
            for (int tileY = dirtStartY; tileY < h + dirtH; tileY += stepY) {
                g2d.drawImage(dirtImage, x, tileY, pillarW, dirtH, this);
            }

            // B. วาดส่วนยอดเสา (plantform.png) ทับด้านบนสุด
            g2d.drawImage(platformImage, x, y, pillarW, topH, this);

            g2d.setClip(oldClip);
        }

        // เส้นขอบเสา
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x, y, pillarW, h - y);

        // ตัวละครยืนบนยอดเสา
        int catX = x + (pillarW - catSize) / 2;
        if (alive[i]) {
            g2d.drawImage(playerImages[i % playerImages.length], catX, y - catSize, catSize, catSize, this);
        }

        // ชื่อ + คะแนน เหนือหัว
        String name = "P" + (i + 1) + (i == myIdx ? " (คุณ)" : "") + "  " + scores[i] + " คะแนน";
        g2d.setFont(new Font("Tahoma", Font.BOLD, 16));
        g2d.setColor(i == myIdx ? Color.YELLOW : Color.WHITE);
        drawCentered(g2d, name, x + pillarW / 2, y - catSize - 8);

        if (!alive[i]) {
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Tahoma", Font.BOLD, 22));
            drawCentered(g2d, "โดนลาวา แพ้!", x + pillarW / 2, y - 30);
        }
    }
}

        // 3. ลาวา
        if (lavaY >= 0) {
            int ly = (int) (lavaY * scale);
            g2d.setColor(new Color(230, 50, 10, 220));
            g2d.fillRect(0, ly, w, h - ly);
            g2d.setColor(Color.YELLOW);
            g2d.fillRect(0, ly, w, 6);
        }

        // 4. กล่องโจทย์ + เวลา + ผลการตอบ
        int boxW = 420;
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(w / 2 - boxW / 2, 20, boxW, 110, 15, 15);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Tahoma", Font.BOLD, 28));
        drawCentered(g2d, "โจทย์: " + currentQuestion + " = ?", w / 2, 58);
        g2d.setColor(timeLeft <= 2 ? Color.RED : Color.GREEN);
        g2d.setFont(new Font("Tahoma", Font.BOLD, 20));
        drawCentered(g2d, "เวลา: " + timeLeft + " วิ", w / 2, 88);
        g2d.setColor(feedback.startsWith("ถูก") ? Color.GREEN : Color.ORANGE);
        if (System.currentTimeMillis() < feedbackUntil) drawCentered(g2d, feedback, w / 2, 118);

        // 5. หน้าจบเกม
        if (gameOver) {
            g2d.setColor(new Color(0, 0, 0, 170));
            g2d.fillRect(0, 0, w, h);
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Tahoma", Font.BOLD, 60));
            String text = (winnerIdx == myIdx) ? "คุณชนะ!" : (winnerIdx >= 0 ? "P" + (winnerIdx + 1) + " ชนะ!" : "ทุกคนโดนลาวา ไม่มีผู้ชนะ");
            drawCentered(g2d, text, w / 2, h / 2);
        }
    }

    // วาดข้อความให้อยู่กึ่งกลางที่ตำแหน่ง centerX
    private void drawCentered(Graphics2D g2d, String text, int centerX, int baselineY) {
        int tw = g2d.getFontMetrics().stringWidth(text);
        g2d.drawString(text, centerX - tw / 2, baselineY);
    }
}
=======
package src;

import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Random;
import javax.swing.*;

/**
 * ไฟล์รวมหน้าเล่นเกม (รวม GamePlayPanel + GameLogic เดิมไว้ด้วยกัน)
 * - GameLogic (คลาสข้างใน) : กติกาเกมทั้งหมด ทำงานบนฝั่ง Server เท่านั้น
 * - GamePlayPanel         : หน้าจอวาดเกม + ช่องกรอกคำตอบ ทำงานบนฝั่ง Client
 *   Client ไม่คำนวณกติกาเอง แค่รับสถานะจาก Server มาวาด ทุกเครื่องจึงเห็นเกมตรงกัน
 */
public class GamePlayPanel extends JPanel implements Runnable {

    // =====================================================================
    //  GameLogic : กติกาของเกม (Server สร้างตอน Host กด Start)
    // =====================================================================
    public static class GameLogic {

        // พิกัดทั้งหมดอยู่ใน "โลกเกม" สูง 600 หน่วย แล้วหน้าจอค่อยย่อ/ขยายตามขนาดจริง
        public static final int WORLD_HEIGHT = 600;
        public static final int LAVA_START_Y = 550;    // ตำแหน่ง Y เริ่มต้นของลาวา (ล่างจอ)
        public static final int PILLAR_START_Y = 450;  // ตำแหน่ง Y เริ่มต้นของยอดเสา
        public static final int PILLAR_STEP = 10;      // เสาขึ้นกี่หน่วยต่อ 1 คะแนน
        public static final int PILLAR_MIN_Y = 50;     // เสาสูงได้มากสุดแค่นี้ (กันทะลุขอบบน)
        public static final int PILLAR_GOAL_Y = 120;   // ยอดเสาถึง Y นี้ก่อน = ชนะทันที
        public static final int LAVA_STEP = 1;         // ลาวาขึ้นกี่หน่วยต่อ 1 tick
        public static final long TICK_MS = 200;        // Server อัปเดตเกมทุก ๆ กี่มิลลิวินาที
        public static final long ROUND_TIME_MS = 5000; // เวลาต่อข้อ
        // คะแนนต่อข้อ = (จำนวนคนที่รอดตอนเริ่มข้อ) - (ลำดับที่ตอบถูก) + 1 เช่น 4 คน: 4, 3, 2, 1
        // ตอบถูกเสาขึ้นเสมอ (ยิ่งเร็วยิ่งขึ้นมาก) / ตอบผิดหรือไม่ตอบ เสาอยู่เท่าเดิม

        private final int[] pillarY;       // Y ของยอดเสาแต่ละคน (index = เลขผู้เล่น)
        private final boolean[] alive;     // ผู้เล่นแต่ละคนยังรอดไหม
        private final int[] score;         // คะแนนสะสมของแต่ละคน
        private final boolean[] answered;  // แต่ละคนส่งคำตอบในข้อนี้แล้วหรือยัง
        private int lavaY = LAVA_START_Y;  // Y ปัจจุบันของลาวา
        private boolean gameOver = false;
        private int winnerIdx = -1;        // index ผู้ชนะ (-1 = ยังไม่มี)

        // สถานะของข้อปัจจุบัน
        private int roundNo = 0;            // เลขข้อ (Client ใช้รู้ว่าขึ้นข้อใหม่แล้ว)
        private int correctCount = 0;       // ข้อนี้มีคนตอบถูกไปแล้วกี่คน (ใช้เป็นลำดับคิดคะแนน)
        private int aliveAtRoundStart = 0;  // จำนวนคนที่รอดตอนเริ่มข้อ
        private long roundStartMs = 0;      // เวลาที่เริ่มข้อนี้
        private String questionText = "";   // ข้อความโจทย์
        private int answer;                 // คำตอบของโจทย์
        private final Random random = new Random();

        public GameLogic(int playerCount) {
            pillarY = new int[playerCount];
            alive = new boolean[playerCount];
            score = new int[playerCount];
            answered = new boolean[playerCount];
            for (int i = 0; i < playerCount; i++) {
                pillarY[i] = PILLAR_START_Y;  // ทุกคนเริ่มที่ความสูงเท่ากัน
                alive[i] = true;
            }
            nextQuestion();
        }

        // สุ่มโจทย์ใหม่ (บวก / ลบ / คูณ) แทนคลังโจทย์แบบตายตัว เล่นได้ไม่จำกัดข้อ
        private void nextQuestion() {
            String old = questionText;
            do {
                int a = random.nextInt(50) + 1;
                int b = random.nextInt(50) + 1;
                switch (random.nextInt(3)) {
                    case 0 -> { questionText = a + " + " + b; answer = a + b; }
                    case 1 -> { // เอาเลขมากลบเลขน้อย คำตอบจะไม่ติดลบ
                        questionText = Math.max(a, b) + " - " + Math.min(a, b);
                        answer = Math.abs(a - b);
                    }
                    default -> { // สูตรคูณ 1-12
                        a = random.nextInt(12) + 1;
                        b = random.nextInt(12) + 1;
                        questionText = a + " x " + b;
                        answer = a * b;
                    }
                }
            } while (questionText.equals(old)); // กันโจทย์ซ้ำกับข้อที่เพิ่งจบ

            // รีเซ็ตสถานะของข้อใหม่
            roundNo++;
            roundStartMs = System.currentTimeMillis();
            correctCount = 0;
            aliveAtRoundStart = 0;
            for (int i = 0; i < answered.length; i++) {
                answered[i] = false;
                if (alive[i]) aliveAtRoundStart++;
            }
        }

        // ข้อจะจบก่อนหมดเวลา ถ้าทุกคนที่ยังรอดส่งคำตอบครบแล้ว
        private void checkRoundComplete() {
            if (gameOver) return;
            for (int i = 0; i < alive.length; i++) {
                if (alive[i] && !answered[i]) return; // ยังมีคนไม่ส่ง
            }
            nextQuestion();
        }

        // ตรวจคำตอบของผู้เล่น คืนค่า true ถ้าตอบถูก
        public synchronized boolean checkAnswer(int idx, int ans) {
            if (gameOver || !isValid(idx) || !alive[idx] || answered[idx]) return false; // ตกรอบ/ตอบซ้ำ = ไม่รับ
            answered[idx] = true;

            boolean correct = (ans == answer);
            if (correct) {
                correctCount++;
                int points = Math.max(1, aliveAtRoundStart - correctCount + 1); // ตอบถูกก่อนได้มากกว่า แต่ได้อย่างน้อย 1
                score[idx] += points;
                raisePillar(idx, points);
            }
            checkRoundComplete();
            return correct;
        }

        // ดันเสาขึ้นตามคะแนน ถ้าถึงเส้นชัยก่อน = ชนะ
        private void raisePillar(int idx, int points) {
            pillarY[idx] = Math.max(PILLAR_MIN_Y, pillarY[idx] - points * PILLAR_STEP); // Y ลด = เสาสูงขึ้น
            if (!gameOver && pillarY[idx] <= PILLAR_GOAL_Y) {
                gameOver = true;
                winnerIdx = idx;
            }
        }

        // Server เรียกทุก tick: ลาวาขึ้น เช็คคนจม และเช็คหมดเวลาข้อ
        public synchronized void update() {
            if (gameOver) return;
            lavaY -= LAVA_STEP;
            checkElimination();
            if (!gameOver && System.currentTimeMillis() - roundStartMs >= ROUND_TIME_MS) {
                nextQuestion(); // หมดเวลา ขึ้นข้อใหม่
            }
        }

        // ผู้เล่นหลุดการเชื่อมต่อระหว่างเกม = ถือว่าตกรอบ
        public synchronized void removePlayer(int idx) {
            if (gameOver || !isValid(idx) || !alive[idx]) return;
            alive[idx] = false;
            checkAllOut();
            checkRoundComplete();
        }

        // ตรวจผู้เล่นที่โดนลาวา: แมวยืนอยู่บนยอดเสา ลาวาถึงยอดเสา = โดนแมว = แพ้ออกจากเกม
        private void checkElimination() {
            boolean someoneOut = false;
            for (int i = 0; i < pillarY.length; i++) {
                if (alive[i] && lavaY <= pillarY[i]) {
                    alive[i] = false;
                    someoneOut = true;
                }
            }
            if (!someoneOut) return;
            checkAllOut();
            checkRoundComplete(); // ถ้าคนที่ยังไม่ตอบโดนลาวาหมดแล้ว ข้อนี้ก็จบ
        }

        // ชนะได้ทางเดียวคือเสาถึงเส้นชัย ถ้าทุกคนโดนลาวาหมดก่อน = เกมจบแบบไม่มีผู้ชนะ
        private void checkAllOut() {
            if (gameOver) return;
            for (boolean a : alive) {
                if (a) return; // ยังมีคนเล่นอยู่
            }
            gameOver = true;
            winnerIdx = -1;
        }

        private boolean isValid(int idx) {
            return idx >= 0 && idx < pillarY.length;
        }

        public synchronized boolean isGameOver() { return gameOver; }

        /**
         * ข้อความสถานะที่ Server ส่งให้ทุก Client
         * ระหว่างเล่น: STATE|เลขข้อ|lavaY|เวลาที่เหลือ(ms)|โจทย์|y1,alive1,score1;y2,alive2,score2;...
         * จบเกม:       GAMEOVER|winnerIdx
         */
        public synchronized String getStateString() {
            if (gameOver) return "GAMEOVER|" + winnerIdx;
            long timeLeft = Math.max(0, ROUND_TIME_MS - (System.currentTimeMillis() - roundStartMs));
            StringBuilder sb = new StringBuilder("STATE|");
            sb.append(roundNo).append('|').append(lavaY).append('|')
              .append(timeLeft).append('|').append(questionText).append('|');
            for (int i = 0; i < pillarY.length; i++) {
                if (i > 0) sb.append(';');
                sb.append(pillarY[i]).append(',').append(alive[i]).append(',').append(score[i]);
            }
            return sb.toString();
        }
    }

    // =====================================================================
    //  GamePlayPanel : หน้าจอเกมฝั่ง Client
    // =====================================================================

    // Game Loop: สั่งวาดจอใหม่ 60 ครั้ง/วินาที
    private Thread gameThread;
    private volatile boolean isRunning = false;

    // สถานะเกมล่าสุดที่ได้รับจาก Server (อัปเดตบน Swing thread เท่านั้น)
    private int[] pillarY;            // Y ยอดเสาของทุกคน (null = ยังไม่ได้รับสถานะ)
    private boolean[] alive;          // ใครยังรอดบ้าง
    private int[] scores;             // คะแนนทุกคน
    private int lavaY = -1;           // Y ของลาวา (-1 = ยังไม่มีข้อมูล)
    private String currentQuestion = "";
    private int timeLeft = 0;         // เวลาที่เหลือ (วินาที)
    private int roundNo = 0;          // เลขข้อปัจจุบัน
    private int answeredRound = -1;   // ข้อล่าสุดที่เราส่งคำตอบไปแล้ว (กันส่งซ้ำ)
    private String feedback = "";     // ข้อความ "ถูก!/ผิด!" หลังส่งคำตอบ
    private long feedbackUntil = 0;   // แสดง feedback ถึงเวลานี้ (ms)
    private int winnerIdx = -1;       // ผู้ชนะ (ใช้ตอนจบเกม)
    private boolean gameOver = false;

    private int myIdx = -1;           // เราคือผู้เล่นคนที่เท่าไร (0-3)
    private GameClient client;        // ใช้ส่งคำตอบไป Server

    // ช่องกรอกคำตอบด้านล่าง
    private final JPanel bottomPanel;
    private final JTextField txtAnswer;
    private final JButton btnSubmit;

    // รูปภาพ: ตัวละครแมว 4 สี + พื้นผิวเสา (ไม่มีรูปลาวา จึงวาดด้วยสีแทน)
    private final Image[] playerImages = new Image[4];
    private final Image pillarImage = new ImageIcon("src/images/dirt.jpg").getImage();

    public GamePlayPanel() {
        setLayout(new BorderLayout());
        setOpaque(false); // โปร่งใส ให้เห็นฉากหลัง bgLevel1 ที่ SencePanel วาดไว้

        for (int i = 0; i < playerImages.length; i++) {
            playerImages[i] = new ImageIcon("src/images/cat" + (i + 1) + ".png").getImage();
        }

        // แผงกรอกคำตอบด้านล่าง
        bottomPanel = new JPanel();
        bottomPanel.setBackground(new Color(40, 40, 40));

        JLabel lblAnswer = new JLabel("คำตอบ: ");
        lblAnswer.setForeground(Color.WHITE);
        lblAnswer.setFont(new Font("Tahoma", Font.BOLD, 20));

        txtAnswer = new JTextField(12);
        txtAnswer.setFont(new Font("Tahoma", Font.PLAIN, 20));

        btnSubmit = new JButton("ส่งคำตอบ");
        btnSubmit.setFont(new Font("Tahoma", Font.BOLD, 18));

        bottomPanel.add(lblAnswer);
        bottomPanel.add(txtAnswer);
        bottomPanel.add(btnSubmit);
        add(bottomPanel, BorderLayout.SOUTH);

        // กด Enter หรือกดปุ่ม = ส่งคำตอบ
        ActionListener submitAction = e -> submitAnswer();
        txtAnswer.addActionListener(submitAction);
        btnSubmit.addActionListener(submitAction);
    }

    // ส่งคำตอบไป Server (ต้องเป็นตัวเลข และส่งได้ข้อละครั้ง)
    private void submitAnswer() {
        String text = txtAnswer.getText().trim();
        txtAnswer.setText("");
        if (text.isEmpty() || client == null || gameOver) return;
        if (answeredRound == roundNo) { showFeedback("ข้อนี้ตอบไปแล้ว รอข้อถัดไป"); return; }
        try {
            client.send("ANSWER|" + Integer.parseInt(text));
            answeredRound = roundNo;
        } catch (NumberFormatException ex) {
            showFeedback("กรอกเป็นตัวเลขเท่านั้น");
        }
    }

    // แสดงข้อความสั้น ๆ 1.5 วินาที
    private void showFeedback(String text) {
        feedback = text;
        feedbackUntil = System.currentTimeMillis() + 1500;
    }

    // เริ่มเกมใหม่ (SencePanel เรียกเมื่อได้รับ START จาก Server)
    public void startGame(GameClient client, int myIdx) {
        this.client = client;
        this.myIdx = myIdx;
        pillarY = null;
        lavaY = -1;
        currentQuestion = "";
        feedback = "";
        roundNo = 0;
        answeredRound = -1;
        gameOver = false;
        winnerIdx = -1;
        txtAnswer.setEnabled(true);
        btnSubmit.setEnabled(true);
        startGameLoop();
        SwingUtilities.invokeLater(txtAnswer::requestFocusInWindow);
    }

    // รับข้อความจาก Server แล้วอัปเดตสถานะที่ใช้วาด
    public void handleServerMessage(String msg) {
        String[] p = msg.split("\\|", -1);
        switch (p[0]) {
            case "STATE" -> {
                roundNo = Integer.parseInt(p[1]);
                lavaY = Integer.parseInt(p[2]);
                timeLeft = (int) Math.ceil(Long.parseLong(p[3]) / 1000.0);
                currentQuestion = p[4];

                String[] players = p[5].split(";");
                pillarY = new int[players.length];
                alive = new boolean[players.length];
                scores = new int[players.length];
                for (int i = 0; i < players.length; i++) {
                    String[] f = players[i].split(",");
                    pillarY[i] = Integer.parseInt(f[0]);
                    alive[i] = Boolean.parseBoolean(f[1]);
                    scores[i] = Integer.parseInt(f[2]);
                }
                // ถ้าเราตกรอบแล้ว ปิดช่องกรอกคำตอบ
                boolean meAlive = myIdx >= 0 && myIdx < alive.length && alive[myIdx];
                txtAnswer.setEnabled(meAlive);
                btnSubmit.setEnabled(meAlive);
            }
            case "RESULT" -> showFeedback(Boolean.parseBoolean(p[1]) ? "ถูกต้อง!" : "ผิด!");
            case "GAMEOVER" -> {
                gameOver = true;
                winnerIdx = Integer.parseInt(p[1]);
                txtAnswer.setEnabled(false);
                btnSubmit.setEnabled(false);
            }
        }
    }

    // เริ่ม Game Loop (Thread แยก) สั่ง repaint ทุกเฟรม
    public void startGameLoop() {
        if (!isRunning) {
            isRunning = true;
            gameThread = new Thread(this, "GameLoop");
            gameThread.setDaemon(true); // ปิดโปรแกรมแล้ว thread ตายตาม
            gameThread.start();
        }
    }

    public void stopGameLoop() {
        isRunning = false;
    }

    @Override
    public void run() {
        long delay = 1000 / 60; // 60 FPS
        while (isRunning) {
            repaint();
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    // วาดทุกอย่างบนหน้าจอ (ฉากหลังถูกวาดโดย SencePanel แล้ว)
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight() - bottomPanel.getHeight();   // พื้นที่วาดไม่รวมแผงคำตอบ
        double scale = (double) h / GameLogic.WORLD_HEIGHT; // แปลงพิกัดโลกเกม -> พิกเซลจริง

        // 1. เส้นชัย (เสาถึงเส้นนี้ก่อน = ชนะ)
        int goalY = (int) (GameLogic.PILLAR_GOAL_Y * scale);
        g2d.setColor(new Color(255, 215, 0));
        g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{15, 10}, 0));
        g2d.drawLine(0, goalY, w, goalY);
        g2d.setStroke(new BasicStroke(1));
        g2d.setFont(new Font("Tahoma", Font.BOLD, 16));
        g2d.drawString("เส้นชัย", 10, goalY - 6);

        // 2. เสาและตัวละครของผู้เล่นทุกคน
        if (pillarY != null) {
            int n = pillarY.length;
            int slot = w / n;                       // แบ่งความกว้างจอให้ผู้เล่นคนละช่อง
            int pillarW = Math.min(160, slot / 2);
            int catSize = (int) (90 * scale);
            for (int i = 0; i < n; i++) {
                int x = slot * i + (slot - pillarW) / 2;
                int y = (int) (pillarY[i] * scale);

                // เสา
                g2d.drawImage(pillarImage, x, y, pillarW, h - y, this);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(x, y, pillarW, h - y);

                // ตัวละคร (ตกรอบแล้วจะไม่วาด)
                int catX = x + (pillarW - catSize) / 2;
                if (alive[i]) {
                    g2d.drawImage(playerImages[i % playerImages.length], catX, y - catSize, catSize, catSize, this);
                }

                // ชื่อ + คะแนน เหนือหัว
                String name = "P" + (i + 1) + (i == myIdx ? " (คุณ)" : "") + "  " + scores[i] + " คะแนน";
                g2d.setFont(new Font("Tahoma", Font.BOLD, 16));
                g2d.setColor(i == myIdx ? Color.YELLOW : Color.WHITE);
                drawCentered(g2d, name, x + pillarW / 2, y - catSize - 8);
                if (!alive[i]) {
                    g2d.setColor(Color.RED);
                    g2d.setFont(new Font("Tahoma", Font.BOLD, 22));
                    drawCentered(g2d, "โดนลาวา แพ้!", x + pillarW / 2, y - 30);
                }
            }
        }

        // 3. ลาวา
        if (lavaY >= 0) {
            int ly = (int) (lavaY * scale);
            g2d.setColor(new Color(230, 50, 10, 220));
            g2d.fillRect(0, ly, w, h - ly);
            g2d.setColor(Color.YELLOW);
            g2d.fillRect(0, ly, w, 6);
        }

        // 4. กล่องโจทย์ + เวลา + ผลการตอบ
        int boxW = 420;
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(w / 2 - boxW / 2, 20, boxW, 110, 15, 15);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Tahoma", Font.BOLD, 28));
        drawCentered(g2d, "โจทย์: " + currentQuestion + " = ?", w / 2, 58);
        g2d.setColor(timeLeft <= 2 ? Color.RED : Color.GREEN);
        g2d.setFont(new Font("Tahoma", Font.BOLD, 20));
        drawCentered(g2d, "เวลา: " + timeLeft + " วิ", w / 2, 88);
        g2d.setColor(feedback.startsWith("ถูก") ? Color.GREEN : Color.ORANGE);
        if (System.currentTimeMillis() < feedbackUntil) drawCentered(g2d, feedback, w / 2, 118);

        // 5. หน้าจบเกม
        if (gameOver) {
            g2d.setColor(new Color(0, 0, 0, 170));
            g2d.fillRect(0, 0, w, h);
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Tahoma", Font.BOLD, 60));
            String text = (winnerIdx == myIdx) ? "คุณชนะ!" : (winnerIdx >= 0 ? "P" + (winnerIdx + 1) + " ชนะ!" : "ทุกคนโดนลาวา ไม่มีผู้ชนะ");
            drawCentered(g2d, text, w / 2, h / 2);
        }
    }

    // วาดข้อความให้อยู่กึ่งกลางที่ตำแหน่ง centerX
    private void drawCentered(Graphics2D g2d, String text, int centerX, int baselineY) {
        int tw = g2d.getFontMetrics().stringWidth(text);
        g2d.drawString(text, centerX - tw / 2, baselineY);
    }
}
>>>>>>> c5947919378dd743a9f8be9b9dd4ac26e9d396d5
