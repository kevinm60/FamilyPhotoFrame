package app.familyphotoframe.model;

public class Contact {
    private String userId;
    private String name;
    private Relationship relationship;

    public Contact(final String userId, final String name, final Relationship relationship) {
        this.userId = userId;
        this.name = name;
        this.relationship = relationship;
    }

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

    public Relationship getRelationship() {
        return relationship;
    }
    public void setRelationship(final Relationship relationship) {
        this.relationship = relationship;
    }

    public String toString() {
        return userId + " " + name;
    }

    public boolean equals(Object other) {
        if (!(other instanceof Contact)) {
            return false;
        }
        Contact otherContact = (Contact) other;
        return this.userId.equals(otherContact.userId);
    }

    public int hashCode() {
        return userId.hashCode();
    }
}
