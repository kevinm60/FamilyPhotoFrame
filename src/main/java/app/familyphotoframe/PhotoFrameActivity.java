package app.familyphotoframe;

import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import android.content.res.Resources;

import android.app.Activity;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
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
    private FlickrClient flickr;
    private PhotoCollection photoCollection;
    private ShowPlanner showPlanner;
    private Display display;
    private Handler uiHandler;
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

        hideSystemUI();

        gestureDetector = new GestureDetectorCompat(this, new GestureListener());

        flickr = (FlickrClient) OAuthBaseClient.getInstance(FlickrClient.class, getApplicationContext());
        photoCollection = new PhotoCollection(this, flickr);

        showPlanner = new ShowPlanner(this, photoCollection);

        CrossFadeGroup groupA = new CrossFadeGroup((ViewGroup)findViewById(R.id.frameA),
                                                   (ImageView)findViewById(R.id.photoA),
                                                   (TextView)findViewById(R.id.captionA));
        CrossFadeGroup groupB = new CrossFadeGroup((ViewGroup)findViewById(R.id.frameB),
                                                   (ImageView)findViewById(R.id.photoB),
                                                   (TextView)findViewById(R.id.captionB));
        display = new Display(this, groupA, groupB, showPlanner);
        sleepCycle = new SleepCycle(getWindow(), uiHandler, display, photoCollection);
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
                                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                                        // Show the nav and status bars only if user swipes from the
                                        // top of the screen. 
                                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    protected void onStop() {
        super.onStop();
        display.pause();
        Log.i("PhotoFrameActivity", "stopped");
    }

    protected void onStart() {
        super.onStart();
        sleepCycle.init();
        Log.i("PhotoFrameActivity", "started");
    }

    protected void onRestart() {
        super.onRestart();
        startShow();
        Log.i("PhotoFrameActivity", "restarted");
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
        if (this.gestureDetector.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }

}
