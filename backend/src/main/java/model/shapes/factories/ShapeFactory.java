package model.shapes.factories;

import model.shapes.templates.Shape;

public interface ShapeFactory {
    Shape create(double r);
}
