package app.familyphotoframe.model;

public class Contact {
    private String userId;
    private String name;
    private boolean friend;
    private boolean family;

    public String getUserId() {
        return userId;
    }
    public void setUserId(final String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }
    public void setName(final String name) {
        this.name = name;
    }

    public boolean isFriend() {
        return friend;
    }
    public void setIsFriend(final boolean friend) {
        this.friend = friend;
    }

    public boolean isFamily() {
        return family;
    }
    public void setIsFamily(final boolean family) {
        this.family = family;
    }
}
