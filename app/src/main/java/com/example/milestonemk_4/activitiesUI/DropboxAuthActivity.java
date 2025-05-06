package com.example.milestonemk_4.activitiesUI;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

import com.example.milestonemk_4.R;
import com.example.milestonemk_4.service.DropboxService;

public class DropboxAuthActivity extends AppCompatActivity {
    private static final String TAG = "DropboxAuthActivity";
    private static final String AUTH_URL = "https://www.dropbox.com/oauth2/authorize";
    private static final String APP_KEY = "v81qta85dv26l87";
    private static final String REDIRECT_URI = "milestonemk4://oauth2redirect";

    private DropboxService dropboxService;
    private TextView statusText;
    private Button authButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dropbox_auth);

        dropboxService = new DropboxService(this);

        statusText = findViewById(R.id.authStatusText);
        authButton = findViewById(R.id.authButton);
        Button logoutButton = findViewById(R.id.logoutButton);
        Button backButton = findViewById(R.id.backButton);

        updateUI();

        authButton.setOnClickListener(v -> startOAuth());

        logoutButton.setOnClickListener(v -> {
            dropboxService.logOut();
            updateUI();
            Toast.makeText(this, "Logged out from Dropbox", Toast.LENGTH_SHORT).show();
        });

        backButton.setOnClickListener(v -> finish());

        // Check if this activity was started by the redirect URI
        Uri uri = getIntent().getData();
        if (uri != null && uri.toString().startsWith(REDIRECT_URI)) {
            handleOAuthResponse(uri);
        }
    }

    private void updateUI() {
        boolean isAuthenticated = dropboxService.isAuthenticated();
        statusText.setText(isAuthenticated ?
                "Connected to Dropbox" :
                "Not connected to Dropbox");
        authButton.setVisibility(isAuthenticated ? View.GONE : View.VISIBLE);
    }

    private void startOAuth() {
        String authUrl = AUTH_URL +
                "?client_id=" + APP_KEY +
                "&response_type=token" +
                "&redirect_uri=" + REDIRECT_URI;

        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(this, Uri.parse(authUrl));
    }

    private void handleOAuthResponse(Uri uri) {
        try {
            // Parse the access token from the URI fragment
            String fragment = uri.getFragment();
            if (fragment != null) {
                String[] params = fragment.split("&");
                for (String param : params) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length == 2 && keyValue[0].equals("access_token")) {
                        String accessToken = keyValue[1];
                        dropboxService.saveAccessToken(accessToken);
                        updateUI();
                        Toast.makeText(this, "Successfully connected to Dropbox", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling OAuth response", e);
            Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show();
        }
    }
}
