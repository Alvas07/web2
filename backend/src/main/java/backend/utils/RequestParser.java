package backend.utils;

import io.vavr.control.Either;
import jakarta.servlet.http.HttpServletRequest;
import model.models.Point;

import java.util.ArrayList;
import java.util.List;

public class RequestParser {

    private static final double EPS = 1e-9;

    private static boolean equalsDouble(double a, double b) {
        return Math.abs(a - b) < EPS;
    }

    public static Either<String, List<Point>> parse(HttpServletRequest req) {
        boolean fromGraph = "true".equalsIgnoreCase(req.getParameter("fromGraph"));

        String[] xParams = req.getParameterValues("x");
        String yParam = req.getParameter("y");
        String rParam = req.getParameter("r");

        double y;
        double r;
        try {
            if (yParam == null || yParam.isBlank()) return Either.left("Поле Y не заполнено");
            y = Double.parseDouble(yParam.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return Either.left("Некорректный формат Y");
        }

        try {
            if (rParam == null || rParam.isBlank()) return Either.left("Не выбрано значение R");
            r = Double.parseDouble(rParam.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return Either.left("Некорректный формат R");
        }

        List<Point> points = new ArrayList<>();
        if (fromGraph) {
            double axisMin = -6;
            double axisMax = 6;
            String axisMinParam = req.getParameter("axisMin");
            String axisMaxParam = req.getParameter("axisMax");
            try {
                if (axisMinParam != null) axisMin = Double.parseDouble(axisMinParam);
                if (axisMaxParam != null) axisMax = Double.parseDouble(axisMaxParam);
            } catch (NumberFormatException ignored) {}

            if (xParams == null || xParams.length == 0)
                return Either.left("Некорректные координаты с графика");

            for (String xs : xParams) {
                try {
                    double x = Double.parseDouble(xs.trim().replace(',', '.'));
                    if (x < axisMin || x > axisMax || y < axisMin || y > axisMax)
                        return Either.left("Координаты точки за пределами графика");

                    points.add(new Point(x, y, r));
                } catch (NumberFormatException ignored) {}
            }
        } else {
            if (xParams == null || xParams.length == 0)
                return Either.left("Не выбрано ни одного значения X");

            if (y < -3 || y > 3)
                return Either.left("Y должен быть в диапазоне [-3; 3]");

            double[] allowedR = {1.0, 1.5, 2.0, 2.5, 3.0};
            boolean rOk = false;
            for (double val : allowedR) if (equalsDouble(r, val)) rOk = true;
            if (!rOk) return Either.left("Недопустимое значение R");

            for (String xs : xParams) {
                try {
                    double x = Double.parseDouble(xs.trim().replace(',', '.'));
                    if (x < -3 || x > 5) return Either.left("Недопустимое значение X");
                    points.add(new Point(x, y, r));
                } catch (NumberFormatException ignored) {}
            }

            if (points.isEmpty()) return Either.left("Некорректный формат X");
        }

        return Either.right(points);
    }
}
