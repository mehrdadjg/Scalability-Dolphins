package util;

import java.util.Timer;
import java.util.TimerTask;

/**
 * INCOMPLETE
 */
public class TimeoutTimer {
    private boolean timeoutFlag = false;
    private Timer timer;

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

    public boolean isTimeoutFlag(){
        return timeoutFlag;
    }

    public void reset(){
        cancel();
        timeoutFlag = false;
    }

    public void cancel(){
        if (timer != null){
            timer.cancel();
            timer.purge();
        }
    }
}
