package demo2;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;

import robocode.*;
import robocode.util.Utils;

/*
	@author: Nguyen Thanh Nam & Nguyen Trung Kien
	@class: IT93
	@algorithm: Melee Strategy + GuessFactor
 */
public class Cham extends AdvancedRobot {
	// create Robot class
	class Robot extends Point2D.Double {
		public long scanTime;  // represents the scan time of the robot
		public boolean alive = true; // check the status of the robot on the battlefield
		public double energy;  // energy of the robot
		public String name;  // name of the robot
		public double gunHeadingRadians;  // the heading angle of the gun
		public double absoluteBearingRadians; // the absolute bearing angle
		public double velocity;  // velocity of robot
		public double heading;  // heading angle of the robot
		public double lastHeading;  // last heading angle of the robot
		public double shootAbleScore; // represents for the shoot score of the robot based on the remaining energy. The smaller the robot’s score, the closer it is to death
		public double dist;  // distance of robot
	}
	
	// create Utility class
	public static class Utility {
		// Returns the value between the three were passed.
		static double clamp(double value, double min, double max) {
			return Math.max(min, Math.min(max, value));
		}
		
		// Returns the value between the three were passed.
		static double randomBetween(double min, double max) {
			return min + Math.random() * (max - min);
		}
		
		// Returns the position of the enemy robot.
		static Point2D project(Point2D sourceLocation, double angle, double length) {
			return new Point2D.Double(sourceLocation.getX() + Math.sin(angle) * length,
					sourceLocation.getY() + Math.cos(angle) * length);
		}
		
		// Calculates the angle of the point in the polar coordinate system
		// which was created from two other points in the rectangular coordinate system
		static double absoluteBearing(Point2D source, Point2D target) {
			return Math.atan2(target.getX() - source.getX(), target.getY() - source.getY());
		}
		
		// reverse direction
		static int sign(double v) {
			return v < 0 ? -1 : 1;
		}
	}
	
	// Movement_1VS1 class is created to advanced portability to fit solo mode
	class Movement_1VS1 {
		private static final double BATTLE_FIELD_WIDTH = 800;  // represents the width of the field robot can move.
		private static final double BATTLE_FIELD_HEIGHT = 600; // represents the height of the field robot can move.
		private static final double MAX_TRY_TIME = 125; // the amount of the time robot tries to move
		private static final double REVERSE_TUNER = 0.421075; // a constant number to tune the direction of the robot.
		private static final double DEFAULT_EVASION = 1.2; // a constant number represents the evasion ratio of the robot.
		private static final double WALL_BOUNCE_TUNER = 0.699484;  // constant number represents the wall-avoiding ratio of the robot.
		private final AdvancedRobot robot; // the robot wants to create the wave
		private final Rectangle2D fireField = new Rectangle2D.Double(WALL_MARGIN, WALL_MARGIN,
				BATTLE_FIELD_WIDTH - WALL_MARGIN * 2, BATTLE_FIELD_HEIGHT - WALL_MARGIN * 2); // represents a fire area.
		private final double enemyFirePower = 3;  // represents the bullet damage of enemy
		private double direction = 0.4; // direction of enemy
		// init
		Movement_1VS1(AdvancedRobot _robot) {
			this.robot = _robot;
		}
		// scan robot
		public void onScannedRobot(ScannedRobotEvent e) {
			// Create a robot that symbolizes the enemy robot and calculate values such as absolute bearing angle, distance.
			Robot enemy = new Robot();
			enemy.absoluteBearingRadians = robot.getHeadingRadians() + e.getBearingRadians();
			enemy.dist = e.getDistance();
			// Then calculate the coordinates 1 point representing the current location of our robot,
			// a representative for the current enemy robot position.
			Point2D robotLocation = new Point2D.Double(robot.getX(), robot.getY());
			Point2D enemyLocation = Utility.project(robotLocation, enemy.absoluteBearingRadians, enemy.dist);
			Point2D robotDestination;
			// Create 1 more point to store the destination of the robot (used for the rear), 1 tryTime value = 0.
			double tryTime = 0;
			// Migration algorithm
			while (!fireField.contains(robotDestination = Utility.project(enemyLocation, enemy.absoluteBearingRadians + Math.PI + direction,
					enemy.dist * (DEFAULT_EVASION - tryTime / 100.0))) && tryTime < MAX_TRY_TIME)
				tryTime++;
			// Change the direction of movement
			if ((Math.random() < (Rules.getBulletSpeed(enemyFirePower) / REVERSE_TUNER) / enemy.dist ||
					tryTime > (enemy.dist / Rules.getBulletSpeed(enemyFirePower) / WALL_BOUNCE_TUNER)))
				direction = -direction;
			// calculate the angle to move
			double angle = Utility.absoluteBearing(robotLocation, robotDestination) - robot.getHeadingRadians();
			// Set up movement and radar rotation for the robot based on the angle just found
			robot.setAhead(Math.cos(angle) * 100);
			robot.setTurnRightRadians(Math.tan(angle));
		}
	}
	
