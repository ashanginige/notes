package com.ginige.ashan.notes;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.ginige.ashan.notes.Models.NoteDetails;
import com.ginige.ashan.notes.Models.User;
import com.ginige.ashan.notes.Util.Constants;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class NoteDetailsActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private FirebaseAuth firebaseAuth;
    public User user;
    private EditText noteTitle;
    private EditText noteBody;
    private FloatingActionButton camButton;
    public NoteDetails noteDetails;
    int processID;
    boolean doubleBackToExitPressedOnce = false;
    boolean isNoteEmpty = true;
    ValueEventListener listener;
    DatabaseReference notesDatabaseRef;
    ConstraintLayout noteDetailsMainLayout;
    private ImageView noteImage;
    private StorageReference storageReference;
    private ProgressDialog progressDialog;
    private String ImageURL;

    private static final int CAMERA_REQUEST_CODE = 1;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_details);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        noteTitle = findViewById(R.id.noteTitle);
        noteBody = findViewById(R.id.noteBody);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        final Intent intent = getIntent();
        processID = intent.getIntExtra(Constants.NOTE_PROCESS, 0);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading the Image");
        progressDialog.setCanceledOnTouchOutside(false);

        storageReference = FirebaseStorage.getInstance().getReference();
        noteImage= findViewById(R.id.noteImage);
        camButton = findViewById(R.id.camButton);
        camButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, CAMERA_REQUEST_CODE);

            }
        });

        firebaseAuth = FirebaseAuth.getInstance();
        if(firebaseAuth.getCurrentUser() != null){
            user = new User(firebaseAuth.getCurrentUser().getUid());
        }
        if(processID == Constants.EDIT_NOTE_VALUE) {
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            noteDetailsMainLayout = findViewById(R.id.noteDetailsMainLayout);
            noteDetailsMainLayout.setFocusable(true);
            noteDetailsMainLayout.setFocusableInTouchMode(true);
            setData(user.getId(),intent.getStringExtra(Constants.NOTE_ID_KEY));
        }


        if (ContextCompat.checkSelfPermission(NoteDetailsActivity.this,
                android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(NoteDetailsActivity.this,
                    android.Manifest.permission.CAMERA)) {

            } else {
                ActivityCompat.requestPermissions(NoteDetailsActivity.this,
                        new String[]{ android.Manifest.permission.CAMERA},
                        CAMERA_PERMISSION_REQUEST_CODE);

            }
        } else {
            camButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, CAMERA_REQUEST_CODE);

                }
            });
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CAMERA_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                }
                return;
            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK){

            progressDialog.show();

            Uri uri = data.getData();

            StorageReference filepath = storageReference.child("photos").child(uri.getLastPathSegment());

            filepath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    progressDialog.dismiss();

                    Uri downloadUri = taskSnapshot.getDownloadUrl();
                    ImageURL = downloadUri.toString();
                    Picasso.get().load(downloadUri).into(noteImage);

                    Toast.makeText(NoteDetailsActivity.this,  "Finish Uploading", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if(!noteTitle.getText().toString().equalsIgnoreCase("") && !noteBody.getText().toString().equalsIgnoreCase("") ){
            isNoteEmpty = false;
        } else {
            isNoteEmpty = true;
        }
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();

            final Intent intent = getIntent();
            processID = intent.getIntExtra(Constants.NOTE_PROCESS, 0);

            if(!noteTitle.getText().toString().equalsIgnoreCase("") && !noteBody.getText().toString().equalsIgnoreCase("") ){
                if(processID == 2000) {
                    addNewNote(user.getId(), noteTitle.getText().toString(), noteBody.getText().toString(), Calendar.getInstance().getTime(), ImageURL);
                } else if(processID == 1000) {
                    editNote(user.getId(), intent.getStringExtra(Constants.NOTE_ID_KEY), noteTitle.getText().toString(), noteBody.getText().toString(), Calendar.getInstance().getTime(), ImageURL);
                }
            }
            if (notesDatabaseRef != null && listener != null) {
                notesDatabaseRef.removeEventListener(listener);
            }

            return;
        }

        this.doubleBackToExitPressedOnce = true;

        if(isNoteEmpty){
            Snackbar.make(findViewById(R.id.noteDetailsMainLayout), "Note is INCOMPLETE and will NOT SAVE", Snackbar.LENGTH_SHORT).show();
        } else {
            Snackbar.make(findViewById(R.id.noteDetailsMainLayout), "Please click BACK again to SAVE and exit", Snackbar.LENGTH_SHORT).show();
        }

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }


    private void addNewNote(String userId, String title, String body, Date date, String imageURL) {
        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
        notesDatabaseRef = mDatabase.child("users").child(userId).child("notes").push();
        NoteDetails noteDetails = new NoteDetails(title, body, formatter.format(date), imageURL);
        notesDatabaseRef.setValue(noteDetails);
    }

    private void editNote(String userId,String noteID, String title, String body, Date date, String imageURL){
        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
        notesDatabaseRef = mDatabase.child("users").child(userId).child("notes").child(noteID);
        NoteDetails noteDetails = new NoteDetails(title, body, formatter.format(date), imageURL);
        notesDatabaseRef.setValue(noteDetails);
    }

    private void setData(String userId, String noteID){
        notesDatabaseRef = mDatabase.child("users").child(userId).child("notes").child(noteID);
        listener = notesDatabaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot != null) {
                    noteDetails = dataSnapshot.getValue(NoteDetails.class);
                    noteDetails.setNoteID(dataSnapshot.getKey());
                    noteTitle.setText(noteDetails.getTitle());
                    noteBody.setText(noteDetails.getBody());
                    ImageURL = noteDetails.getImageUrl();
                    Picasso.get().load(noteDetails.getImageUrl()).into(noteImage);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("The read failed: " ,databaseError.getMessage());
            }

        });

    }
}
