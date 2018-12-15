package app.familyphotoframe.model;

import java.util.List;
import java.util.Date;

public class Photo {
    private String id;
    private String secret;
    private String farmId;
    private String serverId;
    private Contact owner;
    private Date dateTaken;
    private String title;

    public Photo(final String id, final String secret, final String serverId, final String farmId, final Contact owner,
                 final String title, final Date dateTaken) {
        this.id = id;
        this.secret = secret;
        this.serverId = serverId;
        this.farmId = farmId;
        this.owner = owner;
        this.title = title;
        this.dateTaken = dateTaken;
    }

    public String getId() {
        return id;
    }
    public void setId(final String id) {
        this.id = id;
    }

    public String getSecret() {
        return secret;
    }
    public void setSecret(final String secret) {
        this.secret = secret;
    }

    public String getFarmId() {
        return farmId;
    }
    public void setFarmId(final String farmId) {
        this.farmId = farmId;
    }

    public String getServerId() {
        return serverId;
    }
    public void setServerId(final String serverId) {
        this.serverId = serverId;
    }

    public Contact getOwner() {
        return owner;
    }
    public void setOwner(final Contact owner) {
        this.owner = owner;
    }

    public Date getDateTaken() {
        return dateTaken;
    }
    public void setDateTaken(final Date dateTaken) {
        this.dateTaken = dateTaken;
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(final String title) {
        this.title = title;
    }

    /**
     * get the url to fetch the actual image from flickr.
     * size "h" is 1600 on the longest side.
     *
     * @see https://www.flickr.com/services/api/misc.urls.html
     */
    public String getUrl() {
        return String.format("https://farm%s.staticflickr.com/%s/%s_%s_h.jpg",
                             farmId, serverId, id, secret);
    }

    public boolean equals(Object other) {
        if (!(other instanceof Photo)) {
            return false;
        }

        Photo otherPhoto = (Photo) other;
        return this.id.equals(otherPhoto.id);
    }

    public int hashCode() {
        return id.hashCode();
    }
}
