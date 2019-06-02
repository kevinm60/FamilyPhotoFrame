package app.familyphotoframe.slideshow;

import java.util.LinkedList;
import java.util.Date;
import java.text.SimpleDateFormat;
import android.util.Log;
import android.app.Activity;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Handler;
import android.content.Intent;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.RequestListener;

import app.familyphotoframe.R;
import app.familyphotoframe.PhotoFrameActivity;
import app.familyphotoframe.repository.FlickrClient;
import app.familyphotoframe.model.Photo;
import app.familyphotoframe.model.CrossFadeGroup;
import app.familyphotoframe.exception.DiscoveryFailureException;
import app.familyphotoframe.exception.InsufficientPhotosException;

/**
 * This is the slideshow thread. It's a Runnable so it can be used by the timerHandler.
 */
public class Display implements Runnable {
    public static final int NUM_PHOTOS_TO_PLAN = 10;
    private static final int MIN_SLIDESHOW_SIZE = 2;
    private static final int MIN_QUEUE_SIZE = 3;
    private static final int MAX_HISTORY_SIZE = 100;
    private static final int WAIT_DURATION_MS = 100;
    private static final int LOCATION_WAIT_TRIES = 20;
    private static final int FRAME_DURATION_DAY   = 30 * 1000; // 30 secs
    private static final int FRAME_DURATION_NIGHT = 3 * 60 * 1000; // 3 mins
    private final int FADE_DURATION;

    final private Handler timerHandler = new Handler();
    final private SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy");
    final private Activity photoFrameActivity;
    final private ShowPlanner showPlanner;
    final private FlickrClient flickr;
    final private LinkedList<Photo> photoQueue = new LinkedList<>();
    final private LinkedList<Photo> photoHistory = new LinkedList<>();
    private ShareHandler shareHandler;
    private int currentPhotoIndex;
    final private TextView messageView;
    final private CrossFadeGroup groupA;
    final private CrossFadeGroup groupB;
    private boolean isCurrentA;
    private boolean slideshowReady;
    private boolean slideshowIsRunning;
    private int frameDuration = FRAME_DURATION_DAY;

    public Display(final Activity photoFrameActivity,
                   final TextView messageView,
                   final CrossFadeGroup groupA, final CrossFadeGroup groupB,
                   final ShowPlanner showPlanner, final FlickrClient flickr) {
        this.photoFrameActivity = photoFrameActivity;
        this.currentPhotoIndex = 0;
        this.messageView = messageView;
        messageView.setVisibility(View.GONE);
        this.groupA = groupA;
        groupA.getFrame().setAlpha(0f);
        this.groupB = groupB;
        groupB.getFrame().setAlpha(0f);
        isCurrentA = true;
        this.showPlanner = showPlanner;
        this.flickr = flickr;
        shareHandler = new ShareHandler(photoFrameActivity);
        this.slideshowReady = false; // true if photo history has been primed
        this.slideshowIsRunning = false; // true if there is an event in the timerHandler to advance the slideshow
        FADE_DURATION = photoFrameActivity.getResources().getInteger(android.R.integer.config_longAnimTime);
    }

    /**
     * This is called by PhotoFrameActivity to start the slideshow and by the timerHandler to
     * advance the slideshow. It advances the slideshow and sets a timer to call itself again.
     */
    @Override
    public void run() {
        // only need to prime photo history once.
        try {
            if (!slideshowReady) {
                prime();
                slideshowIsRunning = true;
                showPhoto(photoHistory.get(0), photoHistory.get(1));
            } else {
                slideshowIsRunning = true;
                forward();
            }
        } catch (DiscoveryFailureException e) {
            messageView.setText(R.string.check_network);
            messageView.setVisibility(View.VISIBLE);
            Log.i("Display", "discovery failure");
        } catch (InsufficientPhotosException e) {
            messageView.setText(R.string.insufficient_photos);
            messageView.setVisibility(View.VISIBLE);
            Log.i("Display", "insufficient photos");
        }
    }

    private synchronized void prime() throws InsufficientPhotosException, DiscoveryFailureException {
        Log.i("Display", "priming");
        photoQueue.addAll(showPlanner.getPhotosToSchedule(NUM_PHOTOS_TO_PLAN));
        if (photoQueue.size() < MIN_SLIDESHOW_SIZE) {
            throw new InsufficientPhotosException();
        }
        messageView.setVisibility(View.GONE);

        for (int ii=0; ii<MIN_SLIDESHOW_SIZE; ii++) {
            Photo photo = photoQueue.poll();
            Log.i("Display", " adding " + photo);
            flickr.lookupPlace(photo);
            photoHistory.add(photo);
        }

        for (int ii=0; ii<LOCATION_WAIT_TRIES; ii++) {
            if (photoHistory.get(0).getWoeId() == null) {
                Log.i("Display", "  no woeid");
                break;
            }
            if (photoHistory.get(0).getLocation() != null) {
                Log.i("Display", "  got location");
                break;
            }
            try {
                Log.i("Display", "  waiting for location " + photoHistory.get(0));
                Thread.sleep(WAIT_DURATION_MS);
            } catch (InterruptedException e) {
                Log.d("Display", "interrupted");
            }
        }
        slideshowReady = true;
    }

