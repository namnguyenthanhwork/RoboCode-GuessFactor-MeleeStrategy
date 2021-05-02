package demo2;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Random;

import robocode.*;
import robocode.util.Utils;

/*
	@author: Nguyen Thanh Nam & Nguyen Trung Kien
	Tham khảo ý tưởng từ https://robowiki.net/wiki/GuessFactor_Targeting_Tutorial
 */
public class Cham1 extends AdvancedRobot {
	static double direction = 1; // hướng di chuyển
	static double BULLET_POWER = 3; // sát thương đạn tối đa
	// các góc lượng giác
	static final double ONE_QUARTER = Math.PI / 2;
	static final double ONE_EIGHTH = Math.PI / 4;
	static final double APPROACH_ANGLE = Math.PI / 3 + 0.01745d;
	static final double RETREAT_ANGLE = 2 * Math.PI / 3 + 0.01745d;
	
	// thiết lập súng cho robot
	// những biến cần thiết trong GuessFactor Targeting Terminology
	/*
		sử dụng mảng 5 chiều (5-dimensional space) để lưu dữ liệu bằng GuessFactor Targeting
		[] thứ 1: 1 mảng lưu dữ liệu của địch
		[] thứ 2: lưu khoảng cách
		[] thứ 3: lưu vận tốc cuối cùng
		[] thứ 4: lưu toạ độ của địch
		[] thứ 5: lưu toạ độ của địch
	 */
	static int[][][][][] _factors = new int[6][9][3][20][25];
	static Point2D.Double enemy; // toạ độ của địch
	static double lastVelocity;
	static double robotLastEnergy;
	static int hits;
	// đổi màu robot -> tấu hài :)
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
	
