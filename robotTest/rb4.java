package demo2;

import java.awt.Color;
import java.awt.geom.Point2D;

import robocode.AdvancedRobot;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.WinEvent;

public class rb4 extends AdvancedRobot {
	EnemyRobot enemy = new EnemyRobot();
	
	public static double PI = Math.PI;
	private double move;
	private double firePower;
	
	public void run() {
		setColor();
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setAdjustRadarForRobotTurn(true);
		init();
		while (true) {
			if (enemy.getName() == null) {
				setTurnRadarRightRadians(2 * PI);
				execute();
			} else {
				doMove();
				doFirePower();
				doGun();
				fire(firePower);
				execute();
			}
		}
	}
	
	void setColor() {
		setBodyColor(new Color(179, 229, 252));
		setGunColor(new Color(252, 228, 236));
		setRadarColor(new Color(255, 255, 255));
		setBulletColor(new Color(179, 229, 252));
		setScanColor(new Color(226, 250, 255));
	}
	
	public void doNearWall() {
		if (this.getX() > 700 || this.getX() < 100 || this.getY() > 500 || this.getY() < 100) {
			double turnAngle = Math.atan2(this.getBattleFieldWidth() / 2 - this.getX(), this.getBattleFieldHeight() / 2 - this.getY());
			turnAngle = rectify(turnAngle - getHeadingRadians());
			double moveDistance = Point2D.distance(this.getX(), this.getY(), 400, 300);
			double moveDirection = 1;
			if (Math.abs(turnAngle) > Math.PI / 2) {
				turnAngle = rectify(turnAngle + Math.PI);
				moveDirection = -1;
			}
			setTurnRightRadians(turnAngle);
			setAhead(moveDirection * moveDistance * 0.5);
		}
	}
	
	public void init() {
		enemy.init();
		move = 1;
	}
	
	public void onRobotDeath(RobotDeathEvent e) {
		init();
	}
	
	public void onRobotWin(WinEvent e) {
		init();
	}
	
	public void doMove() {
		long time = getTime();
		if (enemy.getDistance() > 50) {
			if (time % 20 == 0 || time % 20 == 1) {
				move *= -1;
				setAhead(500 * move);
			}
			setTurnRightRadians(enemy.getBearingRadian() + (PI / 2) - PI / 8 * move);
		} else {
			if (time % 20 == 0 || time % 20 == 1) {
				move *= -1;
				setAhead(500 * move);
			}
			setTurnRightRadians(enemy.getBearingRadian() + (PI / 2) - (PI / 2) * move);
		}
		if (this.getEnergy() - enemy.getEnergy() > 30) {
			setTurnRightRadians(enemy.getBearingRadian());
			setAhead(enemy.getDistance());
			return;
		}
		doNearWall();
	}
	
	public double getMidRadians() {
		double radians1 = Math.asin(((300 - getY()) / Math.sqrt(Math.pow(300 - getY(), 2) + Math.pow(400 - getX(), 2))));
		double radians2 = enemy.getHeadingRadian();
		return radians2 - radians1;
	}
	
	public void doFirePower() {
		if (enemy.getDistance() < 200) {
			firePower = 3;
		} else if (enemy.getDistance() < 200 && enemy.getDistance() >= 300) {
			firePower = 2;
		} else if (enemy.getDistance() < 300 && enemy.getDistance() >= 400) {
			firePower = 1;
		} else {
			firePower = 0.5;
		}
	}
	
	public void doGun() {
		double[] linearpredict = {predictX(calBulletSpeed(firePower)), predictY(calBulletSpeed(firePower))};
		double offset = getGunOffset(linearpredict[0], linearpredict[1]);
		double GunOffset = rectify(offset);
		setTurnGunRightRadians(robocode.util.Utils.normalRelativeAngle(GunOffset));
	}
	
	public void onScannedRobot(ScannedRobotEvent e) {
		enemy.update(e, this);
		double Offset = rectify(enemy.getDirection() - getRadarHeadingRadians());
		setTurnRadarRightRadians(Offset * 2);
	}
	
