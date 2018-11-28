package app.familyphotoframe.model;

public enum Relationship {
    SELF(0),
    FAMILY(1),
    FRIEND(2),
    STRANGER(3);

    private int value;

    public static final int length = Relationship.values().length;

    private Relationship(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

}
