package IO;

import launcher.Client;
import util.DocumentUpdate;
import util.Resources;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * INCOMPLETE
 * Created by Arnold on 2016-12-27.
 */
public class ClientView extends JFrame {

    private JPanel jPanel;
    private JTextArea editingTextArea;
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
    private final DefaultListModel<String> docsModel = new DefaultListModel<>();

    public ClientView(String windowName) {
        //Init the window
        super(windowName);
        setContentPane(jPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setSize(400, 400);

        //Provide the initial values for the address and port
        addressTextField.setText("127.0.0.1");
        portTextField.setText(Integer.toString(Resources.CLIENTPORT));

        //Init the underlying model that is used to display the list of files
        docsList.setModel(docsModel);

        //Create the button handlers which react to them being pressed
        resetAddressButton.addActionListener(e -> addressTextField.setText("127.0.0.1"));
        resetPortButton.addActionListener(e -> portTextField.setText(Integer.toString(Resources.CLIENTPORT)));
        connectButton.addActionListener(e -> Client.userAcceptedConnection = true);
        refreshButton.addActionListener(e -> Client.sender.getList());
        openButton.addActionListener(e -> Client.sender.open(docsModel.elementAt(docsList.getSelectedIndex())));

        //TODO enable once handlers are in place to detect successful connection to proxy
        //disable tabs that require a connection to the proxy to work properly
        //jTabbedPane.setEnabledAt(jTabbedPane.indexOfComponent(ListTab), false);
        //jTabbedPane.setEnabledAt(jTabbedPane.indexOfComponent(EditingTab), false);

        //disable the highlighter as the communications protocol does not account for its operation
        editingTextArea.setHighlighter(null);

        //add a listener to detect key presses targeted at the editing text area
        editingTextArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                //the backspace key event is executed on keyPressed event as well as keyTyped. We override it here so that it doesn't trigger two separate document updates
                if (e.getKeyChar() != KeyEvent.CHAR_UNDEFINED){
                    //consume the key event to prevent it from being executed now as well as later when the proxy confirms it
                    e.consume();
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                int index = editingTextArea.getCaretPosition();
                if (e.getKeyChar() != KeyEvent.CHAR_UNDEFINED){
                    //consume the key event to prevent it from being executed now as well as later when the proxy confirms it
                    e.consume();

                    //remove operations will delete the character after the caret, but a backspace deletes the character before. we decrement the index to set the removal in the correct position
                    if (e.getKeyChar() == KeyEvent.VK_BACK_SPACE){
                        index--;
                    }
                    //convert delete to a backspace event without decrementing the index as it is already in the correct location
                    else if (e.getKeyChar() == KeyEvent.VK_DELETE) {
                        e.setKeyChar(((char) KeyEvent.VK_BACK_SPACE));
                    }

                    documentChanged(e.getKeyChar(), index);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyChar() != KeyEvent.CHAR_UNDEFINED){
                    e.consume();
                }
            }
        });
    }

    /**
     * Show the GUI
     */
    public void create(){
        setVisible(true);
    }

    /**
     * A getter for the data inserted by the user for the address and port fields
     * @return A string of address and port separated by a colon
     */
    public String getAddress(){
        return addressTextField.getText() + ":" + portTextField.getText();
    }

    /**
     * A setter for the list which shows the documents available
     * @param list
     */
    public void setList(String[] list) {
        docsModel.removeAllElements();
        for(String s : list){
            docsModel.addElement(s);
        }
    }

    /**
     * Report a change that should be made to the document.
     * @param element The character that was added. This may be a backspace to represent a deletion
     * @param index The position that the character should be added to or removed from
     */
    private void documentChanged(char element, int index){
        DocumentUpdate documentUpdate = new DocumentUpdate(element, index, Client.getAndIncreaseTransformationNumber());
        Client.sender.sendUpdate(documentUpdate);
    }

    /**
     * Make a change to the document shown in the editor text area
     * @param element The character to add. This may be a backspace to represent a deletion
     * @param index The position that the character should be added to or removed from
     */
    public void addAt(String element, int index){
        if (element.charAt(0) != DocumentUpdate.BACKSPACE){
            editingTextArea.insert(element,index);
        } else if (element.charAt(0) == DocumentUpdate.BACKSPACE){
            try {
                //jTextArea does not support character removal. We use the remove method of the underlying document class
                editingTextArea.getDocument().remove(index, 1);
            } catch (BadLocationException e) {
                //e.printStackTrace();
            }
        }
    }
}
