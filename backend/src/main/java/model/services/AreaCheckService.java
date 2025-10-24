package model.services;

import model.models.HistoryEntry;
import model.models.Point;
import model.shapes.QuadrantShapeTemplate;
import model.shapes.templates.QuadrantShape;
import model.shapes.templates.Shape;

import java.util.ArrayList;
import java.util.List;

public class AreaCheckService {
    private final List<QuadrantShapeTemplate> shapeTemplates;

    public AreaCheckService(List<QuadrantShapeTemplate> shapeTemplates) {
        this.shapeTemplates = shapeTemplates;
    }

    public List<HistoryEntry> checkPoints(List<Point> points, String now) {
        List<Shape> shapes = new ArrayList<>();
        for (QuadrantShapeTemplate template : shapeTemplates) {
            shapes.add(new QuadrantShape(template.getFactory().create(points.get(0).r()), template.getQuadrant()));
        }
        HitChecker hitChecker = new HitChecker(shapes);

        List<HistoryEntry> entries = new ArrayList<>();
        for (Point p : points) {
            boolean result = hitChecker.isHit(p);
            entries.add(new HistoryEntry(p, result, now));
        }
        return entries;
    }
}
