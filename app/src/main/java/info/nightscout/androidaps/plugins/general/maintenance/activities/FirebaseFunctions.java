package info.nightscout.androidaps.plugins.general.maintenance.activities;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.logging.L;

public class FirebaseFunctions extends AppCompatActivity {
    private static Logger log = LoggerFactory.getLogger(L.CORE);

    private FirebaseAuth firebaseAuth;
    private Button exportButton;
    private Button importButton;
    private StorageReference reference;
    static File path = new File(Environment.getExternalStorageDirectory().toString());
    static public final File file = new File(path, MainApp.gs(R.string.app_name) + "Preferences");
    private Uri filePath = Uri.fromFile(file);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase_functions);
        exportButton = (Button) findViewById(R.id.firebase_export);
        importButton = (Button) findViewById(R.id.firebase_import);

        firebaseAuth = FirebaseAuth.getInstance();
        if(firebaseAuth.getCurrentUser() != null){
            reference = FirebaseStorage.getInstance().getReference();
            // Show the import/export buttons
            exportButton.setOnClickListener(view1 -> {
                // Will open signup activity
                exportToFirebase();
            });
            importButton.setOnClickListener(view1 -> {
                // Will open signup activity
                importFromFirebase();
            });
        }
    }

    private void exportToFirebase(){
        // Get SharedPreferences as a string
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String prefsString = prefs.getAll().toString();

        // save it to storage
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        // creates a file with name == userID and containing all the preferences
        reference.child(user.getUid()).putFile(filePath).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // Get a URL to the uploaded content
                Uri downloadUrl = taskSnapshot.getUploadSessionUri();
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        log.debug("Error: "+exception.getMessage());
                        // ...
                    }
                });
    }

    private void importFromFirebase(){
        

    }
}
