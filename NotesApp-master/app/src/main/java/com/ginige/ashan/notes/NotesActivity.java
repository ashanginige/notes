package com.ginige.ashan.notes;

import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ginige.ashan.notes.Adapters.NotesAdapter;
import com.ginige.ashan.notes.Models.NoteDetails;
import com.ginige.ashan.notes.Models.User;
import com.ginige.ashan.notes.Util.Constants;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class NotesActivity extends AppCompatActivity {

    Button button;
    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener mAuthStateListener;
    FloatingActionButton newNoteButton;
    boolean doubleBackToExitPressedOnce = false;
    private GoogleApiClient mGoogleApiClient;
    private ArrayList<NoteDetails> Notes;
    private DatabaseReference mDatabase;
    public User user;
    private NotesAdapter adapter;
    private RecyclerView notesRecyclerView;

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                finishAndRemoveTask();
            } else {
                finish();
            }
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Snackbar.make(findViewById(R.id.noteActivityMainLayout), "Please click BACK again and EXIT", Snackbar.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.notes_action_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuSignOut:
                sendToLogin();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        button = findViewById(R.id.signOutButton);
        newNoteButton = findViewById(R.id.newNoteButton);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            user = new User(mAuth.getCurrentUser().getUid());
        }

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() == null) {
                    startActivity(new Intent(NotesActivity.this, MainActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
                    finish();
                }
            }
        };

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //todo remove this button from the code and layout
            }
        });

        newNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(NotesActivity.this, NoteDetailsActivity.class)
                        .putExtra(Constants.NOTE_PROCESS, Constants.NEW_NOTE_VALUE));
            }
        });

        getData(user.getId());

        doFirstRun();
    }

    DatabaseReference notesDatabaseRef;

    private void addNewNote(String userId, String title, String body, Date date, String imageURL) {
        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.UK);
        notesDatabaseRef = mDatabase.child("users").child(userId).child("notes").push();
        NoteDetails noteDetails = new NoteDetails(title, body, formatter.format(date), imageURL);
        notesDatabaseRef.setValue(noteDetails);
    }

    private void doFirstRun() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isFirstRun = preferences.getBoolean("first", true);

        if (isFirstRun) {
            String body1 = "Two months ago, my brain was rewired. The first thing I remember about the night it happened was hearing the tin rattle. I was in a part of LA that I would never find on my own again — indeed, a Neverland for artistic expression. As I approached a dimly lit desk, I noticed a sign that said “No Pictures Allowed”; this wasn’t the kind of spot LA bloggers flocked to for the perfect Instagram shot. The warehouse behind it appeared to be breathing — inhaling with each kick — expanding and contracting its metal frame. I wondered how I was going to survive the next five hours inside the belly of a beast that was so abrasive and monotonous on first impression. But I was aware of my own musical naiveté — and I knew that cleansing the crevices of my brain of its dependence on Western music would not happen overnight. In fact, if it was going to happen at all, it had to begin here.";
            String body2 = "As you can see making a record ain't easy, but maybe that’s the point.  That said, as a collector I’m happy to pay full price plus shipping to enjoy a special vinyl release. Each record needs to be planned and well thought out along the above considerations. In that sense it makes the vinyl even more worthwhile knowing the efforts involved.  I’m happy to release music on vinyl, custom CD, but also digitally because peoples listening environments change. For example I stream music all day or play CDs in the car and enjoy vinyl at night. Long format listening is just easier with a CD or digital. Releasing vinyl with Silent Season has been for the most part a break even venture. It is a labour of love to so speak. Anything less than 300 copies and you’ll start to lose money. No one I know is getting rich selling vinyl.";
            String title1 = "Techno is Rewiring Silicon Valley";
            String title2 = "Dub Techno & Vinyl";
            addNewNote(user.getId(), title1, body1, Calendar.getInstance().getTime(), null);
            addNewNote(user.getId(), title2, body2, Calendar.getInstance().getTime(), null);
            preferences.edit().putBoolean("first", false).apply();
        }
    }


    private void getData(String userId) {
        Notes = new ArrayList<>();
        final DatabaseReference notesDatabaseRef = mDatabase.child("users").child(userId).child("notes");
        notesDatabaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Notes.clear();
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {

                    NoteDetails noteDetails = postSnapshot.getValue(NoteDetails.class);
                    noteDetails.setNoteID(postSnapshot.getKey());
                    Notes.add(noteDetails);

                }
                notesRecyclerView = findViewById(R.id.notesRecyclerView);
                notesRecyclerView.setLayoutManager(new LinearLayoutManager(NotesActivity.this));
                adapter = new NotesAdapter(NotesActivity.this, Notes, notesDatabaseRef);
                notesRecyclerView.setAdapter(adapter);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("The read failed: ", databaseError.getMessage());
            }
        });
    }

    private void sendToLogin() {
        GoogleSignInClient mGoogleSignInClient;
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(getBaseContext(), gso);
        mGoogleSignInClient.signOut().addOnCompleteListener(NotesActivity.this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        FirebaseAuth.getInstance().signOut();
                        Intent setupIntent = new Intent(getBaseContext(), MainActivity.class);
                        setupIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(setupIntent);
                        finish();
                    }
                });
    }


}
