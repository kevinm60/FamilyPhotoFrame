package app.familyphotoframe.slideshow;

import java.util.LinkedList;
import java.text.SimpleDateFormat;
import android.util.Log;
import android.app.Activity;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.RequestListener;

import app.familyphotoframe.model.Photo;
import app.familyphotoframe.model.CrossFadeGroup;

/**
 * this is the slideshow thread.
 */
public class Display implements Runnable {
    final private int MIN_QUEUE_SIZE = 3;
    final private int MAX_HISTORY_SIZE = 100;
    final private int NUM_PHOTOS_TO_PLAN = 10;
    final private int FRAME_DURATION_DAY   = 30 * 1000; // 30 secs
    final private int FRAME_DURATION_NIGHT = 3 * 60 * 1000; // 3 mins
    final private int FADE_DURATION;

    final private Handler timerHandler = new Handler();
    final private SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy");
    final private Activity photoFrameActivity;
    final private ShowPlanner showPlanner;
    final private LinkedList<Photo> photoQueue = new LinkedList<>();
    final private LinkedList<Photo> photoHistory = new LinkedList<>();
    private int currentPhotoIndex;
    final private CrossFadeGroup groupA;
    final private CrossFadeGroup groupB;
    private boolean isCurrentA = true;
    private int frameDuration = FRAME_DURATION_DAY;

    public Display(final Activity photoFrameActivity,
                   final CrossFadeGroup groupA, final CrossFadeGroup groupB,
                   final ShowPlanner showPlanner) {
        this.photoFrameActivity = photoFrameActivity;
        this.currentPhotoIndex = 0;
        this.groupA = groupA;
        this.groupB = groupB;
        groupB.getFrame().setAlpha(0f);
        this.showPlanner = showPlanner;
        FADE_DURATION = photoFrameActivity.getResources().getInteger(android.R.integer.config_longAnimTime);
    }

    /**
     * this routine advances the slideshow and sets a timer to call itself again to advance the
     * slideshow again.
     */
    @Override
    public void run() {
        forward();
    }

    public synchronized void prime() {
        photoQueue.addAll(showPlanner.getPhotosToSchedule(NUM_PHOTOS_TO_PLAN));
        photoHistory.add(photoQueue.poll());
        photoHistory.add(photoQueue.poll());
        showNextPhoto(photoHistory.get(0), photoHistory.get(1));
    }

    public synchronized void forward() {
        // Log.i("Display", "moving forward in photo history");
        if (currentPhotoIndex == photoHistory.size()-2) {
            // Log.i("Display", "fetching next photo from queue");
            if (isHistoryFull()) {
                photoHistory.remove();
                --currentPhotoIndex;
            }
            photoHistory.add(photoQueue.poll());
        }
        ++currentPhotoIndex;
        Photo nextPhoto = photoHistory.get(currentPhotoIndex);
        Photo followingPhoto = photoHistory.get(currentPhotoIndex+1);
        showNextPhoto(nextPhoto, followingPhoto);

        if (isQueueLow()) {
            photoQueue.addAll(showPlanner.getPhotosToSchedule(NUM_PHOTOS_TO_PLAN));
        }
    }

    public synchronized void backward() {
        // Log.i("Display", "moving backward in photo history");
        if (currentPhotoIndex > 0) {
            --currentPhotoIndex;
            Photo nextPhoto = photoHistory.get(currentPhotoIndex);
            Photo followingPhoto = null;
            if (currentPhotoIndex > 0) {
                followingPhoto = photoHistory.get(currentPhotoIndex-1);
            }
            showNextPhoto(nextPhoto, followingPhoto);
        }
    }

    private void showNextPhoto(final Photo nextPhoto, final Photo followingPhoto) {
        Log.i("Display", "showing photo #" + (currentPhotoIndex+1) + " of " + photoHistory.size() +
              "; there are " + photoQueue.size() + " photos remaining in the queue");
        Log.i("Display", "nextPhoto " + nextPhoto.getUrl() + ", followingPhoto " +
              (followingPhoto == null ? "null" : followingPhoto.getUrl()));

        nextGroup().getFrame().setAlpha(0f);

        RequestOptions options = new RequestOptions()
            .fitCenter();
        Glide.with(photoFrameActivity)
            .load(nextPhoto.getUrl())
            .apply(options)
            .into(nextGroup().getPhoto());
        nextGroup().getCaption().setText(makePhotoCaption(nextPhoto));

        crossFade(currentGroup().getFrame(), nextGroup().getFrame());
        isCurrentA = !isCurrentA;

        // prefetch the following one
        if (followingPhoto != null) {
            Glide.with(photoFrameActivity)
                .load(followingPhoto.getUrl())
                .preload();
        }

        // reset timer
        // Log.i("Display", "next frame in: " + frameDuration);
        timerHandler.removeCallbacks(this);
        timerHandler.postDelayed(this, frameDuration);
    }

    /**
     * pause the slideshow
     */
    public void pause() {
        timerHandler.removeCallbacks(this);
    }

    /**
     * resume the slideshow
     */
    public void resume() {
        timerHandler.postDelayed(this, frameDuration);
    }

    public void itsNighttime() {
        frameDuration = FRAME_DURATION_NIGHT;
    }

    public void itsDaytime() {
        frameDuration = FRAME_DURATION_DAY;
    }

    private boolean isQueueLow() {
        return photoQueue.size() < MIN_QUEUE_SIZE;
    }

    private boolean isHistoryFull() {
        return photoHistory.size() >= MAX_HISTORY_SIZE;
    }

    private CrossFadeGroup currentGroup() {
        return isCurrentA? groupA : groupB;
    }

    private CrossFadeGroup nextGroup() {
        return isCurrentA? groupB : groupA;
    }

    private String makePhotoCaption(final Photo photo) {
        return String.format("%s\n\n%s\n%s\n", photo.getTitle(),
                             photo.getOwner().getName(),
                             dateFormat.format(photo.getDateTaken()));
    }

    private void crossFade(final ViewGroup currentPhotoView, final ViewGroup nextPhotoView) {

        nextPhotoView.animate()
            .alpha(1f)
            .setDuration(FADE_DURATION)
            .setListener(null);

        currentPhotoView.animate()
            .alpha(0f)
            .setDuration(FADE_DURATION)
            .setListener(null);

    }
}
