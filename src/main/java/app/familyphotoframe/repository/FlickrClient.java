package app.familyphotoframe.repository;

import java.util.List;
import  app.familyphotoframe.model.Contact;
import  app.familyphotoframe.model.PhotoMetadata;
import  app.familyphotoframe.model.Photo;

public class FlickrClient {
    private String accessToken;
    private String appId;
    private String appSecret;

    public void login() {
    }

    public List<Contact> lookupContacts() {
        return null;
    }

    public List<Photo> lookupPhotos(final Contact contact) {
        return null;
    }

    public PhotoMetadata lookupPhotoMetadata(final Photo photo) {
        return null;
    }
}
