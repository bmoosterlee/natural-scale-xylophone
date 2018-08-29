import javafx.util.Builder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class CurrentTable<T> extends HashMap<Double, T> {
    private Builder<T> builder;

    public CurrentTable(Builder<T> builder) {
        this.builder = builder;
    }

    CurrentTable<T> getNewTable(Set<Double> frequencies) {
        CurrentTable<T> newCurrentTable = new CurrentTable<>(builder);
        for (Double frequency : frequencies) {
            if (containsKey(frequencies)) {
                newCurrentTable.put(frequency, get(frequency));
            } else {
                newCurrentTable.put(frequency, builder.build());
            }
        }
        return newCurrentTable;
    }
}