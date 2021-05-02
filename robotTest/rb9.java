package demo2;
import robocode.AdvancedRobot;
import robocode.HitByBulletEvent;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

class WaveBullet {
	private double startX, startY, startBearing, power;
	private long fireTime;
	private int direction;
	private int[] returnSegment;
	
	public WaveBullet(double x, double y, double bearing, double power, int direction, long time, int[] segment) {
		startX = x;
		startY = y;
		startBearing = bearing;
		this.power = power;
		this.direction = direction;
		fireTime = time;
		returnSegment = segment;
	}
	
	public double getBulletSpeed() {
		return 20 - power * 3;
	}
	
	public double maxEscapeAngle() {
		return Math.asin(8 / getBulletSpeed());
	}
	
	public boolean checkHit(double enemyX, double enemyY, long currentTime) {
		// if the distance from the wave origin to our enemy has passed
		// the distance the bullet would have traveled...
		if (Point2D.distance(startX, startY, enemyX, enemyY) <=
				(currentTime - fireTime) * getBulletSpeed()) {
			double desiredDirection = Math.atan2(enemyX - startX, enemyY - startY);
			double angleOffset = Utils.normalRelativeAngle(desiredDirection - startBearing);
			double guessFactor = Math.max(-1, Math.min(1, angleOffset / maxEscapeAngle())) * direction;
			int index = (int) Math.round((returnSegment.length - 1) / 2 * (guessFactor + 1));
			returnSegment[index]++;
			return true;
		}
		return false;
	}
} // end WaveBullet class

public class rb9 extends AdvancedRobot {
	ArrayList<WaveBullet> waves = new ArrayList<WaveBullet>();
	static int[] stats = new int[31]; // 31 is the number of unique GuessFactors we're using
	// Note: this must be odd number so we can get
	// GuessFactor 0 at middle.
	int direction = 1;
	
	
	// This class makes it easy to keep information about the enemy robots
	class Enemy {
		public Point2D.Double pos;
		public double energy;
		public double bearing;
		public double heading;
		public double distance;
		public double velocity;
		public boolean live;
	}
	
	// This class makes it easy to keep information about enemy "waves" to dodge bullets
	class EnemyWave {
		long fireTime;
		int direction;
		double bulletVelocity, directAngle, distanceTraveled;
		Point2D.Double fireLocation;
	}
	
	// Some globals
	
	// Using hashtable because it's less strict than an array (also faster!)
	private static Hashtable<String, Enemy> enemies = new Hashtable<String, Enemy>();
	private static Enemy target;
	// Point2D represents points in an (x, y) coordinate space
	private static Point2D.Double myPos;
	private static Point2D.Double nextDest;
	private static Point2D.Double lastPos;
	private static double myEnergy;
	private static double bulletPower;
	// Wavesurfing (bullet dodging) globals
	public static double oppEnergy = 100d;
	public static int BINS = 47;
	public static double surfStats[] = new double[BINS];
	public ArrayList<EnemyWave> enemyWaves;
	public ArrayList<Integer> surfDirections;
	public ArrayList<Double> surfAbsBearings;
	public Point2D.Double enemyLocation;
	public Point2D.Double myLocation;
	// Wallsmoothing (keeping distance from walls) globals
	public static Rectangle2D.Double fieldRect;
	public static final double WALL_STICK = 160;
	// Math.PI / 2 would be perpendicular movement, less will keep us moving away slightly
	public static final double NOT_PI = 1.25;
	
	
	public void run() {
		setColors(Color.darkGray, Color.black, Color.darkGray);
		
		fieldRect = new Rectangle2D.Double(18, 18, getBattleFieldWidth() - 36, getBattleFieldHeight() - 36);
		enemyWaves = new ArrayList<EnemyWave>();
		surfDirections = new ArrayList<Integer>();
		surfAbsBearings = new ArrayList<Double>();
		
		// This allows for the gun to move independent of robot
		setAdjustGunForRobotTurn(true);
		// This allows for the radar to move independent of the gun
		setAdjustRadarForGunTurn(true);
		
		// Tells robocode to spin my radar infinity times
		setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
		
		// Creates a new Enemy instance, this will be our target robot
		target = new Enemy();
		nextDest = lastPos = myPos = new Point2D.Double(getX(), getY());
		
		while (true) {
			// Updates position
			myPos = new Point2D.Double(getX(), getY());
			// Updates energy
			myEnergy = getEnergy();
			
			// Waits until scanning is complete, which takes at most 9 ticks apparently
			if (getTime() > 9 && target.live) {
				// Updates bulletPower, this calculation is important is takes into account my energy,
				// the enemy's energy, as well as the distance to the enemy
				bulletPower = Math.min(Math.min(myEnergy / 6d, 1300d / target.distance), target.energy / 3d);
				
				
				if (getOthers() > 1)
					movement();
			}
			execute();
		}
	}
	
