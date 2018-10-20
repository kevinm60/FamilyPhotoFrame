package app.familyphotoframe;

import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Calendar;
import java.text.SimpleDateFormat;

import android.content.res.Resources;

import android.app.Activity;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.content.Intent;
import android.view.WindowManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.MotionEvent;


import com.codepath.oauth.OAuthBaseClient;
import com.bumptech.glide.Glide;
import app.familyphotoframe.repository.FlickrClient;
import app.familyphotoframe.repository.PhotoCollection;
import app.familyphotoframe.slideshow.Display;
import app.familyphotoframe.model.CrossFadeGroup;
import app.familyphotoframe.slideshow.ShowPlanner;


/**
 * this is the main activity, it is launched after successfull login.
 */
public class PhotoFrameActivity extends Activity {

    private FlickrClient flickr;
    private PhotoCollection photoCollection;
    private ShowPlanner showPlanner;
    private Display display;
    private Handler uiHandler;
    private Set<ReHideSystemUiTask> uiTasks;
    final private long FULLSCREEN_DELAY = 2000L;
    final private int WAKE_HOUR = 7;  // 7 am
    final private int SLEEP_HOUR = 21; // 9 pm

    /**
     * instantiate photoCollection, showPlanner, display, then start discovery.
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("PhotoFrameActivity", "created");
        setContentView(R.layout.activity_photo_frame);

        uiHandler = new Handler();
        uiTasks = new HashSet<>();

        hideSystemUI();

        flickr = (FlickrClient) OAuthBaseClient.getInstance(FlickrClient.class, getApplicationContext());
        photoCollection = new PhotoCollection(this, flickr);

        showPlanner = new ShowPlanner(photoCollection);

        CrossFadeGroup groupA = new CrossFadeGroup((ViewGroup)findViewById(R.id.frameA),
                                                   (ImageView)findViewById(R.id.photoA),
                                                   (TextView)findViewById(R.id.captionA));
        CrossFadeGroup groupB = new CrossFadeGroup((ViewGroup)findViewById(R.id.frameB),
                                                   (ImageView)findViewById(R.id.photoB),
                                                   (TextView)findViewById(R.id.captionB));
        display = new Display(this, groupA, groupB, showPlanner);

        photoCollection.startDiscovery();
    }

    /**
     * start the slideshow. called by photoCollection at the end of discovery.
     */
    public void startShow() {
        Log.i("PhotoFrameActivity", "starting slideshow");
        display.run();
    }

    /**
     * full screen the activity.
     */
    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                                        // Set the content to appear under the system bars so that the
                                        // content doesn't resize when the system bars hide and show.
                                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                        // Hide the nav bar and status bar
                                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    protected void onStop() {
        super.onStop();
        display.pause();
        Log.i("PhotoFrameActivity", "stopped");
    }

    protected void onStart() {
        super.onStart();
        if (isDaytime()) {
            wake();
        } else {
            sleep();
        }
        Log.i("PhotoFrameActivity", "started");
    }

    protected void onRestart() {
        super.onRestart();
        startShow();
        Log.i("PhotoFrameActivity", "restarted");
    }

    /**
     * stop the slideshow and turn the screen off
     */
    public void sleep() {
        Log.i("PhotoFrameActivity", "in sleep");
        display.itsNighttime();
        setScreenBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF);
        uiHandler.postAtTime(new WakeTask(), tomorrowMorning());
    }

    /**
     * turn the screen on and resume the slideshow
     */
    public void wake() {
        Log.i("PhotoFrameActivity", "in wake");
        display.itsDaytime();
        setScreenBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL);
        uiHandler.postAtTime(new SleepTask(), thisNight());
    }

    private void setScreenBrightness(final float val) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = val;
        getWindow().setAttributes(lp);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i("PhotoFrameActivity", "menu selected");
        switch (item.getItemId()) {
        case R.id.action_logout:
            logout();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * log out from flickr
     */
    private void logout() {
        Log.i("PhotoFrameActivity", "logout selected");
        flickr.logout();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) {
            return true;
        }
        // Log.i("PhotoFrameActivity", "touch event: " + event);
        for (ReHideSystemUiTask task : uiTasks) {
            uiHandler.removeCallbacks(task);
        }
        ReHideSystemUiTask task = new ReHideSystemUiTask();
        uiTasks.add(task);
        uiHandler.postDelayed(task, FULLSCREEN_DELAY);
        return true;
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

    class ReHideSystemUiTask implements Runnable {
        public void run() {
            // Log.i("PhotoFrameActivity", "rehide system ui now");
            uiTasks.remove(this);
            hideSystemUI();
            // Log.i("PhotoFrameActivity", "current time: " + new SimpleDateFormat().format(Calendar.getInstance().getTime()));
        }
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
