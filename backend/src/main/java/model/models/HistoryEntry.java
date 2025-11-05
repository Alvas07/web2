package model.models;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

public record HistoryEntry(
        Point point,
        boolean result,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime now,
        double execTime,
        String sessionId
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 215215125212651L;

    public HistoryEntry(Point point, boolean result, LocalDateTime now, double execTime) {
        this(point, result, now, execTime, null);
    }
}