package backend.utils;

import model.models.Point;

public record CheckResult(Point point, boolean hit) {}
