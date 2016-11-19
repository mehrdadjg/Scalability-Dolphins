package util;

/**
 *
 */
public abstract class Resources {
    public static final int CLIENTPORT = 22;
    public static final int REPLICAPORT = 21;
    public static final int RECOVERYPORT = 880;
    
    /**
     * The time between reconnect attempts in the client.
     */
    public static final int RECONNECTRETRYINTERVAL = 5000;
}
