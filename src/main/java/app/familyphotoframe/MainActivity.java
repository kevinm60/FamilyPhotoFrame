package app.familyphotoframe;

import java.util.ArrayList;
import java.io.File;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity {

    /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("MainActivity", "created");
        setContentView(R.layout.activity_main);
    }
}
