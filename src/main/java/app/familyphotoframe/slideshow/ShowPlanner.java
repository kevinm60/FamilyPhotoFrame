package app.familyphotoframe.slideshow;

import java.util.List;
import java.util.ArrayList;
import android.util.Log;

import app.familyphotoframe.repository.PhotoCollection;
import app.familyphotoframe.model.Photo;

/**
 * chooses the photos for the next time period
 */
public class ShowPlanner {

    private PhotoCollection photoCollection;

    public ShowPlanner(final PhotoCollection photoCollection) {
        this.photoCollection = photoCollection;
    }

    // TODO return count photos, choose and order them deliberately
    public List<Photo> getPhotosToSchedule(final int count) {
        Log.i("ShowPlanner", "filling queue");
        return new ArrayList<Photo>(photoCollection.getPhotos());
    }
}
