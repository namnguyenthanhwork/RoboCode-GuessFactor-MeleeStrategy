package demo2;
import robocode.*;
import robocode.Robot;

import java.awt.*;
import java.util.LinkedList;

import static robocode.util.Utils.normalRelativeAngleDegrees;

public class rb5 extends Robot {
	
	boolean isPink;
	int time = 0;
	double radius = 39.9;
	double angle = 29.9;
	LinkedList<Position> positionHistories = new LinkedList<Position>();
	
	@Override
	public void run() {
		while ( true ) {
			// Increase timer and save position
			time++;
			positionHistories.add( myPositon() );
			paintPink();
			
			// Move the radar all of the time
			turnRadarRight( 360 );
			
			// Move if nothing is in the way
			move();
		}
	}
	
	public void onScannedRobot( ScannedRobotEvent e ) {
		changeColor();
		// Back up if the target is too close
		if ( e.getDistance() < 25 ) {
			back( 10 + getSalt() );
			
			// Else fire!
		} else {
			double absoluteBearing = getHeading() + e.getBearing();
			double bearingFromGun = normalRelativeAngleDegrees( absoluteBearing - getGunHeading() );
			turnGunAndRadar( bearingFromGun );
			
			fire( 3 );
			move();
		}
		
		scan();
	}
	
	public void onBulletHit( BulletHitEvent e ) {
		changeColor();
		fire( 3 );
		move();
	}
	
	public void onHitByBullet( HitByBulletEvent e ) {
		changeColor();
		move();
	}
	
	public void onHitWall( HitWallEvent e ) {
		changeColor();
		back( getRadius() / 2 );
		turnRight( getAngle() );
	}
	
	public void onHitRobot( HitRobotEvent e ) {
		changeColor();
		back( getRadius() / 2 );
		turnRight( getAngle() );
	}
	
	public void onWin( final WinEvent e ) {
		for ( int i = 0; i < 1000; i++ ) {
			changeColor();
			turnLeft( 30 );
		}
	}
	
	/**
	 *    Private utility functions
	 */
	private class Position {
		final double x;
		final double y;
		final double angle;
		
		private Position( double x, double y, double angle ) {
			this.x = x;
			this.y = y;
			this.angle = angle;
		}
		
		@Override public boolean equals( Object o ) {
			if ( this == o )
				return true;
			if ( !( o instanceof Position ) )
				return false;
			
			Position position = (Position) o;
			
			if ( Double.compare( position.x, x ) != 0 )
				return false;
			if ( Double.compare( position.y, y ) != 0 )
				return false;
			return Double.compare( position.angle, angle ) == 0;
		}
	}
	
	private Position myPositon() {
		return new Position( getX(), getY(), getHeading() );
	}
	
	private boolean tooClose( double x, double y ) {
		return ( getX() < 50
				|| getX() > x - 50
				|| getY() < 50
				|| getX() > y - 50 );
	}
	
	private void move() {
		// Back up if stuck
		if ( positionHistories.getLast().equals( myPositon() ) ) {
			back( getRadius() );
			turnRight( getAngle() );
			
			// Move if nothing is in the way
		} else if ( !tooClose( getBattleFieldWidth(), getBattleFieldHeight() ) ) {
			ahead( getRadius() );
			
			// Do not move into a wall
		} else if ( tooClose( getBattleFieldWidth(), getBattleFieldHeight() ) ) {
			turnRight( getAngle() );
			ahead( getRadius() );
		}
	}
	
	private double getSalt() {
		return time % 10;
		//return time % 20;
	}
	
	private double getAngle() {
		return angle + getSalt();
	}
	
	private double getRadius() {
		return radius + getSalt();
	}
	
	private void turnGunAndRadar( double value ) {
		turnGunRight( value );
		turnRadarRight( value );
	}
	
	private void paintPink() {
		setBodyColor( new Color( 255, 0, 102 ) );
		setGunColor( new Color( 0, 255, 0 ) );
		setRadarColor( new Color( 255, 0, 102 ) );
		setScanColor( Color.white );
		setBulletColor( Color.pink );
		isPink = true;
	}
	
	private void paintGreen() {
		setBodyColor( new Color( 0, 255, 0 ) );
		setGunColor( new Color( 255, 0, 102 ) );
		setRadarColor( new Color( 0, 255, 0 ) );
		setScanColor( Color.white );
		setBulletColor( Color.green );
		isPink = false;
	}
	
	private void changeColor() {
		if ( isPink ) {
			paintGreen();
			isPink = false;
		} else {
			paintPink();
			isPink = true;
		}
	}
}