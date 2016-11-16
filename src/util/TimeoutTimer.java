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
        timeoutFlag = false;
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

    public void cancel(){
        if (timer != null){
            timer.cancel();
        }
    }
}
