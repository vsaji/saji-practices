/**
 * 
 */
package com.saji.lift.exercises;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.saji.lift.exercises.Direction.DOWN;
import static com.saji.lift.exercises.Direction.UP;
import static com.saji.lift.exercises.Direction.STALL;

/**
 * @author sv11741
 * 
 */
public class LiftController {

	private final ArrayList<Lift> lifts;
	
	private static final LiftController instance = new LiftController();

	private ExecutorService service = null;
	private BlockingQueue<Integer>[] liftNotifierQ = null;
	private boolean initialized = false;
	
	/**
	 * 
	 */
	private LiftController() {
		lifts = new ArrayList<Lift>();
	}


	/**
	 * 
	 * @return
	 */
	public static LiftController getInstance() {
		return instance;
	}

	
	
	/**
	 * 
	 * @param numberOfLifts
	 * @param liftLevels
	 */
	public void setup(int[] liftLevels) {
		
		service = Executors.newFixedThreadPool(liftLevels.length);
		
		if (!initialized) {
			liftNotifierQ = new BlockingQueue[liftLevels.length];
			for (int i = 0; i < liftLevels.length; i++) {
				liftNotifierQ[i] = new ArrayBlockingQueue<Integer>(1);
				Lift l = new Lift(liftLevels[i], i);
				lifts.add(i, l);
				service.submit(new LiftRunner(l,liftNotifierQ[i]));
				
				System.out.println("LIFT # ["+i+"] ==> CURRENT LEVEL ["+liftLevels[i]+"]");
			}
			initialized = true;
		}
	}

	
	
	/**
	 * 
	 */
	public synchronized void sendRequest(String currFloorAndDestination) {
		
		String[] flrDest = currFloorAndDestination.split("[,]");
		int currFlr = Integer.parseInt(flrDest[0]);
		int destFlr = Integer.parseInt(flrDest[1]);
	
		Direction direction = null;
		Lift lift = null;
		boolean notificationReq = true;
		
		if( DOWN.getQ().isEmpty() && UP.getQ().isEmpty()){
			lift = getNearestAvailableLift(currFlr,null);
		}else{

			//check the direction request currFloor,destFloor
			//check the nearest lift in the same direction
			//check if the nearest lift is > currFloor - if the direction is down
			//check if the nearest lift is < currFloor - if the direction is up
			//reset the destLevel of the lift to destFloor if it is greater than 
			
			direction = (currFlr > destFlr ) ? DOWN : UP;
			lift = getNearestAvailableLift(currFlr,direction);
			if(direction==DOWN ){
				if(lift.getCurrLevel() > currFlr){
					if(destFlr > lift.getDestLevel() ){
						lift.setDestLevel(destFlr);
						notificationReq=false;
					}
				}
			}else{
				if(lift.getCurrLevel() < currFlr){
					if(destFlr > lift.getDestLevel() ){
						lift.setDestLevel(destFlr);
						notificationReq=false;
					}
				}
			}
		}
		
		if(lift.getCurrLevel()==currFlr){
			direction = (currFlr > destFlr ) ? DOWN : UP;
			lift.setReqLevel(-1);	
		}else{
			direction = (lift.getCurrLevel() > currFlr ) ? DOWN : UP;
			lift.setReqLevel(currFlr);
		}
		
		lift.setDestLevel(destFlr);
		lift.setDirection(direction);
		direction.getQ().put(currFlr, destFlr);
		
		if(notificationReq){
			liftNotifierQ[lift.getLiftNum()].offer(currFlr);
		}
	}

	
	/**
	 * 
	 * @return
	 */
	public synchronized Action adviceAction(Lift lift,Direction currDirection) {
		int nextLevel =  lift.getCurrLevel();
		
		synchronized(currDirection.getQ()){
		
			if(currDirection.getQ().keySet().contains(nextLevel) ){
				
				if(currDirection.getQ().get(nextLevel)==null && currDirection.getQ().size() > 0){
					currDirection.getQ().remove(nextLevel);
					return (currDirection.getQ().size()==0) ? Action.STALL : Action.STOP_N_MOVE;
				}
				else if(nextLevel >  lift.getDestLevel() ){
					lift.setDestLevel(currDirection.getQ().get(nextLevel));
					currDirection.getQ().put(lift.getDestLevel(), null); // to stop the dest location;
					currDirection.getQ().remove(nextLevel);
					return Action.STOP_N_MOVE;
					
				}else if(nextLevel <  lift.getDestLevel()){ //change direction 
					lift.setDestLevel(currDirection.getQ().get(nextLevel));
					currDirection.getQ().remove(nextLevel);
					
					if(currDirection==DOWN){
						Direction reverse = currDirection.reverse();
						reverse.getQ().put(lift.getDestLevel(), null); // to stop the dest location;
						lift.setDirection(reverse);
						return Action.MOVE_REVERSE;				
					}else{
						currDirection.getQ().put(lift.getDestLevel(), null); // to stop the dest location;
						return Action.STOP_N_MOVE;				
					}
					
				}
				return Action.STOP_N_MOVE;
			}else if(!currDirection.getQ().keySet().contains(nextLevel) && currDirection.getQ().size() > 0){
				return Action.MOVE_FORWARD;
			}else{
				return Action.STALL;
			}
		}
	}
	

	/**
	 * 
	 * @param level
	 * @return
	 */
	private Lift getNearestAvailableLift(int level, Direction d) {

		Lift nearestLift = getTheFirstAvailableLift();
		int currLevel = nearestLift.getNearestLevel();

		for (Lift lift : lifts) {
			
			if(d!=null && lift.getDirection()!=d){
				continue;
			}
			
			int nearestLevel = lift.getNearestLevel();
			// System.out.println(level+"-"+lift.getCurrLevel()+"("+Math.abs(level-lift.getCurrLevel())+") < ("+Math.abs(level-currLevel)+") "+level+"-"+currLevel);
				if (Math.abs(level - nearestLevel) < Math.abs(level	- currLevel)) {
					currLevel = nearestLevel;
					nearestLift = lift;
			}
		}

		return nearestLift;
	}

	/**
	 * 
	 * @return
	 */
	private Lift getTheFirstAvailableLift() {
		Lift avlift = null;

		for (Lift lift : lifts) {
				avlift = lift;
				break;
		}
		return avlift;
	}
	

	/**
	 * 
	 * @return
	 */
	private Lift getStalledLift(){
		for (Lift lift : lifts) {
			if(lift.getDirection()==STALL){
				return lift;
			}
		}
		return null;
	}
	
	

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		LiftController lc = LiftController.getInstance();
		lc.setup(new int[] { 15 , 2, 10  });

		ExecutorService service1 = Executors.newCachedThreadPool();
		service1.submit(new LiftRequestor());
		service1.shutdown();
	}

}