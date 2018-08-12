package app.familyphotoframe;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;

import com.github.scribejava.core.builder.api.BaseApi;
import com.codepath.oauth.OAuthLoginActivity;
import com.codepath.oauth.OAuthBaseClient;

import app.familyphotoframe.repository.FlickrClient;


/**
 * handles oauth flow to get permision to access flickr account data.
 *
 * once login is complete, kill this activity and start the PhotoFrameActivity.
 */
public class LoginActivity extends OAuthLoginActivity<FlickrClient> implements View.OnClickListener {
    /**
     * called when app starts
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        findViewById(R.id.login_button).setOnClickListener(this);
    }

    /**
     * fires when log button is clicked
     */
    public void onClick(View view) {
        getClient().connect();
    }

    /**
     * fires if auth succeeds
     */
    @Override
    public void onLoginSuccess() {
        Log.i("LoginActivity", "login success, starting photo frame");
        tellLoginResult(true);
        Intent photoFrameIntent = new Intent(this, PhotoFrameActivity.class);
        startActivity(photoFrameIntent);
        finish();
    }

    /**
     * fires if auth fails
     */
    @Override
    public void onLoginFailure(Exception e) {
        e.printStackTrace();
        tellLoginResult(false);
    }

    /**
     * used to tell the user if the login succeeded
     */
    private void tellLoginResult(final boolean success) {
        Context context = getApplicationContext();
        String message = (success) ? getString(R.string.login_success) : getString(R.string.login_failed);
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.show();
    }
}
