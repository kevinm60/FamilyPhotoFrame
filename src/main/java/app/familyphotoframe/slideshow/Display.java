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
    final private int FRAME_DURATION_DAY   = 30 * 1000; // 30 secs
    final private int FRAME_DURATION_NIGHT = 3 * 60 * 1000; // 3 mins
    final private int FADE_DURATION;

    final private Handler timerHandler = new Handler();
    final private SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy");
    final private Activity photoFrameActivity;
    final private ShowPlanner showPlanner;
    final private LinkedList<Photo> photoQueue = new LinkedList<>();
    final private CrossFadeGroup groupA;
    final private CrossFadeGroup groupB;
    private boolean isCurrentA = true;
    private int frameDuration = FRAME_DURATION_DAY;

    public Display(final Activity photoFrameActivity,
                   final CrossFadeGroup groupA, final CrossFadeGroup groupB,
                   final ShowPlanner showPlanner) {
        this.photoFrameActivity = photoFrameActivity;
        this.groupA = groupA;
        this.groupB = groupB;
        groupB.getFrame().setVisibility(View.GONE);
        this.showPlanner = showPlanner;
        FADE_DURATION = photoFrameActivity.getResources().getInteger(android.R.integer.config_longAnimTime);
    }

    /**
     * this routine advances the slideshow and sets a timer to call itself again to advance the
     * slideshow again.
     */
    @Override
    public void run() {
        if (isQueueLow()) {
            photoQueue.addAll(showPlanner.getPhotosToSchedule(10));
        }

        Log.i("Display", "showing " + photoQueue.size());

        // show the next photo
        final Photo nextPhoto = photoQueue.poll();
        Log.i("Display", "nextphoto " + nextPhoto.getUrl());
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
        final Photo followingPhoto = photoQueue.peek();
        Glide.with(photoFrameActivity)
            .load(followingPhoto.getUrl())
            .preload();

        // Log.i("Display", "next frame in: " + frameDuration);
        timerHandler.postDelayed(this, frameDuration);
    }

    /**
     * stop the slideshow
     */
    public void pause() {
        timerHandler.removeCallbacks(this);
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

    private CrossFadeGroup currentGroup() {
        return isCurrentA? groupA : groupB;
    }

    private CrossFadeGroup nextGroup() {
        return isCurrentA? groupB : groupA;
    }

    private String makePhotoCaption(final Photo photo) {
        return String.format("%s\n%s\n\n%s", photo.getOwner().getName(),
                             dateFormat.format(photo.getDateTaken()),
                             photo.getTitle());
    }

    private void crossFade(final ViewGroup currentPhotoView, final ViewGroup nextPhotoView) {
        nextPhotoView.setAlpha(0f);
        nextPhotoView.setVisibility(View.VISIBLE);

        nextPhotoView.animate()
            .alpha(1f)
            .setDuration(FADE_DURATION)
            .setListener(null);

        currentPhotoView.animate()
            .alpha(0f)
            .setDuration(FADE_DURATION)
            .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        currentPhotoView.setVisibility(View.GONE);
                    }
                });
    }
}
