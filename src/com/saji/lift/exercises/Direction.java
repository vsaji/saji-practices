package com.saji.lift.exercises;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * @author sv11741
 *
 */
public enum Direction {
	UP(new ConcurrentHashMap <Integer,Integer>()),DOWN(new ConcurrentHashMap <Integer,Integer>()),STALL(new ConcurrentHashMap <Integer,Integer>());
	
	private final ConcurrentHashMap <Integer,Integer> q;
	
	/**
	 * 
	 * @param q
	 */
	private Direction(ConcurrentHashMap <Integer,Integer> q){
		this.q = q;
	}
	
	
	public Direction reverse(){
		return ("UP".equals(this.name())) ? DOWN : UP;  
	}
	
	/**
	 * 
	 * @return
	 */
	public ConcurrentHashMap <Integer,Integer> getQ(){
		return q;
	}
	
	
}