	/*
	 * Wave class is created to use for collecting data to control movement, target, and shoot enemy,
	 * Wave class is inherited from Condition class to check condition when the robot should shoot the enemy
	 * GuessFactor Targeting is used in solo mode.
	 */
	static class Wave extends Condition {
		static Point2D targetLocation; // represents the location robot is aiming at.
		double bulletPower;  // represents the power of the bullet.
		Point2D gunLocation;  // represents the location of the gun when robot fires.
		double bearing;  // the bearing angle
		double lateralDirection;  // direction of the robot.
		private static final double MAX_DISTANCE = 900;  // maximum distance between our robot and enemy robot.
		private static final int DISTANCE_INDEXES = 5;  // indexes of the segments that stored distant values of the enemy robot were collected
		private static final int VELOCITY_INDEXES = 5;  // indexes of the segments that stored velocity values of the enemy robot were collected.
		private static final int BINS = 25;  //  the number of bins uses to stored data.
		private static final int MIDDLE_BIN = (BINS - 1) / 2;  // middle data of bins
		private static final double MAX_ESCAPE_ANGLE = 0.7;  // max escape angle of robot
		private static final double BIN_WIDTH = MAX_ESCAPE_ANGLE / (double) MIDDLE_BIN;  // bin width
		// Guess Factor
		private static final int[][][][] statBuffers = new int[DISTANCE_INDEXES][VELOCITY_INDEXES][VELOCITY_INDEXES][BINS];
		private int[] buffer;
		private double distanceTraveled;
		private final AdvancedRobot robot;
		// init
		Wave(AdvancedRobot _robot) {
			this.robot = _robot;
		}
		// use for testing the conditions to calculate whether the bullet can hit the target or not.
		public boolean test() {
			advance();
			if (hasArrived()) {
				buffer[currentBin()]++;
				robot.removeCustomEvent(this);
			}
			return false;
		}
		// calculate offset of the bearing angle = direction * 0.05 - ( index of the highest data bin -14).
		double mostVisitedBearingOffset() {
			return (lateralDirection * BIN_WIDTH) * (mostVisitedBin() - MIDDLE_BIN);
		}
		// get data for all bins from data aggregated and analyzed.
		void setSegmentations(double distance, double velocity, double lastVelocity) {
			int distanceIndex = (int) (distance / (MAX_DISTANCE / DISTANCE_INDEXES));
			int velocityIndex = (int) Math.abs(velocity / 2);
			int lastVelocityIndex = (int) Math.abs(lastVelocity / 2);
			buffer = statBuffers[distanceIndex][velocityIndex][lastVelocityIndex];
		}
		// calculate the travel distance of the bullet
		private void advance() {
			distanceTraveled += Rules.getBulletSpeed(bulletPower);
		}
		// check to see if the fired bullet reaches the target
		private boolean hasArrived() {
			return distanceTraveled > gunLocation.distance(targetLocation) - WALL_MARGIN;
		}
		// return the current data bin.
		private int currentBin() {
			int bin = (int) Math.round(((Utils.normalRelativeAngle
					(Cham.Utility.absoluteBearing(gunLocation, targetLocation) - bearing)) /
					(lateralDirection * BIN_WIDTH)) + MIDDLE_BIN);
			return (int) Utility.clamp(bin, 0, BINS - 1);
		}
		// return the index of highest data bin
		private int mostVisitedBin() {
			int mostVisited = MIDDLE_BIN;
			for (int i = 0; i < BINS; i++)
				if (buffer[i] > buffer[mostVisited])
					mostVisited = i;
			return mostVisited;
		}
	}
	
