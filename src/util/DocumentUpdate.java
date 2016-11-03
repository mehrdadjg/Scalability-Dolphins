package util;

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
    int intendedPosition;
    /**
     * The position of the character that was affected by the update.
     * The {@link OperationalTransformation} class will manage this
     * field.
     */
    int actualPosition;
    /**
     * The transformation number of this update.
     */
    int transformationNumber;
    /**
     * The character that must be added to the {@link DocumentUpdate#intendedPosition}.
     * In case of a deletion this character must be set to
     * {@link PositionType#BACKSPACE}.
     */
    char c;
    
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
    
    @Override
    public String toString(){
    	if(isDeletion()) {
    		return ("Delete the character at position " + intendedPosition + " with TN " + transformationNumber + ".");
    	} else {
    		return ("Add " + getChar() + " at position " + intendedPosition + " with TN " + transformationNumber + ".");
    		
    	}
    }
}