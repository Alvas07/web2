package backend.servlets;

import backend.websocket.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.services.HistoryManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "ClearHistoryServlet", urlPatterns = {"/clearHistory"})
public class ClearHistoryServlet extends HttpServlet {
    private final ObjectMapper objectMapper;
    private final WebSocketSessionManager sessionManager = WebSocketSessionManager.getInstance();

    public ClearHistoryServlet() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            String sessionId = session.getId();
            HistoryManager historyManager = (HistoryManager) session.getAttribute("history");
            
            if (historyManager != null) {
                // Удаляем только точки текущей сессии
                historyManager.removeBySessionId(sessionId);
                session.setAttribute("historyRecords", historyManager.getAll());
            }

            try {
                Map<String, String> clearMessage = new HashMap<>();
                clearMessage.put("type", "clear");
                clearMessage.put("sessionId", sessionId);
                String jsonMessage = objectMapper.writeValueAsString(clearMessage);
                sessionManager.broadcast(jsonMessage, sessionId);
            } catch (Exception e) {
                System.err.println("Error broadcasting clear message: " + e.getMessage());
                e.printStackTrace();
            }
        }
        resp.sendRedirect(req.getContextPath() + "/controller?action=home");
    }
}