	// Movement thanks to bot HawkOnFire, I used then commented some of its code
	public void movement() {
		double destDist = myPos.distance(nextDest);
		
		// Find new destination if current one reached
		if (destDist < 15) {
			double addLast = 1 - Math.rint(Math.pow(Math.random(), getOthers()));
			
			Rectangle2D.Double battleField = new Rectangle2D.Double(30, 30, getBattleFieldWidth() - 60, getBattleFieldHeight() - 60);
			Point2D.Double testPoint;
			
			// Calculate test points and test them for feasibility
			// If feasible, make point the new destination
			for (int i = 0; i < 200; i++) {
				testPoint = getPoint(myPos, Math.min(target.distance * 0.8, 100 + 200 * Math.random()), 2 * Math.PI * Math.random());
				if (battleField.contains(testPoint) && evaluate(testPoint, addLast) < evaluate(nextDest, addLast))
					nextDest = testPoint;
			}
			
			lastPos = myPos;
		} else {
			// Get angle of turn relative to current heading
			double angle = getAngle(nextDest, myPos) - getHeadingRadians();
			double direction = 1;
			
			// If cos(angle) is less than zero,
			// We know we will be going backwards
			if (Math.cos(angle) < 0) {
				angle += Math.PI;
				direction = -1;
			}
			
			setAhead(destDist * direction);
			setTurnRightRadians(angle = Utils.normalRelativeAngle(angle));
			
			// If the angle to turn is larger than one, stop (so we can turn faster)
			// Else, set the max velocity to 8.0
			setMaxVelocity(Math.abs(angle) > 1 ? 0 : 8d);
		}
	}
	
	public static double evaluate(Point2D.Double p, double addLast) {
		// Adds more movement, staying still = bad
		double eval = addLast * 0.08 / p.distanceSq(lastPos);
		
		Enumeration enums = enemies.elements();
		while (enums.hasMoreElements()) {
			Enemy en = (Enemy) enums.nextElement();
			// Math.min(en.energy/myEnergy,2) is multiplied because en.energy/myEnergy is an indicator how dangerous an enemy is
			// Math.abs(Math.cos(calcAngle(myPos, p) - calcAngle(en.pos, p))) is bigger if the moving direction isn't good in relation
			// to a certain bot. it would be more natural to use Math.abs(Math.cos(calcAngle(p, myPos) - calcAngle(en.pos, myPos)))
			// 1 / p.distanceSq(en.pos) is anti-gravity (as it's known)
			// http://robowiki.net/wiki/Anti-Gravity_Tutorial
			if (en.live) {
				eval += Math.min(en.energy / myEnergy, 2) * (1 + Math.abs(Math.cos(getAngle(myPos, p) - getAngle(en.pos, p)))) / p.distanceSq(en.pos);
			}
		}
		
		return eval;
	}
	
	// Shoots, turning gun with values given in onScannedRobot method
	public void shoot() {
        /*If in a 1v1, use predictive firing, else shoot ignore physics and just shoot at current position.
        In an arena fight, this works a lot better than you might think
        Also, if my energy is low and the enemy's isn't, then I must not be making progress with predictive firing,
        so I switch back to caveman shooting. DISCLAIMER: Disabled as I find regular shooting more effective and adaptive.
        Predictive firing IS better, but needs much more code to implement correctly and is not worth it.*/
		
		if (getGunTurnRemaining() == 0 && myEnergy > 1)
			setFire(bulletPower);
		
		setTurnGunRightRadians(Utils.normalRelativeAngle(getAngle(target.pos, myPos) - getGunHeadingRadians()));
	}
	
