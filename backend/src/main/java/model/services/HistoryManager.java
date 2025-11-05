package model.services;

import model.models.HistoryEntry;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class HistoryManager implements Serializable {
    @Serial
    private static final long serialVersionUID = 125712582917519L;

    private final Deque<HistoryEntry> history = new ArrayDeque<>();

    public void add(HistoryEntry entry) {
        history.addFirst(entry);
    }

    public List<HistoryEntry> getAll() {
        return new ArrayList<>(history);
    }

    public void clear() {
        history.clear();
    }

    public void removeBySessionId(String sessionId) {
        history.removeIf(entry -> entry.sessionId() != null && entry.sessionId().equals(sessionId));
    }
}
