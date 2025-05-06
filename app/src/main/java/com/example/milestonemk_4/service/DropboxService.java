package com.example.milestonemk_4.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DropboxService {
    private static final String TAG = "DropboxService";
    private static final String APP_KEY = "v81qta85dv26l87";
    private static final String DROPBOX_PREFS = "dropbox_prefs";
    private static final String ACCESS_TOKEN_KEY = "access_token";

    private DbxClientV2 dropboxClient;
    private final Context context;
    private final Executor executor = Executors.newSingleThreadExecutor();

    public interface UploadCallback {
        void onUploadSuccess(String fileName, String path);
        void onUploadFailure(String errorMessage);
    }

    public DropboxService(Context context) {
        this.context = context;
        initializeClient();
    }

    private void initializeClient() {
        String accessToken = getAccessToken();
        if (accessToken != null) {
            // Configure Dropbox client
            DbxRequestConfig config = DbxRequestConfig.newBuilder("milestonemk_4/1.0")
                    .withAutoRetryEnabled()
                    .build();
            dropboxClient = new DbxClientV2(config, accessToken);
        }
    }

    public boolean isAuthenticated() {
        return getAccessToken() != null;
    }

    private String getAccessToken() {
        SharedPreferences prefs = context.getSharedPreferences(DROPBOX_PREFS, Context.MODE_PRIVATE);
        return prefs.getString(ACCESS_TOKEN_KEY, null);
    }

    public void saveAccessToken(String accessToken) {
        SharedPreferences prefs = context.getSharedPreferences(DROPBOX_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(ACCESS_TOKEN_KEY, accessToken).apply();
        initializeClient();
    }

    public void uploadFile(Uri fileUri, String taskName, UploadCallback callback) {
        if (dropboxClient == null) {
            callback.onUploadFailure("Not authenticated with Dropbox");
            return;
        }

        executor.execute(() -> {
            try {
                // Create a folder for completed tasks if it doesn't exist
                String dropboxPath = "/CompletedTasks/" + taskName + "/" + getFileName(fileUri);

                InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
                if (inputStream == null) {
                    callback.onUploadFailure("Could not open file");
                    return;
                }

                FileMetadata metadata = dropboxClient.files().uploadBuilder(dropboxPath)
                        .withMode(WriteMode.OVERWRITE)
                        .uploadAndFinish(inputStream);

                callback.onUploadSuccess(metadata.getName(), metadata.getPathDisplay());
            } catch (DbxException | IOException e) {
                Log.e(TAG, "Failed to upload file", e);
                callback.onUploadFailure("Upload failed: " + e.getMessage());
            }
        });
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (columnIndex != -1) {
                        result = cursor.getString(columnIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Could not get file name", e);
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public void logOut() {
        SharedPreferences prefs = context.getSharedPreferences(DROPBOX_PREFS, Context.MODE_PRIVATE);
        prefs.edit().remove(ACCESS_TOKEN_KEY).apply();
        dropboxClient = null;
    }
}
