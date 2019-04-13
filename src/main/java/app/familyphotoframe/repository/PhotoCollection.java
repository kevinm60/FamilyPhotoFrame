package app.familyphotoframe.repository;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Date;
import android.util.Log;
import android.widget.Toast;

import app.familyphotoframe.PhotoFrameActivity;
import app.familyphotoframe.slideshow.Display;
import app.familyphotoframe.model.Contact;
import app.familyphotoframe.model.Photo;
import app.familyphotoframe.exception.DiscoveryFailureException;
import app.familyphotoframe.R;

/**
 * holds all of the photos of the logged in user as well as their contacts shared photos
 */
public class PhotoCollection {
    /** start slideshow after getting photos from a few contacts */
    private static final int MIN_CONTACTS_TO_START = 3;

    /** activity with the slideshow */
    private PhotoFrameActivity photoFrameActivity;

    /** provides access to flickr data */
    private FlickrClient flickr;

    /** all photos we know about */
    private Set<Photo> photos;

    /** all flickr relations */
    private Set<Contact> contacts;

    /** count of async contact requests in progress */
    private int contactRequestsInProgress;

    /** completion time of last discovery request */
    private Date timeOfLastDiscovery;

    /** indicates discovery is in progress */
    private boolean discoveryInProgress;

    /** if true, we couldn't contact flickr */
    private boolean discoveryFailed;

    public PhotoCollection(final PhotoFrameActivity photoFrameActivity, final FlickrClient flickr) {
        this.photoFrameActivity = photoFrameActivity;
        this.flickr = flickr;
        contacts = new HashSet<>();
        photos = new HashSet<>();
        contactRequestsInProgress = 0;
        timeOfLastDiscovery = null;
        discoveryInProgress = false;
    }

    public synchronized void startDiscovery() {
        if (discoveryInProgress) {
            Log.i("PhotoCollection", "discovery already in progress, not restarting");
            return;
        }
        Log.i("PhotoCollection", "starting discovery");
        discoveryInProgress = true;
        discoveryFailed = false;
        flickr.lookupProfile(this);
        contacts.clear();
        photos.clear();
    }

    public Contact getContact(final String userId) {
        for (Contact contact : contacts) {
            if (contact.getUserId().equals(userId)) {
                return contact;
            }
        }
        throw new NoSuchElementException("userId not found: " + userId + " " + contacts);
    }

    public void addProfileAndContinueDiscovery(Contact newContact) {
        contacts.add(newContact);
        flickr.lookupContacts(this);
        Log.i("PhotoCollection", "added profile: " + newContact);
    }

    public synchronized void addContactsAndContinueDiscovery(final Set<Contact> newContacts) {
        contacts.addAll(newContacts);
        Log.i("PhotoCollection", "added contacts: " + newContacts);
        for (Contact contact : contacts) {
            Log.d("PhotoCollection", "starting contactRequest, num in progress: " + contactRequestsInProgress);
            flickr.lookupPhotos(this, contact);
            contactRequestsInProgress++;
        }
    }

    public synchronized void markContactRequestComplete() {
        contactRequestsInProgress--;
        Log.d("PhotoCollection", "completed contactRequest, num in progress: " + contactRequestsInProgress);

        // discovery complete or good enough, start slideshow
        int numContactsComplete = contacts.size()-contactRequestsInProgress;
        if (contactRequestsInProgress == 0 || discoveryInProgress && eagerStartAllowed()) {
            if (contactRequestsInProgress == 0) {
                Log.i("PhotoCollection", "discovery complete. photo count: " + photos.size());
            } else {
                Log.i("PhotoCollection", "discovery sufficient. numContactsComplete: " + numContactsComplete
                      + " photo count: " + photos.size());
            }
            timeOfLastDiscovery = new Date();
            discoveryInProgress = false;

            Toast.makeText(this.photoFrameActivity.getApplicationContext(),
                           R.string.sync_to_db,
                           Toast.LENGTH_SHORT).show();

            photoFrameActivity.startShow();
        }
    }

    /**
     * we can start even if not all contacts have responded yet if we have an acceptable mix of
     * photos to fill the queue for the first time.
     */
    private boolean eagerStartAllowed() {
        Set<Contact> contactsWithPhotos = new HashSet<>();
        for (Photo photo : photos) {
            contactsWithPhotos.add(photo.getOwner());
        }
        return contactsWithPhotos.size()>=MIN_CONTACTS_TO_START && photos.size()>=Display.NUM_PHOTOS_TO_PLAN;
    }

    public synchronized void reportDiscoveryFailure() {
        discoveryFailed = true;
        discoveryInProgress = false;
        timeOfLastDiscovery = new Date();
        photoFrameActivity.startShow();
    }

    public synchronized boolean hasDiscoveryFailed() {
        return discoveryFailed;
    }

    public synchronized void addPhoto(final Photo photo) {
        photos.add(photo);
    }

    public Set<Photo> getPhotos() throws DiscoveryFailureException {
        if (hasDiscoveryFailed()) {
            throw new DiscoveryFailureException();
        }
        return photos;
    }

    public Date getTimeOfLastDiscovery() {
        return timeOfLastDiscovery;
    }

    public void dumpToLog() {
        Map<Contact,Integer> counts = new HashMap<>();
        for (Photo photo : photos) {
            Contact owner = photo.getOwner();
            if (counts.containsKey(owner)) {
                counts.put(owner, counts.get(owner)+1);
            } else {
                counts.put(owner, 1);
            }
        }
        for (Map.Entry entry : counts.entrySet()) {
            Log.i("PhotoCollection", entry.getKey() + " " + entry.getValue());
        }
    }
}
