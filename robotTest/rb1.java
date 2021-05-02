package demo2;
import robocode.*;
import robocode.util.*;
import java.awt.Color;
import java.awt.geom.*;
import static robocode.util.Utils.normalRelativeAngleDegrees;

/**
 * Aspira - robô criado por Fábia E. Cardoso Sena
 * Estudante do curso de Análise e Desenvolvimento de Sistemas da Ucsal
 * Robo criado para o desafio Talent Sprint Robot Arena da Solutis 2020
 * Programa de Seleção de Jovens Talentos
 */
public class rb1 extends AdvancedRobot {
	
	double moveAmount;
	int direction = 1;
	boolean turning = false;
	double gunTurnAmt;
	String trackName;
	int moveDirection=1;
	public final double PERCENT_BUFFER = .20;
	MoveCompleteCondition moveComplete = new MoveCompleteCondition(this);
	TurnCompleteCondition turnComplete = new TurnCompleteCondition(this);
	
	
	/**
	 * run: metodo principal, define o comportamento do robô.
	 */
	public void run() {
		// Cores do Robô
		setBodyColor(new Color(75, 0, 130));
		setGunColor(new Color(75, 0, 130));
		setRadarColor(new Color(255, 0, 255));
		setBulletColor(Color.white);
		setScanColor(Color.white);
		
		
		setAdjustGunForRobotTurn(true); // Define a arma para girar independente da virada do robô
		setAdjustRadarForGunTurn(true); // Configura o radar para girar independente do giro da arma
		turnRadarRightRadians(Double.POSITIVE_INFINITY);//matenha o radar virando para a direita
		
		// Inicializar com a quantidade máxima de movimentos possíveis para este campo de batalha
		moveAmount = Math.max(getBattleFieldWidth(), getBattleFieldHeight());
		
		
		while (true) {
			
			// se não estiver se movendo nem girando, vire.
			if(turning && moveComplete.test() && turnComplete.test()){
				setTurnGunRight(90); // Configure a arma para girar 90 graus para a direita
				setTurnRight(90); // Configure o robô para girar 90 graus para a direita
				turning = false;
			}
			//caso não esteja girando
			if(!turning && moveComplete.test() && turnComplete.test()){
				ahead(moveAmount);
				turning = true;
			}
			setTurnRadarRight(120); // mantenha o radar girando 360 graus para direita
			execute();
		}
	}
	
	/**
	 * onHitByBullet: É executado quando o robô é atingido por uma bala
	 */
	public void onHitByBullet(HitByBulletEvent e) {
		fire(2); // atire com força 2
		back(100); // se afaste
	}
	
	/**
	 * onHitWall: É executado quando o robô bate na parede
	 */
	public void onHitWall(HitWallEvent e){
		moveDirection=-moveDirection; //vire na direção contraria ao bater na parede
	}
	
	/**
	 * onHitRobot: É executado quando seu robô colide com outro robô
	 */
	public void onHitRobot(HitRobotEvent e) {
		// Aproveita que tem um inimigo tão perto e o defina como alvo
		if (trackName != null && !trackName.equals(e.getName())) {
			out.println("Tracking " + e.getName() + " due to collision"); //Rastreia o robô com quem colidiu
		}
		
		trackName = e.getName(); // Define quem é o alvo
		gunTurnAmt = normalRelativeAngleDegrees(e.getBearing() + (getHeading() - getRadarHeading()));
		turnGunRight(gunTurnAmt);
		fire(3); // Atira!
		back(100); // Afasta-se um pouco
	}
	
	/**
	 * onScannedRobot: executado quando um robô inimigo é avistado
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
		double absBearing=e.getBearingRadians()+getHeadingRadians();// direção para o robô que digitalizado em relação à direção do meu robô
		double latVel=e.getVelocity() * Math.sin(e.getHeadingRadians() -absBearing);//Retorna a velocidade do robô inimigo
		double gunTurnAmt;//quantidade para virar a arma
		
		setTurnRadarLeftRadians(getRadarTurnRemainingRadians());//travar no radar
		setTurnRight(e.getBearing() + 90 - 30 * direction); // Direciona o robo perpendicular a do inimigo
		
		if(Math.random()>.9){
			setMaxVelocity((12*Math.random())+12);//velocidade de mudança aleatória
		}
		if (e.getDistance() > 150) {//se a distância do adiversário for maior que 150
			gunTurnAmt = robocode.util.Utils.normalRelativeAngle(absBearing- getGunHeadingRadians()+latVel/22);//quantidade para virar a arma
			setTurnGunRightRadians(gunTurnAmt); //vire a arma
			setTurnRightRadians(robocode.util.Utils.normalRelativeAngle(absBearing-getHeadingRadians()+latVel/getVelocity()));//se desloque em direção ao mesmo percurso do seu inimigo
			setAhead((e.getDistance() - 140)*moveDirection);//mova-se para frente
			setFire(1);//atire
		}
		else{//se adversário estiver perto o suficiente...
			gunTurnAmt = robocode.util.Utils.normalRelativeAngle(absBearing- getGunHeadingRadians()+latVel/15);//
			setTurnGunRightRadians(gunTurnAmt);//vire a arma
			setTurnLeft(-90-e.getBearing()); //vire perpendicularmente em direção ao inimigo
			setAhead((e.getDistance() - 140)*moveDirection);//mova-se para frente
			setFire(3);//atire
		}
		
	}
	
	/**
	 * onWin: Dancinha da vitória, executada quando o robô vence
	 */
	public void onWin(WinEvent e) {
		for (int i = 0; i < 50; i++) {
			turnRight(30);
			turnLeft(30);
		}
	}
}