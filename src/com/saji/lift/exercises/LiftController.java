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
			System.out.println(currFloorAndDestination + "--->RULE0");
		} else {

			// check the direction request currFloor,destFloor
			// check the nearest lift in the same direction
			// check if the nearest lift is > currFloor - if the direction is
			// down
			// check if the nearest lift is < currFloor - if the direction is up
			// reset the destLevel of the lift to destFloor if it is greater
			// than

			direction = (currFlr > destFlr) ? DOWN : UP;
			lift = getNearestAvailableLift(currFlr, direction);

			lift = (lift==null) ? getNearestAvailableLift(currFlr, Direction.STALL) : lift;
			
			if (lift.getDirection() == Direction.STALL) {// ----------------------------------------------------------
				lift.setDestLevel(destFlr);
				notificationReq = true;
				System.out.println(currFloorAndDestination + "--->RULE0.1-->"
						+ lift);
			} else if (direction == DOWN) {// -----------------------------------------------------------------------
				if (lift.getCurrLevel() > currFlr) {
					if (destFlr > lift.getDestLevel()) {
						lift.setDestLevel(destFlr);
						notificationReq = false;
						System.out.println(currFloorAndDestination
								+ "--->RULE1-->" + lift + "--"
								+ lift.getDirection());
					} else {
						System.out.println(currFloorAndDestination
								+ "--->RULE1.1");
					}
				} else {
					System.out
							.println(currFloorAndDestination + "--->RULE1.11");
				}
			} else {// ------------------------------------------------------------------------------------------------
				if (lift.getCurrLevel() < currFlr) { // lift is below the
														// requested floor
					if (destFlr > lift.getDestLevel()) { // Intended target
															// floor is gt
															// moving lift.
						lift.setDestLevel(destFlr); // set the current target
													// floor as final
													// destination
						notificationReq = false; // no notification is required
													// as this is a moving lift.
						System.out.println(currFloorAndDestination
								+ "--->RULE2");
					} else {
						System.out.println(currFloorAndDestination
								+ "--->RULE2.1");
					}

				} else { // lift is moving upwards and it is above the requested
							// floor.

					lift = getNearestAvailableLift(currFlr, DOWN); //

					if (lift != null && lift.getDestLevel() > currFlr) { // check
																			// if
																			// the
																			// down
																			// coming
																			// lift
																			// last
																			// stop
																			// is
																			// above
																			// the
																			// requested
																			// level.
						lift.setDestLevel(currFlr);
						direction = DOWN;
						notificationReq = false;
						System.out.println(currFloorAndDestination
								+ "--->RULE3--->" + lift);
					} else {
						while ((lift = getNearestAvailableLift(currFlr,
								Direction.STALL)) == null) {// find a stall lift
															// as there is no
															// lift which below
															// the level
							System.out.println("No STALL lift found");
						}
						lift.setDestLevel(destFlr); // as this is stalled lift
													// set the destination.
						notificationReq = true; // to start the lift
						System.out.println(currFloorAndDestination
								+ "--->RULE4");
					}
				}
			}
		}

		if (lift.getCurrLevel() == currFlr) {
			direction = (currFlr > destFlr) ? DOWN : UP;
			lift.setReqLevel(-1);
		} else {
			direction = (lift.getCurrLevel() > currFlr) ? DOWN : UP;
			lift.setReqLevel(currFlr);
		}

		lift.setDestLevel(destFlr);
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

			if (currDirection.getQ().keySet().contains(nextLevel)) {

				if (currDirection.getQ().get(nextLevel) == null
						&& currDirection.getQ().size() > 0) {
					currDirection.getQ().remove(nextLevel);
					// System.out.println("RULE 1");
					return (currDirection.getQ().size() == 0) ? Action.STALL
							: Action.STOP_N_MOVE;
				} else if (nextLevel > lift.getDestLevel()) {

					lift.setDestLevel(currDirection.getQ().get(nextLevel));
					currDirection.getQ().remove(nextLevel);
					// System.out.println("RULE 2");
					return applyReverseAction(lift, currDirection, UP);

				} else if (nextLevel < lift.getDestLevel()) { // change
																// direction
					lift.setDestLevel(currDirection.getQ().get(nextLevel));
					currDirection.getQ().remove(nextLevel);
					// System.out.println("RULE 3");
					return applyReverseAction(lift, currDirection, DOWN);
				}
				// System.out.println("RULE 0");
				return Action.STOP_N_MOVE;
			} else if (!currDirection.getQ().keySet().contains(nextLevel)
					&& currDirection.getQ().size() > 0) {
				// System.out.println("RULE 4");
				return Action.MOVE_FORWARD;
			} else {
				// System.out.println("RULE 5");
				return Action.STALL;
			}
		}
	}

	/**
	 * 
	 * @param lift
	 * @param currDirection
	 * @return
	 */
	private Action applyReverseAction(Lift lift, Direction currDirection,
			Direction toDirection) {
		if (currDirection == toDirection) {
			Direction reverse = currDirection.reverse();
			reverse.getQ().put(lift.getDestLevel(), null); // to stop the dest
															// location;
			lift.setDirection(reverse);
			return Action.MOVE_REVERSE;
		} else {
			currDirection.getQ().put(lift.getDestLevel(), null); // to stop the
																	// dest
																	// location;
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
			int currLevel = nearestLift.getNearestLevel();

			for (Lift lift : l) {

				int nearestLevel = lift.getNearestLevel();
				// System.out.println(level+"-"+lift.getCurrLevel()+"("+Math.abs(level-lift.getCurrLevel())+") < ("+Math.abs(level-currLevel)+") "+level+"-"+currLevel);
				if (Math.abs(level - nearestLevel) < Math
						.abs(level - currLevel)) {
					currLevel = nearestLevel;
					nearestLift = lift;
				}
			}
		}
		//System.out.println("$$$"+nearestLift);
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
		
		//System.out.println("###"+avlift);
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
		//System.out.println("@@@"+l);
		return l;
	}

	/**
	 * 
	 */
	public void printLevels() {
		System.out.println(lifts);
	}

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		LiftController lc = LiftController.getInstance();
		lc.setup(new int[] { 14, 6, 9, 3, 11, 1 });

		ExecutorService service1 = Executors.newCachedThreadPool();
		service1.submit(new LiftRequestor());
		service1.shutdown();
	}

}