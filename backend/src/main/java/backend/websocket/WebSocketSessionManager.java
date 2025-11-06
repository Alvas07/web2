package backend.websocket;

import jakarta.websocket.Session;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketSessionManager {
    private static final WebSocketSessionManager instance = new WebSocketSessionManager();
    private final Set<Session> sessions = ConcurrentHashMap.newKeySet();

    private WebSocketSessionManager() {
    }

    public static WebSocketSessionManager getInstance() {
        return instance;
    }

    public void addSession(Session session) {
        sessions.add(session);
    }

    public void removeSession(Session session) {
        sessions.remove(session);
    }

    public Set<Session> getAllSessions() {
        return Collections.unmodifiableSet(sessions);
    }

    public void broadcast(String message, String excludeHttpSessionId) {
        if (sessions.isEmpty()) {
            System.out.println("No WebSocket sessions connected. Cannot broadcast message.");
            return;
        }
        
        int sentCount = 0;
        int skippedCount = 0;
        
        for (Session session : sessions) {
            try {
                if (!session.isOpen()) {
                    System.out.println("Session " + session.getId() + " is closed, skipping");
                    continue;
                }
                
                // Получаем HTTP session ID из UserProperties WebSocket сессии
                String httpSessionId = (String) session.getUserProperties().get("httpSessionId");
                
                System.out.println("Checking session " + session.getId() + ": HTTP session = " + httpSessionId + ", exclude = " + excludeHttpSessionId);
                
                // Если это не та же HTTP сессия, отправляем сообщение
                boolean shouldSend = httpSessionId == null || !httpSessionId.equals(excludeHttpSessionId);
                
                if (shouldSend) {
                    try {
                        // Используем асинхронную отправку
                        session.getAsyncRemote().sendText(message);
                        sentCount++;
                        System.out.println("✓ Sent message to WebSocket session " + session.getId() + " (HTTP session: " + httpSessionId + ")");
                    } catch (Exception sendError) {
                        System.err.println("✗ Failed to send to session " + session.getId() + ": " + sendError.getMessage());
                        sendError.printStackTrace();
                    }
                } else {
                    skippedCount++;
                    System.out.println("- Skipped sending to own session (HTTP session: " + httpSessionId + " == " + excludeHttpSessionId + ")");
                }
            } catch (Exception e) {
                System.err.println("Error broadcasting to session " + session.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("Broadcast complete: " + sentCount + " sent, " + skippedCount + " skipped, " + sessions.size() + " total sessions");
    }
}

