package app.familyphotoframe.slideshow;

import java.lang.Math;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Date;
import java.util.Set;
import android.util.Log;
import android.widget.Toast;

import app.familyphotoframe.PhotoFrameActivity;
import app.familyphotoframe.repository.PhotoCollection;
import app.familyphotoframe.model.Photo;
import app.familyphotoframe.model.Relationship;
import app.familyphotoframe.R;

/**
 * chooses the photos for the next time period
 */
public class ShowPlanner {

    private static final int numRecencyThresholds = 2;
    static final int recencyThresholds[] = {60, 365}; // days

    private static final int numRecencyIntervals = numRecencyThresholds + 1;
    private static final int nominalRelationshipLikelihood[] = {5,3,2}; // indexed by relationship
    private static final int nominalRecencyLikelihood[] = {5,3,2}; // indexed by recency interval

    private class IndexElement {
        public LinkedList<Photo> photos;
    }

    private class GroupInfo {
        public LinkedList<Photo> photos;
        public int numSelectedPhotos;
        public int likelihood;
        public GroupInfo() {
            photos = new LinkedList<Photo>();
            numSelectedPhotos = 0;
            likelihood = 0;
        }
    }

    /** activity with the slideshow */
    private PhotoFrameActivity photoFrameActivity;

    /** collection of photos */
    private PhotoCollection photoCollection;

    /** array of lists of photos indexed by relationship and recency */
    private IndexElement[][] photoIndex;

    /** time of last indexing request */
    private Date timeOfLastIndex;


    public ShowPlanner(final PhotoFrameActivity photoFrameActivity,
                       final PhotoCollection photoCollection) {
        this.photoFrameActivity = photoFrameActivity;
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
                photoIndex[iRelationship][iRecency] = new IndexElement();
                photoIndex[iRelationship][iRecency].photos = new LinkedList<Photo>();
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

        // Toast
        Toast toast = Toast.makeText(photoFrameActivity.getApplicationContext(),
                       R.string.indexed_photos,
                       Toast.LENGTH_SHORT);
        if (toast != null) {
            toast.show();
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

        // Determine initial list of groups under consideration
        LinkedList<GroupInfo> groupsUnderConsideration = new LinkedList<GroupInfo>();
        LinkedList<GroupInfo> exhaustedGroups = new LinkedList<GroupInfo>();
        int totalLikelihood = 0;
        for (int iRelationship = 0; iRelationship < photoIndex.length; ++iRelationship) {
            for (int iRecency = 0; iRecency < photoIndex[iRelationship].length; ++iRecency) {
                LinkedList<Photo> groupPhotos = photoIndex[iRelationship][iRecency].photos;
                if (groupPhotos.size() > 0) {
                    GroupInfo group = new GroupInfo();
                    group.photos = groupPhotos;
                    group.numSelectedPhotos = 0;
                    group.likelihood =
                        nominalRelationshipLikelihood[iRelationship] *
                        nominalRecencyLikelihood[iRecency];
                    groupsUnderConsideration.add(group);
                    totalLikelihood += group.likelihood;
                }
            }
        }

        // Select photos from groups based on likelihood
        int numPhotosToSelect = count;
        while (numPhotosToSelect > 0 && !groupsUnderConsideration.isEmpty()) {
            int numSelectedPhotos = 0;
            for (GroupInfo group : groupsUnderConsideration) {
                double groupPct = group.likelihood / (double)totalLikelihood;
                int numAdditionalSelectedPhotos = (int)Math.ceil(groupPct * numPhotosToSelect);
                int numAvailablePhotos = group.photos.size() - group.numSelectedPhotos;
                if (numAdditionalSelectedPhotos >= numAvailablePhotos) {
                    numAdditionalSelectedPhotos = numAvailablePhotos;
                    exhaustedGroups.add(group);
                } else {
                    group.numSelectedPhotos += numAdditionalSelectedPhotos;
                }
                numSelectedPhotos += numAdditionalSelectedPhotos;
            }

            // Copy all photos from exhausted groups and remove from groups under consideration
            for (GroupInfo group : exhaustedGroups) {
                selectedPhotos.addAll(group.photos);
                totalLikelihood -= group.likelihood;
            }
            groupsUnderConsideration.removeAll(exhaustedGroups);
            exhaustedGroups.clear();
            numPhotosToSelect -= numSelectedPhotos;
        }

        // The remaining groups were not exhausted. For each group, shuffle the contents and select
        // the desired number of photos
        for (GroupInfo group : groupsUnderConsideration) {
            Collections.shuffle(group.photos);
            selectedPhotos.addAll(group.photos.subList(0,group.numSelectedPhotos));
        }

        // Shuffle the list of selected photos, truncate to the requested size and return
        Collections.shuffle(selectedPhotos);
        if (count < selectedPhotos.size()) {
            selectedPhotos.subList(count,selectedPhotos.size()).clear();
        }

        return selectedPhotos;
    }
}
