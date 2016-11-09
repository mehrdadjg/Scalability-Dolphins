package handlers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class FileHandler {
	
	private BufferedWriter bufferedWriter;
	
	
	public FileHandler(String filename) throws IOException{
		//create the file handle
		File file = new File(System.getProperty("user.dir") + File.separator + filename);
		
		//read all contents of the file
		String temp = "";
		if (file.exists()){
			temp = new String(Files.readAllBytes(file.toPath()));
		}
		
		//create the writer object which will be used in all future write operations
		bufferedWriter = Files.newBufferedWriter(file.toPath());
		
		//write the existing contents back to the file
		//TODO find a more efficient way
		bufferedWriter.newLine();
		bufferedWriter.write(temp);
		bufferedWriter.flush();
	}
	
	public void append(String line) throws IOException{
		bufferedWriter.newLine();
		bufferedWriter.write(line);
		bufferedWriter.flush();
	}
	
	public void close(){
		try {
			bufferedWriter.close();
		} catch (IOException e) {
			// obviously, if closing fails, it is already closed
			//e.printStackTrace();
		}
	}
}
