package com.night.SkyNote.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
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

import com.night.SkyNote.R;
import com.night.SkyNote.database.NotepadDatabase;
import com.night.SkyNote.entities.NoteEntity;

public class NoteEditorActivity extends AppCompatActivity {

    private EditText inputTitle;
    private EditText inputNote;
    private NoteEntity noteAlreadyExists;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String docid;
    private Bundle bundle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);


        inputTitle = findViewById(R.id.noteTitle);
        inputNote = findViewById(R.id.noteBody);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // back button
        ImageView buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
                closeKeyboard();
            }
        });


        // note save button
        TextView saveNoteButton = findViewById(R.id.buttonSave);
        saveNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //invokes the method of saving a note when pressed
                saveNote();
                closeKeyboard();
            }
        });

        if (getIntent().getBooleanExtra("isNoteUpdated", false)) {
            noteAlreadyExists = (NoteEntity) getIntent().getSerializableExtra("note");
            setNoteUpdate();
        }


        // TODO: 26/06/2021 find a way to store formatting, it does not save in android room
// bold button
        ImageView buttonBold = findViewById(R.id.buttonBold);
        buttonBold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int start = inputNote.getSelectionStart();
                int end = inputNote.getSelectionEnd();
                if (start < 0 || end < 0) {
                    return;
                }

                Spannable spannable = new SpannableStringBuilder(inputNote.getText());
                spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, 0);
                inputNote.setText(spannable);
            }
        });

// italics button
        ImageView buttonItalic = findViewById(R.id.buttonItalic);
        buttonItalic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int start = inputNote.getSelectionStart();
                int end = inputNote.getSelectionEnd();
                if (start < 0 || end < 0) {
                    return;
                }

                Spannable spannable = new SpannableStringBuilder(inputNote.getText());
                spannable.setSpan(new StyleSpan(Typeface.ITALIC), start, end, 0);
                inputNote.setText(spannable);
            }
        });

// underline button
        ImageView buttonUnderline = findViewById(R.id.buttonUnderline);
        buttonUnderline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int start = inputNote.getSelectionStart();
                int end = inputNote.getSelectionEnd();
                if (start < 0 || end < 0) {
                    return;
                }

                Spannable spannable = new SpannableStringBuilder(inputNote.getText());
                spannable.setSpan(new UnderlineSpan(), start, end, 0);
                inputNote.setText(spannable);
            }
        });




        // by default, the note is not updated, hence the value false
        // Serializable	the value of an item previously added with putExtra(), or null if no Serializable value was found
        if (getIntent().getBooleanExtra("isNoteUpdated", false)) {
            noteAlreadyExists = (NoteEntity)getIntent().getSerializableExtra("note");

            // calls the note update method
            setNoteUpdate();
        }
    }


    ////////////////////////
    // METODA HIDE KEYBOARD
    ////////////////////////
    // so that it doesn't restart when returning to MainActivity
    public void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }


    ///////////////////////////////
    // NOTE UPDATE METHOD
    ///////////////////////////////
    private void setNoteUpdate() {

        // inserts a new title and text into a note that already exists (noteAlreadyExists to NoteEntity!)
        inputTitle.setText(noteAlreadyExists.getNoteTitle());
        inputNote.setText(noteAlreadyExists.getNoteBody());
    }


    ///////////////////////////
    // METHOD OF RECORDING NOTES
    ///////////////////////////
    private void saveNote() {
        // trim shortens unnecessary spaces, the entire statement checks whether the title is empty
        // if it is empty, a message is displayed
        if (inputTitle.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Title is empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        // checks whether the note content is empty, displays a message
        else if (inputNote.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Note is empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the currently logged-in user
        String userId = auth.getCurrentUser().getUid();

        // Create a reference to the user's collection
        CollectionReference userNotes = db.collection("users").document(userId).collection("notes");

        // if the note contains a title and text, it creates a NoteEntity with what was entered
        final NoteEntity noteEntity = new NoteEntity();
        noteEntity.setNoteTitle(inputTitle.getText().toString());
        noteEntity.setNoteBody(inputNote.getText().toString());


        // if the note already exists, it means that it is being modified and a new ID is set for it
        if (noteAlreadyExists != null) {
            noteEntity.setNoteID(noteAlreadyExists.getNoteID());
        }



        /////////////////////
        // WRITE A NOTE
        /////////////////////
        @SuppressLint("StaticFieldLeak")
        class SaveNoteClass extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... voids) {
                // asynctask creates a note in the database
                NotepadDatabase.getNoteDatabase(getApplicationContext()).notepadDao().createNote(noteEntity);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                // https://developer.android.com/reference/android/content/Intent
                //an intent is created, the result of which is later passed to invoke a specific request code (adding or modifying a note)
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        }

        new SaveNoteClass().execute();
    }
}