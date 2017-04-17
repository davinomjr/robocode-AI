package com.junior.davino.dramed.util;

import java.awt.geom.Point2D;

public class MoveUtil {

	 
    public static double absoluteBearing(Point2D.Double source, Point2D.Double target) {
        return Math.atan2(target.x - source.x, target.y - source.y);
    }
 
    public static double limit(double min, double value, double max) {
        return Math.max(min, Math.min(value, max));
    }
    
    
    public static double maxEscapeAngle(double velocity) {
        return Math.asin(8.0/velocity);
    }
}
