package com.night.SkyNote.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.graphics.Typeface;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;

import com.night.SkyNote.R;

import java.util.HashMap;
import java.util.Map;

public class NoteEditorActivity extends AppCompatActivity {

    private EditText inputTitle;
    private EditText inputNote;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String noteId;
    private boolean isNoteUpdated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        inputTitle = findViewById(R.id.noteTitle);
        inputNote = findViewById(R.id.noteBody);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Back button functionality
        ImageView buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> {
            onBackPressed();
            closeKeyboard();
        });

        // Save note button functionality
        TextView saveNoteButton = findViewById(R.id.buttonSave);
        saveNoteButton.setOnClickListener(v -> {
            saveNote();
            closeKeyboard();
        });

        // Check if this is an existing note
        isNoteUpdated = getIntent().getBooleanExtra("isNoteUpdated", false);
        if (isNoteUpdated) {
            noteId = getIntent().getStringExtra("noteId"); // Pass noteId from MainActivity
            loadNoteForEditing();
        }

        // Text formatting buttons
        setupFormattingButtons();
    }

    // Load an existing note for editing
    private void loadNoteForEditing() {
        String userId = auth.getCurrentUser().getUid();
        DocumentReference noteRef = db.collection("users").document(userId).collection("notes").document(noteId);

        noteRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                inputTitle.setText(documentSnapshot.getString("noteTitle"));
                inputNote.setText(documentSnapshot.getString("noteBody"));
            } else {
                Toast.makeText(this, "Note not found!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to load note: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Save or update a note
    private void saveNote() {
        if (inputTitle.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Title is empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (inputNote.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Note is empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        CollectionReference userNotes = db.collection("users").document(userId).collection("notes");

        Map<String, Object> noteData = new HashMap<>();
        noteData.put("noteTitle", inputTitle.getText().toString());
        noteData.put("noteBody", inputNote.getText().toString());
        noteData.put("timestamp", System.currentTimeMillis());

        if (isNoteUpdated) {
            // Update existing note
            userNotes.document(noteId).set(noteData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Note updated successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to update note: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            // Create a new note
            userNotes.add(noteData)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to save note: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    // Hide the keyboard
    public void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    // Setup text formatting buttons
    private void setupFormattingButtons() {
        // Bold
        ImageView buttonBold = findViewById(R.id.buttonBold);
        buttonBold.setOnClickListener(v -> formatText(Typeface.BOLD));

        // Italic
        ImageView buttonItalic = findViewById(R.id.buttonItalic);
        buttonItalic.setOnClickListener(v -> formatText(Typeface.ITALIC));

        // Underline
        ImageView buttonUnderline = findViewById(R.id.buttonUnderline);
        buttonUnderline.setOnClickListener(v -> underlineText());
    }

    // Format text (bold or italic)
    private void formatText(int style) {
        int start = inputNote.getSelectionStart();
        int end = inputNote.getSelectionEnd();
        if (start < 0 || end < 0) return;

        Spannable spannable = new SpannableStringBuilder(inputNote.getText());
        spannable.setSpan(new StyleSpan(style), start, end, 0);
        inputNote.setText(spannable);
    }

    // Underline text
    private void underlineText() {
        int start = inputNote.getSelectionStart();
        int end = inputNote.getSelectionEnd();
        if (start < 0 || end < 0) return;

        Spannable spannable = new SpannableStringBuilder(inputNote.getText());
        spannable.setSpan(new UnderlineSpan(), start, end, 0);
        inputNote.setText(spannable);
    }
}
