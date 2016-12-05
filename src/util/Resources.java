package util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 *  A container for various values and settings that can be referenced from anywhere
 */
public abstract class Resources {
// To add a new resource, create the variable that will be used to access it in the DEFAULTS section, and assign it a default value.
// To read a new resource from the config, read it's value using properties.getProperty(String key, String defaultValue) and cast it to the correct datatype in the READING section

    //DEFAULTS
    //Define the available resources and give them initial values to be used as defaults
    public static int CLIENTPORT = 2228;                //The port used for client-proxy connections
    public static int REPLICAPORT = 2229;               //The port used for replica-proxy connections
    public static int RECOVERYPORT = 2227;              //The port that replicas accept recovery requests on
    public static int RECONNECTRETRYINTERVAL = 5000;    //The time between reconnect attempts in the client
    public static int TIMEOUT = 1500;              //The interval between pings to the proxy
    public static boolean DEBUG = false;                //For debugging purposes


    //Keeps this class static. No methods needed
    static {
        //The Properties class reads a file and retrieves the values in it, allowing us to retrieve values using their keys
        Properties properties = new Properties();

        //Properties needs a FileInputStream to the config file.
        try (FileInputStream fis = new FileInputStream("config.properties")){
            //Load the values from the FileInputStream. It can be closed afterwards and the values are preserved in this instance of Properties
            properties.load(fis);

            //READING
            //set all of the resources, using the default value for any missing keys
            CLIENTPORT = Integer.parseInt(properties.getProperty("CLIENTPORT", "" + CLIENTPORT));
            REPLICAPORT = Integer.parseInt(properties.getProperty("REPLICAPORT", "" + REPLICAPORT));
            RECOVERYPORT = Integer.parseInt(properties.getProperty("RECOVERYPORT", "" + RECOVERYPORT));
            RECONNECTRETRYINTERVAL = Integer.parseInt(properties.getProperty("RECONNECTRETRYINTERVAL", "" + RECONNECTRETRYINTERVAL));
            TIMEOUT = Integer.parseInt(properties.getProperty("TIMEOUT", "" + TIMEOUT));
            DEBUG = Boolean.parseBoolean(properties.getProperty("DEBUG", "" + DEBUG));
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("Warning: config file not found. Using default settings");
        }
    }

}
