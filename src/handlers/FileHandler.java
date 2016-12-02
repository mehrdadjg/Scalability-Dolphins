package handlers;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * A handler for the storage layer. This one stores in a file in the current working directory of the program.
 */
public class FileHandler {

	private File file;
	private FileWriter fileWriter;
	private static String newLine = System.getProperty("line.separator");

	/**
	 * @param filename name of the file to be added. this does not include the entire path which is always the current working directory.
	 *                 If the named file does not exist, it will be created
	 * @throws IOException if the file could not be created or accessed
	 */
	public FileHandler(String filename) throws IOException{
		//create the file handle
		file = new File(System.getProperty("user.dir") + File.separator + filename);

		//create the writer object which will be used in all future write operations
		fileWriter = new FileWriter(file, true);
	}

	public String getFileName(){
		return file.getName();
	}

	/**
	 * appends a string to the end of a file on it's own line
	 * @param data the line to add
	 * @throws IOException if the file could not be written to. likely means the file was deleted
	 */
	public void append(String data) throws IOException{
		fileWriter.append(data.trim());
		fileWriter.append(newLine);
		fileWriter.flush();
	}

	/**
	 * returns the contents of the file as a string array
	 * @return an array of all the lines in the file, or an empty array if the file could not be read or is empty
	 */
	public String[] read(){
		try {
			String[] output = new String(Files.readAllBytes(file.toPath())).split(newLine);
			if(output.length == 1 && output[0].trim().matches("")) {
				return new String[0];
			} else {
				return output;
			}
		} catch (IOException e) {
			//implies file does not exist
			//e.printStackTrace();

			//return empty array
			System.err.println("sdfsdf");
			return new String[0];
		}
	}

	/**
	 * close the streams used to read and write
	 */
	public void close(){
		try {
			fileWriter.close();
		} catch (IOException e) {
			// obviously, if closing fails, it is already closed
			//e.printStackTrace();
		}
	}

	public int hash(int length){
		return Arrays.hashCode(Arrays.copyOf(read(), length));
	}
}