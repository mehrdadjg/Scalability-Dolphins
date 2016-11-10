package util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Base64;

import transformations.OperationalTransformation;

/**
 * 
 * A container class for the document updates. These updates are of the format
 * of addition. Deletion can be dealt with by adding a backspace in a position.
 * This is very important. We always have to check whether an update in for
 * deletion or not.
 * 
 * 
 * @author Ahmed Al Kawally
 *
 */
public class DocumentUpdate{
	/**
	 * The index of the character that must be affected by the update.
	 */
    int intendedPosition					= -1;
    /**
     * The position of the character that was affected by the update.
     * The {@link OperationalTransformation} class will manage this
     * field.
     */
    int actualPosition						= -1;
    /**
     * The transformation number of this update.
     */
    int transformationNumber				= -1;
    /**
     * The character that must be added to the {@link DocumentUpdate#intendedPosition}.
     * In case of a deletion this character must be set to
     * {@link PositionType#BACKSPACE}.
     */
    char c									= 0;
    
    /**
     * The physical address of the client who initiated the update.
     */
    String mac								= null;
    
    public enum PositionType {
    	Intended,
    	Actual
    }
    
    /**
     * The backspace character.
     */
    public static final char BACKSPACE = 8;
    
    /**
     * Creates a new instance of document update.
     * @param newChar The character that we want to add. If the purpose of the
     * update is to delete a character {@link PositionType#BACKSPACE} must be
     * used.
     * @param position The position that the update must affect.
     * @param TN The transformation number of this update.
     */
    public DocumentUpdate(char newChar, int position, int TN) {
    	this.c = newChar;
    	this.intendedPosition = position;
    	this.transformationNumber = TN;

    	this.actualPosition = -1;
    	
    	this.mac = DocumentUpdate.getSelfMAC();
	}

	private DocumentUpdate() {
    	
    }
    
    /**
     * Returns the position of in which the update will happen. You can
     * wither get the <b>intended</b> position (which is what the editor had in
     * mind when they performed the update) or the <b>actual</b> position (which
     * is where the update was performed considering other updates).
     * @param type The type of the position. Use one of the following: <ul><li>
     * {@link PositionType#Intended}</li><li>{@link PositionType#Actual}</li>
     * </ul> 
     * @return Returns the position that was asked. If the type of the position
     * is invalid or the actual position is not set yet, returns -1.
     */
    public int getPosition(PositionType type) {
    	switch (type) {
		case Intended:
			return intendedPosition;
			
		case Actual:
			return actualPosition;
			
		default:
			return -1;
		}
    }
    
    /**
     * Set the specified position of the update.
     * @param type The type of the position. Use one of the following: <ul><li>
     * {@link PositionType#Intended}</li><li>{@link PositionType#Actual}</li>
     * </ul> 
     * @param position The new position to be set.
     */
    public void setPosition(PositionType type, int position) {
    	switch (type) {
		case Intended:
			intendedPosition = position;
			return;
			
		case Actual:
			actualPosition = position;
			return;
			
		default:
			return;
		}
    }
    
    /**
     * Gets the physical address of the client who caused the update.
     * @return Returns the Base64 representation of the MAC address, or null
	 * if an exception occurs.
     */
    public String getMAC() {
    	return this.mac;
	}
    
    /**
     * Gets the physical address of this client.
     * @return Returns the Base64 representation of the MAC address, or null
	 * if an exception occurs.
     */
    public static String getSelfMAC() {
    	InetAddress ip;
    	try {
    		ip = InetAddress.getLocalHost();
    		NetworkInterface network = NetworkInterface.getByInetAddress(ip);
    		return Base64.getEncoder().encodeToString(network.getHardwareAddress());
    	} catch(IOException e) {
    		return null;
    	}
	}
    
    /**
     * @return Returns the transformation number of the update.
     */
    public int getTransformationNumber() {
    	return transformationNumber;
    }
    
    /**
     * @return Returns the character that must be added. Or returns
     * {@link DocumentUpdate#BACKSPACE} for a deletion update.
     */
    public char getChar() {
    	return c;
    }
    
    /**
     * Determines whether an update is a deletion or not.
     * @return Returns true is the update is for a deletion, false otherwise.
     */
    public boolean isDeletion() {
    	if (getChar() == DocumentUpdate.BACKSPACE) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    public static DocumentUpdate fromString(String input) {
    	input = input.trim();
    	String[] inputList = input.split(" ");
    	
    	DocumentUpdate out = new DocumentUpdate();
//    	out.intendedPosition		= Integer.parseInt(inputList[0]);
//    	out.actualPosition			= Integer.parseInt(inputList[1]);
//    	out.transformationNumber	= Integer.parseInt(inputList[2]);
//    	out.c						= (char) Integer.parseInt(inputList[3]);
//    	out.mac						= inputList[4];
    	
    	String header = inputList[0];
    	if(header.matches("delete")) {
    		out.intendedPosition		= Integer.parseInt(inputList[2]);
        	out.actualPosition			= Integer.parseInt(inputList[3]);
        	out.transformationNumber	= Integer.parseInt(inputList[4]);
        	out.mac						= inputList[5];
        	out.c						= DocumentUpdate.BACKSPACE;
    	} else if(header.matches("add")) {
    		out.intendedPosition		= Integer.parseInt(inputList[2]);
        	out.actualPosition			= Integer.parseInt(inputList[3]);
        	out.transformationNumber	= Integer.parseInt(inputList[4]);
        	out.mac						= inputList[5];
        	out.c						= (char) Integer.parseInt(inputList[6]);
    	}
    	
    	return out;
    }
    
    @Override
    public String toString(){
    	String str = null;
    	if(isDeletion()) {
//    		str = String.valueOf(intendedPosition) + DELIM + String.valueOf(actualPosition) + DELIM + String.valueOf(transformationNumber) + DELIM + String.valueOf((int) c) + DELIM + mac + "\n";
    		
    		
    		str = "delete " + 										// The Header
    				String.valueOf(0) +	" " +						// Document ID
    				String.valueOf(intendedPosition) + " " +		// Intended Position
    				String.valueOf(actualPosition) + " " +			// Actual Position
    				String.valueOf(transformationNumber) + " " +	// Transformation Number
    				mac + "\n";										// The MAC Address
    	} else {
    		str = "add " + 											// The Header
    				String.valueOf(0) +	" " +						// Document ID
    				String.valueOf(intendedPosition) + " " +		// Intended Position
    				String.valueOf(actualPosition) + " " +			// Actual Position
    				String.valueOf(transformationNumber) + " " +	// Transformation Number
    				mac + " " +										// The MAC Address
    				String.valueOf((int) c) + "\n";					// The Message
    	}
    	return str;
    }
    
    @Override
    public boolean equals(Object other) {
    	if(other == null)
    		return false;
    	if(other == this)
    		return true;
    	if(!(other instanceof DocumentUpdate))
    		return false;
    	DocumentUpdate otherUpdate = (DocumentUpdate) other;
    	if(otherUpdate.c ==this.c &&
    			otherUpdate.transformationNumber == this.transformationNumber &&
    			otherUpdate.intendedPosition == this.intendedPosition)
    		return true;
    	else
    		return false;
    }
}