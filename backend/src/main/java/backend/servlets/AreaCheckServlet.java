package backend.servlets;

import backend.utils.CheckResult;
import backend.utils.RequestParser;
import backend.websocket.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vavr.control.Either;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.models.HistoryEntry;
import model.models.Point;
import model.services.AreaCheckService;
import model.services.HistoryManager;
import model.shapes.QuadrantShapeTemplate;
import model.shapes.factories.QuarterCircleFactory;
import model.shapes.factories.RectangleFactory;
import model.shapes.factories.TriangleFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@WebServlet(name = "AreaCheckServlet", urlPatterns = {"/areaCheck"})
public class AreaCheckServlet extends HttpServlet {
    private AreaCheckService areaCheckService;
    private final ObjectMapper objectMapper;
    private final WebSocketSessionManager sessionManager = WebSocketSessionManager.getInstance();

    public AreaCheckServlet() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void init() {
        List<QuadrantShapeTemplate> templates = List.of(
                new QuadrantShapeTemplate(new RectangleFactory(1.0, 1.0), 3),
                new QuadrantShapeTemplate(new TriangleFactory(1.0, 1.0), 4),
                new QuadrantShapeTemplate(new QuarterCircleFactory(1.0), 2)
        );
        areaCheckService = new AreaCheckService(templates);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long start = System.nanoTime();
        HttpSession session = req.getSession();
        LocalDateTime now = LocalDateTime.now();

        HistoryManager historyManager = (HistoryManager) session.getAttribute("history");
        if (historyManager == null) {
            historyManager = new HistoryManager();
            session.setAttribute("history", historyManager);
        }

        Either<String, List<Point>> parseResult = RequestParser.parse(req);

        if (parseResult.isLeft()) {
            req.setAttribute("errorMessage", parseResult.getLeft());
            req.getRequestDispatcher("/error.jsp").forward(req, resp);
        }

        List<Point> points = parseResult.get();
        List<CheckResult> checkResults = areaCheckService.checkPoints(points);

        double execTime = (System.nanoTime() - start) / 1e6;
        
        String sessionId = session.getId();

        List<HistoryEntry> newEntries = checkResults.stream()
                .map(cr -> new HistoryEntry(cr.point(), cr.hit(), now, execTime, sessionId))
                .toList();

        newEntries.forEach(historyManager::add);

        // Отправляем обновления через WebSocket всем остальным пользователям
        try {
            System.out.println("Broadcasting " + newEntries.size() + " entries via WebSocket (excluding HTTP session: " + sessionId + ")");
            for (HistoryEntry entry : newEntries) {
                String jsonMessage = objectMapper.writeValueAsString(entry);
                System.out.println("Broadcasting entry: " + jsonMessage);
                sessionManager.broadcast(jsonMessage, sessionId);
            }
        } catch (Exception e) {
            System.err.println("Error broadcasting WebSocket message: " + e.getMessage());
            e.printStackTrace();
        }

        req.setAttribute("execTime", execTime);
        req.setAttribute("results", newEntries);

        req.getRequestDispatcher("/result.jsp").forward(req, resp);
    }
 }
