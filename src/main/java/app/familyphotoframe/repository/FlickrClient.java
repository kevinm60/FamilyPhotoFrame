package app.familyphotoframe.repository;

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.List;
import java.util.Date;
import java.util.NoSuchElementException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import android.util.Log;
import android.content.Context;
import android.content.res.Resources;

import com.github.scribejava.apis.FlickrApi;
import com.github.scribejava.core.builder.api.BaseApi;
import com.github.scribejava.apis.FlickrApi.FlickrPerm;
import com.codepath.oauth.OAuthBaseClient;
import com.loopj.android.http.RequestParams;
import cz.msebera.android.httpclient.Header;
import com.loopj.android.http.JsonHttpResponseHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import app.familyphotoframe.model.Contact;
import app.familyphotoframe.model.Photo;
import app.familyphotoframe.model.Relationship;
import app.familyphotoframe.R;

/**
 * handles all interactions with flickr.
 *
 * uses oauth client to sign requests.
 */
public class FlickrClient extends OAuthBaseClient {

    public static final BaseApi REST_API_INSTANCE = FlickrApi.instance(FlickrPerm.READ);
    public static final String REST_URL = "https://api.flickr.com/services/rest";
    public static final String REST_CALLBACK_URL = "oauth://flickrCallback.com";
    private String apiKey;

    private static String FLICKR_PROFILE_FIELD = "user";
    private static String FLICKR_USERNAME_FIELD = "username";
    private static String FLICKR_CONTACTS_FIELD = "contacts";
    private static String FLICKR_CONTACT_FIELD = "contact";
    private static String FLICKR_USERID_FIELD = "nsid";
    private static String FLICKR_FAMILY_FIELD = "family";
    private static String FLICKR_FRIEND_FIELD = "friend";
    private static String FLICKR_PHOTOS_FIELD = "photos";
    private static String FLICKR_PHOTO_FIELD = "photo";
    private static String FLICKR_ID_FIELD = "id";
    private static String FLICKR_SECRET_FIELD = "secret";
    private static String FLICKR_SERVERID_FIELD = "server";
    private static String FLICKR_FARMID_FIELD = "farm";
    private static String FLICKR_OWNER_FIELD = "owner";
    private static String FLICKR_DATES_FIELD = "dates";
    private static String FLICKR_TAKEN_FIELD = "taken";
    private static String FLICKR_DATE_FORMAT = "y-M-d H:m:s";

    public FlickrClient(final Context context) {
        super(context,
              REST_API_INSTANCE,
              REST_URL,
              context.getResources().getString(R.string.flickr_api_key),
              context.getResources().getString(R.string.flickr_api_secret),
              REST_CALLBACK_URL);
        apiKey = context.getResources().getString(R.string.flickr_api_key);
        Log.i("FlickrClient", "created");
    }

    public void lookupProfile(final PhotoCollection photoCollection) {
        RequestParams params = makeRequestParams();
        params.put("method", "flickr.test.login");

        String apiUrl = getApiUrl("");
        Log.d("FlickrClient", "apiUrl: " + apiUrl);
        Log.d("FlickrClient", "params: " + params);

        client.get(apiUrl, params, new ProfileResponseHandler(photoCollection));
    }

    public void lookupContacts(final PhotoCollection photoCollection) {
        RequestParams params = makeRequestParams();
        params.put("method", "flickr.contacts.getList");

        String apiUrl = getApiUrl("");
        Log.d("FlickrClient", "apiUrl: " + apiUrl);
        Log.d("FlickrClient", "params: " + params);

        client.get(apiUrl, params, new ContactsResponseHandler(photoCollection));
    }

    public void lookupPhotos(final PhotoCollection photoCollection) {
        RequestParams params = makeRequestParams();
        params.put("method", "flickr.photos.getContactsPhotos");
        params.put("include_self", 1);
        params.put("count", 50);

        String apiUrl = getApiUrl("");
        Log.d("FlickrClient", "apiUrl: " + apiUrl);
        Log.d("FlickrClient", "params: " + params);

        client.get(apiUrl, params, new PhotosResponseHandler(photoCollection));
    }

    public void lookupPhotoMetadata(final PhotoCollection photoCollection, final Photo photo) {
        RequestParams params = makeRequestParams();
        params.put("method", "flickr.photos.getInfo");
        params.put("photo_id", photo.getId());
        params.put("secret", photo.getSecret());

        String apiUrl = getApiUrl("");
        Log.d("FlickrClient", "apiUrl: " + apiUrl);
        Log.d("FlickrClient", "params: " + params);

        client.get(apiUrl, params, new PhotoMetadataResponseHandler(photoCollection, photo));
    }

    private RequestParams makeRequestParams() {
        RequestParams params = new RequestParams();
        params.put("nojsoncallback", "1");
        params.put("format", "json");
        params.put("api_key", apiKey);
        return params;
    }

    class ProfileResponseHandler extends JsonHttpResponseHandler {
        private PhotoCollection photoCollection;

        public ProfileResponseHandler(final PhotoCollection photoCollection) {
            this.photoCollection = photoCollection;
        }

        public void onStart() {
            Log.i("FlickrClient", "profile request started");
        }

