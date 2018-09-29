package harmonics;

import frequency.Frequency;

import java.util.HashMap;
import java.util.Set;

public class CurrentTable<T> extends HashMap<Frequency, T> {
    private Builder<T> builder;

    public CurrentTable(Builder<T> builder) {
        this.builder = builder;
    }

    CurrentTable<T> getNewTable(Set<Frequency> frequencies) {
        CurrentTable<T> newCurrentTable = new CurrentTable<>(builder);
        for (Frequency frequency : frequencies) {
            if (containsKey(frequencies)) {
                newCurrentTable.put(frequency, get(frequency));
            } else {
                newCurrentTable.put(frequency, builder.build());
            }
        }
        return newCurrentTable;
    }
}