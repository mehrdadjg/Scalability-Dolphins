package IO;

import util.Resources;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * INCOMPLETE
 * Created by Arnold on 2016-12-27.
 */
public class ClientView extends JFrame {

    private JPanel jPanel;
    private JTextArea textArea1;
    private JTabbedPane jTabbedPane;
    private JPanel ConnectionTab;
    private JPanel EditingTab;
    private JTextField addressTextField;
    private JTextField portTextField;
    private JButton resetAddressButton;
    private JButton resetPortButton;
    private JButton connectButton;

    public ClientView(String windowName) {
        JFrame frame = new JFrame(windowName);
        frame.setContentPane(jPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setSize(400, 400);
        frame.setVisible(true);
        resetAddressButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addressTextField.setText("127.0.0.1");
            }
        });
        resetPortButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                portTextField.setText(Integer.toString(Resources.CLIENTPORT));
            }
        });
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
    }

    public void create(){
        //TODO
    }
}
