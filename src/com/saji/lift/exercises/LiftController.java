/**
 * 
 */
package com.saji.lift.exercises;

import static com.saji.lift.exercises.Direction.DOWN;
import static com.saji.lift.exercises.Direction.UP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * @author sv11741
 * 
 */
public class LiftController {

	private final ArrayList<Lift> lifts = new ArrayList<Lift>();
	private static final LiftController instance = new LiftController();
	
	private BlockingQueue<Integer>[] liftNotifierQ;
	private BlockingQueue<int[]> waitingReqQ;
	private boolean initialized = false;

	/**
	 * 
	 */
	private LiftController() {
		this.waitingReqQ = new ArrayBlockingQueue<int[]>(2);
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
	@SuppressWarnings("unchecked")
	public void setup(int[] liftLevels) {

		if (!initialized) {
			ExecutorService service = Executors.newFixedThreadPool(liftLevels.length);
			liftNotifierQ = new BlockingQueue[liftLevels.length];

			System.out.println("# OF LIFTS [" + liftLevels.length
					+ "] ==> LEVELS " + Arrays.toString(liftLevels));

			for (int i = 0; i < liftLevels.length; i++) {
				liftNotifierQ[i] = new ArrayBlockingQueue<Integer>(1);
				Lift l = new Lift(liftLevels[i], i);
				lifts.add(i, l);
				service.submit(new LiftRunner(l, liftNotifierQ[i]));
			}
			
			ExecutorService service1 = Executors.newCachedThreadPool();
			service1.submit(new LiftLocator(waitingReqQ));
			
			initialized = true;
		}
	}


	/**
	 * 
	 * @param currFloorAndDestination
	 */
	public synchronized void sendRequest(String currFloorAndDestination) {

		String[] flrDest = currFloorAndDestination.split("[,]");
		int currFlr = Integer.parseInt(flrDest[0]);
		int destFlr = Integer.parseInt(flrDest[1]);

		Direction direction = null;
		Direction secDirection = null;
		Lift lift = null;
		boolean notificationReq = true;

		if (isAllLiftStall()) {
			
			lift = getNearestAvailableLift(currFlr, Direction.STALL);
			direction = (lift.getCurrLevel() > currFlr) ? DOWN : UP;

			
		} else {

			direction = (currFlr > destFlr) ? DOWN : UP;
			lift = getNearestAvailableLift(currFlr, direction);
			lift = (lift==null) ? getNearestAvailableLift(currFlr, Direction.STALL) : lift;
			
			if(lift==null){
				
				waitList(new int[]{currFlr,destFlr});
				return;
			}
			
		
			if (lift.getDirection() == Direction.STALL) {// ----------------------------------------------------------

				if (direction == DOWN && lift.getCurrLevel() < currFlr) { direction = UP;secDirection=DOWN;}
				if (direction == UP && lift.getCurrLevel() > currFlr) { direction = DOWN;secDirection=UP;}
				notificationReq = true;
		
			} else if (direction == DOWN) {// -----------------------------------------------------------------------

				if (lift.getCurrLevel() > currFlr) {
					notificationReq = false;
				
				} else {
					
					lift = getNearestAvailableLift(currFlr, UP); //

						if (lift != null && lift.getDestLevel() < currFlr && (Math.abs(lift.getCurrLevel()-currFlr) < 3)) { // check if the down coming lift last stop is above the requested level.
							lift.addStop(currFlr);
							direction = UP;
							notificationReq = false;
						} else {
							
							waitList(new int[]{currFlr,destFlr});
							
							return;
						}
				}
			}else {// -----------------------------------------------------------------------------------------------


				if (lift.getCurrLevel() < currFlr) { // lift is below the requested floor

					notificationReq = false; // no notification is required as this is a moving lift.
				
				} else { // lift is moving upwards and it is above the requested floor.

					lift = getNearestAvailableLift(currFlr, DOWN); //

					if (lift != null && lift.getDestLevel() > currFlr && (Math.abs(lift.getCurrLevel()-currFlr) < 3)) { // check if the down coming lift last stop is above the requested level.

						lift.addStop(currFlr);
						direction = DOWN;
						notificationReq = false;
						
					} else {
						waitList(new int[]{currFlr,destFlr});
						return;
					}
				}
			}
		}

		if (lift.getCurrLevel() == currFlr) {
			direction = (currFlr > destFlr) ? DOWN : UP;
			lift.setReqLevel(-1);
		} else {
			lift.setReqLevel(currFlr);
		}

		lift.addStop(destFlr);
		lift.setDirection(direction);
		lift.setSecDirection(secDirection==null ? direction : secDirection);
		direction.getQ().put(currFlr, destFlr);
		
		if (notificationReq) {
			liftNotifierQ[lift.getLiftNum()].offer(currFlr);
		}
	}

	
	/**
	 * 
	 * @param currFlr
	 * @return
	 */
	private void waitList(int[] flrs){
		
		sop("All lifts are busy. Adding ["+flrs[0]+"],["+flrs[1]+"] into waiting list.");
		waitingReqQ.offer(flrs);
	}
	
	
	/**
	 * 
	 * @return
	 */
	public synchronized Action adviceAction(Lift lift, Direction currDirection) {
		int nextLevel = lift.getCurrLevel();

		synchronized (currDirection.getQ()) {

			if (currDirection.getQ().keySet().contains(nextLevel)) {//check if anyone called lift for this level [PICK UP]

				if (nextLevel > lift.getDestLevel() && currDirection==UP) {// Lift came from lower level and the requester want to go to the lower level again.
					return applyOppositeAction(lift, currDirection, UP);

				} else if (nextLevel < lift.getDestLevel() && currDirection==DOWN) { // change direction
					return applyOppositeAction(lift, currDirection, DOWN);
				}else{
					currDirection.getQ().remove(nextLevel);
					return (currDirection.getQ().size() == 0 && !lift.hasMoreStops()) ? Action.STALL : Action.STOP_N_MOVE;
				}
			} else if (lift.isCurrLevelIsDest()) { //[DROP]
					lift.removeStop();
					return lift.hasMoreStops() ? Action.STOP_N_MOVE : Action.STALL;
			} else {
				return lift.hasMoreStops() ? Action.MOVE_FORWARD : Action.STALL;
			}
		}
	}

	/**
	 * 
	 * @param lift
	 * @param currDirection
	 * @return
	 */
	private Action applyOppositeAction(Lift lift, Direction currDirection,
			Direction toDirection) {

		currDirection.getQ().remove(lift.getCurrLevel());
		
		if (currDirection == toDirection) {
			Direction reverse = currDirection.reverse();
			lift.setDirection(reverse);
			return Action.MOVE_OPPOSITE;
		} else {
			return Action.STOP_N_MOVE;
		}
	}

	/**
	 * 
	 * @param level
	 * @return
	 */
	private Lift getNearestAvailableLift(int level, Direction d) {

		List<Lift> l = (d != null) ? getLiftsByDirection(d) : lifts;
		
		Lift nearestLift = getTheFirstAvailableLift(l,d);

		if (nearestLift != null) {
			int currLevel = nearestLift.getCurrLevel();

			for (Lift lift : l) {

				int nearestLevel = lift.getCurrLevel();
				if (Math.abs(level - nearestLevel) < Math.abs(level - currLevel)) {
					currLevel = nearestLevel;
					nearestLift = lift;
				}
			}
		}
		
		if(nearestLift!=null){
		//	sop(nearestLift+"--"+Math.abs(nearestLift.getCurrLevel()-level));
			int gap = Math.abs(nearestLift.getCurrLevel()-level);
			if( d!=Direction.STALL && gap > Lift.ACCPT_NEAR_LIFT_GAP){
				nearestLift = null;
			}
		}
					
		return nearestLift;
	}

	/**
	 * 
	 * @return
	 */
	private Lift getTheFirstAvailableLift(List<Lift> l,Direction d) {
		Lift avlift = null;

		if (l != null) {
			for (Lift lift : l) {
				avlift = lift;
				break;
			}
		}
		return avlift;
	}

	/**
	 * 
	 * @return
	 */
	private List<Lift> getLiftsByDirection(Direction d) {

		List<Lift> l = new ArrayList<Lift>();

		for (Lift lift : lifts) {
			if (lift.getDirection() == d) {
				l.add(lift);
			}
		}
		return l;
	}

	
	/**
	 * 
	 * @return
	 */
	private boolean isAllLiftStall(){
		return lifts.size()==getLiftsByDirection(Direction.STALL).size(); 
	}
	
	/**
	 * 
	 */
	private void sop(String msg){
		System.out.println("[CONT]["+msg+"]");
	}
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		LiftController lc = LiftController.getInstance();
		lc.setup(new int[] { 15, 14 });

		ExecutorService service1 = Executors.newCachedThreadPool();
		service1.submit(new LiftRequestor());
		service1.shutdown();
	}
	
	
	/**
	 * 
	 * @author sv11741
	 *
	 */
	public class LiftLocator implements Runnable {
	     
		
		private BlockingQueue<int[]> waitReqQ; 
		
