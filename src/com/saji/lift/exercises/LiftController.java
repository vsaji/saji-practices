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
	
	private ExecutorService service = null;
	private BlockingQueue<Integer>[] liftNotifierQ;
	private boolean initialized = false;

	/**
	 * 
	 */
	private LiftController() {}

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

		service = Executors.newFixedThreadPool(liftLevels.length);

		if (!initialized) {
			liftNotifierQ = new BlockingQueue[liftLevels.length];

			System.out.println("# OF LIFTS [" + liftLevels.length
					+ "] ==> LEVELS " + Arrays.toString(liftLevels));

			for (int i = 0; i < liftLevels.length; i++) {
				liftNotifierQ[i] = new ArrayBlockingQueue<Integer>(1);
				Lift l = new Lift(liftLevels[i], i);
				lifts.add(i, l);
				service.submit(new LiftRunner(l, liftNotifierQ[i]));
			}
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
		Lift lift = null;
		boolean notificationReq = true;

		if (DOWN.getQ().isEmpty() && UP.getQ().isEmpty()) {
			lift = getNearestAvailableLift(currFlr, Direction.STALL);
			direction = (lift.getCurrLevel() > currFlr) ? DOWN : UP;

		} else {

			direction = (currFlr > destFlr) ? DOWN : UP;
			lift = getNearestAvailableLift(currFlr, direction);
			lift = (lift==null) ? getNearestAvailableLift(currFlr, Direction.STALL) : lift;
			
			if (lift.getDirection() == Direction.STALL) {// ----------------------------------------------------------
				
				if (direction == DOWN && lift.getCurrLevel() < currFlr) { direction = UP;}
				if (direction == UP && lift.getCurrLevel() > currFlr) { direction = DOWN;}
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
							while((lift = getNearestAvailableLift(currFlr, Direction.STALL))==null){
							try {
								TimeUnit.MILLISECONDS.sleep(3000);
							} catch (InterruptedException e) {
							
								e.printStackTrace();
							}
							}
							direction = (lift.getCurrLevel() < currFlr) ? UP : DOWN;
							notificationReq = true; // to start the lift
					}
					
				}
			} else {// -----------------------------------------------------------------------------------------------
				
				if (lift.getCurrLevel() < currFlr) { // lift is below the requested floor
						notificationReq = false; // no notification is required as this is a moving lift.
				} else { // lift is moving upwards and it is above the requested floor.

					lift = getNearestAvailableLift(currFlr, DOWN); //

					if (lift != null && lift.getDestLevel() > currFlr && (Math.abs(lift.getCurrLevel()-currFlr) < 3)) { // check if the down coming lift last stop is above the requested level.
						lift.addStop(currFlr);
						direction = DOWN;
						notificationReq = false;
					} else {
						while ((lift = getNearestAvailableLift(currFlr,	Direction.STALL)) == null) {
							try {
								TimeUnit.MILLISECONDS.sleep(3000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						direction = (lift.getCurrLevel() > currFlr) ? DOWN : UP;
						notificationReq = true; // to start the lift
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
		direction.getQ().put(currFlr, destFlr);
		
		if (notificationReq) {
			liftNotifierQ[lift.getLiftNum()].offer(currFlr);
		}
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
	protected Lift getNearestAvailableLift(int level, Direction d) {

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
		
		return nearestLift;
	}

	/**
	 * 
	 * @return
	 */
	protected Lift getTheFirstAvailableLift(List<Lift> l,Direction d) {
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
	protected List<Lift> getLiftsByDirection(Direction d) {

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
	 * @param args
	 */
	public static void main(String[] args) {
		LiftController lc = LiftController.getInstance();
		lc.setup(new int[] { 10, 3, 9,8,15 });

		ExecutorService service1 = Executors.newCachedThreadPool();
		service1.submit(new LiftRequestor());
		service1.shutdown();
	}
}