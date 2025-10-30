package model.models;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

public record HistoryEntry(Point point, boolean result, LocalDateTime now, double execTime) implements Serializable {
    @Serial
    private static final long serialVersionUID = 215215125212651L;
}