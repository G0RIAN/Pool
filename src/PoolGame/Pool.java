package PoolGame;

import java.awt.Color;
import java.awt.Point;
import java.awt.Toolkit;
import java.util.ArrayList;

import ch.aplu.jgamegrid.Actor;
import ch.aplu.jgamegrid.GGActorCollisionListener;
import ch.aplu.jgamegrid.GGMouse;
import ch.aplu.jgamegrid.GGMouseListener;
import ch.aplu.jgamegrid.GGVector;
import ch.aplu.jgamegrid.GameGrid;
import ch.aplu.jgamegrid.Location;
import ch.aplu.jgamegrid.Location.CompassDirection;

public class Pool extends GameGrid implements GGMouseListener, GGActorCollisionListener {

	private static class InstanceHolder {
		private static final Pool INSTANCE = new Pool();
	}

	private static final long serialVersionUID = -3473089005543945418L;
	private static final float MAX_SPEED = 12f;
	private static final int SIM_TIME = 10;
	private static final int IMAGE_MARGIN = 122;
	private static final Location[] holeCenters = { new Location(146, 146), // 0 - top left
			new Location(522, 137), // 1 - top mid
			new Location(896, 146), // 2 - top right
			new Location(146, 534), // 3 - bottom left
			new Location(521, 543), // 4 - bottom mid
			new Location(896, 534), // 5 - bottom right
	};
	private static final int[] holeRadii = new int[] { 35, 24, 35, 35, 24, 35 };
	private static final Location[] ballStartLocation = { new Location(338, 338), // 1
			new Location((int) Math.round(338 - 17.32), 338 - 10), // 3
			new Location((int) Math.round(338 - 2 * 17.32), 338 - 20), // 6
			new Location((int) Math.round(338 - 3 * 17.32), 338 + 3 * 10), // 10
			new Location((int) Math.round(338 - 4 * 17.32), 338 - 4 * 10), // 11
			new Location((int) Math.round(338 - 4 * 17.32), 338 + 2 * 10), // 14
			new Location((int) Math.round(338 - 3 * 17.32), 338 - 10), // 8
			new Location((int) Math.round(338 - 2 * 17.32), 338), // 5
			new Location((int) Math.round(338 - 17.32), 338 + 10), // 2
			new Location((int) Math.round(338 - 2 * 17.32), 338 + 20), // 4
			new Location((int) Math.round(338 - 3 * 17.32), 338 - 3 * 10), // 7
			new Location((int) Math.round(338 - 4 * 17.32), 338 + 4 * 10), // 15
			new Location((int) Math.round(338 - 4 * 17.32), 338 - 2 * 10), // 12
			new Location((int) Math.round(338 - 4 * 17.32), 338), // 13
			new Location((int) Math.round(338 - 3 * 17.32), 338 + 10), // 9
	};
	private static final Location whiteBallStartLocation = new Location(701, 338);
	private static Location[] corners = { // location of all corners
			new Location(getImageMargin() + 38, getImageMargin() + 67), // 0 - corner left of top left hole
			new Location(getImageMargin() + 67, getImageMargin() + 38), // 1 - corner right of top left hole
			new Location(getImageMargin() + 379, getImageMargin() + 38), // 2 - corner left of top middle hole
			new Location(getImageMargin() + 418, getImageMargin() + 38), // 3 - corner right of top middle hole
			new Location(getImageMargin() + 731, getImageMargin() + 38), // 4 - corner left of top right hole
			new Location(getImageMargin() + 759, getImageMargin() + 38), // 5 - corner right of top right hole
			new Location(getImageMargin() + 759, getImageMargin() + 369), // 6 - corner right of bottom right hole
			new Location(getImageMargin() + 731, getImageMargin() + 397), // 7 - corner left of bottom right hole
			new Location(getImageMargin() + 418, getImageMargin() + 397), // 8 - corner right of bottom middle hole
			new Location(getImageMargin() + 379, getImageMargin() + 397), // 9 - corner left of bottom middle hole
			new Location(getImageMargin() + 67, getImageMargin() + 397), // 10 - corner right of bottom left hole
			new Location(getImageMargin() + 38, getImageMargin() + 369) // 11 - corner left of bottom left hole
	};
	// start angle of corner segments (0 deg = east)
	private static final float[] cornerStartAngles = new float[] { -45, 90, 0, 90, 45, -180, 135, -90, -180, -90, -135, 0 };
	// end angle of corner segments (0 deg = east)
	private static final float[] cornerEndAngles = new float[] { 0, 135, 90, 180, 90, -135, 180, -45, -90, 0, -90, 45 };
	private ArrayList<Ball> balls = new ArrayList<Ball>();
	private WhiteBall whiteBall;
	private long startTime = 0;
	private long endTime = 0;
	private boolean gameOver = false;
	private int score = 0;

	public static void main(String[] args) {

		getInstance();

		getInstance().reset();
		getInstance().show();
		getInstance().doRun();
	}

	private Pool() {

		super(800 + 2 * getImageMargin(), 438 + 2 * getImageMargin(), 1, new Color(10, 120, 90), false); // TODO: set false flag
		getBg().drawImage("sprites/PoolTable.png", 122, 122);
		getFrame()
			.setIconImage(Toolkit.getDefaultToolkit().createImage(ClassLoader.getSystemResource("sprites/pool_table_48px.png")));
		setTitle("8-Ball Pool Game");
		setSimulationPeriod(SIM_TIME);
		getBg().save();

	}

