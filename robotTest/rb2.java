package demo2;

import robocode.*;
import robocode.util.Utils;

import java.awt.Color;

public class rb2 extends AdvancedRobot {
	static double BULLET_POWER = 1;
	int moveDirection = 1; //which way to move
	public void run() {
		setupRobotColors();
		setAdjustRadarForRobotTurn(true);//keep the radar still while we turn
		setAdjustGunForRobotTurn(true); // Keep the gun still when we turns
		turnRadarRightRadians(Double.POSITIVE_INFINITY);//keep turning radar right
	}
	
	public void onScannedRobot(ScannedRobotEvent scanEvent) {
		Enemy enemy = new Enemy(scanEvent, moveDirection);
		lockTheRadar();
		slowerRobot();
		
		if (isCloseEnough(scanEvent)) {
			enemy.tryKill();
		} else {
			enemy.findEnemies();
		}
		
	}
	
	private boolean isCloseEnough(ScannedRobotEvent scanEvent) {
		return scanEvent.getDistance() <= 150;
	}
	
	private void lockTheRadar() {
		setTurnRadarLeftRadians(getRadarTurnRemainingRadians());
	}
	
	private void slowerRobot() {
		if (Math.random() > .9) {
			setMaxVelocity((12 * Math.random()) + 12);//randomly change speed
		}
	}
	
	public void onHitWall(HitWallEvent e) {
		moveDirection = -moveDirection;//reverse direction upon hitting a wall
	}
	
	public void onHitRobot(HitRobotEvent e) {
//		double absBearing = getHeadingRadians() + e.getBearingRadians();
//		//stop();
//		setTurnGunRightRadians(Utils.normalRelativeAngle(absBearing - getGunHeadingRadians()));
//		setTurnGunRightRadians(Utils.normalRelativeAngle(absBearing - getRadarHeadingRadians()));
//		fire(2.5);
	
	}
	/**
	 * onWin:  Do a victory dance
	 */
	public void onWin(WinEvent e) {
		for (int i = 0; i < 50; i++) {
			turnRight(30);
			turnLeft(30);
		}
	}
	
	private void setupRobotColors() {
		setColors(Color.black, Color.red, Color.green); // body, gun, radar
		setScanColor(Color.green);
	}
	
	public class Enemy {
		ScannedRobotEvent scanEvent;
		int currentMoveDirection;
		double absBearing;
		double latVel;
		
		public Enemy(ScannedRobotEvent scanEvent, int currentMoveDirection) {
			this.scanEvent = scanEvent;
			this.currentMoveDirection = currentMoveDirection;
			this.absBearing =
					scanEvent.getBearingRadians() + getHeadingRadians();//enemies absolute bearing
			this.latVel = scanEvent.getVelocity() * Math
					.sin(scanEvent.getHeadingRadians() - absBearing);//enemies later velocity
		}
		
		public void tryKill() {
			turnGun(15);
			turnPerpendicularlyToTheEnemy();
			goToTheEnemy();
			setDamage();
		}
		
		public void findEnemies() {
			turnGun(22);
			searchEnemies();
			goToTheEnemy();
			setDamage();
		}
		
		private void setDamage() {
			BULLET_POWER = Math.min(400 / scanEvent.getDistance(), getEnergy() > 30 ? 3 : 1.5);
			setBulletColor(BULLET_POWER >= 1.5 ? Color.RED : Color.YELLOW);
			setFire(BULLET_POWER);
		}
		
		private void searchEnemies() {
			setTurnRightRadians(robocode.util.Utils.normalRelativeAngle(
					absBearing - getHeadingRadians() + latVel
							/ getVelocity()));// dirigir para a localização futura prevista pelos inimigos
		}
		
		private void goToTheEnemy() {
			setAhead((this.scanEvent.getDistance() - 140) * currentMoveDirection);//move forward
		}
		
		private void turnPerpendicularlyToTheEnemy() {
			setTurnLeft(-90 - this.scanEvent.getBearing()); //turn perpendicular to the enemy
		}
		
		private void turnGun(int quantity) {
			double gunTurnAmt = getGunTurnQuantity(quantity);//amount to turn our gun
			setTurnGunRightRadians(gunTurnAmt);//turn our gun
		}
		
		private double getGunTurnQuantity(int quantity) {
			return robocode.util.Utils.normalRelativeAngle(absBearing - getGunHeadingRadians()
					+ latVel / quantity);//amount to turn our gun, lead just a little bit
		}
	}
}