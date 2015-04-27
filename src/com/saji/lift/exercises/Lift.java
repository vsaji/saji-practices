package com.saji.lift.exercises;

import java.util.TreeSet;


/**
 * 
 * @author sv11741
 * 
 */
public class Lift {

	private int currLevel = 0;
	private int reqLevel = 0;
	
	private final int liftNum;

	public static final int MAX_FLOOR = 16;
	public static final int MIN_FLOOR = 0;
	public static final int ACCPT_NEAR_LIFT_GAP = 2;
	
	private Direction direction = Direction.STALL;
		
	private final TreeSet<Integer> stops;
	private Direction secDirection; 

	/**
	 * 
	 * @param pos
	 */
	public Lift(int currLevel, int liftNum) {
		this.currLevel = currLevel;
		this.liftNum = liftNum;
		this.stops = new TreeSet<Integer>();
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
	public boolean isCurrLevelIsDest(){
		return stops.contains(currLevel) && direction==secDirection;
	}
	
	/**
	 * 
	 * @return
	 */
	public synchronized void addStop(int level){
		stops.add(level);
	}
	
	/**
	 * 
	 * @param level
	 */
	public synchronized void removeStop(){
		stops.remove(currLevel);
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean hasMoreStops(){
		return stops.size() > 0;
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
		setSecDirection(currentDirection);
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
		return "[#" + liftNum + "] CL-->[" + getCurrLevel()+"] RL["+reqLevel+"] CD-->["+getDirection()+"] DL-->["+getDestLevel()+"]";
	}


	/**
	 * 
	 * @return
	 */
	public int getDestLevel() {
		if(stops.size()==0) return 0;
		return (direction==Direction.UP) ? stops.last() : stops.first();
		
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

	/**
	 * 
	 * @param secDirection
	 */
	public void setSecDirection(Direction secDirection) {
		this.secDirection = secDirection;
	}

}