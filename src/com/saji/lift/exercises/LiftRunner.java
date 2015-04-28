/**
 * 
 */
package com.saji.lift.exercises;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 
 * This runner class is responsible to move the elevator UP and DOWN based on the instruction obtained from LiftController.
 * This class listens to liftNotifierQ to start the elevator.
 * 
 * @see LiftController#setup(int[])
 * @see LiftController#adviceAction(Lift, Direction)
 * @see LiftController.LiftLocator#run()
 *  
 * @author Saji Venugoapalan
 * 
 */
public class LiftRunner implements Runnable {

	private final Lift lift;
	private final BlockingQueue<Integer> liftNotifierQ;
	private final LiftController lc = LiftController.getInstance();

	/**
	 * 
	 * @param lift
	 * @param liftNotifierQ
	 */
	public LiftRunner(Lift lift, BlockingQueue<Integer> liftNotifierQ) {
		this.lift = lift;
		this.liftNotifierQ = liftNotifierQ;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {

		sop("[" + lift.getLiftNum() + "] Initialized.");

		while (true) {

			try {
		
				liftNotifierQ.take();

				sop("Starting Lift  [#" + lift.getLiftNum() + "]- CD["
						+ lift.getDirection() + "]- CF[" + lift.getCurrLevel()
						+ "] --> RF[" + lift.getReqLevel() + "]-->DF[" + lift.getDestLevel() + "]");

				while (lift.getDirection() != Direction.STALL) { // reverse request

					if (lift.getDirection() == Direction.DOWN) {
						for (; lift.getCurrLevel() > Lift.MIN_FLOOR-1; lift
								.levelDown()) {

							int status = checkLevel(Direction.DOWN);
							if (status == -1) {
								lift.setDirection(Direction.STALL);
								break; // got to stall;
							} else if (status == 1) {
								break; // signal for reverse direction;
							}
						}
					} else if (lift.getDirection() == Direction.UP) {
						for (; lift.getCurrLevel() < Lift.MAX_FLOOR+1; lift
								.levelUp()) {
							int status = checkLevel(Direction.UP);
							if (status == -1) {
								lift.setDirection(Direction.STALL);
								break; // got to stall
							} else if (status == 1) {
								break; // signal for reverse direction
							}
						}
					}
				}

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * Invokes {@link LiftController#adviceAction(Lift, Direction)} to get the instruction for every move
	 * @see LiftRunner#run();
	 * @param d
	 * @return 0=MOVE, 1=Move opposit direction, -1= Stop
	 * @throws InterruptedException
	 */
	private int checkLevel(Direction d) throws InterruptedException {

		int b = 0;

		Action a = lc.adviceAction(lift, d);
		if (a == Action.MOVE_FORWARD) {
			sop("No STOP request from [" + lift.getCurrLevel() + "]. Moving ["+d.name()+"]");
			TimeUnit.SECONDS.sleep(Lift.LIFT_MOVE_DELAY);
		} else if (a == Action.STOP_N_MOVE) {
			sop("Serving Level : [" + lift.getCurrLevel() + "]["+ lift.getDestLevel() + "]");
			TimeUnit.SECONDS.sleep(Lift.LIFT_STOP_DELAY);
		} else if (a == Action.MOVE_OPPOSITE) {
			sop("Moving Opposite : [" + lift.getCurrLevel() + "]["+ lift.getDestLevel() + "]");
			TimeUnit.SECONDS.sleep(Lift.LIFT_STOP_DELAY);
			b = 1;
		} else {
			sop("Dest Level : [" + lift.getCurrLevel() + "]");
			b = -1;
		}
		return b;
	}

	
	/**
	 * 
	 * @param s
	 */
	private void sop(String s) {
		System.out.println("[#"+lift.getLiftNum()+"] [" + Thread.currentThread().getName() + "] " + s);
	}

}