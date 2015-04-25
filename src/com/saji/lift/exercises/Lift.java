package com.saji.lift.exercises;


/**
 * 
 * @author sv11741
 * 
 */
public class Lift {

	private int currLevel = 0;
	private int reqLevel = 0;
	private int destLevel = 0;
	
	private final int liftNum;

	public static final int MAX_FLOOR = 16;
	public static final int MIN_FLOOR = 0;
	
	private Direction direction = Direction.STALL;

	/**
	 * 
	 * @param pos
	 */
	public Lift(int currLevel, int liftNum) {
		this.currLevel = currLevel;
		this.liftNum = liftNum;
	}

	/**
	 * 
	 * @return
	 */
	public int getCurrLevel() {
		return currLevel;
	}

	
	/**
	 * 
	 */
	public void levelDown(){
		currLevel--;
	}
	
	/**
	 * 
	 */
	public void levelUp(){
		currLevel++;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getNearestLevel() {
		if (direction != Direction.STALL) {
			if (Math.abs(destLevel - currLevel) > 2) {
				return (direction == Direction.UP) ? currLevel + 1
						: currLevel - 1;
			}
			return destLevel;
		} else {
			return currLevel;
		}

	}


	/**
	 * 
	 * @param level
	 */
	public void setDestLevel(int destLevel) {
			this.destLevel = destLevel;
	}

	/**
	 * 
	 * @return
	 */
	public int getLiftNum() {	
		return liftNum;
	}

	/**
	 * 
	 * @return
	 */
	public Direction getDirection() {
		return direction;
	}

	/**
	 * 
	 * @param currentDirection
	 */
	public void setDirection(Direction currentDirection) {
		this.direction = currentDirection;
	}

	/**
	 * 
	 * @param currLevel
	 */
	public void setCurrLevel(int currLevel) {
		this.currLevel = currLevel;
	}

	/**
	 * 
	 */
	public String toString() {
		return "# : " + liftNum + "-->" + getCurrLevel()+"-->"+getDirection();
	}


	/**
	 * 
	 * @return
	 */
	public int getDestLevel() {
		return destLevel;
	}

	/**
	 * 
	 * @return
	 */
	public int getReqLevel() {
		return reqLevel;
	}

	/**
	 * 
	 * @param reqLevel
	 */
	public void setReqLevel(int reqLevel) {
		this.reqLevel = reqLevel;
	}

}