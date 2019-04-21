package app.familyphotoframe.slideshow;

import java.lang.Math;
import java.util.Date;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import android.util.Log;

import app.familyphotoframe.repository.PhotoCollection;
import app.familyphotoframe.model.Photo;
import app.familyphotoframe.model.Relationship;
import app.familyphotoframe.exception.DiscoveryFailureException;

/**
 * chooses the photos for the next time period
 */
public class ShowPlanner {

    static final int nominalRelationshipLikelihood[] = {5, 3, 1, 0}; // indexed by relationship {self, family, friend, stranger}
    static final int numRelationshipIntervals = nominalRelationshipLikelihood.length;

    static final int recencyThresholds[] = {60, 365};                // days
    static final int nominalRecencyLikelihood[] = {5, 3, 1};         // indexed by recency interval {recent, moderately recent, old}
    static final int numRecencyIntervals = nominalRecencyLikelihood.length;

    static final int seasonalityThresholds[] = {1, 30, 90};          // days
    static final int nominalSeasonalLikelihood[] = {5, 3, 1, 0};     // indexed by calendar distance {same day, in season, out of season, opposite season}
    static final int numSeasonalityIntervals = nominalSeasonalLikelihood.length;

    private class IndexElement {
        public List<Photo> photos;
        public int likelihood;
    }

    /** collection of photos */
    private PhotoCollection photoCollection;

    /** array of lists of photos indexed by relationship and recency */
    private IndexElement[][][] photoIndex;

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

        // photoCollection.dumpToLog();

        Date now = new Date();
        timeOfLastIndex = now;

        // Create photo index indexed by relationship, recency, seasonality
        photoIndex = new IndexElement[numRelationshipIntervals][numRecencyIntervals][numSeasonalityIntervals];
        for (int iRelationship = 0; iRelationship < photoIndex.length; ++iRelationship) {
            for (int iRecency = 0; iRecency < photoIndex[iRelationship].length; ++iRecency) {
                for (int iSeasonality = 0; iSeasonality < photoIndex[iRelationship][iRecency].length; ++iSeasonality) {
                    IndexElement group = new IndexElement();
                    group.photos = new LinkedList<Photo>();
                    group.likelihood =
                        nominalRelationshipLikelihood[iRelationship] *
                        nominalRecencyLikelihood[iRecency] *
                        nominalSeasonalLikelihood[iSeasonality];
                    photoIndex[iRelationship][iRecency][iSeasonality] = group;
                }
            }
        }

        // Organize photos into groups
        for (Photo photo : allPhotos) {
            int iRelationship = determineRelationship(photo);
            int iRecency = determineRecency(now, photo);
            int iSeasonality = determineSeasonality(now, photo);
            photoIndex[iRelationship][iRecency][iSeasonality].photos.add(photo);
        }
    }

    int determineRelationship(final Photo photo) {
        return photo.getOwner().getRelationship().getValue();
    }

    int determineRecency(final Date now, final Photo photo) {
        long diffInMillis = Math.abs(now.getTime() - photo.getDateTaken().getTime());
        long diffInDays = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
        int iRecency = 0;
        for (; iRecency < recencyThresholds.length; ++iRecency) {
            if (diffInDays < recencyThresholds[iRecency]) {
                break;
            }
        }
        return iRecency;
    }

    int determineSeasonality(final Date now, final Photo photo) {
        int currentYear = calFromDate(now).get(Calendar.YEAR);
        Calendar photoCalSameYear = calFromDate(photo.getDateTaken());
        photoCalSameYear.set(Calendar.YEAR, currentYear);
        long diffInMillisSameYear = Math.abs(now.getTime() - photoCalSameYear.getTimeInMillis());
        long diffInDaysSameYear = TimeUnit.DAYS.convert(diffInMillisSameYear, TimeUnit.MILLISECONDS);
        int iSeasonality = 0;
        for (; iSeasonality < seasonalityThresholds.length; ++iSeasonality) {
            if (diffInDaysSameYear%365 < seasonalityThresholds[iSeasonality]) {
                break;
            }
        }
        return iSeasonality;
    }

    Calendar calFromDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date.getTime());
        return calendar;
    }

    /**
     * return a randomized list of photos based on distribution preferences
     */
    public List<Photo> getPhotosToSchedule(final int count) throws DiscoveryFailureException {
        Log.i("ShowPlanner", "filling queue");

        List<Photo> selectedPhotos = new LinkedList<Photo>();

        // Only index photos when photoCollection is updated
        if (photoCollection.getTimeOfLastDiscovery().after(timeOfLastIndex)) {
            indexPhotos(photoCollection.getPhotos());
        }

        // Select photos from each group. The number of photos selected is equal to the
        // likelihood for the group so that the list of selected photos list will have
        // the desired distribution
        for (int iRelationship = 0; iRelationship < photoIndex.length; ++iRelationship) {
            for (int iRecency = 0; iRecency < photoIndex[iRelationship].length; ++iRecency) {
                for (int iSeasonality = 0; iSeasonality < photoIndex[iRelationship][iRecency].length; ++iSeasonality) {
                    IndexElement group = photoIndex[iRelationship][iRecency][iSeasonality];
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
        }

        // Shuffle the list of selected photos, truncate to the requested size and return
        Collections.shuffle(selectedPhotos);
        if (count < selectedPhotos.size()) {
            selectedPhotos.subList(count,selectedPhotos.size()).clear();
        }

        dumpToLog(selectedPhotos);

        return selectedPhotos;
    }

    private void dumpToLog(final List<Photo> selectedPhotos) {
        for (Photo photo : selectedPhotos) {
            Log.i("ShowPlanner", "selected: " + photo.toString());
        }
    }
}
