package com.junior.davino.dramed.util;

import java.awt.geom.Rectangle2D;

public class Util {

    public static double bulletVelocity(double power) {
        return (20.0 - (3.0*power));
    }
    
    public static Rectangle2D.Double buildRectangleArena(double arenaWidth, double arenaHeight){
    	 return new java.awt.geom.Rectangle2D.Double(18, 18,arenaWidth-36, arenaHeight-36);    	
    }
    
    public static double normalizeValue(final double valueIn, final double baseMin, final double baseMax) {
    	return (100 - baseMin)/(baseMax-baseMin)*(valueIn - baseMax) +baseMax;
    }
}
