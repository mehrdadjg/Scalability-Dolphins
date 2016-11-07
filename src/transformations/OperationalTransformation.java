package transformations;

import java.util.ArrayList;

import util.DocumentUpdate;
import util.DocumentUpdate.PositionType;

/**
 * 
 * This class helps manage document updates that are sent to a server or
 * a client against the set of the approved updates that already exist
 * in there.
 * @author Mehrdad Jafari Giv
 *
 */
public class OperationalTransformation{
	
	public static boolean update(ArrayList<DocumentUpdate> approvedUpdates,
			ArrayList<DocumentUpdate> unapprovedUpdates, DocumentUpdate incomingUpdate) {
		if(approvedUpdates.size() == 0) {
			approvedUpdates.add(incomingUpdate);
			return true;
		}
		
		int index = approvedUpdates.size() - 1;
		
		while(index >= 0) {
			DocumentUpdate previousUpdate = approvedUpdates.get(index);
			if(previousUpdate.getTransformationNumber() < incomingUpdate.getTransformationNumber()) {
				/* I don't need to do anything here. I'm not interested in the transformations
				   that has a TN smaller than the incomingUpdate */
			} else if(previousUpdate.getTransformationNumber() == incomingUpdate.getTransformationNumber()) {
				/* This is of interest to me. If there is a transformation with the same
				   transformation number as the incomingUpdate, this shows that the generator of
				   the incomingUpdate didn't know about this particular transformation so I may
				   have to update the position of the incomingUpdate */
				int incomingIntendedPosition = -1;
				int incomingActualPosition = -1;
				/* I check to see if I have stored an actual position for the incomingUpdate,
				   if so I will add one to it and if not I will initialize it to the intended
				   position plus one */
				if(previousUpdate.getPosition(PositionType.Intended) <=
						(incomingIntendedPosition = incomingUpdate.getPosition(PositionType.Intended))) {
					if((incomingActualPosition = incomingUpdate.getPosition(PositionType.Actual)) < 0) {
						incomingUpdate.setPosition(PositionType.Actual, incomingIntendedPosition + 1);
					} else {
						incomingUpdate.setPosition(PositionType.Actual, incomingActualPosition + 1);
					}
				} else {
					/* I don't need to do anything here. Because the update was done in a position
					   that comes after my intended position. */
				}
			} else { // if(last.getTransformationNumber() == incomingUpdate.getTransformationNumber())
				/* This can never happen. Let's assume that there is an update in a client's approved
				   list with a TN greater than the incoming TN. This would mean that the proxy has already
				   broadcasted an update with the incoming TN to some client and then that client has sent
				   out a bigger TN which is a contradiction, since our links are dedicated TCP links, not
				   allowing this to happen. */
			}
			index--;
		}
		
//		index = unapprovedUpdates.size() - 1;
//		
//		while(index >= 0) {
//			DocumentUpdate previousUpdate = unapprovedUpdates.get(index);
//			if(previousUpdate.getTransformationNumber() < incomingUpdate.getTransformationNumber()) {
//				/* I don't need to do anything here. I'm not interested in the transformations
//				   that has a TN smaller than the incomingUpdate */
//			} else if(previousUpdate.getTransformationNumber() == incomingUpdate.getTransformationNumber()) {
//				/* This is of interest to me. If there is a transformation with the same
//				   transformation number as the incomingUpdate, this shows that the generator of
//				   the incomingUpdate didn't know about this particular transformation so I may
//				   have to update the position of the incomingUpdate */
//				int incomingIntendedPosition = -1;
//				int incomingActualPosition = -1;
//				/* I check to see if I have stored an actual position for the incomingUpdate,
//				   if so I will add one to it and if not I will initialize it to the intended
//				   position plus one */
//				if(previousUpdate.getPosition(PositionType.Intended) <=
//						(incomingIntendedPosition = incomingUpdate.getPosition(PositionType.Intended))) {
//					if((incomingActualPosition = incomingUpdate.getPosition(PositionType.Actual)) < 0) {
//						incomingUpdate.setPosition(PositionType.Actual, incomingIntendedPosition + 1);
//					} else {
//						incomingUpdate.setPosition(PositionType.Actual, incomingActualPosition + 1);
//					}
//				} else {
//					/* I don't need to do anything here. Because the update was done in a position
//					   that comes after my intended position. */
//				}
//			} else { // if(last.getTransformationNumber() == incomingUpdate.getTransformationNumber())
//				/* This can never happen. Let's assume that there is an update in a client's approved
//				   list with a TN greater than the incoming TN. This would mean that the proxy has already
//				   broadcasted an update with the incoming TN to some client and then that client has sent
//				   out a bigger TN which is a contradiction, since our links are dedicated TCP links, not
//				   allowing this to happen. */
//			}
//			index--;
//			
//		}
		
		approvedUpdates.add(incomingUpdate);
		
		return true;
	}
	
}