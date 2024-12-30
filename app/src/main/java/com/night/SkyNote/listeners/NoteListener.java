package com.night.SkyNote.listeners;

import java.util.Map;

// External listener for RecyclerView items (specific notes)
public interface NoteListener {
    void noteClicked(Map<String, Object> note, int position);

    void noteLongClicked(Map<String, Object> note, int position);
}
