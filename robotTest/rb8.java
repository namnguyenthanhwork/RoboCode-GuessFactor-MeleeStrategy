package demo2;

import robocode.*;
import robocode.util.Utils;
import java.awt.Color;
import java.awt.geom.*;

// GFTargetingBot, by PEZ. A simple GuessFactorTargeting bot for tutorial purposes.
// Use the code as you see fit. Of course if I do not mind credits.

public class rb8 extends AdvancedRobot {
	private static final double BULLET_POWER = 1.9;
	
	private static double lateralDirection;
	private static double lastEnemyVelocity;
	private static GFTMovement movement;
	
	public rb8() {
		movement = new GFTMovement(this);
	}
	
	public void run() {
		setColors(Color.BLUE, Color.BLACK, Color.YELLOW);
		lateralDirection = 1;
		lastEnemyVelocity = 0;
		setAdjustRadarForGunTurn(true);
		setAdjustGunForRobotTurn(true);
		do {
			turnRadarRightRadians(Double.POSITIVE_INFINITY);
		} while (true);
	}
	
	public void onScannedRobot(ScannedRobotEvent e) {
		double enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();
		double enemyDistance = e.getDistance();
		double enemyVelocity = e.getVelocity();
		if (enemyVelocity != 0) {
			lateralDirection = GFTUtils.sign(enemyVelocity * Math.sin(e.getHeadingRadians() - enemyAbsoluteBearing));
		}
		GFTWave wave = new GFTWave(this);
		wave.gunLocation = new Point2D.Double(getX(), getY());
		GFTWave.targetLocation = GFTUtils.project(wave.gunLocation, enemyAbsoluteBearing, enemyDistance);
		wave.lateralDirection = lateralDirection;
		wave.bulletPower = BULLET_POWER;
		wave.setSegmentations(enemyDistance, enemyVelocity, lastEnemyVelocity);
		lastEnemyVelocity = enemyVelocity;
		wave.bearing = enemyAbsoluteBearing;
		setTurnGunRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getGunHeadingRadians() + wave.mostVisitedBearingOffset()));
		setFire(wave.bulletPower);
		if (getEnergy() >= BULLET_POWER) {
			addCustomEvent(wave);
		}
		movement.onScannedRobot(e);
		setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getRadarHeadingRadians()) * 2);
	}
}
//
//class GFTWave extends Condition {
//	static Point2D targetLocation;
//
//	double bulletPower;
//	Point2D gunLocation;
//	double bearing;
//	double lateralDirection;
//
//	private static final double MAX_DISTANCE = 1000;
//	private static final int DISTANCE_INDEXES = 5;
//	private static final int VELOCITY_INDEXES = 5;
//	private static final int BINS = 25;
//	private static final int MIDDLE_BIN = (BINS - 1) / 2;
//	private static final double MAX_ESCAPE_ANGLE = 0.7;
//	private static final double BIN_WIDTH = MAX_ESCAPE_ANGLE / (double)MIDDLE_BIN;
//
//	private static int[][][][] statBuffers = new int[DISTANCE_INDEXES][VELOCITY_INDEXES][VELOCITY_INDEXES][BINS];
//
//	private int[] buffer;
//	private AdvancedRobot robot;
//	private double distanceTraveled;
//
//	GFTWave(AdvancedRobot _robot) {
//		this.robot = _robot;
//	}
//
//	public boolean test() {
//		advance();
//		if (hasArrived()) {
//			buffer[currentBin()]++;
//			robot.removeCustomEvent(this);
//		}
//		return false;
//	}
//
//	double mostVisitedBearingOffset() {
//		return (lateralDirection * BIN_WIDTH) * (mostVisitedBin() - MIDDLE_BIN);
//	}
//
//	void setSegmentations(double distance, double velocity, double lastVelocity) {
//		int distanceIndex = (int)(distance / (MAX_DISTANCE / DISTANCE_INDEXES));
//		int velocityIndex = (int)Math.abs(velocity / 2);
//		int lastVelocityIndex = (int)Math.abs(lastVelocity / 2);
//		buffer = statBuffers[distanceIndex][velocityIndex][lastVelocityIndex];
//	}
//
//	private void advance() {
//		distanceTraveled += GFTUtils.bulletVelocity(bulletPower);
//	}
//
//	private boolean hasArrived() {
//		return distanceTraveled > gunLocation.distance(targetLocation) - 18;
//	}
//
//	private int currentBin() {
//		int bin = (int)Math.round(((Utils.normalRelativeAngle(GFTUtils.absoluteBearing(gunLocation, targetLocation) - bearing)) /
//				(lateralDirection * BIN_WIDTH)) + MIDDLE_BIN);
//		return GFTUtils.minMax(bin, 0, BINS - 1);
//	}
//
//	private int mostVisitedBin() {
//		int mostVisited = MIDDLE_BIN;
//		for (int i = 0; i < BINS; i++) {
//			if (buffer[i] > buffer[mostVisited]) {
//				mostVisited = i;
//			}
//		}
//		return mostVisited;
//	}
//}
//
//class GFTUtils {
//	static double bulletVelocity(double power) {
//		return 20 - 3 * power;
//	}
//
//	static Point2D project(Point2D sourceLocation, double angle, double length) {
//		return new Point2D.Double(sourceLocation.getX() + Math.sin(angle) * length,
//				sourceLocation.getY() + Math.cos(angle) * length);
//	}
//
//	static double absoluteBearing(Point2D source, Point2D target) {
//		return Math.atan2(target.getX() - source.getX(), target.getY() - source.getY());
//	}
//
//	static int sign(double v) {
//		return v < 0 ? -1 : 1;
//	}
//
//	static int minMax(int v, int min, int max) {
//		return Math.max(min, Math.min(max, v));
//	}
//}
//
//class GFTMovement {
//	private static final double BATTLE_FIELD_WIDTH = 800;
//	private static final double BATTLE_FIELD_HEIGHT = 600;
//	private static final double WALL_MARGIN = 18;
//	private static final double MAX_TRIES = 125;
//	private static final double REVERSE_TUNER = 0.421075;
//	private static final double DEFAULT_EVASION = 1.2;
//	private static final double WALL_BOUNCE_TUNER = 0.699484;
//
//	private AdvancedRobot robot;
//	private Rectangle2D fieldRectangle = new Rectangle2D.Double(WALL_MARGIN, WALL_MARGIN,
//			BATTLE_FIELD_WIDTH - WALL_MARGIN * 2, BATTLE_FIELD_HEIGHT - WALL_MARGIN * 2);
//	private double enemyFirePower = 3;
//	private double direction = 0.4;
//
//	GFTMovement(AdvancedRobot _robot) {
//		this.robot = _robot;
//	}
//
//	public void onScannedRobot(ScannedRobotEvent e) {
//		double enemyAbsoluteBearing = robot.getHeadingRadians() + e.getBearingRadians();
//		double enemyDistance = e.getDistance();
//		Point2D robotLocation = new Point2D.Double(robot.getX(), robot.getY());
//		Point2D enemyLocation = GFTUtils.project(robotLocation, enemyAbsoluteBearing, enemyDistance);
//		Point2D robotDestination;
//		double tries = 0;
//		while (!fieldRectangle.contains(robotDestination = GFTUtils.project(enemyLocation, enemyAbsoluteBearing + Math.PI + direction,
//				enemyDistance * (DEFAULT_EVASION - tries / 100.0))) && tries < MAX_TRIES) {
//			tries++;
//		}
//		if ((Math.random() < (GFTUtils.bulletVelocity(enemyFirePower) / REVERSE_TUNER) / enemyDistance ||
//				tries > (enemyDistance / GFTUtils.bulletVelocity(enemyFirePower) / WALL_BOUNCE_TUNER))) {
//			direction = -direction;
//		}
//		// Jamougha's cool way
//		double angle = GFTUtils.absoluteBearing(robotLocation, robotDestination) - robot.getHeadingRadians();
//		robot.setAhead(Math.cos(angle) * 100);
//		robot.setTurnRightRadians(Math.tan(angle));
//	}
//}