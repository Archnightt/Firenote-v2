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
        return new NoteViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.note_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, final int position) {
        holder.setNote(notes.get(position));
        holder.noteLayout.setOnClickListener(v -> noteListener.noteClicked(notes.get(position), position));
        holder.noteLayout.setOnLongClickListener(v -> {
            noteListener.noteLongClicked(notes.get(position), position);
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

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView noteLayoutTitle;
        TextView noteLayoutBody;
        LinearLayout noteLayout;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            noteLayoutTitle = itemView.findViewById(R.id.noteLayoutTitle);
            noteLayoutBody = itemView.findViewById(R.id.noteLayoutBody);
            noteLayout = itemView.findViewById(R.id.noteLayout);
        }

        void setNote(Map<String, Object> note) {
            // Set the title and body from Firestore data
            noteLayoutTitle.setText((String) note.get("noteTitle"));
            noteLayoutBody.setText(StringUtils.abbreviate((String) note.get("noteBody"), 120));
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
