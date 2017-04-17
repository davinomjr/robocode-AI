package com.junior.davino.dramed.model;

import java.awt.geom.Point2D;

public class Enemy {
	private String name;
	private Point2D.Double pos;
	private double energy;
	private boolean isAlive;
	private double lastKnowDistance;
	
	public double getLastKnowDistance() {
		return lastKnowDistance;
	}
	public void setLastKnowDistance(double lastKnowDistance) {
		this.lastKnowDistance = lastKnowDistance;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Point2D.Double getPos() {
		return pos;
	}
	public void setPos(Point2D.Double pos) {
		this.pos = pos;
	}
	public double getEnergy() {
		return energy;
	}
	public void setEnergy(double energy) {
		this.energy = energy;
	}
	public boolean isAlive() {
		return isAlive;
	}
	public void setAlive(boolean isAlive) {
		this.isAlive = isAlive;
	}	
}
