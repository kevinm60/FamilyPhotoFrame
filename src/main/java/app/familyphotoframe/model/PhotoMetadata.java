package app.familyphotoframe.model;

import java.util.List;
import java.util.Date;

public class PhotoMetadata {
    private Date dateTaken;
    private List<String> comments;
    private List<String> tags;

    public Date getDateTaken() {
        return dateTaken;
    }
    public void setDateTaken(final Date dateTaken) {
        this.dateTaken = dateTaken;
    }

    public List<String> getComments() {
        return comments;
    }
    public void setComments(final List<String> comments) {
        this.comments = comments;
    }

    public List<String> getTags() {
        return tags;
    }
    public void setTags(final List<String> tags) {
        this.tags = tags;
    }
}
