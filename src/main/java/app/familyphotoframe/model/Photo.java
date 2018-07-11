package app.familyphotoframe.model;

import java.util.List;
import java.util.Date;

public class Photo {
    private String id;
    private String secret;
    private String serverId;
    private PhotoMetadata metadata;
    private Contact takenBy;

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

    public String getServerId() {
        return serverId;
    }
    public void setServerId(final String serverId) {
        this.serverId = serverId;
    }

    public PhotoMetadata getMetadata() {
        return metadata;
    }
    public void setMetadata(final PhotoMetadata metadata) {
        this.metadata = metadata;
    }

    public Contact getTakenBy() {
        return takenBy;
    }
    public void setTakenBy(final Contact takenBy) {
        this.takenBy = takenBy;
    }
}
