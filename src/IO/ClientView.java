package IO;

import launcher.Client;
import network.ClientSender;
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
    private JPanel ListTab;
    private JList docsList;
    private JButton refreshButton;
    private JButton openButton;
    private DefaultListModel<String> docsModel = new DefaultListModel<>();

    public ClientView(String windowName) {
        //Init the window
        super(windowName);
        setContentPane(jPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setSize(400, 400);


        docsList.setModel(docsModel);   //Init the underlying model that is used to display the list of files

        //Create the button handlers
        resetAddressButton.addActionListener(e -> addressTextField.setText("127.0.0.1"));
        resetPortButton.addActionListener(e -> portTextField.setText(Integer.toString(Resources.CLIENTPORT)));
        connectButton.addActionListener(e -> Client.userAcceptedConnection = true);
        refreshButton.addActionListener(e -> Client.sender.getList());
        openButton.addActionListener(e -> Client.sender.open(docsModel.elementAt(docsList.getSelectedIndex())));


        //TODO enable once handlers are in place to detect successful connection to proxy
        //jTabbedPane.setEnabledAt(jTabbedPane.indexOfComponent(ListTab), false);
        //jTabbedPane.setEnabledAt(jTabbedPane.indexOfComponent(EditingTab), false);
    }

    public void create(){
        setVisible(true);
        addressTextField.setText("127.0.0.1");
        portTextField.setText(Integer.toString(Resources.CLIENTPORT));
    }

    public String getAddress(){
        return addressTextField.getText() + ":" + portTextField.getText();
    }

    public void setList(String[] list) {
        docsModel.removeAllElements();
        for(String s : list){
            docsModel.addElement(s);
        }
    }
}
