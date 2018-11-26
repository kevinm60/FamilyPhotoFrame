package app.familyphotoframe;

import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import android.content.res.Resources;

import android.app.Activity;
import android.app.ActionBar;
import android.app.ActionBar.OnMenuVisibilityListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.support.v4.view.GestureDetectorCompat;

import com.codepath.oauth.OAuthBaseClient;
import com.bumptech.glide.Glide;
import app.familyphotoframe.repository.FlickrClient;
import app.familyphotoframe.repository.PhotoCollection;
import app.familyphotoframe.slideshow.Display;
import app.familyphotoframe.model.CrossFadeGroup;
import app.familyphotoframe.slideshow.ShowPlanner;
import app.familyphotoframe.slideshow.SleepCycle;


/**
 * this is the main activity, it is launched after successfull login.
 */
public class PhotoFrameActivity extends Activity {

    private GestureDetectorCompat gestureDetector;
    private int playDrawableId;
    private int pauseDrawableId;
    private ImageButton playPauseButton;
    private FlickrClient flickr;
    private PhotoCollection photoCollection;
    private ShowPlanner showPlanner;
    private Display display;
    private Handler uiHandler;
    private Set<ReHideSystemUiTask> uiTasks;
    private SleepCycle sleepCycle;
    final private long FULLSCREEN_DELAY = 2000L;
    final private int SWIPE_MIN_DISTANCE = 120;
    final private int SWIPE_THRESHOLD_VELOCITY = 200;

    class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent event) {
            // Log.i("PhotoFrameActivity","onDown: " + event.toString());
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            // Log.i("PhotoFrameActivity", "onSingleTapUp: " + event.toString());
            clearRehideTasks();
            configureFullscreenMode(false);
            scheduleRehideTask();
            return true;
        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            // Log.i("PhotoFrameActivity", "onFling: " + event1.toString() + event2.toString());
            if(event1.getX() - event2.getX() > SWIPE_MIN_DISTANCE &&
               Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                display.forward();
                return true; // Right to left
            } else if (event2.getX() - event1.getX() > SWIPE_MIN_DISTANCE &&
                       Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                display.backward();
                return true; // Left to right
            }
            return false;
        }
    }

    /**
     * instantiate photoCollection, showPlanner, display, then start discovery.
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("PhotoFrameActivity", "created");
        setContentView(R.layout.activity_photo_frame);

        uiHandler = new Handler();
        uiTasks = new HashSet<>();

        gestureDetector = new GestureDetectorCompat(this, new GestureListener());

        getActionBar().addOnMenuVisibilityListener(
            new ActionBar.OnMenuVisibilityListener() {
                @Override
                public void onMenuVisibilityChanged (boolean isVisible) {
                    if (isVisible) {
                        clearRehideTasks();
                    } else {
                        configureFullscreenMode(true);
                    }
                }
            });

        playDrawableId = getResources().getIdentifier("ic_media_play", "drawable", getPackageName());
        pauseDrawableId = getResources().getIdentifier("ic_media_pause", "drawable", getPackageName());
        playPauseButton = (ImageButton) findViewById(R.id.buttonPlayPause);
        playPauseButton.setImageResource(pauseDrawableId);
        playPauseButton.setOnClickListener(
            new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    if (display.isSlideshowRunning()) {
                        display.pause();
                        playPauseButton.setImageResource(playDrawableId);
                    } else {
                        display.resume();
                        playPauseButton.setImageResource(pauseDrawableId);
                    }
                }
            });

        configureFullscreenMode(true);

        flickr = (FlickrClient) OAuthBaseClient.getInstance(FlickrClient.class, getApplicationContext());
        photoCollection = new PhotoCollection(this, flickr);

        showPlanner = new ShowPlanner(photoCollection);

        TextView textInsufficientPhotos = (TextView)findViewById(R.id.textInsufficientPhotos);
        CrossFadeGroup groupA = new CrossFadeGroup((ViewGroup)findViewById(R.id.frameA),
                                                   (ImageView)findViewById(R.id.photoA),
                                                   (TextView)findViewById(R.id.captionA));
        CrossFadeGroup groupB = new CrossFadeGroup((ViewGroup)findViewById(R.id.frameB),
                                                   (ImageView)findViewById(R.id.photoB),
                                                   (TextView)findViewById(R.id.captionB));
        display = new Display(this, textInsufficientPhotos, groupA, groupB, showPlanner);
        sleepCycle = new SleepCycle(getWindow(), uiHandler, display, photoCollection);

        sleepCycle.init(); // initiate discovery

    }

    @Override
    protected void onStop() {
        super.onStop();
        display.pause();
        playPauseButton.setImageResource(playDrawableId);
        Log.i("PhotoFrameActivity", "stopped");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        display.resume();
        playPauseButton.setImageResource(pauseDrawableId);
        Log.i("PhotoFrameActivity", "restarted");
    }

    /**
     * start the slideshow. called by photoCollection at the end of discovery.
     */
    public void startShow() {
        Log.i("PhotoFrameActivity", "starting slideshow");
        display.prime();
    }

    /**
     * full screen the activity.
     */
    private void configureFullscreenMode(boolean fullscreenMode) {
        View decorView = getWindow().getDecorView();
        int uiVisibilityFlags =
            // Set the content to appear under the system bars so that the
            // content doesn't resize when the system bars hide and show.
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            // Show the system bar only if user swipes from the top edge of
            // the screen
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        if (fullscreenMode) {
            // Hide the nav bar and status bar
            uiVisibilityFlags = uiVisibilityFlags
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;

            playPauseButton.setVisibility(View.GONE);
        } else {
            playPauseButton.setVisibility(View.VISIBLE);
        }
        
        decorView.setSystemUiVisibility(uiVisibilityFlags);

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
        case R.id.action_synchronize:
            photoCollection.startDiscovery();
            return true;
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
        if (this.gestureDetector.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    
    private void clearRehideTasks() {
        for (ReHideSystemUiTask task : uiTasks) {
            uiHandler.removeCallbacks(task);
        }
    }

    private void scheduleRehideTask() {
        ReHideSystemUiTask task = new ReHideSystemUiTask();
        uiTasks.add(task);
        uiHandler.postDelayed(task, FULLSCREEN_DELAY);
    }

    class ReHideSystemUiTask implements Runnable {
        public void run() {
            // Log.i("PhotoFrameActivity", "rehide system ui now");
            uiTasks.remove(this);
            configureFullscreenMode(true);
        }
    }

}
