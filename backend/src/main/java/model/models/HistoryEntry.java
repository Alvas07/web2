package model.models;

import java.io.Serial;
import java.io.Serializable;

public record HistoryEntry(Point point, boolean result, String now) implements Serializable {
    @Serial
    private static final long serialVersionUID = 215215125212651L;
}