    public synchronized void forward() throws DiscoveryFailureException {
        Log.i("Display", "forward");
        if (!slideshowReady) {
            Log.i("Display", "slideshow not ready");
            return;
        }

        if (currentPhotoIndex == photoHistory.size()-MIN_SLIDESHOW_SIZE) {
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
        Log.i("Display", String.format("next two: %s, %s", nextPhoto.toString(),
                                       (followingPhoto == null ? "null" : followingPhoto.toString())));
        flickr.lookupPlace(followingPhoto);
        showPhoto(nextPhoto, followingPhoto);

        if (isQueueLow()) {
            photoQueue.addAll(showPlanner.getPhotosToSchedule(NUM_PHOTOS_TO_PLAN));
        }
    }

    public synchronized void backward() {
        Log.i("Display", "backward");
        if (!slideshowReady) {
            Log.i("Display", "slideshow not ready");
            return;
        }
        // Log.i("Display", "moving backward in photo history");
        if (currentPhotoIndex > 0) {
            --currentPhotoIndex;
            Photo nextPhoto = photoHistory.get(currentPhotoIndex);
            Photo followingPhoto = null;
            if (currentPhotoIndex > 0) {
                followingPhoto = photoHistory.get(currentPhotoIndex-1);
            }
            showPhoto(nextPhoto, followingPhoto);
        }
    }

    // only called by synchronized methods
    private void showPhoto(final Photo nextPhoto, final Photo followingPhoto) {
        Log.i("Display", String.format("showing photo: %d, queueSize: %d, historySize: %d",
                                       (currentPhotoIndex+1), photoQueue.size(), photoHistory.size()));

        nextGroup().getFrame().setAlpha(0f);

        RequestOptions options = new RequestOptions()
            .fitCenter();
        try {
            Log.i("Display", String.format("showing photo: %s", nextPhoto.getUrl()));
            Glide.with(photoFrameActivity)
                .asBitmap()
                .load(nextPhoto.getUrl())
                .apply(options)
                .into(nextGroup().getPhoto());
            nextGroup().getCaption().setText(makePhotoCaption(nextPhoto));
        } catch (IllegalArgumentException e) {
            // I'm not sure about this. I've seen the app crash saying that 'photoFrameActivity' was
            // 'destroyed.' This may happen if the device runs low on memory and recycles it. This
            // is meant to restart the activity in that case.
            Log.w("Display", "PhotoFrameActivity was destroyed. Restarting.");
            Intent photoFrameIntent = new Intent(photoFrameActivity, PhotoFrameActivity.class);
            photoFrameActivity.startActivity(photoFrameIntent);
            photoFrameActivity.finish();
        }

        crossFade(currentGroup().getFrame(), nextGroup().getFrame());
        isCurrentA = !isCurrentA;

        // prefetch the following one
        if (followingPhoto != null) {
            Glide.with(photoFrameActivity)
                .load(followingPhoto.getUrl())
                .preload();
        }

        if (slideshowIsRunning) {
            // reset timer
            Log.i("Display", "next frame in: " + frameDuration);
            timerHandler.removeCallbacks(this);
            timerHandler.postDelayed(this, frameDuration);
        }
    }

    /**
     * pause the slideshow
     */
    public synchronized void pause() {
        Log.i("Display", "pause slideshow");
        timerHandler.removeCallbacks(this);
        slideshowIsRunning = false;
    }

    /**
     * resume the slideshow
     */
    public synchronized void resume() {
        Log.i("Display", "resume slideshow");
        timerHandler.postDelayed(this, frameDuration);
        slideshowIsRunning = true;
    }

    public boolean isSlideshowRunning() {
        return slideshowIsRunning;
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

    private Spanned makePhotoCaption(final Photo photo) {
        return Html.fromHtml(String.format("%s<br/><br/>%s<br/>%s<br/>%s",
                                           photo.getTitle(),
                                           "Taken by: " + photo.getOwner().getName(),
                                           "Date taken: " + getDateString(photo),
                                           photo.getLocation()==null ? "" : "Location: " + photo.getLocation()));
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

    private String getDateString(final Photo photo) {
        Date now = new Date();
        int diffInDaysSameYear = photo.computeSameYearDaysDifference(now);
        Log.i("Display", "now: " + dateFormat.format(now) + " dateTaken: " + dateFormat.format(photo.getDateTaken())
              + " diff: " + diffInDaysSameYear);
        if (diffInDaysSameYear < -1 || diffInDaysSameYear > 1) {
            return dateFormat.format(photo.getDateTaken());
        } else {
            String dayString;
            switch (diffInDaysSameYear) {
            case -1: dayString = "Tomorrow"; break;
            case 0: dayString = "Today"; break;
            default: dayString = "Yesterday";
            }
            Log.i("Display", "dayString: " + dayString + " " + diffInDaysSameYear);
            long diffInYears = photo.computeDifferenceInYears(now);
            String yearsString;
            if (diffInYears == 0) {
                yearsString = "";
            } else if (diffInYears == 1) {
                yearsString = ", last year";
            } else {
                yearsString = String.format(", %d years ago", diffInYears);
            }
            Log.i("Display", "diffInYears: " + yearsString + " " + diffInYears);
            return String.format("<big><font color=\"yellow\">%s</font></big>%s", dayString, yearsString);
        }
    }

    public void shareCurrentPhoto() {
        // pass in the ImageView containing the photo that's currently visible
        final ImageView imageView = (ImageView) photoFrameActivity.findViewById(isCurrentA? R.id.photoA : R.id.photoB);
        shareHandler.sharePhoto(imageView);
    }
}
