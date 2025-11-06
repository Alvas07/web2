package backend.websocket;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@ServerEndpoint("/websocket")
public class HitCheckerWebSocket {
    private final WebSocketSessionManager sessionManager = WebSocketSessionManager.getInstance();

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        // Получаем HTTP session ID из query параметров
        String queryString = session.getQueryString();
        String httpSessionId = null;
        
        System.out.println("WebSocket onOpen - Query string: " + queryString);
        
        if (queryString != null) {
            try {
                String[] params = queryString.split("&");
                for (String param : params) {
                    String[] keyValue = param.split("=", 2);
                    if (keyValue.length == 2 && "sessionId".equals(keyValue[0])) {
                        // Декодируем URL-encoded значение
                        httpSessionId = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                        System.out.println("Extracted HTTP session ID: " + httpSessionId);
                        break;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error parsing query string: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // Сохраняем HTTP session ID в UserProperties
        if (httpSessionId != null) {
            session.getUserProperties().put("httpSessionId", httpSessionId);
            System.out.println("Saved HTTP session ID to UserProperties");
        } else {
            System.err.println("WARNING: HTTP session ID not found in query string!");
        }
        
        sessionManager.addSession(session);
        System.out.println("WebSocket connection opened: WS=" + session.getId() + ", HTTP=" + httpSessionId + ", Total sessions: " + sessionManager.getAllSessions().size());
    }

    @OnClose
    public void onClose(Session session) {
        sessionManager.removeSession(session);
        System.out.println("WebSocket connection closed: " + session.getId());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.err.println("WebSocket error on session " + session.getId() + ": " + error.getMessage());
        error.printStackTrace();
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // Можно использовать для heartbeat или других сообщений
        System.out.println("Received message from " + session.getId() + ": " + message);
    }
}

