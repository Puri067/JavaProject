package src;

import javax.swing.*;

public class Mainframe extends JFrame {

    Mainframe() {

        setTitle("Escape From the Lava");
        setSize(1920, 1080);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
       
    }

    public static void main(String[] args) {
        Mainframe frame = new Mainframe();
        SencePanel panel = new SencePanel(frame);
        frame.add(panel);   
        frame.setVisible(true);
    }

}