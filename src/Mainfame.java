package src;

import javax.swing.*;

public class Mainfame extends JFrame {

    Mainfame() {

        setTitle("Escape From the Lava");
        setSize(1920, 1080);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
       
    }

    public static void main(String[] args) {
        Mainfame frame = new Mainfame();
        SencePanel panel = new SencePanel(frame);
        frame.add(panel);
        frame.setVisible(true);
    }

}