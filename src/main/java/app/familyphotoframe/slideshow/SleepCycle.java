package app.familyphotoframe.slideshow;

import java.util.Calendar;
import java.text.SimpleDateFormat;

import android.view.WindowManager;
import android.view.Window;
import android.os.SystemClock;
import android.os.Handler;
import android.util.Log;


/**
 * this controls sleep and wake events
 */
public class SleepCycle {

    final private Window window;
    final private Handler uiHandler;
    private Display display;

    final private int WAKE_HOUR = 7;  // 7 am
    final private int SLEEP_HOUR = 21; // 9 pm

    public SleepCycle(Window window, Handler uiHandler, Display display) {
        this.window = window;
        this.uiHandler = uiHandler;
        this.display = display;
    }

    public void init() {
        if (isDaytime()) {
            wake();
        } else {
            sleep();
        }
    }

    /**
     * stop the slideshow and turn the screen off
     */
    public void sleep() {
        Log.i("PhotoFrameActivity", "begin sleep");
        display.itsNighttime();
        setScreenBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF);
        uiHandler.postAtTime(new WakeTask(), tomorrowMorning());
    }

    /**
     * turn the screen on and resume the slideshow
     */
    public void wake() {
        Log.i("PhotoFrameActivity", "begin wake");
        display.itsDaytime();
        setScreenBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL);
        uiHandler.postAtTime(new SleepTask(), thisNight());
    }

    private boolean isDaytime() {
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.HOUR_OF_DAY) > WAKE_HOUR && now.get(Calendar.HOUR_OF_DAY) < SLEEP_HOUR;
    }

    private long thisNight() {
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.set(Calendar.HOUR_OF_DAY, SLEEP_HOUR);
        then.set(Calendar.MINUTE, 0);
        then.set(Calendar.SECOND, 0);
        then.set(Calendar.MILLISECOND, 0);
        Log.i("PhotoFrameActivity", "sleep thisNight: " + new SimpleDateFormat().format(then.getTime()));
        return SystemClock.uptimeMillis() + then.getTimeInMillis() - now.getTimeInMillis() ;
    }

    private long tomorrowMorning() {
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        if (then.get(Calendar.AM_PM) == Calendar.PM) {
            then.roll(Calendar.DATE, 1);
        }
        then.set(Calendar.HOUR_OF_DAY, WAKE_HOUR);
        then.set(Calendar.MINUTE, 0);
        then.set(Calendar.SECOND, 0);
        then.set(Calendar.MILLISECOND, 0);
        Log.i("PhotoFrameActivity", "wake tomorrowMorning: " + new SimpleDateFormat().format(then.getTime()));
        return SystemClock.uptimeMillis() + then.getTimeInMillis() - now.getTimeInMillis() ;
    }

    private void setScreenBrightness(final float val) {
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.screenBrightness = val;
        window.setAttributes(lp);
    }

    class SleepTask implements Runnable {
        public void run() {
            sleep();
        }
    }

    class WakeTask implements Runnable {
        public void run() {
            wake();
        }
    }
}
