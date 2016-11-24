package util;

/**
 *
 */
public abstract class Resources {
    public static final int CLIENTPORT = 2228;
    public static final int REPLICAPORT = 2229;
    public static final int RECOVERYPORT = 2227;
    
    /**
     * The time between reconnect attempts in the client.
     */
    public static final int RECONNECTRETRYINTERVAL = 5000;
}