	public void onScannedRobot(ScannedRobotEvent e) {
		myLocation = new Point2D.Double(getX(), getY());
		Enemy baddie = enemies.get(e.getName());
		
		// If the detected enemy isn't already in our "database", add it
		if (baddie == null) {
			baddie = new Enemy();
			enemies.put(e.getName(), baddie);
		}
		
		// Collects some info about the enemy
		baddie.energy = e.getEnergy();
		baddie.live = true;
		// Calculates enemy's position, passing the TOTAL angle
		// we add getHeadingRadians() because we want the total angle
		// and e.getBearingRadians() only gives the angle relative to us
		baddie.pos = getPoint(myLocation, e.getDistance(), getHeadingRadians() + e.getBearingRadians());
		baddie.bearing = e.getBearingRadians();
		baddie.heading = e.getHeadingRadians();
		baddie.velocity = e.getVelocity();
		baddie.distance = e.getDistance();
		
		// If the current target is dead, or this newly scanned enemy is closer,
		// or if weak, then the newly scanned enemy becomes the new target
		if (!target.live || e.getDistance() < target.distance || e.getEnergy() <= 16d) {
			target = baddie;
		}
		
		double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
		
		// If there's only one enemy left, then the radar is locked
		if (getOthers() == 1)
			setTurnRadarLeftRadians(getRadarTurnRemainingRadians());
		//shoot();
		
		// The last section here is for wave surfing (dodging bullets)
		
		// My velocity sideways
		double lateralVelocity = getVelocity() * Math.sin(e.getBearingRadians());
		
		// If robot's sideways velocity is positive (or 0), direction is 1. Else, it's -1
		surfDirections.add(0, lateralVelocity >= 0 ? 1 : -1);
		surfAbsBearings.add(0, absoluteBearing + Math.PI);
		
		// This DOES NOT refer to the global called bulletPower
		double bulletPower = oppEnergy - e.getEnergy();
		if (bulletPower < 3.01 && bulletPower > 0.09 && surfDirections.size() > 2) {
			EnemyWave ew = new EnemyWave();
			ew.fireTime = getTime() - 1;
			ew.bulletVelocity = bulletVelocity(bulletPower);
			ew.distanceTraveled = bulletVelocity(bulletPower);
			ew.direction = surfDirections.get(2);
			ew.directAngle = surfAbsBearings.get(2);
			ew.fireLocation = (Point2D.Double) enemyLocation.clone();
			
			enemyWaves.add(ew);
		}
		
		oppEnergy = e.getEnergy();
		
		// Update after, as prediction needs previous, not current, position as wave source
		enemyLocation = project(myLocation, absoluteBearing, e.getDistance());
		
		if (getOthers() == 1) {
			updateWaves();
			doSurfing();
		}
		
		// Enemy absolute bearing, you can use your one if you already declare it.
		double absBearing = getHeadingRadians() + e.getBearingRadians();
		
		// find our enemy's location:
		double ex = getX() + Math.sin(absBearing) * e.getDistance();
		double ey = getY() + Math.cos(absBearing) * e.getDistance();
		
		// Let's process the waves now:
		for (int i = 0; i < waves.size(); i++) {
			WaveBullet currentWave = (WaveBullet) waves.get(i);
			if (currentWave.checkHit(ex, ey, getTime())) {
				waves.remove(currentWave);
				i--;
			}
		}
		
		// don't try to figure out the direction they're moving
		// they're not moving, just use the direction we had before
		if (e.getVelocity() != 0) {
			if (Math.sin(e.getHeadingRadians() - absBearing) * e.getVelocity() < 0)
				direction = -1;
			else
				direction = 1;
		}
		int[] currentStats = stats; // This seems silly, but I'm using it to
		// show something else later
		WaveBullet newWave = new WaveBullet(getX(), getY(), absBearing, bulletPower, direction, getTime(), currentStats);
		
		int bestindex = 15;    // initialize it to be in the middle, guessfactor 0.
		for (int i = 0; i < 31; i++)
			if (currentStats[bestindex] < currentStats[i])
				bestindex = i;
		
		// this should do the opposite of the math in the WaveBullet:
		double guessfactor = (double) (bestindex - (stats.length - 1) / 2) / ((stats.length - 1) / 2);
		double angleOffset = direction * guessfactor * newWave.maxEscapeAngle();
		double gunAdjust = Utils.normalRelativeAngle(absBearing - getGunHeadingRadians() + angleOffset);
		setTurnGunRightRadians(gunAdjust);
		
		if (getGunHeat() == 0 && gunAdjust < Math.atan2(9, e.getDistance()) && setFireBullet(bulletPower) != null)
			waves.add(newWave);
	}
	
	public void onRobotDeath(RobotDeathEvent e) {
		// When an enemy robot dies, update the hashtable
		// so that we can stop shooting at him
		enemies.get(e.getName()).live = false;
	}
	
	// Calculations
	
	public static double limit(double value, double min, double max) {
		// Finds the smallest of either: 1. the maximum possible value
		// 2. the larger of either: the minimum possible value and the value
		// Useful for the annoying predictive firing above
		return Math.min(max, Math.max(min, value));
	}
	
