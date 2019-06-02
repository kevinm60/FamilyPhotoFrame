package app.familyphotoframe.model;

import java.util.List;
import java.util.Date;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class Photo {
    private String id;
    private String secret;
    private String farmId;
    private String serverId;
    private Contact owner;
    private Date dateTaken;
    private String title;
    private String woeId;
    private String location;

    public Photo(final String id, final String secret, final String serverId, final String farmId, final Contact owner,
                 final String title, final Date dateTaken, final String woeId) {
        this.id = id;
        this.secret = secret;
        this.serverId = serverId;
        this.farmId = farmId;
        this.owner = owner;
        this.title = title;
        this.dateTaken = dateTaken;
        this.woeId = woeId;
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

    public String getWoeId() {
        return woeId;
    }

    public synchronized String getLocation() {
        return location;
    }
    public synchronized void setLocation(final String location) {
        this.location = location;
    }

    /**
     * get the url to fetch the actual image from flickr.
     *
     * @see https://www.flickr.com/services/api/misc.urls.html
     */
    public String getUrl() {
        return String.format("https://farm%s.staticflickr.com/%s/%s_%s.jpg",
                             farmId, serverId, id, secret);
    }

    /**
     * compute the number of days difference between two dates, as if they were in the same year.
     */
    public int computeSameYearDaysDifference(final Date now) {
        int photoDate = calFromDate(dateTaken).get(Calendar.DAY_OF_YEAR);
        int currentDate = calFromDate(now).get(Calendar.DAY_OF_YEAR);
        return Math.abs(currentDate - photoDate);
    }

    public int computeDifferenceInYears(final Date now) {
        int photoYear = calFromDate(dateTaken).get(Calendar.YEAR);
        int currentYear = calFromDate(now).get(Calendar.YEAR);
        return currentYear - photoYear;
    }

    private Calendar calFromDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date.getTime());
        return calendar;
    }

    public String toString() {
        return String.format("%s/%s", owner.getName(), id);
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
