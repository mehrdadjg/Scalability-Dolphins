package util;

import java.util.Timer;
import java.util.TimerTask;

/**
 * A container with methods for using a timer as a stopwatch
 */
public class TimeoutTimer {
    private boolean timeoutFlag = false;
    private Timer timer;

    /**
     * Starts a timer which will set this object's timoutFlag variable to true if it runs to completion
     * @param timeout The timer's duration
     */
    public void startTimer(int timeout){
        reset();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                timeoutFlag = true;
                timer.cancel();
            }
        }, timeout);
    }

    /**
     * Getter for timeoutFlag
     * @return the value of timeoutFlag
     */
    public boolean isTimeoutFlag(){
        return timeoutFlag;
    }

    /**
     * Trigger for timeout flag to manually set it off
     */
    public void setTimeoutFlag(){
        timeoutFlag = true;
    }

    /**
     * Resets the timer's values to default to allow it to be used again
     */
    public void reset(){
        cancel();
        timeoutFlag = false;
    }

    /**
     * Stops any currently operating timers before they complete
     */
    private void cancel(){
        if (timer != null){
            timer.cancel();
            timer.purge();
        }
    }
}
