package com.saji.lift.exercises;

import java.util.LinkedHashMap;

/**
 * 
 * @author sv11741
 *
 */
public enum Direction {
	UP(new LinkedHashMap <Integer,Integer>()),DOWN(new LinkedHashMap <Integer,Integer>()),STALL(new LinkedHashMap <Integer,Integer>());
	
	private final LinkedHashMap <Integer,Integer> q;
	
	/**
	 * 
	 * @param q
	 */
	private Direction(LinkedHashMap <Integer,Integer> q){
		this.q = q;
	}
	
	
	public Direction reverse(){
		return ("UP".equals(this.name())) ? DOWN : UP;  
	}
	
	/**
	 * 
	 * @return
	 */
	public LinkedHashMap <Integer,Integer> getQ(){
		return q;
	}
	
	
}