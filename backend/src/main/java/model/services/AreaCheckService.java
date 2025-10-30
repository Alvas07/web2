package model.services;

import backend.utils.CheckResult;
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

    public List<CheckResult> checkPoints(List<Point> points) {
       List<CheckResult> results = new ArrayList<>();
       for (Point p : points) {
           List<Shape> shapesForPoint = new ArrayList<>();
           for (QuadrantShapeTemplate template : shapeTemplates) {
               shapesForPoint.add(new QuadrantShape(template.getFactory().create(p.r()), template.getQuadrant()));
           }
           HitChecker checker = new HitChecker(shapesForPoint);
           results.add(new CheckResult(p, checker.isHit(p)));
       }
       return results;
    }
}
