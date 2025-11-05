package backend.servlets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.models.HistoryEntry;
import model.models.Point;
import model.services.HistoryManager;

import java.io.IOException;
import java.time.LocalDateTime;

@WebServlet(name = "AddPointServlet", urlPatterns = {"/addPoint"})
public class AddPointServlet extends HttpServlet {
    private final ObjectMapper objectMapper;

    public AddPointServlet() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        
        HistoryManager historyManager = (HistoryManager) session.getAttribute("history");
        if (historyManager == null) {
            historyManager = new HistoryManager();
            session.setAttribute("history", historyManager);
        }

        try {
            // Читаем JSON из тела запроса
            HistoryEntry entry = objectMapper.readValue(req.getReader(), HistoryEntry.class);
            
            // Добавляем точку в историю (даже если она чужая)
            historyManager.add(entry);
            
            // Обновляем historyRecords в сессии
            session.setAttribute("historyRecords", historyManager.getAll());
            
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write("{\"status\":\"ok\"}");
        } catch (Exception e) {
            System.err.println("Error adding point: " + e.getMessage());
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}

