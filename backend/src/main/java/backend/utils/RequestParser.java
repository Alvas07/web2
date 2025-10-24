package backend.utils;

import jakarta.servlet.http.HttpServletRequest;
import model.models.Point;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RequestParser {
    public record ParseResult(List<Point> points, String errorMessage) implements Serializable {
        @Serial
        private static final long serialVersionUID = 51298571985983L;

        public boolean hasError() {
            return errorMessage != null;
        }
    }

    private static final double EPS = 1e-9;

    private static boolean equalsDouble(double a, double b) {
        return Math.abs(a - b) < EPS;
    }

    public static ParseResult parse(HttpServletRequest req) {
        String[] xParams = req.getParameterValues("x");
        String yParam = req.getParameter("y");
        String rParam = req.getParameter("r");

        if (xParams == null || xParams.length == 0) {
            return new ParseResult(null, "Не выбрано ни одного значения X");
        }
        if (yParam == null || yParam.isBlank()) {
            return new ParseResult(null, "Поле Y не заполнено");
        }
        if (rParam == null || rParam.isBlank()) {
            return new ParseResult(null, "Не выбрано значение R");
        }

        double y;
        double r;
        try {
            y = Double.parseDouble(yParam.trim());
        } catch (NumberFormatException e) {
            return new ParseResult(null, "Некорректный формат Y");
        }
        try {
            r = Double.parseDouble(rParam.trim());
        } catch (NumberFormatException e) {
            return new ParseResult(null, "Некорректный формат R");
        }

        if (y < -3.0 || y > 3.0) {
            return new ParseResult(null, "Y должен быть в диапазоне [-3; 3]");
        }
        if (!equalsDouble(r, 1.0) && !equalsDouble(r, 1.5) && !equalsDouble(r, 2.0) && !equalsDouble(r, 2.5) && !equalsDouble(r, 3.0)) {
            return new ParseResult(null, "Недопустимое значение R");
        }

        List<Point> points = new ArrayList<>();
        for (String xs : xParams) {
            if (xs == null) continue;
            try {
                double x = Double.parseDouble(xs.trim());
                if (x < -3 || x > 5) {
                    return new ParseResult(null, "Недопустимое значение X");
                }
                points.add(new Point(x, y, r));
            } catch (NumberFormatException ignored) {}
        }

        if (points.isEmpty()) {
            return new ParseResult(null, "Некорректный формат X");
        }

        return new ParseResult(points, null);
    }
}
