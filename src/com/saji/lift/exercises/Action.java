package com.saji.lift.exercises;


/**
 * 
 * This class defines the action signals for LiftRunner Class
 * 
 * @see LiftController#adviceAction(Lift, Direction)
 * @see LiftRunner#checkLevel(Direction d)
 * 
 * @author Saji Venugopalan
 *
 */
public enum Action {
	
	STOP_N_MOVE,MOVE_FORWARD,STALL,MOVE_OPPOSITE;

}