	public double rectify(double angle) {
		if (angle < -Math.PI)
			angle += 2 * Math.PI;
		if (angle > Math.PI)
			angle -= 2 * Math.PI;
		return angle;
	}
	
	public double predictX(double bulletspeed) {
		double Enemy_x = this.getX() + enemy.getDistance() * Math.sin(enemy.getDirection());
		double predict_x = Enemy_x + enemy.getDistance() / bulletspeed * enemy.getVelocity() * Math.sin(enemy.getHeadingRadian());// - enemy.getDistance()/bulletspeed*this.getVelocity()* Math.sin(this.getHeadingRadians());
		return predict_x;
	}
	
	public double predictY(double bulletspeed) {
		double Enemy_y = this.getY() + enemy.getDistance() * Math.cos(enemy.getDirection());
		double predict_y = Enemy_y + enemy.getDistance() / bulletspeed * enemy.getVelocity() * Math.cos(enemy.getHeadingRadian());// - enemy.getDistance()/bulletspeed*this.getVelocity()* Math.cos(this.getHeadingRadians());
		return predict_y;
	}
	
	public double getGunOffset(double x, double y) {
		double gunoffset = Math.asin((x - this.getX()) / Math.sqrt(Math.pow((x - this.getX()), 2) + Math.pow((y - this.getY()), 2)));
		if (y - this.getY() < 0) {
			gunoffset = PI - gunoffset;
		}
		return gunoffset - this.getGunHeadingRadians();
	}
	
	public double calBulletSpeed(double power) {
		return 20 - 3 * power;
	}
	
	public class EnemyRobot {
		private double x, y;
		private String name = null;
		private double headingRadian = 0.0D;
		private double bearingRadian = 0.0D;
		private double distance = 1000D;
		private double direction = 0.0D;
		private double velocity = 0.0D;
		private double energy = 100.0D;
		private double previousEnergy = 100.0D;
		
		private long time;
		private long fireTime;
		
		public long getTime() {
			return time;
		}
		
		public void setTime(long time) {
			this.time = time;
		}
		
		public long getFireTime() {
			return fireTime;
		}
		
		public void setFireTime(long fireTime) {
			this.fireTime = fireTime;
		}
		
		public double getPreviousEnergy() {
			return previousEnergy;
		}
		
		public void setPreviousEnergy(double previousEnergy) {
			this.previousEnergy = previousEnergy;
		}
		
		public double getX() {
			return x;
		}
		
		public void setX(double x) {
			this.x = x;
		}
		
		public double getY() {
			return y;
		}
		
		public void setY(double y) {
			this.y = y;
		}
		
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public double getHeadingRadian() {
			return headingRadian;
		}
		
		public void setHeadingRadian(double headingRadian) {
			this.headingRadian = headingRadian;
		}
		
		public double getBearingRadian() {
			return bearingRadian;
		}
		
		public void setBearingRadian(double bearingRadian) {
			this.bearingRadian = bearingRadian;
		}
		
		public double getDistance() {
			return distance;
		}
		
		public void setDistance(double distance) {
			this.distance = distance;
		}
		
		public double getDirection() {
			return direction;
		}
		
		public void setDirection(double direction) {
			this.direction = direction;
		}
		
		public double getVelocity() {
			return velocity;
		}
		
		public void setVelocity(double velocity) {
			this.velocity = velocity;
		}
		
		public double getEnergy() {
			return energy;
		}
		
		public void setEnergy(double energy) {
			this.energy = energy;
		}
		
		public void init() {
			
			name = null;
			headingRadian = 0.0D;
			bearingRadian = 0.0D;
			distance = 1000D;
			direction = 0.0D;
			velocity = 0.0D;
			energy = 100.0D;
		}
		
		public void update(ScannedRobotEvent e, AdvancedRobot me) {
			name = e.getName();
			headingRadian = e.getHeadingRadians();
			bearingRadian = e.getBearingRadians();
			this.energy = e.getEnergy();
			this.velocity = e.getVelocity();
			this.distance = e.getDistance();
			direction = bearingRadian + me.getHeadingRadians();
			x = me.getX() + Math.sin(direction) * distance;
			y = me.getY() + Math.cos(direction) * distance;
		}
	}
}