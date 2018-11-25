package app.familyphotoframe.slideshow;

import java.lang.Math;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Date;
import java.util.Set;
import android.util.Log;

import app.familyphotoframe.repository.PhotoCollection;
import app.familyphotoframe.model.Photo;
import app.familyphotoframe.model.Relationship;

/**
 * chooses the photos for the next time period
 */
public class ShowPlanner {

    private static final int numRecencyThresholds = 2;
    static final int recencyThresholds[] = {60, 365}; // days

    private static final int numRecencyIntervals = numRecencyThresholds + 1;
    static final int nominalRelationshipLikelihood[] = {5,3,2}; // indexed by relationship
    static final int nominalRecencyLikelihood[] = {5,3,2}; // indexed by recency interval

    private class IndexElement {
        public LinkedList<Photo> photos;
        public int likelihood;
    }

    /** collection of photos */
    private PhotoCollection photoCollection;

    /** array of lists of photos indexed by relationship and recency */
    private IndexElement[][] photoIndex;

    /** time of last indexing request */
    private Date timeOfLastIndex;


    public ShowPlanner(final PhotoCollection photoCollection) {
        this.photoCollection = photoCollection;
        photoIndex = null;  // created in indexPhotos

        // Discovery has not occurred at the time of construction, so using the current
        // time will trigger indexing during the first call to getPhotosToSchedule()
        timeOfLastIndex = new Date();
    }

    /**
     * populate photoIndex according to photo attributes
     */
    public void indexPhotos(Set<Photo> allPhotos) {
        Log.i("ShowPlanner", "indexing photos");

        Date now = new Date();
        timeOfLastIndex = now;

        // Determine recency cutoff dates (i.e. photos more recent than the cutoff are within
        // the recency interval with the same index)
        Date recencyCutoffDates[] = new Date[recencyThresholds.length];
        for (int i = 0; i < recencyCutoffDates.length; ++i) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(now);
            calendar.add(Calendar.DATE, -recencyThresholds[i]);
            recencyCutoffDates[i] = new Date(calendar.getTime().getTime());
        }

        // Create photo index indexed by relationship and recency
        photoIndex = new IndexElement[Relationship.length][numRecencyIntervals];
        for (int iRelationship = 0; iRelationship < photoIndex.length; ++iRelationship) {
            for (int iRecency = 0; iRecency < photoIndex[iRelationship].length; ++iRecency) {
                IndexElement group = new IndexElement();
                group.photos = new LinkedList<Photo>();
                group.likelihood =
                        nominalRelationshipLikelihood[iRelationship] *
                        nominalRecencyLikelihood[iRecency];
                photoIndex[iRelationship][iRecency] = group;
            }
        }

        // Organize photos into groups
        for (Photo photo : allPhotos) {
            int iRelationship = photo.getOwner().getRelationship().getValue();
            int iRecency = 0;
            for (; iRecency < recencyCutoffDates.length; ++iRecency) {
                if (photo.getDateTaken().after(recencyCutoffDates[iRecency])) {
                    break;
                }
            }
            photoIndex[iRelationship][iRecency].photos.add(photo);
        }
    }


    /**
     * return a randomized list of photos based on distribution preferences
     */
    public List<Photo> getPhotosToSchedule(final int count) {
        Log.i("ShowPlanner", "filling queue");

        LinkedList<Photo> selectedPhotos = new LinkedList<Photo>();

        // Only index photos when photoCollection is updated
        if (photoCollection.getTimeOfLastDiscovery().after(timeOfLastIndex)) {
            indexPhotos(photoCollection.getPhotos());
        }

        // Select photos from each group. The number of photos selected is equal to the
        // likelihood for the group so that the list of selected photos list will have
        // the desired distribution
        for (int iRelationship = 0; iRelationship < photoIndex.length; ++iRelationship) {
            for (int iRecency = 0; iRecency < photoIndex[iRelationship].length; ++iRecency) {
                IndexElement group = photoIndex[iRelationship][iRecency];
                // If there are more than enough photos in a group, shuffle the photos
                // and select from the beginning. Otherwise select them all.
                if (group.photos.size() > group.likelihood) {
                    Collections.shuffle(group.photos);
                    selectedPhotos.addAll(group.photos.subList(0,group.likelihood));
                } else {
                    selectedPhotos.addAll(group.photos);
                }
            }
        }

        // Shuffle the list of selected photos, truncate to the requested size and return
        Collections.shuffle(selectedPhotos);
        if (count < selectedPhotos.size()) {
            selectedPhotos.subList(count,selectedPhotos.size()).clear();
        }

        return selectedPhotos;
    }
}
