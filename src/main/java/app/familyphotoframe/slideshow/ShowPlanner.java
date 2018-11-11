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
    private static final int recencyThresholds[] = {60, 365}; // days

    private static final int numRecencyIntervals = numRecencyThresholds + 1;
    private static final int nominalRelationshipLikelihood[] = {5,3,2}; // indexed by relationship
    private static final int nominalRecencyLikelihood[] = {5,3,2}; // indexed by recency interval

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

    private PhotoCollection photoCollection;

    public ShowPlanner(final PhotoCollection photoCollection) {
        this.photoCollection = photoCollection;
    }

    public List<Photo> getPhotosToSchedule(final int count) {
        Log.i("ShowPlanner", "filling queue");

	LinkedList<Photo> selectedPhotos = new LinkedList<Photo>();

	// Determine recency cutoff dates (i.e. photos more recent than the cutoff are within
	// the recency interval with the same index)
	Date now = new Date();
	Date recencyCutoffDates[] = new Date[recencyThresholds.length];
	for (int i = 0; i < recencyCutoffDates.length; ++i) {
	    Calendar calendar = Calendar.getInstance();
	    calendar.setTime(now);
	    calendar.add(Calendar.DATE, -recencyThresholds[i]);
	    recencyCutoffDates[i] = new Date(calendar.getTime().getTime());
	}

	// Create array of groups indexed by relationship and recency
	GroupInfo[][] allGroups = new GroupInfo[Relationship.length][numRecencyIntervals];
	for (int relationshipIndex = 0; relationshipIndex < allGroups.length; ++relationshipIndex) {
	    for (int recencyIndex = 0; recencyIndex < allGroups[relationshipIndex].length; ++recencyIndex) {
		allGroups[relationshipIndex][recencyIndex] = new GroupInfo();
	    }
	}

	// Organize photos into groups
	for (Photo photo : photoCollection.getPhotos()) {
	    int relationshipIndex = photo.getOwner().getRelationship().getValue();
	    int recencyIndex = 0;
	    for (; recencyIndex < recencyCutoffDates.length; ++recencyIndex) {
		if (photo.getDateTaken().after(recencyCutoffDates[recencyIndex])) {
		    break;
		}
	    }
	    allGroups[relationshipIndex][recencyIndex].photos.add(photo);
	}

	// Determine initial list of groups under consideration
	LinkedList<GroupInfo> groupsUnderConsideration = new LinkedList<GroupInfo>();
	LinkedList<GroupInfo> exhaustedGroups = new LinkedList<GroupInfo>();
	int totalLikelihood = 0;
	for (int relationshipIndex = 0; relationshipIndex < allGroups.length; ++relationshipIndex) {
	    for (int recencyIndex = 0; recencyIndex < allGroups[relationshipIndex].length; ++recencyIndex) {
		GroupInfo group = allGroups[relationshipIndex][recencyIndex];
		if (group.photos.size() > 0) {
		    group.numSelectedPhotos = 0;
		    group.likelihood =
			nominalRelationshipLikelihood[relationshipIndex] *
			nominalRecencyLikelihood[recencyIndex];
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

