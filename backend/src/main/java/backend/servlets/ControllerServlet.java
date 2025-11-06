package backend.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.services.HistoryManager;

import java.io.IOException;

@WebServlet(name = "ControllerServlet", urlPatterns = {"/controller"})
public class ControllerServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();

        HistoryManager historyManager = (HistoryManager) session.getAttribute("history");
        if (historyManager == null) {
            historyManager = new HistoryManager();
            session.setAttribute("history", historyManager);
        }

        session.setAttribute("historyRecords", historyManager.getAll());

        String action = req.getParameter("action");

        if (action == null || action.equals("home")) {
            req.getRequestDispatcher("/index.jsp").forward(req, resp);
        }

        switch (action) {
            case "check" -> req.getRequestDispatcher("/areaCheck").forward(req, resp);
            case "clear" -> req.getRequestDispatcher("/clearHistory").forward(req, resp);
            default -> {
                req.setAttribute("errorMessage", "Неизвестное действие: " + action);
                req.getRequestDispatcher("/error.jsp").forward(req, resp);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

}
