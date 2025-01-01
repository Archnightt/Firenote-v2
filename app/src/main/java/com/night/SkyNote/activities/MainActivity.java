package com.night.SkyNote.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Query;

import com.night.SkyNote.R;
import com.night.SkyNote.adapters.NoteAdapter;
import com.night.SkyNote.listeners.NoteListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements NoteListener {

    /////////////////////
    // REQUEST CODES
    /////////////////////
    public static final int addNoteCode = 1;
    public static final int updateNoteCode = 2;
    private RecyclerView noteRecycler;
    private List<Map<String, Object>> noteList;
    private NoteAdapter noteAdapter;
    private int clickedNotePosition = -1;
    public int longClickedNotePosition = -2;
    public int noteCounter;

    private AlertDialog deleteNoteDialog;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ImageView profileImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();

        db = FirebaseFirestore.getInstance();

        // Check if the user is signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Initialize views
        profileImageView = findViewById(R.id.settingsButton);
        fetchAndDisplayProfilePicture();
        FloatingActionButton addNoteButton = findViewById(R.id.buttonAddNote);
        addNoteButton.setOnClickListener(v -> startActivityForResult(new Intent(getApplicationContext(), NoteEditorActivity.class), addNoteCode));
        findViewById(R.id.settingsButton).setOnClickListener(v -> showBottomSheetMenu());

        //Search pop-up functionality
        ImageView searchIcon = findViewById(R.id.searchIcon);
        searchIcon.setOnClickListener(v -> showSearchDialog());

        // Setup RecyclerView
        noteRecycler = findViewById(R.id.noteRecycler);
        noteRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        noteList = new ArrayList<>();
        noteAdapter = new NoteAdapter(noteList, this);
        noteRecycler.setAdapter(noteAdapter);


        // Start real-time listener for Firestore updates
        listenToNotesInFirestore();

        // Search bar functionality
        View searchDialogView = LayoutInflater.from(this).inflate(R.layout.search_dialog, null);
        EditText searchBar = searchDialogView.findViewById(R.id.searchBar);
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                noteAdapter.cancelTimer();
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!noteList.isEmpty()) {
                    noteAdapter.searchNotes(s.toString());
                }
            }
        });
    }

    // Search dialog method
    private void showSearchDialog() {
        Dialog searchDialog = new Dialog(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View searchDialogView = inflater.inflate(R.layout.search_dialog, null);
        searchDialog.setContentView(searchDialogView);

        EditText searchBar = searchDialogView.findViewById(R.id.searchBar);
        searchBar.requestFocus();

        searchDialog.show();
    }

    private void fetchAndDisplayProfilePicture() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.getPhotoUrl() != null) {
            Glide.with(this).load(currentUser.getPhotoUrl()).into(profileImageView);
        } else {
            profileImageView.setImageResource(R.drawable.ic_account);
        }
    }

    private void listenToNotesInFirestore() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        CollectionReference userNotesRef = db.collection("users").document(userId).collection("notes");

        userNotesRef.orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Failed to listen for updates: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots != null) {
                        noteList.clear();
                        for (QueryDocumentSnapshot document : snapshots) {
                            Map<String, Object> note = new HashMap<>(document.getData());
                            note.put("noteId", document.getId());
                            noteList.add(note);
                        }

                        // Update RecyclerView and note counter
                        noteAdapter.notifyItemRangeInserted(0, noteList.size());
                        noteCounterSetLabel();
                    }
                });
    }


    private void showBottomSheetMenu() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialog);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_menu, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        TextView accountSettings = bottomSheetView.findViewById(R.id.account_settings);
        TextView logout = bottomSheetView.findViewById(R.id.logout);

        accountSettings.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
        });

        logout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        bottomSheetDialog.show();
    }

    @Override
    public void noteClicked(Map<String, Object> note, int position) {
        clickedNotePosition = position;
        Intent intent = new Intent(getApplicationContext(), NoteEditorActivity.class);
        intent.putExtra("isNoteUpdated", true);
        intent.putExtra("noteId", (String) note.get("noteId"));
        startActivityForResult(intent, updateNoteCode);
    }

    @Override
    public void noteLongClicked(Map<String, Object> note, int position) {
        longClickedNotePosition = position;
        deleteNoteDialog(note, longClickedNotePosition);
        getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
    }

    // Assuming the position of the note to delete is passed
    private void deleteNoteDialog(Map<String, Object> note, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        View view = LayoutInflater.from(this).inflate(R.layout.delete_layout, null);
        builder.setView(view);
        AlertDialog deleteNoteDialog = builder.create();
        deleteNoteDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));

        view.findViewById(R.id.buttonDelete).setOnClickListener(v -> {
            String userId = mAuth.getCurrentUser().getUid();
            String noteId = (String) note.get("noteId");

            db.collection("users").document(userId).collection("notes").document(noteId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        // Remove the note from the adapter list
                        noteAdapter.deleteNoteAtPosition(position);
                        // Optionally update any other UI elements or counters
                        noteCounterSetLabel();
                        // Dismiss the dialog and reload activity if needed
                        deleteNoteDialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        deleteNoteDialog.dismiss();
                        Toast.makeText(this, "Failed to delete note!", Toast.LENGTH_SHORT).show();
                    });
        });

        view.findViewById(R.id.buttonCancel).setOnClickListener(v -> deleteNoteDialog.dismiss());
        deleteNoteDialog.show();
    }




    public void noteCounterSetLabel() {
        TextView noteCountView = findViewById(R.id.noteCount);
        noteCountView.setText(noteList.size() + " Notes" );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == addNoteCode) {
                // Handle adding a new note
                String noteId = data.getStringExtra("noteId");
                fetchNoteById(noteId);
            } else if (requestCode == updateNoteCode) {
                // Handle updating an existing note
                String noteId = data.getStringExtra("noteId");
                updateNoteInList(noteId);
            }
            Toast.makeText(this, "Changes applied successfully!", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchNoteById(String noteId) {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).collection("notes").document(noteId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> note = new HashMap<>(documentSnapshot.getData());
                        note.put("noteId", documentSnapshot.getId());
                        noteList.add(0, note);
                        noteAdapter.notifyItemInserted(0);
                        noteCounterSetLabel();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to fetch note!", Toast.LENGTH_SHORT).show());
    }

    private void updateNoteInList(String noteId) {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).collection("notes").document(noteId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> updatedNote = new HashMap<>(documentSnapshot.getData());
                        updatedNote.put("noteId", documentSnapshot.getId());
                        noteList.set(clickedNotePosition, updatedNote);
                        noteAdapter.notifyItemChanged(clickedNotePosition);
                        noteCounterSetLabel();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update note!", Toast.LENGTH_SHORT).show());
    }


    public void reloadActivity() {
        Intent intent = new Intent(MainActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
    }

}
