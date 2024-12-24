package com.night.SkyNote.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.night.SkyNote.R;
import com.night.SkyNote.adapters.NoteAdapter;
import com.night.SkyNote.database.NotepadDatabase;
import com.night.SkyNote.entities.NoteEntity;
import com.night.SkyNote.listeners.NoteListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NoteListener {

    /////////////////////
    // REQUEST CODES
    /////////////////////
    public static final int addNoteCode = 1;
    public static final int updateNoteCode = 2;
    public static final int displayNotesCode = 3;

    private RecyclerView noteRecycler;
    private List<NoteEntity> noteList;
    private NoteAdapter noteAdapter;
    private int clickedNotePosition = -1;        //responsible for the currently clicked note
    public int longClickedNotePosition = -2;     //responsible for the currently long-clicked note
    public int noteCounter;                      // is responsible for the note counter

    private AlertDialog deleteNoteDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // add note button
        FloatingActionButton addNoteButton = findViewById(R.id.buttonAddNote);
        addNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(getApplicationContext(), NoteEditorActivity.class), addNoteCode);
            }
        });

        FloatingActionButton settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });



        // setting the recycler layout, in this case vertical linearlayout
        noteRecycler = findViewById(R.id.noteRecycler);
        noteRecycler.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        );


        noteList = new ArrayList<>();
        noteAdapter = new NoteAdapter(noteList, this);
        noteRecycler.setAdapter(noteAdapter);


        // getNote method is called at the start of the application (onCreate)
        // a request code is passed, which is responsible for displaying all notes at the beginning
        getNote(displayNotesCode);


        // is responsible for the operation of the search bar
        EditText searchBar = findViewById(R.id.searchBar);
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // when the text changes, the timer is reset (so that it does not search immediately)
                noteAdapter.cancelTimer();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // uses the note search method only when the list is not empty
                if (noteList.size() != 0) {
                    noteAdapter.searchNotes(s.toString());
                }
            }
        });
    }


    /////////////////////////////
    // NOTE CLICK METHOD
    /////////////////////////////
    @Override
    public void noteClicked(NoteEntity noteEntity, int position) {
        clickedNotePosition = position;
        Intent intent = new Intent(getApplicationContext(), NoteEditorActivity.class);
        intent.putExtra("isNoteUpdated", true);
        intent.putExtra("note", noteEntity);
        startActivityForResult(intent, updateNoteCode);
    }


    //////////////////////////////////////
    // LONG NOTE CLICK METHOD
    //////////////////////////////////////
    @Override
    public void noteLongClicked(NoteEntity noteEntity, int position) {
        longClickedNotePosition = position;
//        Toast.makeText(this, String.valueOf(position) ,Toast.LENGTH_SHORT).show();


//        works, convert it to a dialog box
////
//        AsyncTask.execute(() -> NotepadDatabase.getNoteDatabase(getApplicationContext()).notepadDao().deleteNote(noteEntity));
//        noteCounter--;
//        noteCounterSetLabel();
//        noteAdapter.notifyDataSetChanged();

        deleteDialog(noteEntity, longClickedNotePosition);
        getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
    }


    //////////////////////////////////////
    // METHOD OF SHOWING THE DELETION DIALOGUE
    //////////////////////////////////////
    private void deleteDialog(NoteEntity noteEntity, int position) {
        if (deleteNoteDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.delete_layout, (ViewGroup) findViewById(R.id.layoutDeleteNoteBox));

            builder.setView(view);
            deleteNoteDialog = builder.create();

            if (deleteNoteDialog.getWindow() != null) {
                deleteNoteDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }


            // blocks other exit options from alertdialog except buttons
            deleteNoteDialog.setCancelable(false);
            deleteNoteDialog.setCanceledOnTouchOutside(false);


            view.findViewById(R.id.buttonDelete).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    // regular asynctask caused errors, lambda works
                    // problem with deleting multiple notes from the database, you can only delete one note at a time, the rest are only deleted visually
                    // todo temporarily resolved by reloading MainActivity
                    AsyncTask.execute(() -> NotepadDatabase.getNoteDatabase(getApplicationContext()).notepadDao().deleteNote(noteEntity));


                    // removes a specific note from the list, decrements the counter, sets the note label, informs about changes in the database
                    noteList.remove(position);
                    noteCounter--;
                    noteCounterSetLabel();
                    noteAdapter.notifyDataSetChanged();


                    // reloads the recycler and ends the dialog
                    deleteNoteDialog.dismiss();


                    // reloads MainActivity, temporary
                    reloadActivity();
                }
            });

            view.findViewById(R.id.buttonCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteNoteDialog.dismiss();
                    reloadActivity();
                }
            });
        }

        // shows alertdialog
        deleteNoteDialog.show();

        // positions it at the bottom for convenience
        // https://www.geeksforgeeks.org/how-to-change-the-position-of-alertdialog-in-android/
        deleteNoteDialog.getWindow().setGravity(Gravity.BOTTOM);
    }


    /////////////////////
    // METHOD DELETENOTE
    /////////////////////
    // todo needs to be corrected, there are errors unlike lambda
    private void deleteNote(NoteEntity noteEntity, int position) {

        @SuppressLint("StaticFieldLeak")
        class DeleteNoteClass extends AsyncTask<Void, Void, Void> {

            @Override
            protected Void doInBackground(Void... voids) {
                NotepadDatabase.getNoteDatabase(getApplicationContext()).notepadDao().deleteNote(noteEntity);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                noteList.remove(position);
                noteCounter--;
                noteCounterSetLabel();
                noteAdapter.notifyDataSetChanged();
            }
        }

        new DeleteNoteClass().execute();
    }


    ////////////////////////////
    // METHOD RELOAD ACTIVITY
    ////////////////////////////
    public void reloadActivity() {
        Intent intent = new Intent(MainActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
    }


    ////////////////////////////
    // METODA NOTECOUNTER LABEL
    ////////////////////////////
    public void noteCounterSetLabel() {
        TextView noteCountView = findViewById(R.id.noteCount);
        noteCountView.setText(noteCounter + " notes");
    }


    /////////////////////
    // METHOD GETNOTE
    /////////////////////
    private void getNote(final int request) {

        @SuppressLint("StaticFieldLeak")
        // https://developer.android.com/reference/android/os/AsyncTask
        class SaveNoteClass extends AsyncTask<Void, Void, List<NoteEntity>> {

            // returns notes from room database
            @Override
            protected List<NoteEntity> doInBackground(Void... voids) {
                return NotepadDatabase.getNoteDatabase(getApplicationContext()).notepadDao().getAllNotes();
            }

            @Override
            protected void onPostExecute(List<NoteEntity> noteEntities) {

                super.onPostExecute(noteEntities);

                switch (request) {
                    case displayNotesCode: {
                        noteList.addAll(noteEntities);
                        noteAdapter.notifyDataSetChanged();
                        noteCounter = noteList.size();

                        noteCounterSetLabel();
                        break;
                    }

                    case addNoteCode: {
                        noteList.add(0, noteEntities.get(0));

                        // informs about changing the note in a specific position to avoid using notifyDataSetChanged, which informs about changing the whole
                        noteAdapter.notifyItemInserted(0);

                        // automatically scrolls the recyclerview of notes to the beginning
                        noteRecycler.getLayoutManager().scrollToPosition(0);

                        noteCounter++;
                        noteCounterSetLabel();
                        reloadActivity();
                        break;
                    }

                    case updateNoteCode: {

                        // when a note is modified, it deletes the previous one and inserts a new one in the same place
                        noteList.remove(clickedNotePosition);
                        noteList.add(clickedNotePosition, noteEntities.get(clickedNotePosition));
                        noteAdapter.notifyItemChanged(clickedNotePosition);
                        reloadActivity();
                        break;
                    }
                }
            }
        }

        new SaveNoteClass().execute();
    }


    @Override
    // https://developer.android.com/training/basics/intents/result
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // if a new note has been created, the request code for adding a single note is passed to the onActivityResult method
        // for okzai, the result code of the previous activity (NoteEditor) must equal RESULT_OK
        // when the application crashed, the code would be RESULT_CANCELED
        if (requestCode == addNoteCode && resultCode == RESULT_OK) {
            getNote(addNoteCode);
        }

        // if the note already existed, then as a result NoteEditorActivity receives a request code for updating a specific note
        else if (requestCode == updateNoteCode && resultCode == RESULT_OK) {

            // getNote is only called when the onActivityResult data is not empty
            // data element is the intent that activity returned as response parameters
            if (data != null) {
                getNote(updateNoteCode);
            }
        }
    }


}