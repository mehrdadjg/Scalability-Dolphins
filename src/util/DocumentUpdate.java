package util;

/**
 * Container class for add and delete operations
 */
public class DocumentUpdate{
    int originalPosition;
    int actualPosition;
    int transformationNumber;
    char c;

    @Override
    public String toString(){
        return ("Add " + c + " " + originalPosition + " " + transformationNumber);
    }
}