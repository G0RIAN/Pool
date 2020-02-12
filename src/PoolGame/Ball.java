package PoolGame;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;

import ch.aplu.jgamegrid.Actor;
import ch.aplu.jgamegrid.GGVector;
import ch.aplu.jgamegrid.Location;

public class Ball extends Actor {

	private static final int DIAGONAL_PADDING_OFFSET = 15;
	private final static int RADIUS = 10;
	private int lastCornerHit;
	private int lastCornerHitCycle;
	private GGVector position;
	private GGVector velocity;
	private static final float DELTA_V = 0.05f; // deceleration per time step
	private static final int WALL_SIZE = Pool.getImageMargin() + 50; // width/height of the green strip around the pool
																		// image

	Ball(int number, Location loc) {
		this(true, "sprites/kugel_" + number + ".gif", loc);
	}

	Ball(boolean isRotatable, String imagePath, Location loc) {
		super(isRotatable, imagePath);
		setCollisionCircle(new Point(0, 0), Ball.RADIUS);
		addActorCollisionListener(Pool.getInstance());
		setVelocity(new GGVector());
		setPosition(new GGVector(loc.getX(), loc.getY()));
	}

	void resolveCollision(Ball ball2) {

		GGVector delta = getPosition().sub(ball2.getPosition());
		// minimum translation distance to push balls apart after intersecting
		// impact speed
		GGVector v = getVelocity().sub(ball2.getVelocity());
		delta.normalize();
		double vn = v.dot(delta);
		// sphere intersecting but moving away from each other already
		if (vn > 0)
			return;
		// collision impulse
		GGVector impulse = delta.mult(-vn);
		GGVector v1 = getVelocity().add(impulse);
		GGVector v2 = ball2.getVelocity().sub(impulse);
		setVelocity(v1);
		ball2.setVelocity(v2);

	}

	@Override
	public void act() {
		if (Pool.getInstance().isGameOver()) {
			setVelocity(new GGVector());
		} else {
			move();
		}
	}

