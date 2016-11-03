package transformations;

import java.util.ArrayList;

import util.DocumentUpdate;

/**
 * 
 * This class helps manage document updates that are sent to a server or
 * a client against the set of the approved updates that already exist
 * in there.
 * @author Mehrdad Jafari Giv
 *
 */
public class OperationalTransformation{
	
	public static boolean update(ArrayList<DocumentUpdate> approvedUpdates, DocumentUpdate incomingUpdate) {
		if(approvedUpdates.size() == 0) {
			approvedUpdates.add(incomingUpdate);
			return true;
		}
		
		DocumentUpdate last = approvedUpdates.get(approvedUpdates.size() - 1);
		
		do {
			
		} while(last != null);
		
		return true;
	}
	
}