package app.familyphotoframe.slideshow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.widget.ImageView;
import android.content.Intent;
import android.os.Environment;
import android.net.Uri;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;

/**
 * Handles sharing actions, which can be triggered from the slideshow menu.
 *
 * @see https://github.com/codepath/android_guides/wiki/Sharing-Content-with-Intents
 */
public class ShareHandler {
    private Activity activity;
    private File baseDir;
    private static final String FNAME_PREFIX = "FamilyPhotoFrameShare_";

    public ShareHandler(final Activity activity) {
        this.activity = activity;
        this.baseDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    }

    /**
     * Share the image in the given drawable.
     */
    public void sharePhoto(final ImageView imageView) {
        Log.i("ShareHandler", "sharing a photo");

        clearOldShareFiles();

        // Get access to the URI for the bitmap
        Uri bmpUri = getLocalBitmapUri(imageView, baseDir);
        Log.i("ShareHandler", "bmpUri: " + bmpUri);
        if (bmpUri != null) {
            // Construct a ShareIntent with link to image
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
            shareIntent.setType("image/*");
            // Launch sharing dialog for image
            activity.startActivity(Intent.createChooser(shareIntent, "Share Image"));
        } else {
            Log.e("ShareHandler", "sharing failed");
        }
    }

    /**
     * Delete any old temp files that were created while sharing photos previously.
     */
    private void clearOldShareFiles() {
        for (File file : baseDir.listFiles()) {
            if (file.isFile() && file.getName().startsWith(FNAME_PREFIX)) {
                Log.i("ShareHandler", "deleting old temp file: " + file.getName());
                file.delete();
            }
        }
    }

    /**
     * Save image in drawable to a local file and return the filename.
     */
    private Uri getLocalBitmapUri(final ImageView imageView, final File dir) {
        // Extract Bitmap from ImageView drawable
        Drawable drawable = imageView.getDrawable();
        Bitmap bmp = null;
        if (drawable instanceof BitmapDrawable){
            bmp = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        } else {
            Log.e("ShareHandler", "drawable isn't a bitmap");
            return null;
        }
        // Store image to default external storage directory
        Uri bmpUri = null;
        try {
            // Use methods on Context to access package-specific directories on external storage.
            // This way, you don't need to request external read/write permission.
            // See https://youtu.be/5xVh-7ywKpE?t=25m25s
            File file =  new File(dir, FNAME_PREFIX + System.currentTimeMillis() + ".jpg");
            FileOutputStream out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.close();
            // **Warning:** This will fail for API >= 24, use a FileProvider as shown below instead.
            bmpUri = Uri.fromFile(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bmpUri;
    }
}
