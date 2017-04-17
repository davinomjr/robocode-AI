package com.junior.davino.dramed.movement;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.junior.davino.dramed.util.Util;

import robocode.AdvancedRobot;
import robocode.control.BattleSpecification;
import robocode.util.Utils;

public class Move {
	
	private AdvancedRobot robot;
	private double arenaWidth, arenaHeight;
	// _bfWidth and _bfHeight set to battle field width and height
	private static double WALL_STICK = 140;
	
	/* Retangulo representando uma arena de no maximo 2292x2292
	 * Utilizado para evitar colisão com paredes
	 * CREDIT: PEZ */
//	private Rectangle2D.Double _fieldRect = new java.awt.geom.Rectangle2D.Double(54, 54, 2238, 2238);	
	private Rectangle2D.Double _fieldRect;
	
	public Move(AdvancedRobot robot, double arenaWidth, double arenaHeight){
		this.robot = robot;
		this.arenaWidth = arenaWidth;
		this.arenaHeight = arenaHeight;
		this._fieldRect = Util.buildRectangleArena(arenaWidth,arenaHeight);
	}
	
	

	 /*
	 * location = Current position
	 * startAngle = absolute angle that tank starts off moving - this is the angle
	 *   they will be moving at if there is no wall smoothing taking place.
	 * orientation = 1 if orbiting enemy clockwise, -1 if orbiting counter-clockwise
	 * smoothTowardEnemy = 1 if smooth towards enemy, -1 if smooth away
	 * NOTE: this method is designed based on an orbital movement system; these
	 *   last 2 arguments could be simplified in any other movement system.
	 * CREDIT: Voidious
	 */
	public double wallSmoothing(Point2D.Double location, double startAngle,
	    int orientation, int smoothTowardEnemy) {
	 
		double x = location.x;
		double y = location.y;
	    double angle = startAngle;
	 
	    // in Java, (-3 MOD 4) is not 1, so make sure we have some excess
	    // positivity here
	    angle += (4*Math.PI);
	 
	    double testX = x + (Math.sin(angle)*WALL_STICK);
	    double testY = y + (Math.cos(angle)*WALL_STICK);
	    double wallDistanceX = Math.min(x - 18, arenaWidth - x - 18);
	    double wallDistanceY = Math.min(y - 18, arenaHeight - y - 18);
	    double testDistanceX = Math.min(testX - 18, arenaWidth - testX - 18);
	    double testDistanceY = Math.min(testY - 18, arenaHeight - testY - 18);
	 
	    double adjacent = 0;
	    int g = 0; // because I'm paranoid about potential infinite loops
	 
	    while (!_fieldRect.contains(testX, testY) && g++ < 25) {
	        if (testDistanceY < 0 && testDistanceY < testDistanceX) {
	            // wall smooth North or South wall
	            angle = ((int)((angle + (Math.PI/2)) / Math.PI)) * Math.PI;
	            adjacent = Math.abs(wallDistanceY);
	        } else if (testDistanceX < 0 && testDistanceX <= testDistanceY) {
	            // wall smooth East or West wall
	            angle = (((int)(angle / Math.PI)) * Math.PI) + (Math.PI/2);
	            adjacent = Math.abs(wallDistanceX);
	        }
	 
	        // use your own equivalent of (1 / POSITIVE_INFINITY) instead of 0.005
	        // if you want to stay closer to the wall ;)
	        angle += smoothTowardEnemy*orientation*
	            (Math.abs(Math.acos(adjacent/WALL_STICK)) + 0.005);
	 
	        testX = x + (Math.sin(angle)*WALL_STICK);
	        testY = y + (Math.cos(angle)*WALL_STICK);
	        testDistanceX = Math.min(testX - 18, arenaWidth - testX - 18);
	        testDistanceY = Math.min(testY - 18, arenaHeight - testY - 18);
	 
	        if (smoothTowardEnemy == -1) {
	            // this method ended with tank smoothing away from enemy... you may
	            // need to note that globally, or maybe you don't care.
	        }
	    }
	 
	    return angle; // you may want to normalize this
	}
	
 
	// Projeta a distancia a ser percorrida de acordo com o angulo
    public static Point2D.Double project(Point2D.Double sourceLocation,
        double angle, double length) {
        return new Point2D.Double(sourceLocation.x + Math.sin(angle) * length,
        					      sourceLocation.y + Math.cos(angle) * length);
    }
    

    public static void setBackAsFront(AdvancedRobot robot, double goAngle) {
        double angle =
            Utils.normalRelativeAngle(goAngle - robot.getHeadingRadians());
        if (Math.abs(angle) > (Math.PI/2)) {
            if (angle < 0) {
                robot.setTurnRightRadians(Math.PI + angle);
            } else {
                robot.setTurnLeftRadians(Math.PI - angle);
            }
            robot.setBack(100);
        } else {
            if (angle < 0) {
                robot.setTurnLeftRadians(-1*angle);
           } else {
                robot.setTurnRightRadians(angle);
           }
            robot.setAhead(100);
        }        
    }
    
    
	
}
