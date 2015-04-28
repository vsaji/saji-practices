package com.saji.lift.exercises;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class defines the Elevator Directions through enumeration
 * Uses ConcurrentHashMap to store the req
 * 
 * @author Saji Venugopalan
 *
 */
public enum Direction {
	UP,DOWN,STALL;
	
	//Set backed up ConcurrentHashMap
	private final Set <Integer> q;
	
	/**
	 * 
	 * @param q
	 */
	private Direction(){
		this.q = Collections.newSetFromMap(new ConcurrentHashMap <Integer,Boolean>());
	}
	
	/**
	 * 
	 * @return reverse of the current direction
	 */
	public Direction reverse(){
		return ("UP".equals(this.name())) ? DOWN : UP;  
	}
	
	/**
	 * 
	 * @return Set that stores requester level
	 */
	public Set <Integer> getQ(){
		return q;
	}
	
	
}