		/**
		 * 
		 * @param waitReqQ
		 */
	    public LiftLocator(BlockingQueue<int[]> waitReqQ){ 
	    	this.waitReqQ=waitReqQ; 
	    }
	    

		@Override
		public void run() {
			try{
				while(true){
					int[] flrs = waitReqQ.take();
					
					Lift lift = null;
				    
					while ((lift = getNearestAvailableLift(flrs[0],	Direction.STALL)) == null) {
						TimeUnit.MILLISECONDS.sleep(1000);
				    }
				    	
			    	Direction direction = (flrs[0] > flrs[1]) ? DOWN : UP;
			    	Direction secDirection = direction;
			    	
			    	if(direction==DOWN){
			    		if(lift.getCurrLevel() < flrs[0]){direction = UP; secDirection=DOWN;}
			    	}else{
			    		if(lift.getCurrLevel() > flrs[0]){direction = DOWN;	secDirection=UP;}
			    	}
				    	
					if (lift.getCurrLevel() == flrs[0]) { lift.setReqLevel(-1);	}
					else {lift.setReqLevel(flrs[0]);}

					lift.addStop(flrs[1]);
					lift.setDirection(direction);
					lift.setSecDirection(secDirection==null ? direction : secDirection);
					direction.getQ().put(flrs[0], flrs[1]);
					liftNotifierQ[lift.getLiftNum()].offer(flrs[0]);
				    	
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	 
	}
	
}