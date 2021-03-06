package spectrum.harmonics;

import frequency.Frequency;

import java.util.HashMap;
import java.util.Set;

class CurrentTable<T> extends HashMap<Frequency, T> {
    private final Builder<T> builder;

    CurrentTable(Builder<T> builder) {
        this.builder = builder;
    }

    CurrentTable<T> update(Set<Frequency> frequencies) {
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