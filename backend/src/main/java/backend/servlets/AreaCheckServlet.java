package backend.servlets;

import backend.utils.CheckResult;
import backend.utils.RequestParser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.models.HistoryEntry;
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

        RequestParser.ParseResult pr = RequestParser.parse(req);
        if (pr.hasError()) {
            req.setAttribute("errorMessage", pr.errorMessage());
            req.getRequestDispatcher("/error.jsp").forward(req, resp);
        }

        List<CheckResult> checkResults = areaCheckService.checkPoints(pr.points());

        double execTime = (System.nanoTime() - start) / 1e6;

        List<HistoryEntry> newEntries = checkResults.stream().map(cr -> new HistoryEntry(cr.point(), cr.hit(), now, execTime)).toList();

        newEntries.forEach(historyManager::add);

        req.setAttribute("execTime", execTime);
        req.setAttribute("results", newEntries);

        req.getRequestDispatcher("/result.jsp").forward(req, resp);
    }
 }
