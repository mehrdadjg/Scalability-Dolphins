package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;

public class Logger {
	
	private static File				root	= null;
	
	private static PrintWriter		writer	= null;
	
	private static int				index	= 1;
	
	private static ProcessType		type	= null;
	
	public enum ProcessType {
		Proxy,
		Replica,
		Client;
		
		@Override
		public String toString() {
			switch (this) {
			case Proxy:
				return "Proxy";
			case Replica:
				return "Replica";
			case Client:
				return "Client";
			default:
				throw new IllegalArgumentException();
			}
		}
	}
	
	public enum LogType {
		Default,
		Error,
		Warning,
		Info;
		
		@Override
		public String toString() {
			switch (this) {
			case Default:
				return "Default";
			case Error:
				return "Error";
			case Warning:
				return "Warning";
			case Info:
				return "Info";
			default:
				throw new IllegalArgumentException();
			}
		}
		
		public static LogType fromString(String type) {
			if(type.compareTo("Default") == 0) {
				return LogType.Default;
			} else if(type.compareTo("Error") == 0) {
				return LogType.Error;
			} else if(type.compareTo("Warning") == 0) {
				return LogType.Warning;
			} else if(type.compareTo("Info") == 0) {
				return LogType.Info;
			} else {
				throw new IllegalArgumentException();
			}
		}
	}
	
	public static boolean initialize(ProcessType type) {
		Logger.type = type;
		switch (type) {
		case Proxy:
			root = new File("logs/proxy");
			break;
			
		case Replica:
			root = new File("logs/replica");
			break;
			
		case Client:
			root = new File("logs/client");
			break;

		default:
			break;
		}
		
		if(!root.exists()) {
			if(!root.mkdirs()) {
				System.err.println("Logger::Could not initilize the logger.");
			}
		}
		
		File[] listOfFiles = Logger.root.listFiles();
		
		int maximum;
		
		if(listOfFiles == null || listOfFiles.length < 1) {
			maximum = 0;
		} else {
			try {
				maximum = Integer.parseInt(listOfFiles[0].getName());
			} catch(NumberFormatException e) {
				return false;
			}
			for (int i = 1; i < listOfFiles.length; i++) {
				try {
					int current = Integer.parseInt(listOfFiles[i].getName());
					if(current > maximum) {
						maximum = current;
					}
				} catch(NumberFormatException e) {
					return false;
				}
			}
		}
		
		File logFile = new File(root.getAbsolutePath() + File.separator + (maximum + 1));
		if(!logFile.exists()) {
			try {
				if(!logFile.createNewFile()) {
					return false;
				}
			} catch (IOException e) {
				return false;
			}
		}
		
		try {
			writer = new PrintWriter(logFile);
		} catch (FileNotFoundException e) {
			return false;
		}
		log(type.toString() + " logger started.", LogType.Info);
		return true;
	}
	
	public static void log(String msg) {
		log(msg, LogType.Default);
	}
	
	public static void log(String msg, LogType type) {
		writer.println( index++ + "::" +
						Calendar.getInstance().getTimeInMillis() + "::" +
						type.toString() + "::" +
						msg.trim());
		writer.flush();
		
		if(type == LogType.Error)
			System.err.println(index + "(" + type.toString().charAt(0) + "). " + msg);
		else
			System.out.println(index + "(" + type.toString().charAt(0) + "). " + msg);
	}
	
	@Override
	public void finalize() {
		log(type.toString() + " logger closed.", LogType.Info);
		writer.flush();
		writer.close();
	}
	
	private static int findMinimumIndex(long[] timestamps) {
		long	min			= -1;
		int		minIndex	= -1;
		for(int i = 0; i < timestamps.length; i++) {
			if(timestamps[i] > 0) {
				if(min == -1) {
					min = timestamps[i];
					minIndex = i;
				} else {
					if(min > timestamps[i]) {
						min = timestamps[i];
						minIndex = i;
					}
				}
			}
		}
		
		return minIndex;
	}
	
