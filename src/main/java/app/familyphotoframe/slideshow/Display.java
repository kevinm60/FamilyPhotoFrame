package app.familyphotoframe.slideshow;

import java.util.LinkedList;
import android.util.Log;
import android.app.Activity;
import android.widget.ImageView;
import android.os.Handler;
import android.view.View;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.RequestListener;

import app.familyphotoframe.model.Photo;

/**
 * this is the slideshow thread.
 */
public class Display implements Runnable {
    final private int MIN_QUEUE_SIZE = 3;
    final private int FRAME_DURATION = 10000; // 10 secs
    final private int FADE_DURATION;

    final private Activity photoFrameActivity;
    final private ShowPlanner showPlanner;
    final private LinkedList<Photo> photoQueue = new LinkedList<>();
    final private ImageView photoViewA;
    final private ImageView photoViewB;
    private ImageView currentPhotoView;
    private ImageView nextPhotoView;

    public Display(final Activity photoFrameActivity, final ImageView photoViewA, final ImageView photoViewB,
                   final ShowPlanner showPlanner) {
        this.photoFrameActivity = photoFrameActivity;
        this.photoViewA = photoViewA;
        this.photoViewB = photoViewB;
        photoViewB.setVisibility(View.GONE);
        this.currentPhotoView = photoViewA;
        this.nextPhotoView = photoViewB;
        this.showPlanner = showPlanner;
        FADE_DURATION = photoFrameActivity.getResources().getInteger(android.R.integer.config_longAnimTime);
    }

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
            .into(nextPhotoView);

        crossFade(currentPhotoView, nextPhotoView);
        swapViews();

        // prefetch the following one
        final Photo followingPhoto = photoQueue.peek();
        Glide.with(photoFrameActivity)
            .load(followingPhoto.getUrl())
            .preload();

        Handler timerHandler = new Handler();
        timerHandler.postDelayed(this, FRAME_DURATION);
    }

    private boolean isQueueLow() {
        return photoQueue.size() < MIN_QUEUE_SIZE;
    }

    private void swapViews() {
        if (currentPhotoView == photoViewA) {
            currentPhotoView = photoViewB;
            nextPhotoView    = photoViewA;
        } else {
            currentPhotoView = photoViewA;
            nextPhotoView    = photoViewB;
        }
    }

    private void crossFade(final ImageView currentPhotoView, final ImageView nextPhotoView) {
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