        public void onSuccess(int statusCode, Header[] headers, JSONObject json) {
            try {
                Log.i("FlickrClient", "got profile: " + json);
                Set<Contact> profile = new HashSet<>();
                JSONObject jsonProfile = json.getJSONObject(FLICKR_PROFILE_FIELD);
                Log.i("FlickrClient", "got jsonProfile: " + jsonProfile);
                String userId = jsonProfile.getString(FLICKR_ID_FIELD);
                Contact self = new Contact(userId, "me", Relationship.SELF);
                photoCollection.addProfileAndContinueDiscovery(self);
            } catch (JSONException e) {
                Log.e("FlickrClient", "getProfile JSONException: " + e);
            }
        }
        public void onFailure(int statusCode, Header[] headers, Throwable err, JSONObject json) {
            Log.e("FlickrClient", "fail: " + err);
        }
    }

    // TODO paging
    class ContactsResponseHandler extends JsonHttpResponseHandler {
        private PhotoCollection photoCollection;

        public ContactsResponseHandler(final PhotoCollection photoCollection) {
            this.photoCollection = photoCollection;
        }

        public void onStart() {
            Log.d("FlickrClient", "contacts request started");
        }

        public void onSuccess(int statusCode, Header[] headers, JSONObject json) {
            try {
                Log.d("FlickrClient", "got contacts: " + json);
                Set<Contact> contacts = new HashSet<>();
                JSONArray jsonContacts = json.getJSONObject(FLICKR_CONTACTS_FIELD).getJSONArray(FLICKR_CONTACT_FIELD);
                for (int ii=0; ii<jsonContacts.length(); ii++) {
                    JSONObject jsonContact = jsonContacts.getJSONObject(ii);
                    Log.d("FlickrClient", "got jsonContact: " + jsonContact);
                    String name = jsonContact.getString(FLICKR_USERNAME_FIELD);
                    String userId = jsonContact.getString(FLICKR_USERID_FIELD);
                    Relationship relationship = null;
                    if (jsonContact.getInt(FLICKR_FAMILY_FIELD) == 1) {
                        relationship = Relationship.FAMILY;
                    } else if (jsonContact.getInt(FLICKR_FRIEND_FIELD) == 1) {
                        relationship = Relationship.FRIEND;
                    }
                    contacts.add(new Contact(userId, name, relationship));
                }
                photoCollection.addContactsAndContinueDiscovery(contacts);
            } catch (JSONException e) {
                Log.e("FlickrClient", "getContacts JSONException: " + e);
            }
        }
        public void onFailure(int statusCode, Header[] headers, Throwable err, JSONObject json) {
            Log.e("FlickrClient", "fail: " + err);
        }
    }

    // TODO paging
    class PhotosResponseHandler extends JsonHttpResponseHandler {
        private PhotoCollection photoCollection;

        public PhotosResponseHandler(final PhotoCollection photoCollection) {
            this.photoCollection = photoCollection;
        }

        public void onStart() {
            Log.d("FlickrClient", "photos request started");
        }

        public void onSuccess(int statusCode, Header[] headers, JSONObject json) {
            try {
                Log.d("FlickrClient", "got photos: " + json);
                JSONArray jsonPhotos = json.getJSONObject(FLICKR_PHOTOS_FIELD).getJSONArray(FLICKR_PHOTO_FIELD);
                for (int ii=0; ii<jsonPhotos.length(); ii++) {
                    JSONObject jsonPhoto = jsonPhotos.getJSONObject(ii);
                    Log.d("FlickrClient", "got jsonPhoto: " + jsonPhoto);
                    String id = jsonPhoto.getString(FLICKR_ID_FIELD);
                    String secret = jsonPhoto.getString(FLICKR_SECRET_FIELD);
                    String serverId = jsonPhoto.getString(FLICKR_SERVERID_FIELD);
                    String farmId = jsonPhoto.getString(FLICKR_FARMID_FIELD);
                    Contact owner = photoCollection.getContact(jsonPhoto.getString(FLICKR_OWNER_FIELD));
                    Photo photo = new Photo(id, secret, serverId, farmId, owner);
                    photoCollection.addPhotoAndContinueDiscovery(photo);
                }
            } catch (JSONException | NoSuchElementException e) {
                Log.e("FlickrClient", "getContactsPhotos: " + e);
            }
        }
        public void onFailure(int statusCode, Header[] headers, Throwable err, JSONObject json) {
            Log.e("FlickrClient", "fail: " + err);
        }
    }

    // TODO title, description, comments, tags
    class PhotoMetadataResponseHandler extends JsonHttpResponseHandler {
        private DateFormat dateFormat = new SimpleDateFormat(FLICKR_DATE_FORMAT);
        private PhotoCollection photoCollection;
        private Photo photo;

        public PhotoMetadataResponseHandler(final PhotoCollection photoCollection, final Photo photo) {
            this.photoCollection = photoCollection;
            this.photo = photo;
        }

        public void onStart() {
            Log.d("FlickrClient", "photo request started");
        }

        public void onSuccess(int statusCode, Header[] headers, JSONObject json) {
            try {
                Log.d("FlickrClient", "got photo metadata: " + json);
                JSONObject jsonPhoto = json.getJSONObject(FLICKR_PHOTO_FIELD);
                String dateTakenString = jsonPhoto.getJSONObject(FLICKR_DATES_FIELD).getString(FLICKR_TAKEN_FIELD);
                Date dateTaken = dateFormat.parse(dateTakenString);
                photo.setDateTaken(dateTaken);
            } catch (JSONException | ParseException e ) {
                Log.e("FlickrClient", "getContactsPhotos Exception: " + e);
            }
            photoCollection.markAsyncRequestComplete();
        }
        public void onFailure(int statusCode, Header[] headers, Throwable err, JSONObject json) {
            Log.e("FlickrClient", "fail: " + err);
        }
    }
}
