package com.junior.davino.dramed;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.junior.davino.dramed.model.Enemy;
import com.junior.davino.dramed.model.EnemyWave;
import com.junior.davino.dramed.model.EnumRobotMode;
import com.junior.davino.dramed.model.PerformanceMeasure;
import com.junior.davino.dramed.movement.Move;
import com.junior.davino.dramed.util.Constants;
import com.junior.davino.dramed.util.MoveUtil;
import com.junior.davino.dramed.util.Util;

import robocode.AdvancedRobot;
import robocode.HitByBulletEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

public class Dramed extends AdvancedRobot {

	public Point2D.Double _myLocation;
	public Point2D.Double _enemyLocation;
	public static double _enemyEnergy = 100.0;
	public Map<String, Enemy> _enemies;

	public ArrayList _enemyWaves;
	public ArrayList _surfDirections; // Direcoes em relacao ao inimigo (turnos
										// passados)
	public ArrayList _surfAbsBearings; // Angulacoes passadas do inimigo (2
										// ticks no passado)

	public static int SURFSTATSCOUNT = 47;
	public static int NORMALIZABLE_FACTOR_BIN = 47;
	public static double _surfStats[] = new double[SURFSTATSCOUNT];

	private Move _move;
	private double oldEnemyHeading;
	private PerformanceMeasure currentPerformanceMeasured;
	private EnumRobotMode currentMode;
	private int turnsWithoutChangeMode = 0;
	private ArrayList<PerformanceMeasure> lastPerformancesMeasured;

	public void run() {
		setColors(Color.WHITE, Color.BLACK, Color.BLACK);
		_move = new Move(this, getBattleFieldWidth(), getBattleFieldHeight());
		_enemies = new HashMap<String, Enemy>();
		_enemyWaves = new ArrayList();
		_surfDirections = new ArrayList();
		_surfAbsBearings = new ArrayList();
		lastPerformancesMeasured = new ArrayList();

		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);

		do {
			turnRadarRightRadians(Double.POSITIVE_INFINITY);
		} while (true);
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		_myLocation = new Point2D.Double(getX(), getY());
		Enemy currentEnemy = _enemies.get(e.getName());
		if (currentEnemy == null) {
			currentEnemy = buildEnemy(e);
			_enemies.put(e.getName(), currentEnemy);
		}

		currentPerformanceMeasured = getPerformanceMeasure(e);
		evaluatePerformance(currentPerformanceMeasured, currentEnemy);
		evaluateUtility(currentPerformanceMeasured, currentEnemy);
		lastPerformancesMeasured.add(currentPerformanceMeasured);

		fireAtWill(e, currentPerformanceMeasured);