	@Override
	public synchronized void move() {

		if (velocity.magnitude2() == 0) {
			return;
		}
		// move ball
		GGVector endPosition = position.add(velocity);
		// rotate Ball while rolling
		rotate(getLocation(), getRotationIndex() / 64 * 360 + velocity.magnitude());
		fieldCheck: if (isOutOfField(endPosition)) {
			int hole;
			if ((hole = isNearHole(endPosition)) >= 0) {
				if (isInHole(endPosition, hole)) {
					setPosition(endPosition);
					setLocation(toLocation(getPosition()));
					Pool.getInstance().hit(this);
					return;
				} else {
					// bounce off at corners next to the holes
					int corner;
					cornerCheck: if ((corner = isNearCorner(endPosition)) >= 0) {
						if (corner == lastCornerHit) {
							// dont hit corner twice
							break cornerCheck;
						}
						double dx = endPosition.x - Pool.getCorners()[corner].x;
						double dy = endPosition.y - Pool.getCorners()[corner].y;
						if (dx * dy == 0) {
							break cornerCheck;
						}
						double direction = Math.atan2(dy, dx);
						if (Math.toDegrees(direction) >= Pool.getCornerstartangles()[corner]
							&& Math.toDegrees(direction) <= Pool.getCornerendangles()[corner]) {
							// ball hits corner
							double rotateAngle = 2 * (Math.atan2(velocity.y, velocity.x) - direction - Math.PI / 2);
							velocity.rotate((dy * dx > 0 ? -1 : -1) * rotateAngle);
							GGVector delta = new GGVector(dx, dy).getNormalized()
								.mult(2 * (RADIUS - Math.sqrt(dx * dx + dy * dy)));
							endPosition.add(delta);
							setPosition(endPosition);
							lastCornerHit = corner;
							lastCornerHitCycle = getNbCycles();
							break fieldCheck;
						}
					}
					// dont bounce off directly after corner hit
					if (lastCornerHitCycle + 1 == getNbCycles() || lastCornerHitCycle + 2 == getNbCycles()) {
						setPosition(endPosition);
						break fieldCheck;
					}
					// bounce off paddings with 45 degrees at holes 0, 2, 3 and 5
					if (new ArrayList<Integer>(Arrays.asList(0, 2, 3, 5)).contains(hole)) {
						if (isNearHole(endPosition, hole, Pool.getCorners()[0].x + Pool.getCorners()[0].y)) {
							bounceAngle(hole, endPosition);
							break fieldCheck;
						}
					}
					// bounce off paddings with 90 degrees at holes 1 and 4
					else if (new ArrayList<Integer>(Arrays.asList(1, 4)).contains(hole)) {
						if (isNearHole(endPosition, hole, Pool.getCorners()[2].y)) {
							if (endPosition.x < (501 + RADIUS)) {
								velocity = new GGVector(-velocity.x, velocity.y);
								endPosition.x = 2 * (501 + RADIUS) - endPosition.x;
							} else if (endPosition.x > (540 - RADIUS)) {
								velocity = new GGVector(-velocity.x, velocity.y);
								endPosition.x = 2 * (540 - RADIUS) - endPosition.x;
							}
							setPosition(endPosition);
							break fieldCheck;
						}
					}
					setPosition(endPosition);
				}
			} else {
				// dont bounce off directly after corner hit
				if (lastCornerHitCycle + 1 == getNbCycles()) {
					setPosition(endPosition);
					break fieldCheck;
				}
				// bounce off wall
				if (isOutOfFieldLeft(endPosition)) {
					endPosition.x = 2 * (WALL_SIZE - 1) - endPosition.x;
					velocity = new GGVector(-velocity.x, velocity.y);
				} else if (isOutOfFieldRight(endPosition)) {
					endPosition.x = 2 * (Pool.getInstance().getWidth() - (WALL_SIZE + 1)) - endPosition.x;
					velocity = new GGVector(-velocity.x, velocity.y);
				} else if (isOutOfFieldTop(endPosition)) {
					endPosition.y = 2 * (WALL_SIZE - 2) - endPosition.y;
					velocity = new GGVector(velocity.x, -velocity.y);
				} else if (isOutOfFieldBottom(endPosition)) {
					endPosition.y = 2 * (Pool.getInstance().getHeight() - (WALL_SIZE + 2)) - endPosition.y;
					velocity = new GGVector(velocity.x, -velocity.y);
				}
				setPosition(endPosition);
			}
		} else {
			setPosition(endPosition);
		}
		setLocation(toLocation(getPosition()));
		// slow down
		if (velocity.magnitude() < DELTA_V) {
			velocity = new GGVector();

		} else {
			velocity = velocity.sub(velocity.getNormalized().mult(DELTA_V));
		}
		// reset last hit corner
		if (velocity.magnitude2() == 0) {
			// reset last hit corner
			lastCornerHit = -1;
		}
	}

	boolean ballIsColliding(Location loc) {
		for (Ball ball : Pool.getInstance().getBalls()) {
			if (loc.x - 2 * RADIUS > ball.getLocation().x || loc.x + 2 * RADIUS < ball.getLocation().x) {
				if (loc.y - 2 * RADIUS > ball.getLocation().y || loc.y + 2 * RADIUS < ball.getLocation().y)
					return false;
			}

		}
		return true;
	}

	private void bounceAngle(int holeNumber, GGVector endPosition) {
		double dx = endPosition.x - Pool.getHolecenters()[holeNumber].x;
		double dy = endPosition.y - Pool.getHolecenters()[holeNumber].y;
		double sqmag = 0.5 * (dx - dy) * (dx - dy); // square offset from diagonal through hole center
		if (sqmag > DIAGONAL_PADDING_OFFSET * DIAGONAL_PADDING_OFFSET) {
			// bounce off wall
			if (holeNumber == 0 || holeNumber == 5) {
				velocity = new GGVector(velocity.y, velocity.x);
			} else {
				velocity = new GGVector(-velocity.y, -velocity.x);
			}
			endPosition.add(getBounceVector(holeNumber, endPosition).mult(2 * (DIAGONAL_PADDING_OFFSET - Math.sqrt(sqmag))));
		}
		setPosition(endPosition);
	}

	private GGVector getBounceVector(int holeNumber, GGVector endPosition) {
		if (holeNumber == 0 || holeNumber == 5) {
			// right or left corner padding
			if (velocity.x > velocity.y) {
				return new GGVector(1, -1);
			} else {
				return new GGVector(-1, 1);
			}
		} else if (holeNumber == 2 || holeNumber == 3) {
			// right or left corner padding
			if (velocity.x > -velocity.y) {
				return new GGVector(1, 1);
			} else {
				return new GGVector(-1, -1);
			}
		} else {
			System.err.println("Hole number does not specify a corner hole. ");
			return null;
		}
	}

