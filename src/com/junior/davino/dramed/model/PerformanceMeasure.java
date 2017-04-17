package com.junior.davino.dramed.model;

public class PerformanceMeasure {
	
    private double energy;
    private int enemiesCount;
    private double performanceFactor;
    private EnumRobotMode chooseMode;
	
	public double getEnergy() {
		return energy;
	}
	public void setEnergy(double energy) {
		this.energy = energy;
	}
	public int getEnemiesCount() {
		return enemiesCount;
	}
	public void setEnemiesCount(int enemiesCount) {
		this.enemiesCount = enemiesCount;
	}

	public double getPerformanceFactor() {
		return performanceFactor;
	}
	public void setPerformanceFactor(double performanceFactor) {
		this.performanceFactor = performanceFactor;
	}
	
		public EnumRobotMode getChooseMode() {
		return chooseMode;
	}
	public void setChooseMode(EnumRobotMode chooseMode) {
		this.chooseMode = chooseMode;
	}          
}
