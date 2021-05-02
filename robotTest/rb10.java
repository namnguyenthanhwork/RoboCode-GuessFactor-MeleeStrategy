package demo2;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import robocode.*;
import robocode.util.Utils;

public class rb10 extends AdvancedRobot {
	
	// Static variables or objects in robocode keep their data from round to round
	static final int PREDICTION_POINTS = 200;
	
	//Keep track of all my enemy and their data for movement and aiming calculations
	HashMap<String, Robot> enemies = new HashMap<>();
	
	//My own information to limit the get function usage and the targetBot to know what I am aiming at after a scan
	Robot me = new Robot();
	Robot targetBot;
	
	//List of all my possible movement points
	List<Point2D.Double> possibleLocations = new ArrayList<>();
	//The lowest risked point from the possiblePoints
	Point2D.Double targetPoint = new Point2D.Double(60, 60);
	
	//To limit getBattleFieldHeight and Width calls
	Rectangle2D.Double battleField = new Rectangle2D.Double();
	
	//Time before I force a new target point
	int idleTime = 30;
	
	
	private static final double BULLET_POWER = 1.9;
	
	private static double lateralDirection;
	private static double lastEnemyVelocity;
	private static GFTMovement movement;
	
	public rb10() {
		movement = new GFTMovement(this);
	}
	
	
	public void run() {
		if (getOthers() > 1) {
			battleField.height = getBattleFieldHeight();
			battleField.width = getBattleFieldWidth();
			
			//Initial variable update
			me.x = getX();
			me.y = getY();
			me.energy = getEnergy();
			
			//init point which is lowest risk
			targetPoint.x = me.x;
			targetPoint.y = me.y;
			
			//init robot which i am targeting
			targetBot = new Robot();
			targetBot.alive = false;
			
			//Need to make my robot a nice colour. The better the skins the better the wins
			setColors(new Color(28, 98, 219), new Color(28, 212, 219), new Color(131, 0, 255), new Color(226, 220, 24), new Color(255, 255, 255));
			setAdjustGunForRobotTurn(true);
			setAdjustRadarForGunTurn(true);
			setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
			
			while (true) {
				//Update my variables every loop call
				me.lastHeading = me.heading;
				me.heading = getHeadingRadians();
				me.x = getX();
				me.y = getY();
				me.energy = getEnergy();
				me.gunHeadingRadians = getGunHeadingRadians();
				
				// If the robot isn't scanned in 25 ticks get rid of it because all the data is old and outdated
				Iterator<Robot> enemiesIter = enemies.values().iterator();
				while (enemiesIter.hasNext()) {
					Robot r = enemiesIter.next();
					if (getTime() - r.scanTime > 25) {
						// If the information is not updated lets just assume its dead so we don't shoot at it
						r.alive = false;
						if (targetBot.name != null && r.name.equals(targetBot.name))
							targetBot.alive = false;
					}
				}
				
				// Once the robot scans once and sees other robots start moving and shooting
				// if (getTime() > 9) {
				movement();
				if (targetBot.alive)
					shooting();
				// }
				execute();
			}
		} else {
			lateralDirection = 1;
			lastEnemyVelocity = 0;
			setAdjustRadarForGunTurn(true);
			setAdjustGunForRobotTurn(true);
			do {
				turnRadarRightRadians(Double.POSITIVE_INFINITY);
			} while (true);
		}
	}
	
