package com.example.milestonemk_4.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GoogleDriveHelper {
    private static final String TAG = "GoogleDriveHelper";
    private static final int REQUEST_CODE_SIGN_IN = 1;

    private final Activity activity;
    private Drive driveService;
    private final Executor executor = Executors.newSingleThreadExecutor();

    public interface DriveUploadCallback {
        void onUploadSuccess(String fileId);
        void onUploadFailure(Exception e);
    }

    public GoogleDriveHelper(Activity activity) {
        this.activity = activity;
    }

    public void signIn() {
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();

        GoogleSignInClient client = GoogleSignIn.getClient(activity, signInOptions);
        activity.startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    public boolean handleSignInResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SIGN_IN && resultCode == Activity.RESULT_OK) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            if (task.isSuccessful()) {
                GoogleSignInAccount account = task.getResult();
                initializeDriveService(account);
                return true;
            } else {
                Log.e(TAG, "Sign-in failed: " + task.getException());
            }
        }
        return false;
    }

    private void initializeDriveService(GoogleSignInAccount account) {
        // Use GoogleHttpTransport (NetHttpTransport for Android)
        HttpTransport transport = new NetHttpTransport();

        // Initialize GoogleAccountCredential
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                activity, Collections.singleton(DriveScopes.DRIVE_FILE));
        credential.setSelectedAccount(account.getAccount());

        // Initialize the Drive service using the transport and credential
        driveService = new Drive.Builder(
                transport,
                new GsonFactory(),
                credential)
                .setApplicationName("MilestoneMK_4")
                .build();
    }

    public void uploadFileToDrive(Uri fileUri, String fileName, String mimeType, DriveUploadCallback callback) {
        if (driveService == null) {
            callback.onUploadFailure(new Exception("Drive service not initialized. Please sign in first."));
            return;
        }

        executor.execute(() -> {
            try {
                // Create folder for task uploads if it doesn't exist
                String folderId = getOrCreateFolder("MilestoneMK4_Task_Uploads");

                // Create file metadata
                File fileMetadata = new File();
                fileMetadata.setName(fileName);
                fileMetadata.setParents(Collections.singletonList(folderId));

                // Get file content from Uri
                InputStream inputStream = activity.getContentResolver().openInputStream(fileUri);
                if (inputStream == null) {
                    throw new IOException("Could not open file input stream");
                }

                // Upload file to Drive
                File uploadedFile = driveService.files().create(fileMetadata,
                                new com.google.api.client.http.InputStreamContent(mimeType, inputStream))
                        .setFields("id")
                        .execute();

                String fileId = uploadedFile.getId();
                activity.runOnUiThread(() -> callback.onUploadSuccess(fileId));
            } catch (Exception e) {
                Log.e(TAG, "Error uploading file", e);
                activity.runOnUiThread(() -> callback.onUploadFailure(e));
            }
        });
    }

    private String getOrCreateFolder(String folderName) throws IOException {
        // Check if folder already exists
        FileList result = driveService.files().list()
                .setQ("mimeType='application/vnd.google-apps.folder' and name='" + folderName + "' and trashed=false")
                .setSpaces("drive")
                .execute();

        // If folder exists, return its ID
        if (result.getFiles() != null && !result.getFiles().isEmpty()) {
            return result.getFiles().get(0).getId();
        }

        // Otherwise, create a new folder
        File folderMetadata = new File();
        folderMetadata.setName(folderName);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");

        File folder = driveService.files().create(folderMetadata)
                .setFields("id")
                .execute();

        return folder.getId();
    }
}