	// change color of robot in battlefield, it so funny
	static Random random = new Random();
	
	private void changeColor() {
		setColors(new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)),
				new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)),
				new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)),
				new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)),
				new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
	}
	
	// robot do something when it win
	public void onWin(WinEvent event) {
		while (true) {
			changeColor();
			turnRadarRight(360);
		}
	}
	
	// Represents the amount of the possible location which the robot can reach. In this case, we need 150 points.
	static final int AMOUNT_PREDICTED_POINTS = 150;
	// Represents for the least distance from the robot to the wall for avoiding wall
	static final double WALL_MARGIN = 18;
	// Use HashMap to a stored list of the opponent robot (each enemy is unique) to calculate reasonable movement, target location,...
	HashMap<String, Robot> enemyList = new HashMap<>();
	// Represents our robot to store some necessary data.
	Robot myRobot = new Robot();
	// Represents an opponent robot that we are targeting.
	Robot targetBot;
	// Uses to stored a list of points to which our robot can move to.
	List<Point2D.Double> possibleLocations = new ArrayList<>();
	// Represents the location of the point which has the lowest risk.
	Point2D.Double targetPoint = new Point2D.Double(60, 60);
	// Uses to reduce call methods get information of the battlefield (getBattleFieldWidth và getBattleFieldHeight).
	// At the same time, it determines the size of the battlefield.
	Rectangle2D.Double battleField = new Rectangle2D.Double();
	// Represents the amount of time that the robot needs to reach the lowest risk location.
	// If the robot needs a too long time, the robot will recalculate the destination
	int idleTime = 30;
	private static double lateralDirection; // lateral direction of the robot.
	private static double preEnemyVelocity; // previous lateral velocity.
	private static Movement_1VS1 movement1VS1; // object performs how the robot moves in solo mode.
	
	// init
	public Cham() {
		movement1VS1 = new Movement_1VS1(this);
	}
	
	// run robot
	public void run() {
		// Size of the battle field assigns to battleField.
		battleField.height = getBattleFieldHeight();
		battleField.width = getBattleFieldWidth();
		// Coordinate, default energy assigns to variable which represents for our robot
		myRobot.x = getX();
		myRobot.y = getY();
		myRobot.energy = getEnergy();
		// Current coordinate of our robot assigns to lowest target point
		targetPoint.x = myRobot.x;
		targetPoint.y = myRobot.y;
		// Create a robot to represent the enemy robot that we are aiming at.
		// Since we have not yet targeted any specific object, keep its status is dead.
		targetBot = new Robot();
		targetBot.alive = false;
		// The gun turn setting doesn't depend on the direction of the robot turn so
		// that the robot can move in one direction and still shoot the target in the other direction
		setAdjustGunForRobotTurn(true);
		// The radar turn setting doesn't depend on the direction of the gun turn so
		// that the robot can shoot in one direction and still scan the target in the other direction.
		setAdjustRadarForGunTurn(true);
		// if getOthers > 1 => chaos mode
		if (getOthers() > 1) {
			updateListLocations(AMOUNT_PREDICTED_POINTS); // Update list of possible positions of the robot with a specific amount
			setTurnRadarRightRadians(Double.POSITIVE_INFINITY); // Set the radar turn right infinitely.
			// Update data of our robot (location, previous direction, current direction, energy, current radar direction)
			while (true) {
				myRobot.lastHeading = myRobot.heading;
				myRobot.heading = getHeadingRadians();
				myRobot.x = getX();
				myRobot.y = getY();
				myRobot.energy = getEnergy();
				myRobot.gunHeadingRadians = getGunHeadingRadians();
				// Check each robot in the list of the enemy robot,
				// if the result when the current time subtracts scanned time is greater than 25,
				// we know the data is because it isn’t updated after 25 ticks. We will assign dead to the status of that robot.
				// After that, we check the targeting robot is that robot, we also assign dead to the status of the targeting robot
				Iterator<Robot> enemiesIterator = enemyList.values().iterator();
				while (enemiesIterator.hasNext()) {
					Robot r = enemiesIterator.next();
					if (getTime() - r.scanTime > 25) {
						r.alive = false;
						if (targetBot.name != null && r.name.equals(targetBot.name))
							targetBot.alive = false;
					}
				}
				// Setting for robot move
				// If targeting robot’s status is alive, shoot it
				movement();
				if (targetBot.alive)
					shooting();
				execute();
			}
		}
		// solo mode
		else {
			// Set default lateral direction is 1 (go ahead)
			lateralDirection = 1;
			// Set the value of the variable which stored the previous velocity of the enemy robot as 0
			// (when starting the game, the enemy robot doesn't have the previous velocity so we set it as 0)
			preEnemyVelocity = 0;
			do {
				turnRadarRightRadians(Double.POSITIVE_INFINITY); // Turn radar right infinitely to scan enemy robot
			} while (true);
		}
	}
	
	// scan robot
	public void onScannedRobot(ScannedRobotEvent e) {
		changeColor(); // change color my robot in battlefield
		// if getOthers > 1 => chaos mode
		if (getOthers() > 1) {
			// we get information about the robot in the enemy robot list which has the name is the same as the name of the scanned robot.
			// If there no have a robot in the list, add the scanned robot to the list
			Robot en = enemyList.get(e.getName());
			if (en == null) {
				en = new Robot();
				enemyList.put(e.getName(), en);
			}
			// Update necessary data for the enemy robot (bearing, heading, energy, position, shootAbleScore..)
			en.absoluteBearingRadians = e.getBearingRadians();
			en.setLocation(new Point2D.Double(
					myRobot.x + e.getDistance() * Math.sin(getHeadingRadians() + en.absoluteBearingRadians),
					myRobot.y + e.getDistance() * Math.cos(getHeadingRadians() + en.absoluteBearingRadians)));
			en.lastHeading = en.heading;
			en.name = e.getName();
			en.energy = e.getEnergy();
			en.alive = true;
			en.scanTime = getTime();
			en.velocity = e.getVelocity();
			en.heading = e.getHeadingRadians();
			en.shootAbleScore = en.energy < 25 ? (en.energy < 5 ?
					(en.energy == 0 ? Double.MIN_VALUE : en.distance(myRobot) * 0.1) :
					en.distance(myRobot) * 0.75) : en.distance(myRobot);
			// In case there have only one enemy robot, we will lock radar with achieving always target enemy to shot
			if (getOthers() == 1) {
				setTurnRadarLeftRadians(getRadarTurnRemainingRadians());
			}
			// If the targeting robot has status is dead or has shootAbleScore is greater than shootAbleScore of our robot,
			// we will convert the target into a robot that has just been scanned
			if (!targetBot.alive || en.shootAbleScore < targetBot.shootAbleScore)
				targetBot = en;
		}
		// solo mode
		else {
			setScanColor(Color.red);
			// Create a new robot represent the enemy robot and set some information for it
			// (absolute bearing angle, distance between that robot and our robot, current velocity of that robot)
			Robot enemy = new Robot();
			enemy.absoluteBearingRadians = getHeadingRadians() + e.getBearingRadians();
			enemy.dist = e.getDistance();
			enemy.velocity = e.getVelocity();
			// Check if the robot is moving or not (based on speed). If it moves (velocity > 0 ),
			// set its direction of movement by checking the calculated velocity value.
			// If the value is negative, the robot will move backward, otherwise, the robot will move forward.
			if (enemy.velocity != 0)
				lateralDirection = Utility.sign(enemy.velocity * Math.sin(e.getHeadingRadians() - enemy.absoluteBearingRadians));
			// Create a wave for our robot and set appropriate values for it
			// (our shot position, the enemy robot’s coordinates, the enemy’s movement direction)
			Wave wave = new Wave(this);
			wave.gunLocation = new Point2D.Double(getX(), getY());
			Wave.targetLocation = Utility.project(wave.gunLocation, enemy.absoluteBearingRadians, enemy.dist);
			wave.lateralDirection = lateralDirection;
			// Set segment values (enemy distance, current enemy velocity, previous enemy velocity)
			wave.setSegmentations(enemy.dist, enemy.velocity, preEnemyVelocity);
			// Update the enemy robot’s previous velocity to its current velocity
			preEnemyVelocity = enemy.velocity;
			// then set the wave bearing angle to be the absolute bearing angle of the enemy robot.
			wave.bearing = enemy.absoluteBearingRadians;
			// We then set up the radar to turn an angle after it has been normalized within the range (-π,π)
			// Then the rotation angle o the radar = absolute bearing angle
			// of the enemy robot – the angle of the robot’s gun + the offset angle calculated by the wave
			setTurnGunRightRadians(Utils.normalRelativeAngle(
					enemy.absoluteBearingRadians - getGunHeadingRadians() + wave.mostVisitedBearingOffset()));
			// Set the energy of the bullets fired based o the energy of us an enemy to limit energy expenditure in a wasteful manner and shoot.
			wave.bulletPower = Math.min(2.5, Math.min(this.getEnergy(), e.getEnergy()) / (double) 4);
			setFire(wave.bulletPower);
			// Check the remaining energy of the robot. If there is still enough energy, continue adding an event to create waves for the next wave
			if (getEnergy() >= wave.bulletPower)
				addCustomEvent(wave);
			// Perform a scan of the enemy robot while moving,
			// then reset the radar rotation to continue to adjust the scan angle for the next wave.
			movement1VS1.onScannedRobot(e);
			setTurnRadarRightRadians(Utils.normalRelativeAngle(enemy.absoluteBearingRadians - getRadarHeadingRadians()) * 2);
		}
	}
	
	// check enemy death
	public void onRobotDeath(RobotDeathEvent event) {
		// Check if there have any robot in the list that has to name that is
		// the same as the name of the dead robot, set status of that robot is dead.
		if (enemyList.containsKey(event.getName())) {
			enemyList.get(event.getName()).alive = false;
		}
		// Check if our targeting robot has a name that is the same as the name of the dead robot,
		// set status of that robot is dead
		if (event.getName().equals(targetBot.name))
			targetBot.alive = false;
	}
	
	// shot robot
	public void shooting() {
		// Checks the status of the robot we are aiming for. If it is alive, takes the following actions
		if (targetBot != null && targetBot.alive) {
			// Calculates the energy of the bullets we will fire with the target achieves the greatest amount of damage with the least energy.
			double dist = myRobot.distance(targetBot);
			double power = (dist > 850 ? 0.1 : (dist > 700 ? 0.5 : (dist > 250 ? 2.0 : 3.0)));
			power = Math.min(myRobot.energy / 4d, Math.min(targetBot.energy / 3d, power));
			power = Utility.clamp(power, 0.1, 3.0);
			/* Targeting: Uses the algorithm of Circular Targeting method */
			// Step 1: Get the current location of our targeting robot to set the current targeting location,
			// time needs to hit the enemy is 0, deltaHead
			long deltaHitTime;
			Point2D.Double shootAt = new Point2D.Double();
			double head, deltaHead, bulletSpeed;
			double predictX, predictY;
			predictX = targetBot.getX();
			predictY = targetBot.getY();
			head = targetBot.heading;
			deltaHead = head - targetBot.lastHeading;
			shootAt.setLocation(predictX, predictY);
			deltaHitTime = 0;
			// Step 2: Loop until the distance between our robot and aiming position – WALL_MARGIN) / velocity of the bullet
			// with specific power <= amount of time needs to hit (t =s/v)
			do {
				predictX += Math.sin(head) * targetBot.velocity;
				predictY += Math.cos(head) * targetBot.velocity;
				head += deltaHead;
				deltaHitTime++;
				Rectangle2D.Double fireField = new Rectangle2D.Double(WALL_MARGIN, WALL_MARGIN,
						battleField.width - WALL_MARGIN, battleField.height - WALL_MARGIN);
				if (!fireField.contains(predictX, predictY)) {
					bulletSpeed = shootAt.distance(myRobot) / deltaHitTime;
					power = Utility.clamp((20 - bulletSpeed) / 3.0, 0.1, 3.0);
					break;
				}
				shootAt.setLocation(predictX, predictY);
			} while ((int) Math.round((shootAt.distance(myRobot) - WALL_MARGIN) / Rules.getBulletSpeed(power)) > deltaHitTime);
			// Step 3: Set new value for the targeting location based on predict coordinates and size of the battle field to ensure it is always correct
			shootAt.setLocation(Utility.clamp(predictX, 34, getBattleFieldWidth() - 34),
					Utility.clamp(predictY, 34, getBattleFieldHeight() - 34));
			// Step 4: Set condition to fire (with purpose is saving as much energy as possible
			if ((getGunHeat() == 0.0) && (getGunTurnRemaining() == 0.0) && (power > 0.0) && (myRobot.energy > 0.1)) {
				setFire(power);
			}
			// Step 5: Turn radar
			setTurnGunRightRadians(Utils.normalRelativeAngle(((Math.PI / 2) - Math.atan2(shootAt.y - myRobot.getY(),
					shootAt.x - myRobot.getX())) - getGunHeadingRadians()));
		}
	}
	
	// move robot
	public void movement() {
		// Check if the distance between the target point and current robot location is less than 15
		// or targeting time is too long (idleTime > 25 tick), perform the following actions.
		if (targetPoint.distance(myRobot) < 15 || idleTime > 25) {
			idleTime = 0; // Initialize value for idleTime = 0 aim to new target point
			updateListLocations(AMOUNT_PREDICTED_POINTS); // Update list of possible points around the robot
			// Traversal list of points to find out the least risk point and change the new point
			// which has just been found into the new target point of robot.
			Point2D.Double lowRiskP = null;
			double lowestRisk = Double.MAX_VALUE;
			for (Point2D.Double p : possibleLocations) {
				double currentRisk = evaluatePoint(p);
				if (currentRisk <= lowestRisk || lowRiskP == null) {
					lowestRisk = currentRisk;
					lowRiskP = p;
				}
			}
			targetPoint = lowRiskP;
		}
		// Keep moving to the target point and perform the following
		else {
			idleTime++; // Increase idleTime by 1 unit
			// Compute the angle between our robot and a target point (alpha)
			double angle = Utility.absoluteBearing(myRobot, targetPoint) - getHeadingRadians();
			double direction = 1; // Set the direction as 1 (go ahead)
			// Check if cos(alpha) < 0, reverse direction because moving back and turning robot
			// will be faster than moving ahead in this case. (Choose the best direction to move)
			if (Math.cos(angle) < 0) {
				angle += Math.PI;
				direction *= -1;
			}
			// Set the fit velocity for the robot to the number of turns is the least
			setMaxVelocity(10 - (4 * Math.abs(getTurnRemainingRadians())));
			// Moving ahead or back a distance equals the distance between our robot and target point
			setAhead(myRobot.distance(targetPoint) * direction);
			// Normalize alpha angle (for a range of alpha is in (-π,π))
			angle = Utils.normalRelativeAngle(angle);
			// Turn radar right an angle equals to angle has just normalized
			setTurnRightRadians(angle);
		}
	}
	
	// update location
	public void updateListLocations(int n) {
		possibleLocations.clear(); // Clear all old positions in the list.
		// Set the range of x coordinate for the robot’s movement by using 125 * 1.5. The value will be rounded down to the nearest whole number.
		final int xRange = (int) (125 * 1.5);
		// Use the loop with the maximum time as the amount of the predicted locations
		for (int i = 0; i < n; i++) {
			double randXMod = Utility.randomBetween(-xRange, xRange);
			double yRange = Math.sqrt(xRange * xRange - randXMod * randXMod);
			double randYMod = Utility.randomBetween(-yRange, yRange);
			double y = Utility.clamp(myRobot.y + randYMod, 75, battleField.height - 75);
			double x = Utility.clamp(myRobot.x + randXMod, 75, battleField.width - 75);
			possibleLocations.add(new Point2D.Double(x, y));
		}
	}
	
	// evaluate point
	public double evaluatePoint(Point2D.Double p) {
		// Anti – Gravity Movement technique
		double rickValue = Utility.randomBetween(1, 2.25) / p.distanceSq(myRobot);
		rickValue += (6 * (getOthers() - 1)) / p.distanceSq(battleField.width / 2, battleField.height / 2);
		double cornerFactor = getOthers() <= 5 ? getOthers() == 1 ? 0.25 : 0.5 : 1;
		rickValue += cornerFactor / p.distanceSq(0, 0);
		rickValue += cornerFactor / p.distanceSq(battleField.width, 0);
		rickValue += cornerFactor / p.distanceSq(0, battleField.height);
		rickValue += cornerFactor / p.distanceSq(battleField.width, battleField.height);
		// Check the status of the targeting robot is alive or dead
		if (targetBot.alive) {
			double robotAngle = Utils.normalRelativeAngle(Utility.absoluteBearing(p, targetBot) - Utility.absoluteBearing(myRobot, p));
			Iterator<Robot> enemiesIterator = enemyList.values().iterator();
			while (enemiesIterator.hasNext()) {
				Robot en = enemiesIterator.next();
				rickValue += (en.energy / myRobot.energy) * (1 / p.distanceSq(en)) * (1.0 + ((1 - (Math.abs(Math.sin(robotAngle)))) +
						Math.abs(Math.cos(robotAngle))) / 2) * (1 + Math.abs(Math.cos(Utility.absoluteBearing(myRobot, p) - Utility.absoluteBearing(en, p))));
			}
		}
		// If there have no enemy robot on the list
		else if (enemyList.values().size() >= 1) {
			Iterator<Robot> enIterator = enemyList.values().iterator(); // Get each enemy robot in the list to compute risk.
			// New risk = old risk + danger level of the enemy robot * gravity force * danger level when move closely enemy robot
			while (enIterator.hasNext()) {
				Robot en = enIterator.next();
				rickValue += (en.energy / myRobot.energy) * (1 / p.distanceSq(en)) * (1 + Math.abs(Math.cos(Utility.absoluteBearing(myRobot, p) - Utility.absoluteBearing(en, p))));
			}
		}
		// New risk = old risk + danger level of the point which our robot is aiming to
		else {
			rickValue += (1 + Math.abs(Utility.absoluteBearing(myRobot, targetPoint) - getHeadingRadians()));
		}
		return rickValue;
	}
}