	public void onScannedRobot(ScannedRobotEvent e) {
		if (getOthers() > 1) {
			// Add enemy to the map of other enemies
			Robot en = enemies.get(e.getName());
			// Not an enemy with that name in the hashmap
			if (en == null) {
				en = new Robot();
				enemies.put(e.getName(), en);
			}
			
			// Setting/Updating enemy variables
			en.bearingRadians = e.getBearingRadians();
			en.setLocation(new Point2D.Double(me.x + e.getDistance() * Math.sin(getHeadingRadians() + en.bearingRadians),
					me.y + e.getDistance() * Math.cos(getHeadingRadians() + en.bearingRadians)));
			en.lastHeading = en.heading;
			en.name = e.getName();
			en.energy = e.getEnergy();
			en.alive = true;
			en.scanTime = getTime();
			en.velocity = e.getVelocity();
			en.heading = e.getHeadingRadians();
			//Based on robot distance and energy chose my best enemy to shoot at
			en.shootAbleScore = en.energy < 25 ? (en.energy < 5 ? (en.energy == 0 ? Double.MIN_VALUE : en.distance(me) * 0.1) : en.distance(me) * 0.75) : en.distance(me);
			
			// LOGIC NEEDED FOR 1v1 SUPER SAYAN MODE ACTIVATE
			if (getOthers() == 1) {
				// Nano Bot Lock - Very Simple
				setTurnRadarLeftRadians(getRadarTurnRemainingRadians());
			}
			
			// If the target I was shooting at died switch to a new one or if a new challenger has a lower shootableScore
			if (!targetBot.alive || en.shootAbleScore < targetBot.shootAbleScore)
				targetBot = en;
		} else {
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
	
	public void onRobotDeath(RobotDeathEvent event) {
		// If a robot is dead we need to know
		if (enemies.containsKey(event.getName())) {
			enemies.get(event.getName()).alive = false;
		}
		
		if (event.getName().equals(targetBot.name))
			targetBot.alive = false;
	}
	
	public void shooting() {
		if (targetBot != null && targetBot.alive) {
			double dist = me.distance(targetBot);
			double power = (dist > 850 ? 0.1 : (dist > 700 ? 0.5 : (dist > 250 ? 2.0 : 3.0)));
			power = Math.min(me.energy / 4d, Math.min(targetBot.energy / 3d, power));
			power = Utility.clamp(power, 0.1, 3.0);
			
			//Circular targeting which also works as linear targeting due to the heading change being 0 in linear
			long deltahittime;
			Point2D.Double shootAt = new Point2D.Double();
			double head, deltaHead, bspeed;
			double predictX, predictY;
			
			//Setting up variables
			predictX = targetBot.getX();
			predictY = targetBot.getY();

//            predictXTemp=predictX = targetBot.getX();
//            predictYTemp=predictY = targetBot.getY();
			
			head = targetBot.heading;
			deltaHead = head - targetBot.lastHeading;
			shootAt.setLocation(predictX, predictY);
			deltahittime = 0;
			//Repeat until the bullet distance / speed or velocity >= the time taken. A variation of d=v*t.
			boolean flag = false;
			do {
				//Add to x and y based on the velocity and the heading (predict)
				predictX += Math.sin(head) * targetBot.velocity;
				predictY += Math.cos(head) * targetBot.velocity;
				
				//For circular targeting the heading will always change in a theoretical universe by the same change in heading
				head += deltaHead;
				deltahittime++;
				Rectangle2D.Double fireField = new Rectangle2D.Double(18, 18,
						battleField.width - 18, battleField.height - 18);
				// if position not in field shoot at the current best location
				if (!fireField.contains(predictX, predictY)) {
//                    double ROBOT_WIDTH = 18, ROBOT_HEIGHT = 18;
//                    // Variables prefixed with e- refer to enemy, b- refer to bullet and r- refer to robot
//                    double bulletVelocity = Rules.getBulletSpeed(power);
//                    // These constants make calculating the quadratic coefficients below easier
//                    double A = (targetBot.getX() - getX()) / bulletVelocity;
//                    double B = targetBot.velocity / bulletVelocity * Math.sin(targetBot.heading);
//                    double C = (targetBot.getY() - getY()) / bulletVelocity;
//                    double D = targetBot.velocity / bulletVelocity * Math.cos(targetBot.heading);
//                    // Quadratic coefficients: a*(1/t)^2 + b*(1/t) + c = 0
//                    double a = A * A + C * C;
//                    double b = 2 * (A * B + C * D);
//                    double c = (B * B + D * D - 1);
//                    double delta = b * b - 4 * a * c;
//                    if (delta >= 0) {
//                        // Reciprocal of quadratic formula
//                        double t1 = 2 * a / (-b - Math.sqrt(delta));
//                        double t2 = 2 * a / (-b + Math.sqrt(delta));
//                        double t = Math.min(t1, t2) >= 0 ? Math.min(t1, t2) : Math.max(t1, t2);
//                        // Assume enemy stops at walls
//                        predictX = Utility.clamp(
//                                targetBot.getX() + targetBot.velocity * t * Math.sin(targetBot.heading),
//                                ROBOT_WIDTH / 2, getBattleFieldWidth() - ROBOT_WIDTH / 2);
//                        predictY = Utility.clamp(
//                                targetBot.getY() + targetBot.velocity * t * Math.cos(targetBot.heading),
//                                ROBOT_HEIGHT / 2, getBattleFieldHeight() - ROBOT_HEIGHT / 2);
//                    }
//
////                    The best bullet speed is the distance over the calculated time
//                    flag=true;
					bspeed = shootAt.distance(me) / deltahittime;
					// Clamping the bullet speed to a reasonable amount
					power = Utility.clamp((20 - bspeed) / 3.0, 0.1, 3.0);
					
					break;
				}
				//Change the current location to the current one
				shootAt.setLocation(predictX, predictY);
			} while ((int) Math.round((shootAt.distance(me) - 18) / Rules.getBulletSpeed(power)) > deltahittime);
			//      if(!flag)
			shootAt.setLocation(Utility.clamp(predictX, 34, getBattleFieldWidth() - 34),
					Utility.clamp(predictY, 34, getBattleFieldHeight() - 34));
			if ((getGunHeat() == 0.0) && (getGunTurnRemaining() == 0.0) && (power > 0.0) && (me.energy > 0.1)) {
				// Only fire the gun is ready
				setFire(power);
			}
			// Turn gun after firing so that the gun is not in an infinitely not ready to fire loop
			setTurnGunRightRadians(Utils.normalRelativeAngle(((Math.PI / 2) - Math.atan2(shootAt.y - me.getY(),
					shootAt.x - me.getX())) - getGunHeadingRadians()));
		}
	}
	
	public void movement() {
		if (targetPoint.distance(me) < 15 || idleTime > 25) {
			// Reset idle time, I'm at my location or took too long to get there
			idleTime = 0;
			// Get a new array of points
			updateListLocations(PREDICTION_POINTS);
			// Lowest Risk Point
			Point2D.Double lowRiskP = null;
			// Current Risk Value
			double lowestRisk = Double.MAX_VALUE;
			for (Point2D.Double p : possibleLocations) {
				// Make sure that if lowRiskP is not assigned yet give it a new
				// value no matter what
				double currentRisk = evaluatePoint(p);
				if (currentRisk <= lowestRisk || lowRiskP == null) {
					lowestRisk = currentRisk;
					lowRiskP = p;
				}
			}
			targetPoint = lowRiskP;
		} else {
			// Increase idle time if still not at position
			idleTime++;
			// GO TO POINT
			double angle = Utility.calcAngle(me, targetPoint) - getHeadingRadians();
			double direction = 1;
			//If Math.cos(angle) is negative its faster to go backwards and turn than going forwards and turn much more
			if (Math.cos(angle) < 0) {
				//Math.PI in radians is half so changing the turn by 180
				angle += Math.PI;
				direction *= -1;
			}
			//Increase velocity as my remaining amount to turn becomes less
			setMaxVelocity(10 - (4 * Math.abs(getTurnRemainingRadians())));
			setAhead(me.distance(targetPoint) * direction);
			angle = Utils.normalRelativeAngle(angle);
			setTurnRightRadians(angle);
		}
	}
	
	public void updateListLocations(int n) {
		possibleLocations.clear();
		final int xRange = (int) (125 * 1.5);
		// Create x points in a radius pixel radius around the bot
		for (int i = 0; i < n; i++) {
			double randXMod = Utility.randomBetween(-xRange, xRange);
			//yRange is dependant on the x current value to create a circle
			double yRange = Math.sqrt(xRange * xRange - randXMod * randXMod);
			double randYMod = Utility.randomBetween(-yRange, yRange);
			double y = Utility.clamp(me.y + randYMod, 75, battleField.height - 75);
			double x = Utility.clamp(me.x + randXMod, 75, battleField.width - 75);
			possibleLocations.add(new Point2D.Double(x, y));
		}
	}
	
	public double evaluatePoint(Point2D.Double p) {
		// You don't want to stay in one spot. Antigrav from starting point as
		// init value to enhance movement.
		double eval = Utility.randomBetween(1, 2.25) / p.distanceSq(me);
		// PRESET ANTIGRAV POINTS
		// If its a 1v1 the center is fine. You can use getOthers to see if its a 1v1.
		eval += (6 * (getOthers() - 1)) / p.distanceSq(battleField.width / 2, battleField.height / 2);
		double cornerFactor = getOthers() <= 5 ? getOthers() == 1 ? 0.25 : 0.5 : 1;
		eval += cornerFactor / p.distanceSq(0, 0);
		eval += cornerFactor / p.distanceSq(battleField.width, 0);
		eval += cornerFactor / p.distanceSq(0, battleField.height);
		eval += cornerFactor / p.distanceSq(battleField.width, battleField.height);
		
		if (targetBot.alive) {
			double botangle = Utils.normalRelativeAngle(Utility.calcAngle(p, targetBot) - Utility.calcAngle(me, p));
			Iterator<Robot> enemiesIter = enemies.values().iterator();
			while (enemiesIter.hasNext()) {
				Robot en = enemiesIter.next();
				// (1 / p.distanceSq(en)) AntiGrav stuff
				// (en.energy / me.energy) How dangerous a robot it
				// (1.0 + ((1 - (Math.abs(Math.sin(botangle)))) + Math.abs(Math.cos(botangle))) / 2) Better to move perpendicular to the target bot
				// (1 + Math.abs(Math.cos(Utility.calcAngle(me, p) - Utility.calcAngle(en, p)))) Worse if the enemy is closer to the point than I am in heading
				eval += (en.energy / me.energy) * (1 / p.distanceSq(en)) * (1.0 + ((1 - (Math.abs(Math.sin(botangle)))) +
						Math.abs(Math.cos(botangle))) / 2) * (1 + Math.abs(Math.cos(Utility.calcAngle(me, p) - Utility.calcAngle(en, p))));
			}
		} else if (enemies.values().size() >= 1) {
			Iterator<Robot> enemiesIter = enemies.values().iterator();
			while (enemiesIter.hasNext()) {
				Robot en = enemiesIter.next();
				eval += (en.energy / me.energy) * (1 / p.distanceSq(en)) * (1 + Math.abs(Math.cos(Utility.calcAngle(me, p) - Utility.calcAngle(en, p))));
			}
		} else {
			eval += (1 + Math.abs(Utility.calcAngle(me, targetPoint) - getHeadingRadians()));
		}
		return eval;
	}
	
	class Robot extends Point2D.Double {
		
		public long scanTime;
		public boolean alive = true;
		public double energy;
		public String name;
		public double gunHeadingRadians;
		public double bearingRadians;
		public double velocity;
		public double heading;
		public double lastHeading;
		public double shootAbleScore;
		
	}
	
	static class Utility {
		
		static double clamp(double value, double min, double max) {
			return Math.max(min, Math.min(max, value));
		}
		
		static double randomBetween(double min, double max) {
			return min + Math.random() * (max - min);
		}
		
		static double calcAngle(Point2D.Double p1, Point2D.Double p2) {
			return Math.atan2(p2.x - p1.x, p2.y - p1.y);
		}
		
	}
	
}


class GFTWave extends Condition {
	static Point2D targetLocation;
	
	double bulletPower;
	Point2D gunLocation;
	double bearing;
	double lateralDirection;
	
	private static final double MAX_DISTANCE = 1000;
	private static final int DISTANCE_INDEXES = 5;
	private static final int VELOCITY_INDEXES = 5;
	private static final int BINS = 25;
	private static final int MIDDLE_BIN = (BINS - 1) / 2;
	private static final double MAX_ESCAPE_ANGLE = 0.7;
	private static final double BIN_WIDTH = MAX_ESCAPE_ANGLE / (double) MIDDLE_BIN;
	
	private static int[][][][] statBuffers = new int[DISTANCE_INDEXES][VELOCITY_INDEXES][VELOCITY_INDEXES][BINS];
	
	private int[] buffer;
	private AdvancedRobot robot;
	private double distanceTraveled;
	
	GFTWave(AdvancedRobot _robot) {
		this.robot = _robot;
	}
	
	public boolean test() {
		advance();
		if (hasArrived()) {
			buffer[currentBin()]++;
			robot.removeCustomEvent(this);
		}
		return false;
	}
	
	double mostVisitedBearingOffset() {
		return (lateralDirection * BIN_WIDTH) * (mostVisitedBin() - MIDDLE_BIN);
	}
	
	void setSegmentations(double distance, double velocity, double lastVelocity) {
		int distanceIndex = (int) (distance / (MAX_DISTANCE / DISTANCE_INDEXES));
		int velocityIndex = (int) Math.abs(velocity / 2);
		int lastVelocityIndex = (int) Math.abs(lastVelocity / 2);
		buffer = statBuffers[distanceIndex][velocityIndex][lastVelocityIndex];
	}
	
	private void advance() {
		distanceTraveled += GFTUtils.bulletVelocity(bulletPower);
	}
	
	private boolean hasArrived() {
		return distanceTraveled > gunLocation.distance(targetLocation) - 18;
	}
	
	private int currentBin() {
		int bin = (int) Math.round(((Utils.normalRelativeAngle(GFTUtils.absoluteBearing(gunLocation, targetLocation) - bearing)) /
				(lateralDirection * BIN_WIDTH)) + MIDDLE_BIN);
		return GFTUtils.minMax(bin, 0, BINS - 1);
	}
	
	private int mostVisitedBin() {
		int mostVisited = MIDDLE_BIN;
		for (int i = 0; i < BINS; i++) {
			if (buffer[i] > buffer[mostVisited]) {
				mostVisited = i;
			}
		}
		return mostVisited;
	}
}

class GFTUtils {
	static double bulletVelocity(double power) {
		return 20 - 3 * power;
	}
	
	static Point2D project(Point2D sourceLocation, double angle, double length) {
		return new Point2D.Double(sourceLocation.getX() + Math.sin(angle) * length,
				sourceLocation.getY() + Math.cos(angle) * length);
	}
	
	static double absoluteBearing(Point2D source, Point2D target) {
		return Math.atan2(target.getX() - source.getX(), target.getY() - source.getY());
	}
	
	static int sign(double v) {
		return v < 0 ? -1 : 1;
	}
	
	static int minMax(int v, int min, int max) {
		return Math.max(min, Math.min(max, v));
	}
}

class GFTMovement {
	private static final double BATTLE_FIELD_WIDTH = 800;
	private static final double BATTLE_FIELD_HEIGHT = 600;
	private static final double WALL_MARGIN = 18;
	private static final double MAX_TRIES = 125;
	private static final double REVERSE_TUNER = 0.421075;
	private static final double DEFAULT_EVASION = 1.2;
	private static final double WALL_BOUNCE_TUNER = 0.699484;
	
	private AdvancedRobot robot;
	private Rectangle2D fieldRectangle = new Rectangle2D.Double(WALL_MARGIN, WALL_MARGIN,
			BATTLE_FIELD_WIDTH - WALL_MARGIN * 2, BATTLE_FIELD_HEIGHT - WALL_MARGIN * 2);
	private double enemyFirePower = 3;
	private double direction = 0.4;
	
	GFTMovement(AdvancedRobot _robot) {
		this.robot = _robot;
	}
	
	public void onScannedRobot(ScannedRobotEvent e) {
		double enemyAbsoluteBearing = robot.getHeadingRadians() + e.getBearingRadians();
		double enemyDistance = e.getDistance();
		Point2D robotLocation = new Point2D.Double(robot.getX(), robot.getY());
		Point2D enemyLocation = GFTUtils.project(robotLocation, enemyAbsoluteBearing, enemyDistance);
		Point2D robotDestination;
		double tries = 0;
		while (!fieldRectangle.contains(robotDestination = GFTUtils.project(enemyLocation, enemyAbsoluteBearing + Math.PI + direction,
				enemyDistance * (DEFAULT_EVASION - tries / 100.0))) && tries < MAX_TRIES) {
			tries++;
		}
		if ((Math.random() < (GFTUtils.bulletVelocity(enemyFirePower) / REVERSE_TUNER) / enemyDistance ||
				tries > (enemyDistance / GFTUtils.bulletVelocity(enemyFirePower) / WALL_BOUNCE_TUNER))) {
			direction = -direction;
		}
		// Jamougha's cool way
		double angle = GFTUtils.absoluteBearing(robotLocation, robotDestination) - robot.getHeadingRadians();
		robot.setAhead(Math.cos(angle) * 100);
		robot.setTurnRightRadians(Math.tan(angle));
	}
}