	private static double getAngle(Point2D.Double p2, Point2D.Double p1) {
		/**
		 *  This gets the inverse tangent of x/y
		 *  In this instance x = p2.x - p1.x
		 *  And y = p2.y - p1.y
		 *
		 *  This method helps by getting the angle (in radians)
		 *  between the current heading and the heading that is
		 *  necessary to reach the next destination.
		 */
		
		return Math.atan2(p2.x - p1.x, p2.y - p1.y);
	}
	
	private static Point2D.Double getPoint(Point2D.Double p, double dist, double angle) {
		/**
		 *  Returns a new instance of Point2D.Double that contains the enemy position
		 *
		 *  How this works: it takes my robot's current position on the x axis,
		 *  then it takes the distance multiplied by the sin of the angle
		 *  (the angle being the distance in radians of my robot's current bearing
		 *  to the enemy robot), which gives us the distance from my robot to the enemy
		 *  robot just on the x axis, and then it adds my robot's position on the x axis
		 *  to give the enemy's total position on the x axis.
		 *
		 *  It does the same thing for the y axis and voila! We have the enemy's coordinates.
		 * */
		
		return new Point2D.Double(p.x + dist * Math.sin(angle), p.y + dist * Math.cos(angle));
	}
	
	// The rest of the file is dedicated to dodging bullets aka "Wave Surfing"
	
	public void updateWaves() {
		for (int i = 0; i < enemyWaves.size(); i++) {
			EnemyWave ew = enemyWaves.get(i);
			
			ew.distanceTraveled = (getTime() - ew.fireTime) * ew.bulletVelocity;
			// If the bullet has safely passed me, remove it from the list
			if (ew.distanceTraveled > myLocation.distance(ew.fireLocation) + 50) {
				enemyWaves.remove(i);
				i--;
			}
		}
	}
	
	// Assesses enemy waves and returns the closest one
	public EnemyWave getClosestSurfableWave() {
		double closestDistance = 50000d; // just some big number at first
		EnemyWave surfWave = null;
		
		// For each EnemyWave in enemyWaves, find the closest wave
		for (EnemyWave ew : enemyWaves) {
			double distance = myLocation.distance(ew.fireLocation) - ew.distanceTraveled;
			
			if (distance > ew.bulletVelocity && distance < closestDistance) {
				surfWave = ew;
				closestDistance = distance;
			}
		}
		
		return surfWave;
	}
	
	// Returns the index of the GuessFactor... hard to explain
	// http://robowiki.net/wiki/GuessFactor
	public static int getFactorIndex(EnemyWave ew, Point2D.Double targetLocation) {
		double offsetAngle = (getAngle(ew.fireLocation, targetLocation) - ew.directAngle);
		double factor = Utils.normalRelativeAngle(offsetAngle) / maxEscapeAngle(ew.bulletVelocity) * ew.direction;
		
		return (int) limit((factor * ((BINS - 1) / 2)) + ((BINS - 1) / 2), 0, BINS - 1);
	}
	
	// Given the EnemyWave that the bullet was on, and the point where we
	// were hit, update our stat array to reflect the danger in that area.
	public void logHit(EnemyWave ew, Point2D.Double targetLocation) {
		int index = getFactorIndex(ew, targetLocation);
		
		for (int i = 0; i < BINS; i++) {
			// for the spot bin that we were hit on, add 1;
			// for the bins next to it, add 1 / 2;
			// the next one, add 1 / 5; and so on...
			surfStats[i] += 1d / (Math.pow(index - i, 2) + 1);
		}
	}
	
	public void onHitByBullet(HitByBulletEvent e) {
		// If enemyWaves is empty, and still we were hit by a bullet,
		// then we somehow missed detecting the wave.
		if (!enemyWaves.isEmpty()) {
			Point2D.Double hitBulletLocation = new Point2D.Double(e.getBullet().getX(), e.getBullet().getY());
			EnemyWave hitWave = null;
			
			// Check all EnemyWaves, try to find the one that hit us
			for (EnemyWave ew : enemyWaves) {
				// If the enemy wave is within 50 pixels and the bullet details (velocity) match, then we found it
				if (Math.abs(ew.distanceTraveled - myLocation.distance(ew.fireLocation)) < 50
						&& Math.abs(bulletVelocity(e.getBullet().getPower()) - ew.bulletVelocity) < 0.001) {
					hitWave = ew;
					break;
				}
			}
			
			// If we did indeed find the correct wave for the bullet that hit us...
			if (hitWave != null) {
				// Then log the hit to update surfing stats
				logHit(hitWave, hitBulletLocation);
				
				// And remove the wave
				enemyWaves.remove(enemyWaves.lastIndexOf(hitWave));
			}
		}
	}
	