	@Override
	public void act() {
		if (!isGameOver()) {
			updateUI();
			Location mouseLocation = getMouseLocation();
			if (mouseLocation != null && !ballsMoving()) {
				getBg().drawLine(toPoint(whiteBall.getLocation()), toPoint(mouseLocation));
			}
			// check for remaining time
			if (endTime != 0 && System.currentTimeMillis() >= endTime) {
				gameOver = true;
				removeAllActors();
				getBg().restore();
				getBg().drawText("Game Over. Score: " + score, new Point(getWidth() / 2 - 155, getHeight() / 2 - 20));
				getBg().drawText("Klick     to start a new game.", new Point(getWidth() / 2 - 155, getHeight() / 2 + 20));
				getBg().drawImage("sprites/mouse_l.png", getWidth() / 2 - 99, getHeight() / 2);
				return;
			}
		}
	}

	private boolean ballsMoving() {
		for (Ball ball : balls) {
			if (ball.getVelocity().magnitude() > 0.01) {
				return true;
			}
		}
		if (whiteBall.getVelocity().magnitude() > 0.01) {
			return true;
		}
		return false;
	}

	@Override
	public int collide(Actor actor1, Actor actor2) {
		if (!isGameOver()) {
			Ball ball1 = (Ball) actor1;
			Ball ball2 = (Ball) actor2;
			ball1.resolveCollision(ball2);
		}
		return 0;
	}

	void hit(Ball ball) {
		if (!ball.equals(whiteBall)) {
			endTime += 10 * 1000;
			score += 100;
			balls.remove(ball);
			ball.removeSelf();
			if (balls.size() == 0) {
				newRack();
			}
		} else {
			resetWhiteBall();
		}
	}

	private void resetWhiteBall() {
		removeActor(whiteBall);
		whiteBall = new WhiteBall(whiteBallStartLocation);
		// ensure a distance > RADIUS between the white ball and the other balls
		addActor(whiteBall, whiteBallStartLocation);
		while (whiteBall.ballIsColliding(whiteBall.getLocation())) {
			whiteBall.setLocation(whiteBall.getLocation().getAdjacentLocation(CompassDirection.NORTH));
		}
		for (Ball ball : balls) {
			ball.addCollisionActor(whiteBall);
		}
	}

	@Override
	public boolean mouseEvent(GGMouse mouse) {
		switch (mouse.getEvent()) {
		case GGMouse.lClick:
			if (isGameOver()) {
				reset();
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else if (!ballsMoving()) {
				// first shot starts the game
				if (startTime == 0) {
					for (Ball b : balls) {
						b.setActEnabled(true);
					}
					whiteBall.setActEnabled(true);
					startTime = System.currentTimeMillis();
					endTime = startTime + 120 * 1000;
				}
				GGVector impulse = new GGVector(whiteBall.getX() - mouse.getX(), whiteBall.getY() - mouse.getY()).mult(0.07);
				if (impulse.magnitude() > MAX_SPEED) {
					impulse = impulse.mult(MAX_SPEED / impulse.magnitude());
				}
				whiteBall.setVelocity(impulse);
			}
			break;
		default:
			break;
		}
		return true;
	}

	private void newRack() {
		getActors().forEach(e -> e.removeSelf());
		removeAllActors();
		whiteBall = new WhiteBall(whiteBallStartLocation);
		addActor(whiteBall, whiteBallStartLocation);
		for (int ii = 1; ii <= ballStartLocation.length; ii++) {
			Ball ball = new Ball(ii, ballStartLocation[ii - 1]);
			balls.add(ball);
			addActor(ball, ballStartLocation[ii - 1]);
			ball.rotate(ball.getLocation(), Math.random() * 360);
		}
		for (Ball ball : balls) {
			ball.addCollisionActor(whiteBall);
			for (Ball otherBall : balls) {
				if (!(ball.equals(otherBall)) && !ball.getCollisionActors().contains(otherBall)) {
					ball.addCollisionActor(otherBall);
				}
			}
		}
	}

	@Override
	public void reset() {
		updateUI();
		addMouseListener(this, GGMouse.move | GGMouse.lClick);

		gameOver = false;
		score = 0;
		startTime = 0;
		endTime = 0;
		newRack();
	}

	private void updateUI() {
		long remainingTime = (endTime - System.currentTimeMillis()) / 1000;
		getBg().restore();
		if (startTime == 0) {
			getBg().drawText("Klick     to shoot the ball", new Point(getWidth() / 2 - 100, getHeight() / 2 - 100));
			getBg().drawImage("sprites/mouse_l.png", getWidth() / 2 - 44, getHeight() / 2 - 120);
		}
		int minutes = Math.max(0, (int) (remainingTime / 60));
		int seconds = Math.max(0, (int) ((remainingTime % 60)));
		String time = String.format("%d:%02d", minutes, seconds);
		getBg().drawText(time + "   Score: " + score, new Point(442, 50));
	}

	public static Pool getInstance() {
		return InstanceHolder.INSTANCE;
	}

	static int getImageMargin() {
		return IMAGE_MARGIN;
	}

	static Location[] getHolecenters() {
		return holeCenters;
	}

	static Location[] getCorners() {
		return corners;
	}

	static float[] getCornerstartangles() {
		return cornerStartAngles;
	}

	static float[] getCornerendangles() {
		return cornerEndAngles;
	}

	static int[] getHoleradii() {
		return holeRadii;
	}

	public boolean isGameOver() {
		return gameOver;
	}

	public ArrayList<Ball> getBalls() {
		return balls;
	}

}
