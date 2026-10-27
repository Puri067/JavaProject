package src;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.*;

/**
 * [หน้าที่ของคุณ - คนที่ 3: Gameplay Render Component]
 * - ทำระบบ Game Loop (Thread 60 FPS) สั่ง repaint() ตลอดเวลา
 * - ทำหน้าจอวาดภาพกราฟิก (paintComponent)
 * - ทำช่องกรอกคำตอบ (JTextField) สำหรับผู้เล่น
 */
public class GamePlayPanel extends JPanel implements Runnable {

    // --- 1. ตัวแปรควบคุม Game Loop ---
    private Thread gameThread;
    private boolean isRunning = false;

    // --- 2. ตัวแปรเก็บพิกัด (รอรับค่าจาก Gameplay Logic) ---
    private int[] pillarY; // พิกัดความสูงเสาของผู้เล่น 4 คน
    private int lavaY;     // พิกัดความสูงของลาวา

    // --- 3. ตัวแปรเก็บโจทย์และเวลา (รอรับค่าจาก Gameplay Logic) ---
    private String currentQuestion = ""; 
    private int timeLeft = 0;                       

    // --- 4. องค์ประกอบ UI สำหรับรับคำตอบ ---
    private JTextField txtAnswer;
    private JButton btnSubmit;

    // --- 5. ตัวแปรเก็บรูปภาพ Assets ---
    private Image playerImage;  // รูปตัวละครผู้เล่น
    private Image pillarImage; // รูปเสา
    private Image lavaImage;   // รูปลาวา

    // --- 6. ตัวแปรเชื่อมต่อระบบเพื่อน ---
    // private GameClient gameClient; // ของคนที่ 1 (Network)