	public static void main(String[] args) throws NumberFormatException, IOException {
		System.out.println("Merging log files started.");
		File		proxyRoot							= new File("logs/proxy");
		File		replicaRoot							= new File("logs/replica");
		File		clientRoot							= new File("logs/client");
		
		File[]		listOfProxyFiles					= new File[0];
		File[]		listOfReplicaFiles					= new File[0];
		File[]		listOfClientFiles					= new File[0];
		
		if(proxyRoot.exists()) {
			listOfProxyFiles							= proxyRoot.listFiles();
			if(listOfProxyFiles == null) {
				listOfProxyFiles						= new File[0];
			}
		}
		
		if(replicaRoot.exists()) {
			listOfReplicaFiles							= replicaRoot.listFiles();
			if(listOfReplicaFiles == null) {
				listOfReplicaFiles						= new File[0];
			}
		}
		
		if(clientRoot.exists()) {
			listOfClientFiles							= clientRoot.listFiles();
			if(listOfClientFiles == null) {
				listOfClientFiles						= new File[0];
			}
		}
		
		int			count								= listOfProxyFiles.length +
														  listOfReplicaFiles.length +
														  listOfClientFiles.length;
		
		if(count == 0) {
			System.out.println("There are no files to be merged.");
			return;
		} else {
			System.out.println(count + " files are ready to be merged.");
		}
		
		File		root								= new File("logs");
		
		File[]		listOfRootFiles						= root.listFiles();
		
		int			maximum;
		
		if(listOfRootFiles == null || listOfRootFiles.length < 1) {
			maximum = 0;
		} else {
			if(listOfRootFiles[0].isFile()) {
				maximum = Integer.parseInt(listOfRootFiles[0].getName());
			} else {
				maximum = 0;
			}
			for (int i = 1; i < listOfRootFiles.length; i++) {
				if(listOfRootFiles[i].isFile()) {
					int current = Integer.parseInt(listOfRootFiles[i].getName());
					if(current > maximum) {
						maximum = current;
					}
				}
			}
		}
		
		File		newLogFile							= new File(root.getAbsolutePath() + File.separator + (maximum + 1));
		
		PrintWriter	logWriter = null;
		try {
			logWriter									= new PrintWriter(newLogFile);
		} catch (FileNotFoundException e1) {
			return;
		}
		
		BufferedReader[]	listOfReaders				= new BufferedReader[count];
		for(int i = 0; i < count; i++) {
			try {
				if(i < listOfProxyFiles.length) {
					listOfReaders[i] = new BufferedReader(new FileReader(listOfProxyFiles[i]));
				} else if(i < listOfReplicaFiles.length) {
					listOfReaders[i] = new BufferedReader(new FileReader(listOfReplicaFiles[i - listOfProxyFiles.length]));
				} else if(i < listOfClientFiles.length) {
					listOfReaders[i] = new BufferedReader(new FileReader(listOfClientFiles[i - listOfProxyFiles.length - listOfReplicaFiles.length]));
				}
			} catch(FileNotFoundException e) {
				return;
			}
		}
		
		String[]	log									= new String[count];
		long[]		timestamps							= new long[count];
		for(int i = 0; i < count; i++) {
			log[i] = listOfReaders[i].readLine();
			timestamps[i] = Long.parseLong(log[i].split("::")[1]);
		}
		
		int			minimumIndex						= findMinimumIndex(timestamps);
		
		while(minimumIndex >= 0) {
			logWriter.println(log[minimumIndex]);
			logWriter.flush();
			
			log[minimumIndex] = listOfReaders[minimumIndex].readLine();
			try {
				timestamps[minimumIndex] = Long.parseLong(log[minimumIndex].split("::")[1]);
			} catch(NullPointerException e) {
				timestamps[minimumIndex] = -1;
			}
			
			minimumIndex								= findMinimumIndex(timestamps);
		}
		
		logWriter.close();
		
		System.out.println("Merging completed. The output address is: " + newLogFile.getAbsolutePath());
		
		for(int i = 0; i < count; i++) {
			listOfReaders[i].close();
		}
		
		for(int i = 0; i < listOfProxyFiles.length; i++) {
			listOfProxyFiles[i].delete();
		}
		
		for(int i = 0; i < listOfReplicaFiles.length; i++) {
			listOfReplicaFiles[i].delete();
		}
		
		for(int i = 0; i < listOfClientFiles.length; i++) {
			listOfClientFiles[i].delete();
		}
		
		System.out.println(count + " files deleted.");
	}
	
}
