package demo2;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;

import robocode.*;
import robocode.util.Utils;

/*
	@author: Nguyen Thanh Nam & Nguyen Trung Kien
 */
public class rb7 extends AdvancedRobot {
	static class Robot extends Point2D.Double {
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
	
	static final int AMOUNT_PREDICTED_POINTS = 150;
	static final int WALL_MARGIN = 18;
	HashMap<String, Robot> enemyList = new HashMap<>();
	Robot myRobot = new Robot();
	Robot targetRobot;
	List<Point2D.Double> possibleLocations = new ArrayList<>();
	Point2D.Double targetPoint = new Point2D.Double(60, 60);
	Rectangle2D.Double battleField = new Rectangle2D.Double();
	int idleTime = 30;
	static Random random = new Random();
	
	@Override
	public void onWin(WinEvent event) {
		while (true) {
			changeColor();
			turnRadarRight(360);
		}
	}
	
	private void changeColor() {
		setColors(new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)),
				new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)),
				new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)),
				new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)),
				new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
	}
	
	public void run() {
		battleField.height = getBattleFieldHeight();
		battleField.width = getBattleFieldWidth();
		myRobot.x = getX();
		myRobot.y = getY();
		myRobot.energy = getEnergy();
		targetPoint.x = myRobot.x;
		targetPoint.y = myRobot.y;
		targetRobot = new Robot();
		targetRobot.alive = false;
		updateListLocations(AMOUNT_PREDICTED_POINTS);
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
		while (true) {
			myRobot.lastHeading = myRobot.heading;
			myRobot.heading = getHeadingRadians();
			myRobot.x = getX();
			myRobot.y = getY();
			myRobot.energy = getEnergy();
			myRobot.gunHeadingRadians = getGunHeadingRadians();
			Iterator<Robot> enIterator = enemyList.values().iterator();
			
			while (enIterator.hasNext()) {
				Robot r = enIterator.next();
				if (getTime() - r.scanTime > 25) {
					r.alive = false;
					if (targetRobot.name != null && r.name.equals(targetRobot.name))
						targetRobot.alive = false;
				}
			}
			movement();
			if (targetRobot.alive)
				shooting();
			execute();
		}
	}
	
	public void onScannedRobot(ScannedRobotEvent e) {
		changeColor();
		Robot en = enemyList.get(e.getName());
		if (en == null) {
			en = new Robot();
			enemyList.put(e.getName(), en);
		}
		en.bearingRadians = e.getBearingRadians();
		en.setLocation(new Point2D.Double(myRobot.x + e.getDistance() * Math.sin(getHeadingRadians() +
				en.bearingRadians), myRobot.y + e.getDistance() * Math.cos(getHeadingRadians() + en.bearingRadians)));
		en.lastHeading = en.heading;
		en.name = e.getName();
		en.energy = e.getEnergy();
		en.alive = true;
		en.scanTime = getTime();
		en.velocity = e.getVelocity();
		en.heading = e.getHeadingRadians();
		en.shootAbleScore = en.energy < 25 ? en.energy < 5 ? en.energy == 0 ?
				Double.MIN_VALUE : en.distance(myRobot) * 0.1 : en.distance(myRobot) * 0.75 : en.distance(myRobot);
		if (getOthers() == 1)
			setTurnRadarLeftRadians(getRadarTurnRemainingRadians());
		if (!targetRobot.alive || en.shootAbleScore < targetRobot.shootAbleScore)
			targetRobot = en;
		
	}
	
	public void onRobotDeath(RobotDeathEvent event) {
		if (enemyList.containsKey(event.getName()))
			enemyList.get(event.getName()).alive = false;
		if (event.getName().equals(targetRobot.name))
			targetRobot.alive = false;
	}
	
	public void shooting() {
		if (targetRobot != null && targetRobot.alive) {
			double dist = myRobot.distance(targetRobot);
			double power = (dist > 850 ? 0.1 :
					(dist > 700 ? 0.5 : (dist > 250 ? 2.0 : 3.0)));
			power = Math.min(myRobot.energy / 4d,
					Math.min(targetRobot.energy / 3d, power));
			power = Utility.clamp(power, 0.1, 3.0);
			long deltaHitTime;
			Point2D.Double shootAt = new Point2D.Double();
			double head, deltaHead, bulletSpeed;
			double predictX, predictY;
			predictX = targetRobot.getX();
			predictY = targetRobot.getY();
			head = targetRobot.heading;
			deltaHead = head - targetRobot.lastHeading;
			shootAt.setLocation(predictX, predictY);
			deltaHitTime = 0;
			
			do {
				predictX += Math.sin(head) * targetRobot.velocity;
				predictY += Math.cos(head) * targetRobot.velocity;
				head += deltaHead;
				deltaHitTime++;
				Rectangle2D.Double fireField = new Rectangle2D.Double(WALL_MARGIN, WALL_MARGIN,
						battleField.width - 36, battleField.height - 36);
				if (!fireField.contains(predictX, predictY)) {
					bulletSpeed = shootAt.distance(myRobot) / deltaHitTime;
					power = Utility.clamp((20 - bulletSpeed) / 3.0, 0.1, 3.0);
					break;
				}
				shootAt.setLocation(predictX, predictY);
			} while ((int) Math.round((shootAt.distance(myRobot) - WALL_MARGIN) /
					Rules.getBulletSpeed(power)) > deltaHitTime);
			shootAt.setLocation(Utility.clamp(predictX, 34, getBattleFieldWidth() - 34), Utility.clamp(predictY, 34, getBattleFieldHeight() - 34));
			if ((getGunHeat() == 0.0) && (getGunTurnRemaining() == 0.0)
					&& (power > 0.0) && (myRobot.energy > 0.1))
				setFire(power);
			setTurnGunRightRadians(Utils.normalRelativeAngle((
					(Math.PI / 2) - Math.atan2(shootAt.y - myRobot.getY(),
							shootAt.x - myRobot.getX())) - getGunHeadingRadians()));
		}
	}
	
	public void movement() {
		if (targetPoint.distance(myRobot) < 15 || idleTime > 25) {
			idleTime = 0;
			updateListLocations(AMOUNT_PREDICTED_POINTS);
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
		} else {
			idleTime++;
			double angle = Utility.calcAngle(myRobot, targetPoint) - getHeadingRadians();
			double direction = 1;
			if (Math.cos(angle) < 0) {
				angle += Math.PI;
				direction *= -1;
			}
			setMaxVelocity(10 - (4 * Math.abs(getTurnRemainingRadians())));
			setAhead(myRobot.distance(targetPoint) * direction);
			angle = Utils.normalRelativeAngle(angle);
			setTurnRightRadians(angle);
		}
	}
	
	public void updateListLocations(int n) {
		possibleLocations.clear();
		final int radius = (int) (125 * 1.5);
		// Create x points in a radius pixel radius around the bot
		for (int i = 0; i < n; i++) {
			double randXMod = Utility.randomBetween(-radius, radius);
			//yRange is dependant on the x current value to create a circle
			double yRange = Math.sqrt(radius * radius - randXMod * randXMod);
			double randYMod = Utility.randomBetween(-yRange, yRange);
			double y = Utility.clamp(myRobot.y + randYMod, 75, battleField.height - 75);
			double x = Utility.clamp(myRobot.x + randXMod, 75, battleField.width - 75);
			possibleLocations.add(new Point2D.Double(x, y));
		}
	}
	
	public double evaluatePoint(Point2D.Double p) {
		double rickValue = Utility.randomBetween(0.75, 2) / p.distanceSq(myRobot);
		rickValue += (6 * (getOthers() - 1)) /
				p.distanceSq(battleField.width / 2, battleField.height / 2);
		double cornerFactor = getOthers() <= 5 ? getOthers() == 1 ? 0.25 : 0.5 : 1;
		rickValue += cornerFactor / p.distanceSq(0, 0);
		rickValue += cornerFactor / p.distanceSq(battleField.width, 0);
		rickValue += cornerFactor / p.distanceSq(0, battleField.height);
		rickValue += cornerFactor / p.distanceSq(battleField.width, battleField.height);
		
		if (targetRobot.alive) {
			double robotAngle = Utils.normalRelativeAngle(
					Utility.calcAngle(p, targetRobot) - Utility.calcAngle(myRobot, p));
			Iterator<Robot> enIterator = enemyList.values().iterator();
			while (enIterator.hasNext()) {
				Robot en = enIterator.next();
				rickValue += (en.energy / myRobot.energy) * (1 / p.distanceSq(en)) *
						(1.0 + ((1 - (Math.abs(Math.sin(robotAngle)))) + Math.abs(Math.cos(robotAngle))) / 2) *
						(1 + Math.abs(Math.cos(Utility.calcAngle(myRobot, p) - Utility.calcAngle(en, p))));
			}
		} else if (enemyList.values().size() >= 1) {
			Iterator<Robot> enIterator = enemyList.values().iterator();
			while (enIterator.hasNext()) {
				Robot en = enIterator.next();
				rickValue += (en.energy / myRobot.energy) * (1 / p.distanceSq(en)) *
						(1 + Math.abs(Math.cos(Utility.calcAngle(myRobot, p) - Utility.calcAngle(en, p))));
			}
		} else
			rickValue += (1 + Math.abs(Utility.calcAngle(myRobot, targetPoint) - getHeadingRadians()));
		return rickValue;
	}
	
}