		move(e, currentEnemy);
	}

	private Enemy buildEnemy(ScannedRobotEvent e) {
		Enemy enemy = new Enemy();
		enemy.setAlive(true);
		enemy.setEnergy(e.getEnergy());
		enemy.setName(e.getName());
		enemy.setLastKnowDistance(e.getDistance());
		return enemy;
	}

	private void evaluatePerformance(PerformanceMeasure performance, Enemy e) {
		evaluateEnergy(performance);
		evaluateEnemies(performance, e);
		performance.setPerformanceFactor(Util.normalizeValue(performance.getPerformanceFactor(), Constants.BASEMIN, Constants.BASEMAX));
	}

	private void setPerformanceMode(PerformanceMeasure performance) {
		if (performance.getPerformanceFactor() >= 75.0) {
			currentMode = EnumRobotMode.CHUCKNORRIS_MODE;
		} else if (performance.getPerformanceFactor() >= 50.0) {
			currentMode = EnumRobotMode.RAMBO_MODE;
		} else if (performance.getPerformanceFactor() >= 25.0) {
			currentMode = EnumRobotMode.DEFENSIVE_MODE;
		} else {
			currentMode = EnumRobotMode.CHICKEN_MODE;
		}

		turnsWithoutChangeMode = 0;
	}

	private void evaluateEnergy(PerformanceMeasure performance) {
		if (performance.getEnergy() <= Constants.DANGER_ENERGY_LEVEL
				&& performance.getPerformanceFactor() - Constants.DANGER_ENERGY_LEVEL * 2 > 0) {
			performance.setPerformanceFactor(performance.getPerformanceFactor() - Constants.DANGER_ENERGY_LEVEL * 2);
			;
		} else if (performance.getEnergy() < Constants.STABLE_ENERGY_LEVEL
				&& performance.getPerformanceFactor() - Constants.STABLE_ENERGY_LEVEL / 2 > 0) {
			performance.setPerformanceFactor(performance.getPerformanceFactor() - Constants.STABLE_ENERGY_LEVEL / 2);
		} else if (performance.getEnergy() < Constants.GOOD_ENERGY_LEVEL
				&& performance.getPerformanceFactor() - Constants.GOOD_ENERGY_LEVEL / 5 > 0) {
			performance.setPerformanceFactor(performance.getPerformanceFactor() - Constants.GOOD_ENERGY_LEVEL / 5);
		} else if (performance.getPerformanceFactor() - Constants.GOOD_ENERGY_LEVEL / 10 > 0) {
			performance.setPerformanceFactor(performance.getPerformanceFactor() - Constants.GOOD_ENERGY_LEVEL / 10);
		}
	}

	private void evaluateEnemies(PerformanceMeasure performance, Enemy e) {
		if (e.getEnergy() < Constants.DANGER_ENERGY_LEVEL) {
			performance.setPerformanceFactor(performance.getPerformanceFactor() * 2);
		}

		boolean isClosestRobot = checkIfClosestEnemy(e);
		performance.setPerformanceFactor(
				performance.getPerformanceFactor() + (isClosestRobot ? e.getEnergy() : -e.getEnergy() / 2));
	}

	private boolean checkIfClosestEnemy(Enemy e) {
		boolean isClosest = true;
		for (Enemy enemy : _enemies.values()) {
			if (enemy.getLastKnowDistance() < e.getLastKnowDistance()) {
				isClosest = false;
				break;
			}
		}

		return isClosest;
	}

	private void evaluateUtility(PerformanceMeasure measure, Enemy enemy) {		
		if (turnsWithoutChangeMode == 0 || turnsWithoutChangeMode == 5) {
			setPerformanceMode(measure);
			analyseHistory(measure);
			double energyAfterFire = measure.getEnergy() - getBulletPower(measure);
			if(energyAfterFire <= Constants.DANGER_ENERGY_LEVEL && currentMode != EnumRobotMode.CHICKEN_MODE){
				measure.setPerformanceFactor(measure.getPerformanceFactor() - 10);
				measure.setChooseMode(EnumRobotMode.CHICKEN_MODE);
			}
			
			if(getEnemiesAliveCount() == 1 && enemy.getEnergy() < Constants.STABLE_ENERGY_LEVEL){ // Existe apenas um inimigo vivo?
				measure.setChooseMode(EnumRobotMode.CHUCKNORRIS_MODE);
				measure.setPerformanceFactor(measure.getPerformanceFactor() + 30);
			}
			
		
			
			
		} else {
			turnsWithoutChangeMode++;
		}

		measure.setChooseMode(currentMode);
	}

	private void analyseHistory(PerformanceMeasure currentMeasure) {
		double medianFactorValue, sumFactor = 0;
		int measuresCount;

		if (lastPerformancesMeasured.size() >= 7) {
			measuresCount = 7;
		} else if (lastPerformancesMeasured.size() >= 5) {
			measuresCount = 5;
		} else if (lastPerformancesMeasured.size() >= 3) {
			measuresCount = 3;
		} else {
			measuresCount = lastPerformancesMeasured.size();
		}

		for (int i = measuresCount; i > 0; i--) {
			if(!lastPerformancesMeasured.get(lastPerformancesMeasured.size() - i).isSpecialCase()){
				sumFactor += lastPerformancesMeasured.get(lastPerformancesMeasured.size() - i).getPerformanceFactor();	
			}
		}

		medianFactorValue = sumFactor / measuresCount;
		if (Math.abs(currentMeasure.getPerformanceFactor() - medianFactorValue) > 50) { // Destoa muito da media
			currentMeasure.setSpecialCase(true);
		}
	}

	private void fireAtWill(ScannedRobotEvent e, PerformanceMeasure performance) {
		double bulletPower = Math.min(3.0, getEnergy());
		double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
		double enemyX = getX() + e.getDistance() * Math.sin(absoluteBearing);
		double enemyY = getY() + e.getDistance() * Math.cos(absoluteBearing);
		double enemyHeading = e.getHeadingRadians();
		double enemyHeadingChange = enemyHeading - oldEnemyHeading;
		double enemyVelocity = e.getVelocity();
		oldEnemyHeading = enemyHeading;

		double myX = getX();
		double myY = getY();

		double deltaTime = 0;
		double battleFieldHeight = getBattleFieldHeight(), battleFieldWidth = getBattleFieldWidth();
		double predictedX = enemyX, predictedY = enemyY;
		while ((++deltaTime) * (20.0 - 3.0 * bulletPower) < Point2D.Double.distance(myX, myY, predictedX, predictedY)) {
			predictedX += Math.sin(enemyHeading) * enemyVelocity;
			predictedY += Math.cos(enemyHeading) * enemyVelocity;
			enemyHeading += enemyHeadingChange;
			if (predictedX < 18.0 || predictedY < 18.0 || predictedX > battleFieldWidth - 18.0
					|| predictedY > battleFieldHeight - 18.0) {

				predictedX = Math.min(Math.max(18.0, predictedX), battleFieldWidth - 18.0);
				predictedY = Math.min(Math.max(18.0, predictedY), battleFieldHeight - 18.0);
				break;
			}
		}

		double theta = Utils.normalAbsoluteAngle(Math.atan2(predictedX - getX(), predictedY - getY()));

		setTurnRadarRightRadians(Utils.normalRelativeAngle(absoluteBearing - getRadarHeadingRadians()));
		setTurnGunRightRadians(Utils.normalRelativeAngle(theta - getGunHeadingRadians()));

		double firePower = getBulletPower(performance);
		fire(firePower);
	}

	private PerformanceMeasure getPerformanceMeasure(ScannedRobotEvent e) {
		PerformanceMeasure newPerformance = getLastPerformance();
		newPerformance.setEnemiesCount(getOthers());
		newPerformance.setEnergy(getEnergy());
		newPerformance.setEnemiesCount(getEnemiesAliveCount());
		return newPerformance;
	}

	public void updateWaves() {
		for (int x = 0; x < _enemyWaves.size(); x++) {
			EnemyWave ew = (EnemyWave) _enemyWaves.get(x);

			ew.setDistanceTraveled((getTime() - ew.getFireTime()) * ew.getBulletVelocity());
			if (ew.getDistanceTraveled() > _myLocation.distance(ew.getFireLocation()) + 50) {
				_enemyWaves.remove(x);
				x--;
			}
		}
	}

	public EnemyWave getClosestSurfableWave() {
		double closestDistance = 80000;
		EnemyWave surfWave = null;

		for (int x = 0; x < _enemyWaves.size(); x++) {
			EnemyWave ew = (EnemyWave) _enemyWaves.get(x);
			double distance = _myLocation.distance(ew.getFireLocation()) - ew.getDistanceTraveled();

			if (distance > ew.getBulletVelocity() && distance < closestDistance) {
				surfWave = ew;
				closestDistance = distance;
			}
		}

		return surfWave;
	}

	private int getEnemiesAliveCount() {
		int enemiesAliveCount = 0;
		for (Enemy enemy : _enemies.values()) {
			if (enemy.isAlive()) {
				enemiesAliveCount++;
			}
		}

		return enemiesAliveCount;
	}

	/*
	 * Dado a onda contendo a bala e o ponto que o bot foi atingido Calcular o
	 * index no array de stats para o fator
	 */
	public static int getFactorIndex(EnemyWave ew, Point2D.Double targetLocation) {
		double offsetAngle = (MoveUtil.absoluteBearing(ew.getFireLocation(), targetLocation) - ew.getDirectAngle());
		double factor = Utils.normalRelativeAngle(offsetAngle) / MoveUtil.maxEscapeAngle(ew.getBulletVelocity())
				* ew.getDirection();

		return (int) MoveUtil.limit(0,
				(factor * ((NORMALIZABLE_FACTOR_BIN - 1) / 2)) + ((NORMALIZABLE_FACTOR_BIN - 1) / 2),
				NORMALIZABLE_FACTOR_BIN - 1);
	}

	/*
	 * Dado a onda contendo a bala e o ponto que o bot foi atingido Atualizar
	 * array de stats para alertar do perigo da onda
	 */
	public void logHit(EnemyWave ew, Point2D.Double targetLocation) {
		int index = getFactorIndex(ew, targetLocation);

		for (int x = 0; x < NORMALIZABLE_FACTOR_BIN; x++) {
			_surfStats[x] += 1.0 / (Math.pow(index - x, 2) + 1);
		}
	}

	public void onHitByBullet(HitByBulletEvent e) {
		if (!_enemyWaves.isEmpty()) { // Caso nao exista onda, a deteccao
										// falhou.. tratar
			Point2D.Double hitBulletLocation = new Point2D.Double(e.getBullet().getX(), e.getBullet().getY());
			EnemyWave hitWave = null;

			// Busca de ondas localizando uma segura
			for (int x = 0; x < _enemyWaves.size(); x++) {
				EnemyWave ew = (EnemyWave) _enemyWaves.get(x);

				if (Math.abs(ew.getDistanceTraveled() - _myLocation.distance(ew.getFireLocation())) < 50
						&& Math.abs(Util.bulletVelocity(e.getBullet().getPower()) - ew.getBulletVelocity()) < 0.001) {
					hitWave = ew;
					break;
				}
			}

			if (hitWave != null) {
				logHit(hitWave, hitBulletLocation);
				_enemyWaves.remove(_enemyWaves.lastIndexOf(hitWave));
			}
		}
	}

	public void onRobotDeath(RobotDeathEvent e) {
		Enemy enemy = _enemies.get(e.getName());
		if (enemy != null) {
			enemy.setAlive(false);
		}
	}

	/*
	 * Metodo baseado em PrecisePrediction Prediz aonde estaremos quando a onda
	 * interceptar-nos OBS: Orbita de direcoes varia de -1 a 1
	 * 
	 * CREDIT: mini sized predictor from Apollon, by rozu
	 * http://robowiki.net?Apollon
	 */
	public Point2D.Double predictPosition(EnemyWave surfWave, int direction) {
		Point2D.Double predictedPosition = (Point2D.Double) _myLocation.clone();
		double predictedVelocity = getVelocity();
		double predictedHeading = getHeadingRadians();
		double maxTurning, moveAngle, moveDir;

		int counter = 0; // number of ticks in the future
		boolean intercepted = false;

		do {
			moveAngle = _move
					.wallSmoothing(predictedPosition,
							MoveUtil.absoluteBearing(surfWave.getFireLocation(), predictedPosition)
									+ (direction * (Math.PI / 2)),
							direction, currentMode.getValue())
					- predictedHeading;
			moveDir = 1;

			if (Math.cos(moveAngle) < 0) {
				moveAngle += Math.PI;
				moveDir = -1;
			}

			moveAngle = Utils.normalRelativeAngle(moveAngle);

			// maxTurning is built in like this, you can't turn more then this
			// in one tick
			maxTurning = Math.PI / 720d * (40d - 3d * Math.abs(predictedVelocity));
			predictedHeading = Utils
					.normalRelativeAngle(predictedHeading + MoveUtil.limit(-maxTurning, moveAngle, maxTurning));

			// this one is nice ;). if predictedVelocity and moveDir have
			// different signs you want to breack down
			// otherwise you want to accelerate (look at the factor "2")
			predictedVelocity += (predictedVelocity * moveDir < 0 ? 2 * moveDir : moveDir);
			predictedVelocity = MoveUtil.limit(-8, predictedVelocity, 8);

			// calculate the new predicted position
			predictedPosition = _move.project(predictedPosition, predictedHeading, predictedVelocity);

			counter++;

			if (predictedPosition.distance(surfWave.getFireLocation()) < surfWave.getDistanceTraveled()
					+ (counter * surfWave.getBulletVelocity()) + surfWave.getBulletVelocity()) {
				intercepted = true;
			}
		} while (!intercepted && counter < 500);

		return predictedPosition;
	}

	public double checkDanger(EnemyWave surfWave, int direction) {
		int index = getFactorIndex(surfWave, predictPosition(surfWave, direction));

		return _surfStats[index];
	}

	public void doSurfing() {
		EnemyWave surfWave = getClosestSurfableWave();

		if (surfWave == null) {
			return;
		}

		double dangerLeft = checkDanger(surfWave, -1);
		double dangerRight = checkDanger(surfWave, 1);

		double goAngle = MoveUtil.absoluteBearing(surfWave.getFireLocation(), _myLocation);
		if (dangerLeft < dangerRight) {
			goAngle = _move.wallSmoothing(_myLocation, goAngle - (Math.PI / 2), 1, currentMode.getValue());
		} else {
			goAngle = _move.wallSmoothing(_myLocation, goAngle + (Math.PI / 2), 1, currentMode.getValue());
		}

		_move.setBackAsFront(this, goAngle);
	}

	private PerformanceMeasure getLastPerformance() {
		if (currentPerformanceMeasured == null) {
			PerformanceMeasure newPerformance = new PerformanceMeasure();
			newPerformance.setPerformanceFactor(0);
			return newPerformance;
		}

		return currentPerformanceMeasured;
	}

	private void move(ScannedRobotEvent e, Enemy currentEnemy) {
		double lateralVelocity = getVelocity() * Math.sin(e.getBearingRadians());
		double absBearing = e.getBearingRadians() + getHeadingRadians();

		setTurnRadarRightRadians(Utils.normalRelativeAngle(absBearing - getRadarHeadingRadians()) * 2);

		_surfDirections.add(0, new Integer((lateralVelocity >= 0) ? 1 : -1));
		_surfAbsBearings.add(0, new Double(absBearing + Math.PI));

		double bulletPower = _enemyEnergy - e.getEnergy();
		if (bulletPower <= Constants.MAXIMUM_FIRE_POWER && bulletPower > Constants.MINIUM_FIRE_POWER
				&& _surfDirections.size() > 2) {
			EnemyWave ew = new EnemyWave();
			ew.setFireTime(getTime() - 1);
			ew.setBulletVelocity(Util.bulletVelocity(bulletPower));
			ew.setDistanceTraveled(Util.bulletVelocity(bulletPower));
			ew.setDirection(((Integer) _surfDirections.get(2)).intValue());
			ew.setDirectAngle(((Double) _surfAbsBearings.get(2)).doubleValue());
			ew.setFireLocation((Point2D.Double) _enemyLocation.clone()); // last
																			// tick
			_enemyWaves.add(ew);
		}

		_enemyEnergy = e.getEnergy();
		_enemyLocation = _move.project(_myLocation, absBearing, e.getDistance());
		currentEnemy.setPos(_enemyLocation);

		updateWaves();
		doSurfing();
	}

	private double getBulletPower(PerformanceMeasure measure) {
		return (3 - Constants.MINIUM_FIRE_POWER) / (Constants.MAXIMUM_FIRE_POWER - Constants.MINIUM_FIRE_POWER)
				* (measure.getPerformanceFactor() - Constants.MAXIMUM_FIRE_POWER) + Constants.MAXIMUM_FIRE_POWER;
	}

	private boolean lastModesOnChickenMode() {
		for (int i = 3; i > 0; i--) {
			if (lastPerformancesMeasured.get(lastPerformancesMeasured.size() - i)
					.getChooseMode() != EnumRobotMode.CHICKEN_MODE) {
				return false;
			}
		}

		return true;
	}

}
