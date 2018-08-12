package app.familyphotoframe.slideshow;

import java.util.LinkedList;
import android.util.Log;
import android.app.Activity;
import android.widget.ImageView;
import android.os.Handler;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;

import app.familyphotoframe.model.Photo;

/**
 * this is the slideshow thread.
 */
public class Display implements Runnable {
    private int MIN_QUEUE_SIZE = 3;
    private int FRAME_DURATION = 10000; // 10 secs
    private int CROSS_FADE_DURATION = 1000;

    private Activity photoFrameActivity;
    private ImageView photoView;
    private ShowPlanner showPlanner;
    private LinkedList<Photo> photoQueue = new LinkedList<>();

    public Display(final Activity photoFrameActivity, final ImageView photoView, final ShowPlanner showPlanner) {
        this.photoFrameActivity = photoFrameActivity;
        this.photoView = photoView;
        this.showPlanner = showPlanner;
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
            .asBitmap()
            .load(nextPhoto.getUrl())
            .apply(options)
            .transition(BitmapTransitionOptions.withCrossFade(CROSS_FADE_DURATION))
            .into(photoView);

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
}