	private boolean isOutOfFieldBottom(GGVector position) {
		return position.y > (Pool.getInstance().getHeight() - (WALL_SIZE + 2));
	}

	private boolean isOutOfFieldTop(GGVector position) {
		return position.y < (WALL_SIZE - 2);
	}

	private boolean isOutOfFieldRight(GGVector position) {
		return position.x > (Pool.getInstance().getWidth() - (WALL_SIZE + 1));
	}

	private boolean isOutOfFieldLeft(GGVector position) {
		return position.x < (WALL_SIZE - 1);
	}

	private boolean isOutOfField(GGVector position) {
		return isOutOfFieldBottom(position) || isOutOfFieldLeft(position) || isOutOfFieldRight(position)
			|| isOutOfFieldTop(position);
	}

	private boolean isInHole(GGVector position, int holeNumber) {
		double sqmag = position.sub(new GGVector(Pool.getHolecenters()[holeNumber].x, Pool.getHolecenters()[holeNumber].y))
			.magnitude2();
		if (sqmag < Pool.getHoleradii()[holeNumber] * Pool.getHoleradii()[holeNumber]) {
			return true;
		}
		return false;
	}

	private int isNearHole(GGVector position) {

		// is near middle holes (hole 1 or 4)
		if (isOutOfField(position) && position.x >= (379 + Pool.getImageMargin())
			&& position.x <= (418 + Pool.getImageMargin())) {
			return position.y > Pool.getInstance().getNbVertCells() / 2 ? 4 : 1;
		}
		// is near top left hole (hole 0)
		if (isOutOfField(position) && position.x < (67 + Pool.getImageMargin()) && position.y < (67 + Pool.getImageMargin())) {
			return 0;
		}
		// is near top right hole (hole 2)
		if (isOutOfField(position) && position.x > (Pool.getInstance().getWidth() - Pool.getImageMargin() - 67)
			&& position.y < (67 + Pool.getImageMargin())) {
			return 2;
		}
		// is near bottom left hole (hole 3)
		if (isOutOfField(position) && position.x < (67 + Pool.getImageMargin())
			&& position.y > (Pool.getInstance().getHeight() - Pool.getImageMargin() - 67)) {
			return 3;
		}
		// is near bottom right hole (hole 5)
		if (isOutOfField(position) && position.x > (Pool.getInstance().getWidth() - Pool.getImageMargin() - 67)
			&& position.y > (Pool.getInstance().getHeight() - Pool.getImageMargin() - 67)) {
			return 5;
		}
		return -1;
	}

	private boolean isNearHole(GGVector position, int holeNumber, double offsetValue) {
		switch (holeNumber) {
		case 0:
			if (position.x + position.y <= offsetValue) {
				return true;
			}
			break;
		case 1:
			if (position.y <= offsetValue) {
				return true;
			}
			break;
		case 2:
			if (Pool.getInstance().getNbHorzCells() - position.x + position.y <= offsetValue) {
				return true;
			}
			break;
		case 3:
			if (Pool.getInstance().getNbVertCells() - position.y + position.x <= offsetValue) {
				return true;
			}
			break;
		case 4:
			if (Pool.getInstance().getNbVertCells() - position.y <= offsetValue) {
				return true;
			}
			break;
		case 5:
			if (Pool.getInstance().getNbHorzCells() - position.x + Pool.getInstance().getNbVertCells()
				- position.y <= offsetValue) {
				return true;
			}
			break;
		}
		return false;
	}

	private int isNearCorner(GGVector position) {
		for (int ii = 0; ii < Pool.getCorners().length; ii++) {
			if (isNearCorner(position, ii)) {
				return ii;
			}
		}
		return -1;
	}

	private boolean isNearCorner(GGVector position, int number) {
		double sqmag = position.sub(toPosition(Pool.getCorners()[number])).magnitude2();
		if (sqmag < (RADIUS * RADIUS)) {
			return true;
		}
		return false;
	}

	static GGVector toPosition(Location location) {
		return new GGVector(location.x, location.y);
	}

	static Location toLocation(GGVector position) {
		return new Location((int) (position.x), (int) (position.y));
	}

	GGVector getPosition() {
		return position;
	}

	void setPosition(GGVector position) {
		this.position = position;
	}

	GGVector getVelocity() {
		return velocity;
	}

	void setVelocity(GGVector velocity) {
		this.velocity = velocity;
	}

}
