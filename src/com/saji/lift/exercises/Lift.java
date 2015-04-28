package com.saji.lift.exercises;

import java.util.TreeSet;


/**
 * This is a POJO representation of Lift.
 * 
 * 
 * @author Saji Venugopalan
 * 
 */
public class Lift {

	private int currLevel = 0;
	private int reqLevel = 0;
	
	private final int liftNum;

	public static final int MAX_FLOOR = 15;
	public static final int MIN_FLOOR = 1;
	public static final int ACCEPTABLE_DISTANCE = 2;
	public static final int LIFT_MOVE_DELAY = 1;
	public static final int LIFT_STOP_DELAY = 2;
	
	
	private Direction direction = Direction.STALL;
		
	private final TreeSet<Integer> stops;
	private Direction secDirection; 

	/**
	 * 
	 * @param currLevel
	 * @param liftNum
	 */
	public Lift(int currLevel, int liftNum) {
		this.currLevel = currLevel;
		this.liftNum = liftNum;
		this.stops = new TreeSet<Integer>();
	}

	/**
	 * 
	 * @return Current Level of the elevator
	 */
	public int getCurrLevel() {
		return currLevel;
	}

	
	/**
	 * Decrement the current level
	 * @see LiftRunner#run()
	 */
	public void levelDown(){
		currLevel--;
	}
	
	
	/**
	 * Increment the current level
	 * @see LiftRunner#run()
	 */
	public void levelUp(){
		currLevel++;
	}
	
	
	/**
	 * 
	 * @return true if the current level is also a destination
	 */
	public boolean isCurrLevelIsDest(){
		return stops.contains(currLevel) && direction==secDirection;
	}
	
	/**
	 * To accept a new destination for this elevator
	 * @see LiftController#sendRequest(String);
	 */
	public synchronized void addStop(int level){
		stops.add(level);
	}
	
	
	/**
	 * Remove if the destination has been already served. 
	 * @see LiftController#adviceAction(Lift, Direction);
	 */
	public synchronized void removeStop(){
		stops.remove(currLevel);
	}
	
	
	
	/**
	 * @see LiftController#adviceAction(Lift, Direction);
	 * @return true if the elevator has some more stops. 
	 * 
	 */
	public boolean hasMoreStops(){
		return stops.size() > 0;
	}
	

	/**
	 * 
	 * @return Number associated to this elevator
	 */
	public int getLiftNum() {	
		return liftNum;
	}

	/**
	 * 
	 * @return current direction of this elevator
	 */
	public Direction getDirection() {
		return direction;
	}

	/**
	 * Sets the current direction of the lift.
	 * Also sets secondary direction of this lift. It will be overridden in setSecDirection.
	 * 
	 * @param currentDirection
	 */
	public void setDirection(Direction currentDirection) {
		setSecDirection(currentDirection);
		this.direction = currentDirection;
	}

	/**
	 * Sets the current level
	 * @param currLevel
	 */
	public void setCurrLevel(int currLevel) {
		this.currLevel = currLevel;
	}

	
	/**
	 * 
	 */
	public String toString() {
		return "[#" + liftNum + "] CL-->[" + getCurrLevel()+"] RL["+reqLevel+"] CD-->["+getDirection()+"] DL-->["+getDestLevel()+"]";
	}


	/**
	 * 
	 * @return Max/Min destination level based on the direction. 
	 */
	public int getDestLevel() {
		if(stops.size()==0) return 0;
		return (direction==Direction.UP) ? stops.last() : stops.first();
		
	}

	/**
	 * 
	 * @return requested level
	 */
	public int getReqLevel() {
		return reqLevel;
	}

	/**
	 * Sets the requested level
	 * @param reqLevel
	 */
	public void setReqLevel(int reqLevel) {
		this.reqLevel = reqLevel;
	}

	/**
	 * Sets secondary direction of this lift.
	 * This method should be set only after setDirection, else  secondary direction will be overridden by primary direction.
	 * 
	 * @param secDirection
	 */
	public void setSecDirection(Direction secDirection) {
		this.secDirection = secDirection;
	}

}