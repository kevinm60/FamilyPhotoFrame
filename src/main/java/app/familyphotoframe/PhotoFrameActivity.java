package app.familyphotoframe;

import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.io.File;

import android.content.res.Resources;

import android.app.Activity;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.content.Intent;
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
    private Handler hideUiHandler;
    private Set<ReHideSystemUiTask> hideUiTasks;

    final private long FULLSCREEN_DELAY = 3000L;

    /**
     * instantiate photoCollection, showPlanner, display, then start discovery.
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("PhotoFrameActivity", "created");
        setContentView(R.layout.activity_photo_frame);

        hideUiHandler = new Handler();
        hideUiTasks = new HashSet<>();

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
        for (ReHideSystemUiTask task : hideUiTasks) {
            hideUiHandler.removeCallbacks(task);
        }
        ReHideSystemUiTask task = new ReHideSystemUiTask();
        hideUiTasks.add(task);
        hideUiHandler.postDelayed(task, FULLSCREEN_DELAY);
        return true;
    }

    class ReHideSystemUiTask implements Runnable {
        public void run() {
            // Log.i("PhotoFrameActivity", "rehide system ui now");
            hideUiTasks.remove(this);
            hideSystemUI();
        }
    }
}
