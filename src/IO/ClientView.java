package IO;

import launcher.Client;
import util.DocumentUpdate;
import util.Resources;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Observable;
import java.util.Observer;

/**
 * INCOMPLETE
 * Created by Arnold on 2016-12-27.
 */
public class ClientView extends JFrame implements Observer {

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
    private JList<String> docsList;
    private JButton refreshButton;
    private JButton openButton;
    private final DefaultListModel<String> docsModel = new DefaultListModel<>();

    private boolean listUpdated = false;

    public ClientView(String windowName) {
        //Init the window
        super(windowName);
        setContentPane(jPanel);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        pack();
        setSize(410, 400);

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

        //disable tabs that require a connection to the proxy to work properly
        jTabbedPane.setEnabledAt(jTabbedPane.indexOfComponent(ListTab), false);
        jTabbedPane.setEnabledAt(jTabbedPane.indexOfComponent(EditingTab), false);


        //disable the highlighter as the communications protocol does not account for its operation
        editingTextArea.setHighlighter(null);

        //add a listener to detect key presses targeted at the editing text area
        editingTextArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                //the backspace key event is executed on keyPressed event as well as keyTyped. We override it here so that it doesn't trigger two separate document updates
                if (e.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
                    //consume the key event to prevent it from being executed now as well as later when the proxy confirms it
                    e.consume();
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                int index = editingTextArea.getCaretPosition();
                if (e.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
                    //consume the key event to prevent it from being executed now as well as later when the proxy confirms it
                    e.consume();

                    //remove operations will delete the character after the caret, but a backspace deletes the character before. we decrement the index to set the removal in the correct position
                    if (e.getKeyChar() == KeyEvent.VK_BACK_SPACE) {
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
                if (e.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
                    e.consume();
                }
            }
        });
        jTabbedPane.addChangeListener(e -> {
            if (jTabbedPane.getSelectedIndex() == jTabbedPane.indexOfComponent(ListTab)) {
                if (!listUpdated) {
                    Client.sender.getList();
                    listUpdated = true;
                }
            }
        });

        Client.observable.addObserver(this);
    }

    /**
     * Show the GUI
     */
    public void create() {
        setVisible(true);
    }

    /**
     * A getter for the data inserted by the user for the address and port fields
     *
     * @return A string of address and port separated by a colon
     */
    public String getAddress() {
        return addressTextField.getText() + ":" + portTextField.getText();
    }

    /**
     * A setter for the list which shows the documents available
     *
     * @param list A String array of available documents
     */
    private void setList(String[] list) {
        docsModel.removeAllElements();
        for (String s : list) {
            docsModel.addElement(s);
        }
    }

    /**
     * Report a change that should be made to the document.
     *
     * @param element The character that was added. This may be a backspace to represent a deletion
     * @param index   The position that the character should be added to or removed from
     */
    private void documentChanged(char element, int index) {
        DocumentUpdate documentUpdate = new DocumentUpdate(element, index, Client.getAndIncreaseTransformationNumber());
        Client.sender.sendUpdate(documentUpdate);
    }

    /**
     * Make a change to the document shown in the editor text area
     *
     * @param documentUpdate The documentUpdate representing the change to be made to a document state
     */
    private void addAt(DocumentUpdate documentUpdate) {
        String element = documentUpdate.getString();
        int actualPos = documentUpdate.getPosition(DocumentUpdate.PositionType.Actual);
        int intendedPos = documentUpdate.getPosition(DocumentUpdate.PositionType.Intended);
        int index = (actualPos < 0) ? intendedPos : actualPos;

        if (element.charAt(0) != DocumentUpdate.BACKSPACE) {
            editingTextArea.insert(element, index);
        } else if (element.charAt(0) == DocumentUpdate.BACKSPACE) {
            try {
                //jTextArea does not support character removal. We use the remove method of the underlying document class
                editingTextArea.getDocument().remove(index, 1);
            } catch (BadLocationException e) {
                //e.printStackTrace();
            }
        }
    }

    private void connected(boolean isConnected) {
        jTabbedPane.setEnabledAt(jTabbedPane.indexOfComponent(ListTab), isConnected);
        jTabbedPane.setEnabledAt(jTabbedPane.indexOfComponent(EditingTab), isConnected);
    }

    @Override
    public void update(Observable observable, Object arg) {
        if (arg instanceof DocumentUpdate) {
            addAt((DocumentUpdate) arg);
        } else if (arg instanceof String[]) {
            setList((String[]) arg);
        } else {
            connected(Client.isConnected);
        }
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        jPanel = new JPanel();
        jPanel.setLayout(new BorderLayout(0, 0));
        jTabbedPane = new JTabbedPane();
        jPanel.add(jTabbedPane, BorderLayout.SOUTH);
        ConnectionTab = new JPanel();
        ConnectionTab.setLayout(new GridBagLayout());
        jTabbedPane.addTab("Connection", ConnectionTab);
        addressTextField = new JTextField();
        addressTextField.setColumns(10);
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        ConnectionTab.add(addressTextField, gbc);
        final JPanel spacer1 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        ConnectionTab.add(spacer1, gbc);
        portTextField = new JTextField();
        portTextField.setColumns(5);
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        ConnectionTab.add(portTextField, gbc);
        final JLabel label1 = new JLabel();
        label1.setText("Proxy Address");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        ConnectionTab.add(label1, gbc);
        final JLabel label2 = new JLabel();
        label2.setText("Proxy Port");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        ConnectionTab.add(label2, gbc);
        resetAddressButton = new JButton();
        resetAddressButton.setText("Reset");
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        ConnectionTab.add(resetAddressButton, gbc);
        resetPortButton = new JButton();
        resetPortButton.setText("Reset");
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        ConnectionTab.add(resetPortButton, gbc);
        final JPanel spacer2 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        ConnectionTab.add(spacer2, gbc);
        final JPanel spacer3 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.ipady = 200;
        ConnectionTab.add(spacer3, gbc);
        connectButton = new JButton();
        connectButton.setText("Connect");
        connectButton.setVerticalAlignment(0);
        gbc = new GridBagConstraints();
        gbc.gridx = 5;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.SOUTHEAST;
        ConnectionTab.add(connectButton, gbc);
        ListTab = new JPanel();
        ListTab.setLayout(new BorderLayout(0, 0));
        jTabbedPane.addTab("List", ListTab);
        docsList = new JList();
        ListTab.add(docsList, BorderLayout.CENTER);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridBagLayout());
        ListTab.add(panel1, BorderLayout.SOUTH);
        refreshButton = new JButton();
        refreshButton.setText("Refresh");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        panel1.add(refreshButton, gbc);
        openButton = new JButton();
        openButton.setText("Open");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        panel1.add(openButton, gbc);
        EditingTab = new JPanel();
        EditingTab.setLayout(new BorderLayout(0, 0));
        jTabbedPane.addTab("Editing", EditingTab);
        final JScrollPane scrollPane1 = new JScrollPane();
        EditingTab.add(scrollPane1, BorderLayout.CENTER);
        editingTextArea = new JTextArea();
        editingTextArea.setRows(20);
        editingTextArea.setText("");
        scrollPane1.setViewportView(editingTextArea);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return jPanel;
    }
}
