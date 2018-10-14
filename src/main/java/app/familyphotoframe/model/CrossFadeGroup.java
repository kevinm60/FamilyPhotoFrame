package app.familyphotoframe.model;

import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Glide cross fade doesn't work with images loaded by different requests, so we use android for
 * cross fade. That means we need to render to alternating frames and fade between them.
 */
public class CrossFadeGroup {
    final private ViewGroup frame;
    final private ImageView photo;
    final private TextView caption;

    public CrossFadeGroup(final ViewGroup frame, final ImageView photo, final TextView caption) {
        this.frame = frame;
        this.photo = photo;
        this.caption = caption;
    }

    public ViewGroup getFrame() {
        return frame;
    }

    public ImageView getPhoto() {
        return photo;
    }

    public TextView getCaption() {
        return caption;
    }
}
