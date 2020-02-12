package PoolGame;

import ch.aplu.jgamegrid.Location;

public class WhiteBall extends Ball {

	final static String imagePath = "sprites/kugel_weiﬂ.gif";

	public WhiteBall(Location loc) {
		super(false, imagePath, loc);
	}

}