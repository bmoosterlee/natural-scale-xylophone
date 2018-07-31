import javafx.util.Builder;

import java.util.HashMap;
import java.util.HashSet;

public class CurrentTable<T> extends HashMap<Note, T> {
    private Builder<T> builder;

    public CurrentTable(Builder<T> builder) {
        this.builder = builder;
    }

    CurrentTable<T> getNewTable(HashSet<Note> liveNotes) {
        CurrentTable<T> newCurrentTable = new CurrentTable<>(builder);
        for (Note note : liveNotes) {
            if (containsKey(note)) {
                newCurrentTable.put(note, get(note));
            } else {
                newCurrentTable.put(note, builder.build());
            }
        }
        return newCurrentTable;
    }
}