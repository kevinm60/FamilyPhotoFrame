package app.familyphotoframe.repository;

import java.util.Set;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Date;
import android.util.Log;

import app.familyphotoframe.PhotoFrameActivity;
import app.familyphotoframe.model.Contact;
import app.familyphotoframe.model.Photo;

/**
 * holds all of the photos of the logged in user as well as their contacts shared photos
 */
public class PhotoCollection {

    /** activity with the slideshow */
    private PhotoFrameActivity photoFrameActivity;

    /** provides access to flickr data */
    private FlickrClient flickr;

    /** all photos we know about */
    private Set<Photo> photos;

    /** all flickr relations */
    private Set<Contact> contacts;

    /** count of async contact requests in progress */
    private int contactRequestsInProgress = 0;

    /** completion time of last discovery request */
    private Date timeOfLastDiscovery;

    public PhotoCollection(final PhotoFrameActivity photoFrameActivity, final FlickrClient flickr) {
        this.photoFrameActivity = photoFrameActivity;
        this.flickr = flickr;
        contacts = new HashSet<>();
        photos = new HashSet<>();
        timeOfLastDiscovery = null;
    }

    public void startDiscovery() {
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

        // discovery complete, start slideshow timer
        if (contactRequestsInProgress == 0) {
            Log.i("PhotoCollection", "photo count: " + photos.size());
            timeOfLastDiscovery = new Date();
            photoFrameActivity.startShow();
        }
    }

    public synchronized void addPhoto(final Photo photo) {
        photos.add(photo);
    }

    public Set<Photo> getPhotos() {
        return photos;
    }

    public Date getTimeOfLastDiscovery() {
        return timeOfLastDiscovery;
    }
}
