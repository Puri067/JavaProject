package src;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
public class SencePanel extends JPanel implements ActionListener {
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

    

    private JPanel lobbyPanel;
    private JPanel joinRoomPanel;
    private JButton CreateBtn;
    private JButton JoinBtn;
    private JButton backlobbyButton;
    private JButton backJoinButton;
    private JButton StartBtn;
    private JButton JoinRoomBtn;
    private JButton StartGameBtn;
    private JFrame frame;
    private CardLayout cardLayout;
    private String currentPanel = "mainPanel"; //เช็คว่าตอนนี้อยู่ที่ panel ไหน

    //Constructor
public SencePanel(JFrame frame) {
        this.frame = frame;
       cardLayout = new CardLayout();
        setLayout(cardLayout);
 
    add(createMainPanel(), "mainPanel");
    add(createLobbyPanel(), "lobbyPanel");
    add(createJoinRoomPanel(), "joinRoomPanel");
    add(createGamePanel(), "GamePanel");
    }
    
    //หน้าหลัก
    private JPanel createMainPanel() {
        
        JPanel mainPanel = new JPanel();
        mainPanel.setOpaque(false);
        mainPanel.setLayout(null);
        
        //ปุ่ม Create
    CreateBtn = new JButton(CBtn);
        CreateBtn.setBounds(frame.getWidth() / 2 - 300, frame.getHeight() / 2 - 150 , 200, 100);
        CreateBtn.setContentAreaFilled(false);
        CreateBtn.setBorderPainted(false);
        CreateBtn.setFocusPainted(false);
        CreateBtn.setOpaque(false);
        CreateBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        CreateBtn.addActionListener(this);
       
       //ปุ่ม Join
    JoinBtn = new JButton(JBtn);
        JoinBtn.setBounds(frame.getWidth() / 2 -300, frame.getHeight() / 2  , 200, 100);
        JoinBtn.setContentAreaFilled(false);
        JoinBtn.setBorderPainted(false);
        JoinBtn.setFocusPainted(false);
        JoinBtn.setOpaque(false);
        JoinBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JoinBtn.addActionListener(this);

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
        backlobbyButton.setBounds(10, 15, 190, 50);
        backlobbyButton.setContentAreaFilled(false); //ทำให้ปุ่มไม่แสดงพื้นหลัง
        backlobbyButton.setBorderPainted(false); //ทำให้ปุ่มไม่แสดงขอบ
        backlobbyButton.setFocusPainted(false); //ทำให้ปุ่มไม่แสดงขอบ
        backlobbyButton.setOpaque(false); //ทำให้ปุ่มโปร่งใส
        backlobbyButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backlobbyButton.addActionListener(this);
         
        //ปุ่ม Start
        StartBtn = new JButton();
        StartBtn.setBounds(frame.getWidth() /2 -300, frame.getHeight() / 2 + 140, 220, 50);
        StartBtn.setContentAreaFilled(false);
        StartBtn.setBorderPainted(false);
        StartBtn.setFocusPainted(false);
        StartBtn.setOpaque(false);
        StartBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        StartBtn.addActionListener(this);

        lobbyPanel.add(backlobbyButton);
        lobbyPanel.add(StartBtn);

        return lobbyPanel;
    }

    //หน้าห้อง join room
    private JPanel createJoinRoomPanel() {
        JPanel joinRoomPanel = new JPanel();
        joinRoomPanel.setOpaque(false);
        joinRoomPanel.setLayout(null);
        
        //ปุ่ม Back
        backJoinButton= new JButton();
        backJoinButton.setBounds(10, 15, 190, 50);
        backJoinButton.setContentAreaFilled(false);
        backJoinButton.setBorderPainted(false);
        backJoinButton.setFocusPainted(false);
        backJoinButton.setOpaque(false);
        backJoinButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backJoinButton.addActionListener(this);

          //ปุ่ม Join Room
        
        joinRoomPanel.add(backJoinButton);
        return joinRoomPanel;
    }
    
    //หน้าด่านเกม
    private JPanel createGamePanel() {
        JPanel gamePanel = new JPanel();
        gamePanel.setOpaque(false);
        gamePanel.setLayout(null);       
        
        return gamePanel;
    }
    
    
    @Override 
        public void actionPerformed(ActionEvent e) {
            if(e.getSource() == CreateBtn) {      //เข้าสู่ lobbyPanel
                 currentPanel = "lobbyPanel";
                cardLayout.show(this, "lobbyPanel");
                repaint();
               // System.out.println("Create button clicked");

            } else if(e.getSource() == JoinBtn) { //เข้าสู่ joinRoomPanel
               currentPanel = "joinRoomPanel";
                cardLayout.show(this, "joinRoomPanel");
               // System.out.println("Join button clicked");
                repaint();
            } else if(e.getSource() == backlobbyButton || e.getSource() == backJoinButton) { //กลับสู่ mainPanel
              currentPanel = "mainPanel";
                cardLayout.show(this, "mainPanel");
                repaint();
               // System.out.println("Back button clicked");
            } 
            else if(e.getSource() == StartBtn) { //เริ่มเกม
                currentPanel = "GamePanel";
                cardLayout.show(this, "GamePanel");
                repaint();
               // System.out.println("Start button clicked");
            } else if(e.getSource() == JoinRoomBtn) { //เข้าห้อง
                // Join room logic here 
               // System.out.println("Join Room button clicked");
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
                   // System.out.println("lobbyPanel paintComponent called: " + currentPanel);
                }
                else if(currentPanel.equals("joinRoomPanel")){
            
                    g.drawImage(bgJoin.getImage(), 0, 0, getWidth(), getHeight(), this);
                   
                    //System.out.println("joinRoomPanel paintComponent called: " + currentPanel);
                }
                else if(currentPanel.equals("GamePanel")){
                    g.drawImage(bgGame.getImage(), 0, 0, getWidth(), getHeight(), this);
                }
            }
}

