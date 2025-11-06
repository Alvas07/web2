package model.shapes.factories;

import model.shapes.templates.QuarterCircle;
import model.shapes.templates.Shape;

public class QuarterCircleFactory implements ShapeFactory {
    private final double radiusRatio;

    public QuarterCircleFactory(double radiusRatio) {
        this.radiusRatio = radiusRatio;
    }

    @Override
    public Shape create(double r) {
        return new QuarterCircle(radiusRatio*r);
    }
}