    /**
     * Constructor: สร้างหน้าจอและช่องกรอกคำตอบด้านล่าง
     */
    public GamePlayPanel() {
        setLayout(new BorderLayout());

        // สร้างแผงกรอกคำตอบด้านล่าง
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(new Color(40, 40, 40));
        
        JLabel lblAnswer = new JLabel("คำตอบ: ");
        lblAnswer.setForeground(Color.WHITE);
        lblAnswer.setFont(new Font("Tahoma", Font.BOLD, 16));
        
        txtAnswer = new JTextField(12);
        txtAnswer.setFont(new Font("Tahoma", Font.PLAIN, 16));
        
        btnSubmit = new JButton("ส่งคำตอบ");
        btnSubmit.setFont(new Font("Tahoma", Font.BOLD, 14));

        bottomPanel.add(lblAnswer);
        bottomPanel.add(txtAnswer);
        bottomPanel.add(btnSubmit);

        add(bottomPanel, BorderLayout.SOUTH);

        // --- Event เมื่อผู้เล่นกดส่งคำตอบ ---
        ActionListener submitAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String ansText = txtAnswer.getText().trim();
                
                if (!ansText.isEmpty()) {
                    System.out.println("ผู้เล่นส่งคำตอบ: " + ansText);
                    
                    // =========================================================================
                    // 🔌 [หน้าที่ของเพื่อนคนที่ 1 - Network]: 
                    // นำเมธอดส่งคำตอบผ่าน Socket มาใส่ตรงนี้แทน System.out.println
                    // ตัวอย่าง: if (gameClient != null) { gameClient.sendAnswerToServer(ansText); }
                    // =========================================================================
                    
                    txtAnswer.setText(""); // เคลียร์ช่องพิมพ์
                }
            }
        };

        txtAnswer.addActionListener(submitAction);
        btnSubmit.addActionListener(submitAction);
    }

    /**
     * เริ่มต้น Game Loop Thread 60 FPS
     */
    public void startGameLoop() {
        if (!isRunning) {
            isRunning = true;
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    /**
     * หยุด Game Loop Thread
     */
    public void stopGameLoop() {
        isRunning = false;
    }

    @Override
    public void run() {
        int targetFPS = 60;
        long delay = 1000 / targetFPS;

        while (isRunning) {
            repaint(); // สั่งวาดหน้าจอใหม่ทุกๆ เฟรม (60 ครั้งต่อวินาที)
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * เมธอดหลักในการเรนเดอร์ภาพกราฟิกทั้งหมด 60 FPS
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. วาดฉากหลังเกม
        g2d.setColor(new Color(25, 25, 35));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // 2. วาดเสาและตัวละครของผู้เล่นทั้ง 4 คน
        if (pillarY != null && pillarY.length == 4) {
            int pillarWidth = 70;
            int startX = 80;
            int spacing = 120;

            for (int i = 0; i < pillarY.length; i++) {
                int x = startX + (i * spacing);
                int y = pillarY[i];
                int height = getHeight() - y;

                if (pillarImage != null) {
                    g2d.drawImage(pillarImage, x, y, pillarWidth, height, this);
                } else {
                    g2d.setColor(new Color(120, 140, 160));
                    g2d.fillRect(x, y, pillarWidth, height);
                    g2d.setColor(Color.BLACK);
                    g2d.drawRect(x, y, pillarWidth, height);
                }

                // วาดตัวละคร
                if (playerImage != null) {
                    g2d.drawImage(playerImage, x + 15, y - 40, 40, 40, this);
                } else {
                    g2d.setColor(new Color(255, 200, 100));
                    g2d.fillOval(x + 20, y - 35, 30, 30);
                }

                // ชื่อผู้เล่น
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Tahoma", Font.BOLD, 12));
                g2d.drawString("P" + (i + 1), x + 28, y - 42);
            }
        }

        // 3. วาดลาวาด้านล่าง
        if (lavaY > 0) {
            if (lavaImage != null) {
                g2d.drawImage(lavaImage, 0, lavaY, getWidth(), getHeight() - lavaY, this);
            } else {
                g2d.setColor(new Color(230, 50, 10));
                g2d.fillRect(0, lavaY, getWidth(), getHeight() - lavaY);
                g2d.setColor(Color.YELLOW);
                g2d.fillRect(0, lavaY, getWidth(), 6);
            }
        }

        // 4. วาดกล่องโจทย์และเวลานับถอยหลัง
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(getWidth() / 2 - 160, 20, 320, 75, 15, 15);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Tahoma", Font.BOLD, 20));
        g2d.drawString("โจทย์: " + currentQuestion, getWidth() / 2 - 110, 48);

        if (timeLeft <= 2) {
            g2d.setColor(Color.RED);
        } else {
            g2d.setColor(Color.GREEN);
        }
        g2d.setFont(new Font("Tahoma", Font.BOLD, 16));
        g2d.drawString("เวลา: " + timeLeft + " วิ", getWidth() / 2 - 40, 78);
    }

    /**
     * ⚙️ เมธอดสำหรับฝั่ง Logic โยนพิกัดเสาและลาวาใหม่เข้ามาอัปเดตหน้าจอ
     */
    public void updateDisplayState(int[] pillars, int lava) {
        this.pillarY = pillars;
        this.lavaY = lava;
    }

    /**
     * ⚙️ เมธอดสำหรับฝั่ง Logic โยนโจทย์และเวลาเข้ามาอัปเดต
     */
    public void updateQuestionAndTimer(String question, int time) {
        this.currentQuestion = question;
        this.timeLeft = time;
    }

    /**
     * 🎨 เมธอดสำหรับส่งไฟล์รูปภาพ Asset เข้ามาใส่ในคลาส
     */
    public void setGameAssets(Image playerImg, Image pillarImg, Image lavaImg) {
        this.playerImage = playerImg;
        this.pillarImage = pillarImg;
        this.lavaImage = lavaImg;
    }
}

/**
 * [ส่วนของ Gameplay Logic]
 */
class GameLogic {

    public static class Question {                     
        private final String questionText;           
        private final int answer;                 

        public Question(String questionText, int answer) {
            this.questionText = questionText;
            this.answer = answer;
        }

        public String getQuestionText() { return questionText; } 
        public int getAnswer() { return answer; }             
    }

    public static final int lavaStart_Y = 500;        // ตำแหน่ง Y เริ่มต้นของลาวา
    public static final int pillarsStart_Y = 350;      // ตำแหน่ง Y เริ่มต้นเสา
    public static final int pillarStep = 3;          // เสาขึ้นกี่พิกเซลต่อ 1 คะแนน 
    public static final int pillarsMin_Y = 50;         // เสาขึ้นได้สูงสุดแค่นี้
    public static final int pillarsGoal_Y = 200;       // เสาถึง Y นี้ = ชนะทันที
    public static final int lavaStep = 1;          
    public static final long roundTimeMs = 5000;       // เวลาต่อข้อ

    private final List<Integer> playersPillars = new ArrayList<>(); // Y ของเสาแต่ละคน
    private final List<Boolean> playersAlive = new ArrayList<>();  
    private final List<Integer> playersScores = new ArrayList<>();
    private int lavaY = lavaStart_Y;                  // Y ปัจจุบันของลาวา
    private boolean gameOver = false;              
    private int winnerIdx = -1;            // index ผู้ชนะ (-1 = ยังไม่มี)

    // สถานะข้อปัจจุบัน
    private final List<Boolean> answeredThisRound = new ArrayList<>(); 
    private int correctCountThisRound = 0;             
    private int aliveAtRoundStart = 0;                 
    private long roundStartMs = 0;                     

    private final List<Question> questionPool = new ArrayList<>();        
    private List<Question> availableQuestions = new ArrayList<>();        
    private Question currentQuestion;                

    public GameLogic(int playerCount) {                
        for (int i = 0; i < playerCount; i++) {        
            playersPillars.add(pillarsStart_Y);        
            playersAlive.add(true);                  
            playersScores.add(0);                  
            answeredThisRound.add(false);       
        }
        initQuestions();     
        nextRandomQuestion();     
    }

    private void initQuestions() {                    
        questionPool.add(new Question("15 + 27", 42));
        questionPool.add(new Question("8 x 7", 56));
        questionPool.add(new Question("100 - 37", 63));
        questionPool.add(new Question("144 / 12", 12));
        questionPool.add(new Question("25 + 38", 63));
        questionPool.add(new Question("9 x 9", 81));
        questionPool.add(new Question("50 - 18", 32));
        questionPool.add(new Question("12 x 11", 132));
        questionPool.add(new Question("3!", 6));
        resetQuestionPool();                        
    }

    private void resetQuestionPool() {
        availableQuestions = new ArrayList<>(questionPool);
        Collections.shuffle(availableQuestions);
    }

    private void nextRandomQuestion() {
        if (availableQuestions.isEmpty()) {
            resetQuestionPool();
        }
        if (!availableQuestions.isEmpty()) {
            currentQuestion = availableQuestions.remove(0);
        }
    }

    public Question getCurrentQuestion() {
        return currentQuestion;
    }
}