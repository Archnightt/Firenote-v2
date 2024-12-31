package com.night.SkyNote.adapters;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.night.SkyNote.R;
import com.night.SkyNote.listeners.NoteListener;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private List<Map<String, Object>> notes;
    private NoteListener noteListener;
    private Timer timer;
    private List<Map<String, Object>> notesSearchedList;

    public NoteAdapter(List<Map<String, Object>> notes, NoteListener noteListener) {
        this.notes = notes;
        this.noteListener = noteListener;
        notesSearchedList = new ArrayList<>(notes);
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_item, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, final int position) {
        Map<String, Object> note = notes.get(position);

        // Fetch note body directly from the Map or Firestore
        String title = note.get("noteTitle") != null ? note.get("noteTitle").toString() : "Untitled";
        String content = note.get("noteBody") != null ? note.get("noteBody").toString() : "";

        holder.noteTitle.setText(title);
        holder.noteContent.setText(content);

// Adjust height based on content length
        int contentLength = content.length();
        ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();

// Set height dynamically for long content; use WRAP_CONTENT for all cases to avoid cropping
        if (contentLength > 100) {
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT; // Allow dynamic resizing for longer content
        } else {
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT; // Short notes should also wrap their content
        }

        holder.itemView.setLayoutParams(params);


        // Handle click listeners as before
        holder.itemView.setOnClickListener(v -> noteListener.noteClicked(note, position));
        holder.itemView.setOnLongClickListener(v -> {
            noteListener.noteLongClicked(note, position);
            return true;
        });
    }


    @Override
    public int getItemCount() {
        return notes.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView noteTitle;
        TextView noteContent;
        LinearLayout noteLayout;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            noteTitle = itemView.findViewById(R.id.noteTitle);
            noteContent = itemView.findViewById(R.id.noteContent);
            noteLayout = itemView.findViewById(R.id.noteLayout);
            noteContent = itemView.findViewById(R.id.noteContent);
        }

        void setNote(Map<String, Object> note) {
            // Set the title and body from Firestore data
            noteTitle.setText((String) note.get("noteTitle"));
            noteContent.setText(StringUtils.abbreviate((String) note.get("noteBody"), 120));
        }
    }

    public void searchNotes(final String keyword) {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (keyword.trim().isEmpty()) {
                    notes = new ArrayList<>(notesSearchedList);
                } else {
                    List<Map<String, Object>> temporaryNotes = new ArrayList<>();
                    for (Map<String, Object> note : notesSearchedList) {
                        String title = (String) note.get("noteTitle");
                        String body = (String) note.get("noteBody");

                        if ((title != null && title.toLowerCase().contains(keyword.toLowerCase())) ||
                                (body != null && body.toLowerCase().contains(keyword.toLowerCase()))) {
                            temporaryNotes.add(note);
                        }
                    }
                    notes = temporaryNotes;
                }

                new Handler(Looper.getMainLooper()).post(() -> notifyDataSetChanged());
            }
        }, 400);
    }

    public void cancelTimer() {
        if (timer != null) {
            timer.cancel();
        }
    }
}
