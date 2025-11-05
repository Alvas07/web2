package backend.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.services.HistoryManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

@WebServlet(name = "RemovePointsServlet", urlPatterns = {"/removePoints"})
public class RemovePointsServlet extends HttpServlet {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        
        HistoryManager historyManager = (HistoryManager) session.getAttribute("history");
        if (historyManager == null) {
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write("{\"status\":\"ok\",\"message\":\"No history to clean\"}");
            return;
        }

        try {
            // Читаем JSON из тела запроса
            BufferedReader reader = req.getReader();
            StringBuilder jsonBody = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBody.append(line);
            }
            
            Map<String, String> requestData = objectMapper.readValue(jsonBody.toString(), Map.class);
            String sessionIdToRemove = requestData.get("sessionId");
            
            if (sessionIdToRemove != null && !sessionIdToRemove.isEmpty()) {
                // Удаляем точки из указанной сессии
                historyManager.removeBySessionId(sessionIdToRemove);
                session.setAttribute("historyRecords", historyManager.getAll());
                
                System.out.println("Removed points from session " + sessionIdToRemove + " for user " + session.getId());
            }
            
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write("{\"status\":\"ok\"}");
        } catch (Exception e) {
            System.err.println("Error removing points: " + e.getMessage());
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}