	public Point2D.Double predictPosition(EnemyWave surfWave, int direction) {
		Point2D.Double predictedPos = (Point2D.Double) myLocation.clone();
		double predictedV = getVelocity();
		double predictedH = getHeadingRadians();
		double maxTurning, moveAngle, moveDir;
		
		int counter = 0; // # of ticks in future
		boolean intercepted = false;
		
		do {
			moveAngle = wallSmoothing(predictedPos, getAngle(surfWave.fireLocation, predictedPos) + (direction * NOT_PI), direction) - predictedH;
			moveDir = 1;
			
			// Checks for direction, flips if negative
			if (Math.cos(moveAngle) < 0) {
				moveAngle += Math.PI;
				moveDir = -1;
			}
			
			moveAngle = Utils.normalRelativeAngle(moveAngle);
			
			// This is the most you can turn in one tick, robocode rules
			maxTurning = Math.PI / 720d * (40d - 3d * Math.abs(predictedV));
			predictedH = Utils.normalRelativeAngle(predictedH + limit(-maxTurning, moveAngle, maxTurning));
			
			// If the predicted velocity and the moveDir have different signs, we want to decelerate
			// Else, we want to accelerate so we multiply moveDir by 2 and add that to the current predictedV
			predictedV += (predictedV * moveDir < 0 ? 2 * moveDir : moveDir);
			predictedV = limit(predictedV, -8d, 8d);
			
			// Calculates new predicted position
			predictedPos = project(predictedPos, predictedH, predictedV);
			
			++counter;
			
			// Checks if the wave has been intercepted
			if (predictedPos.distance(surfWave.fireLocation) < surfWave.distanceTraveled
					+ (counter * surfWave.bulletVelocity) + surfWave.bulletVelocity) {
				intercepted = true;
			}
		} while (!intercepted && counter < 500);
		
		return predictedPos;
	}
	
	// By getting the GuessFactor we can search the surfStats and return the safest direction to surf in
	public double checkDanger(EnemyWave surfWave, int direction) {
		int index = getFactorIndex(surfWave, predictPosition(surfWave, direction));
		
		return surfStats[index];
	}
	
	public void doSurfing() {
		EnemyWave surfWave = getClosestSurfableWave();
		
		if (surfWave == null) return;
		
		double dangerLeft = checkDanger(surfWave, -1);
		double dangerRight = checkDanger(surfWave, 1);
		
		double goAngle = getAngle(surfWave.fireLocation, myLocation);
		if (dangerLeft < dangerRight) {
			goAngle = wallSmoothing(myLocation, goAngle - (Math.PI / 2), -1);
		} else {
			goAngle = wallSmoothing(myLocation, goAngle + (Math.PI / 2), 1);
		}
		
		setBackAsFront(this, goAngle);
	}
	
	// Calculations for wave surfing
	
	// Calculates the largest angle that could cause the bullet to hit me
	public static double maxEscapeAngle(double v) {
		return Math.asin(8d / v);
	}
	
	// Calculates velocity of a bullet based on the power
	public static double bulletVelocity(double power) {
		return 20d - (3d * power);
	}
	
	// Changes my angle as I approach walls and stuff, hitting walls is bad
	public static double wallSmoothing(Point2D.Double botLocation, double angle, int orientation) {
		while (!fieldRect.contains(project(botLocation, angle, WALL_STICK))) {
			angle += orientation * 0.05;
		}
		
		return angle;
	}
	
	// Dictates movement
	public static void setBackAsFront(AdvancedRobot robot, double goAngle) {
		double angle = Utils.normalRelativeAngle(goAngle - robot.getHeadingRadians());
		if (Math.abs(angle) > (Math.PI / 2)) {
			if (angle < 0) {
				robot.setTurnRightRadians(Math.PI + angle);
			} else {
				robot.setTurnLeftRadians(Math.PI - angle);
			}
			robot.setBack(100);
		} else {
			if (angle < 0) {
				robot.setTurnLeftRadians(-angle);
			} else {
				robot.setTurnRightRadians(angle);
			}
			robot.setAhead(100);
		}
	}
	
	// Takes my position, enemy's heading angle, and distance to enemy to predict enemy bullet pos
	public static Point2D.Double project(Point2D.Double source, double angle, double length) {
		return new Point2D.Double(source.x + Math.sin(angle) * length, source.y + Math.cos(angle) * length);
	}
}