	// thực thi robot
	public void run() {
		robotLastEnergy = 100;
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
		setColors(Color.white, Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE);
	}
	
	
	// hành vi khi phát hiện địch
	public void onScannedRobot(ScannedRobotEvent robotEvent) {
		changeColor();
		// Di chuyển (sử dụng Musashi trick + Random oscillation)
		RoundRectangle2D.Double battlefield =
				new RoundRectangle2D.Double(50, 50, getBattleFieldWidth() - 100,
						getBattleFieldHeight() - 100, 100, 100);
		double nextX = 0, nextY = 0;
		double distance, enemyHeading, r;
		double x = getX();
		double y = getY();
		
		// Dự đoán toạ độ của robot của địch
		enemy = new Point2D.Double(
				Math.sin(enemyHeading = getHeadingRadians() + robotEvent.getBearingRadians())
						* (distance = robotEvent.getDistance()) + x,
				Math.cos(enemyHeading) * distance + y);
		
		/*
			đếm số lần bắn trúng địch (nếu năng lượng trước - năng lượng hiện tại > 3) thì
		    tăng số lần bắn trúng lên 1 đơn vị vì khi bắn trúng địch thì
		    năng lượng của mình nhận lại được là 3 * firePower
		 */
		if (robotLastEnergy - (robotLastEnergy = getEnergy()) > 3)
			hits++;
		
		//  đảo hướng di chuyển (cùng chiều or ngược chiều kim đồng hồ)
		if (Math.random() < 0.085d && hits > getRoundNum())
			direction = -direction;
		
		// Tham khảo ý tưởng từ https://robowiki.net/wiki/Wall_Smoothing
		// https://www.youtube.com/watch?v=SrbNxXiNp3k
		for (int i = 0; i < 2; i++) {
			double a = RETREAT_ANGLE;
			if (distance >= 400)
				a = APPROACH_ANGLE;
			
			do {
				nextX = x + Math.sin(r = enemyHeading + (a -= .01745d) * direction) * 65;
				nextY = y + Math.cos(r) * 65;
			} while (!battlefield.contains(nextX, nextY) && a > ONE_EIGHTH);
			
			if (battlefield.contains(nextX, nextY))
				break;
			direction = -direction;
		}
		
		nextX = Utils.normalRelativeAngle(Math.atan2(nextX - x, nextY - y) - getHeadingRadians());
		nextY = 65;
		if (Math.abs(nextX) > ONE_QUARTER) {
			nextY = -nextY;
			nextX += nextX > 0 ? -Math.PI : Math.PI;
		}
		setTurnRightRadians(nextX);
		setAhead(nextY);
		
		// phân tích dữ liệu của địch
		// https://robowiki.net/wiki/GuessFactor_Targeting_Tutorial
		r = -(lastVelocity - (lastVelocity = Math.abs(nextX = robotEvent.getVelocity())));
		r = r == 0 ? 1 : 1 + r / Math.abs(r);
		int[] factors = _factors[(int) (distance * 0.00625d)]
				[(int) lastVelocity]
				[(int) r]
				[(int) (Utils.normalAbsoluteAngle(robotEvent.getHeadingRadians() - enemyHeading
				+ (nextX < 0 ? Math.PI : 0)) * 3.023943d)];
		
		// xây dựng đối tượng đạn ảo để kiểm tra bắn trúng địch hay không
		VirtualBullet vb;
		addCustomEvent(vb = new VirtualBullet());
		vb.rx = x;
		vb.ry = y;
		vb.enemyHeading = enemyHeading;
		vb.time = getTime();
		vb.factors = factors;
		
		// thiết lập vị trí tốt nhất để quay radar
		int bestIndex = 12;
		for (int i = 24; i >= 0; i--)
			if (factors[i] > factors[bestIndex])
				bestIndex = i;
		setTurnGunRightRadians(Utils.normalRelativeAngle(enemyHeading - getGunHeadingRadians() +
				(double) (bestIndex - 12) * 0.067861661d));
		
		// thiết lập năng lượng đạn bắn ra dựa vào năng lượng của ta và địch
		BULLET_POWER = Math.min((double) 3, Math.min(this.getEnergy(), robotEvent.getEnergy()) / (double) 4);
		setFire(BULLET_POWER);
		// thiết lập góc radar khi quay trái
		setTurnRadarLeftRadians(getRadarTurnRemaining());
	}
	
	// va chạm với robot địch
	public void onHitRobot(HitRobotEvent e) {
		// xác định loại đạn khi mình nhắm mục tiêu mà có 1 mục tiêu khác va chạm mình
		if ((e.getBearing() >= 0 && e.getBearing() <= 20) ||
				(e.getBearing() <= 0 && e.getBearing() >= -20))
			setFire(BULLET_POWER);
	}
	
	/*
			Một đối tượng theo dõi vị trí của một viên đạn nếu nó được bắn theo một quỹ đạo nhất định và
			tìm hiểu xem nó có bắn trúng quỹ đạo đó hay không. Chúng được sử dụng
			trong tất cả các loại yếu tố dự đoán sớm nhất để phân tích MovementProfile của bot.
			Ưu điểm: sau 1 vài lần nhận ra đạn và địch
			Link: https://robowiki.net/wiki/Virtual_Bullet
	*/
	class VirtualBullet extends Condition {
		// toạ độ lúc bắn robot của ta
		double rx;
		double ry;
		double enemyHeading; // hướng di chuyển của địch
		long time; // thời gian bắt đầu bắn
		int[] factors; // hướng mục tiêu robot của ta lúc bắn
		
		// kiểm tra đạn ảo bắn trúng địch
		public boolean test() {
			if ((getTime() - time) * 11 > enemy.distance(rx, ry) - 18) {
				try {
					factors[12 + (int) (Utils.normalRelativeAngle(
							Math.atan2(enemy.x - rx, enemy.y - ry) - enemyHeading) * 14.73586076d)]++;
				} catch (Exception ignored) {
				}
				removeCustomEvent(this);
			}
			return false;
		}
	}
}