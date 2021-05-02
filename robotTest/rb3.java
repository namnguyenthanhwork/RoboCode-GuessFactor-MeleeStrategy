package demo2;

import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

public class rb3
		extends AdvancedRobot {
	static StringBuffer pattern = new StringBuffer("\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\ufff8\ufff9\ufffa\ufffb\ufffc\ufffd\ufffe\uffff\u0001\u0002\u0003\u0004\u0005\u0006\u0007\b");
	
	public void run() {
		this.setAdjustGunForRobotTurn(true);
		this.turnRadarRight(Double.POSITIVE_INFINITY);
	}
	
	public void onScannedRobot(ScannedRobotEvent e) {
		double absbearing = e.getBearingRadians();
		this.setTurnRightRadians(absbearing - 1.5707963267948966);
		pattern.insert(0, (char) Math.round(Math.sin(e.getHeadingRadians() -
				(absbearing += this.getHeadingRadians())) * e.getVelocity()));
		int index = 0;
		int searchlength = 30;
		while ((index = pattern.toString().indexOf(pattern.substring(0, searchlength--), 1)) < 0) {
		}
		double dist = e.getDistance();
		double power = Math.min((double) 3, Math.min(this.getEnergy(), e.getEnergy()) / (double) 4);
		searchlength = index - (int) (dist / (20.0 - power * (double) 3));
		do {
			absbearing += Math.asin((double) ((byte) pattern.charAt(index--)) / dist);
		} while (index >= Math.max(0, searchlength));
		this.setTurnGunRightRadians(Utils.normalRelativeAngle((double)
				(absbearing - this.getGunHeadingRadians())));
		this.setFire(power);
		if (this.getDistanceRemaining() == 0.0) {
			this.setAhead((Math.random() - 0.5) * dist * 1.2);
		}
		this.setTurnRadarLeft(this.getRadarTurnRemaining());
	}
}
