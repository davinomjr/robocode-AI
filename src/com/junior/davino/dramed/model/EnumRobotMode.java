package com.junior.davino.dramed.model;

public enum EnumRobotMode {
	
	CHICKEN_MODE (0),
	DEFENSIVE_MODE (1),
	RAMBO_MODE (2),
	CHUCKNORRIS_MODE (3);
	
	private int value;

	EnumRobotMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
	
}
