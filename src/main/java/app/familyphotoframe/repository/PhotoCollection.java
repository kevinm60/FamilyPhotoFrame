package app.familyphotoframe.repository;

import java.util.Set;
import java.util.HashSet;
import java.util.NoSuchElementException;
import android.util.Log;

import app.familyphotoframe.PhotoFrameActivity;
import app.familyphotoframe.model.Contact;
import app.familyphotoframe.model.Photo;

/**
 * holds all of the photos of the logged in user as well as their contacts shared photos
 */
public class PhotoCollection {

    private PhotoFrameActivity photoFrameActivity;
    private FlickrClient flickr;
    private Set<Photo> photos;
    private Set<Contact> contacts;

    private int asyncRequestsInProgress = 0;

    public PhotoCollection(final PhotoFrameActivity photoFrameActivity, final FlickrClient flickr) {
        this.photoFrameActivity = photoFrameActivity;
        this.flickr = flickr;
        contacts = new HashSet<>();
        photos = new HashSet<>();
    }

    public void startDiscovery() {
        flickr.lookupProfile(this);
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

    public void addContactsAndContinueDiscovery(final Set<Contact> newContacts) {
        contacts.addAll(newContacts);
        Log.i("PhotoCollection", "added contacts: " + newContacts);
        flickr.lookupPhotos(this);
    }

    public synchronized void addPhotoAndContinueDiscovery(final Photo photo) {
        photos.add(photo);
        Log.d("PhotoCollection", "starting asyncRequest, count: " + asyncRequestsInProgress);
        flickr.lookupPhotoMetadata(this, photo);
        asyncRequestsInProgress++;
    }

    public synchronized void markAsyncRequestComplete() {
        asyncRequestsInProgress--;
        Log.d("PhotoCollection", "asyncRequestsInProgress: " + asyncRequestsInProgress);

        // discovery complete, start slideshow timer
        if (asyncRequestsInProgress == 0) {
            photoFrameActivity.startShow();
        }
    }

    // TODO getRecentPhotosBy, getOldPhotosBy
    public Set<Photo> getPhotos() {
        return photos;
    }
}
