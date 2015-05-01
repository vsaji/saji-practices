/**
 * 
 */
package com.saji.lift.exercises;

import static com.saji.lift.exercises.Direction.DOWN;
import static com.saji.lift.exercises.Direction.UP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * LiftController class has the following responsibility
 * --> Receive concurrent user request 
 * --> assign the nearest and same direction traveling lift to the request
 * --> wait-list the user request if the lifts are not available
 * --> receive lift notification and instruct the next action/direction
 * 
 * @author Saji Venugopalan
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
	 * Singleton Method.
	 * @return
	 */
	public static LiftController getInstance() {
		return instance;
	}

	/**
	 * 
	 * Initializer method to setup and start the LiftController and LiftLocator.
	 * 
	 * @param liftLevels - levels where lifts are currently halted
	 */
	@SuppressWarnings("unchecked")
	public void setup(int[] liftLevels) throws ProcessingException{

		if (!initialized) {
			
			validateLevels(liftLevels);
			
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
		}else{
			throw new ProcessingException("Lift[s] have been already intialized.");
		}
	}


	/**
	 * Checks if the requested lift levels are within the defined range.
	 * @param liftLevels
	 * @throws ProcessingException
	 */
	private void validateLevels(int[] liftLevels) throws ProcessingException {
		
		for (int i = 0; i<liftLevels.length ; i++) {
			if(liftLevels[i] > Lift.MAX_FLOOR || liftLevels[i] < Lift.MIN_FLOOR){
				throw new ProcessingException("Lift Level ["+liftLevels[i]+"] is > MAX Floor ["+Lift.MAX_FLOOR+"] or < MIN Floor ["+Lift.MIN_FLOOR+"] ");
			}
		}
	}
	

	/**
	 * This method is kernel of the LiftController. 
	 * It receive elevator request from LiftRequestor and evaluate the direction 
	 * and locate the appropriate elevator for the requester.
	 * If no elevator is found, it wait-list the request for future processing.
	 * 
	 * @see LiftRequestor#run()
	 * @param reqLevelAndDestination - format currentLevel,Distination Level
	 */
	public synchronized void sendRequest(String reqLevelAndDestination) {

		String[] LvlDest = reqLevelAndDestination.split("[,]");
		int reqLvl = Integer.parseInt(LvlDest[0]);
		int destLvl = Integer.parseInt(LvlDest[1]);

		try {
			validateLevels(new int[] {reqLvl,destLvl});
		} catch (ProcessingException e) {
			e.printStackTrace();
			sop("Discarding request ["+reqLevelAndDestination+"]");
		}
		
		Direction direction = null;
		Direction secDirection = null;
		Lift lift = null;
		boolean notificationReq = true;

		if (isAllLiftStall()) {
			
			lift = getNearestAvailableLift(reqLvl, Direction.STALL);
			direction = (lift.getCurrLevel() > reqLvl) ? DOWN : UP;

			
		} else {

			direction = (reqLvl > destLvl) ? DOWN : UP;
			lift = getNearestAvailableLift(reqLvl, direction);
			lift = (lift==null) ? getNearestAvailableLift(reqLvl, Direction.STALL) : lift;
			
			if(lift==null){
				
				waitList(new int[]{reqLvl,destLvl});
				return;
			}
			
		
			if (lift.getDirection() == Direction.STALL) {// ----------------------------------------------------------

				if (direction == DOWN && lift.getCurrLevel() < reqLvl) { direction = UP;secDirection=DOWN;}
				if (direction == UP && lift.getCurrLevel() > reqLvl) { direction = DOWN;secDirection=UP;}
				notificationReq = true;
		
			} else if (direction == DOWN) {// -----------------------------------------------------------------------

				if (lift.getCurrLevel() > reqLvl) {
					notificationReq = false;
				
				} else {
					
					lift = getNearestAvailableLift(reqLvl, UP); //

						if (lift != null && lift.getDestLevel() < reqLvl && isWithinTheAcceptableDistance(lift.getCurrLevel(),reqLvl)) {
							lift.addStop(reqLvl);
							direction = UP;
							notificationReq = false;
						} else {
							
							waitList(new int[]{reqLvl,destLvl});
							
							return;
						}
				}
			}else {// -----------------------------------------------------------------------------------------------


				if (lift.getCurrLevel() < reqLvl) { 

					notificationReq = false; 
				
				} else { 

					lift = getNearestAvailableLift(reqLvl, DOWN); //

					if (lift != null && lift.getDestLevel() > reqLvl && isWithinTheAcceptableDistance(lift.getCurrLevel(),reqLvl)) { 

						lift.addStop(reqLvl);
						direction = DOWN;
						notificationReq = false;
						
					} else {
						waitList(new int[]{reqLvl,destLvl});
						return;
					}
				}
			}
		}

		if (lift.getCurrLevel() == reqLvl) {
			direction = (reqLvl > destLvl) ? DOWN : UP;
			lift.setReqLevel(-1);
		} else {
			lift.setReqLevel(reqLvl);
		}

		lift.addStop(destLvl);
		lift.setDirection(direction);
		lift.setSecDirection(secDirection==null ? direction : secDirection);
		direction.getQ().add(reqLvl);
		
		if (notificationReq) {
			liftNotifierQ[lift.getLiftNum()].offer(reqLvl);
		}
	}

	
	/**
	 * 
	 * Directs the request to waiting Q.
	 * 
	 * @see LiftLocator#run()
	 * @param Lvls - [currentLevel,destination Level]
	 * 
	 */
	private void waitList(int[] Lvls){
		
		sop("All lifts are busy. Adding ["+Lvls[0]+"],["+Lvls[1]+"] into waiting list.");
		waitingReqQ.offer(Lvls);
	}
	
	
	/**
	 * Checks if the given elevator is within the acceptable distance.
	 * 
	 * @see Lift#ACCEPTABLE_DISTANCE
	 * @param liftLevel
	 * @param reqLevel
	 * @return true if the list is within the acceptable distance
	 */
	private boolean isWithinTheAcceptableDistance(int liftLevel,int reqLevel){
		return (liftLevel-reqLevel) >= Lift.ACCEPTABLE_DISTANCE;
	}
	
	
	/**
	 * 
	 * This method will be called by all elevators at each level.
	 * This method will determine 
	 * --> if any passenger is waiting for the current direction elevator  
	 * --> if any passenger need to get down.
	 * 
	 *  
	 * @see LiftRunner#checkLevel
	 * @return Action 
	 */
	public Action adviceAction(Lift lift, Direction currDirection) {
		int nextLevel = lift.getCurrLevel();

		synchronized (currDirection.getQ()) {

			if (currDirection.getQ().contains(nextLevel)) {//check if anyone called lift for this level [PICK UP]

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
	 * Checks if the elevator need to travel to the opposit direction.
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
	 * Helper method to locate the nearest elevator based on their direction.
	 * Also ensures the elevator is within the acceptable distance.
	 * 
	 * @param level
	 * @return Lift
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

			if( d!=Direction.STALL && isWithinTheAcceptableDistance(nearestLift.getCurrLevel(),level)){
				nearestLift = null;
			}
		}
					
		return nearestLift;
	}

	
	
	
	/**
	 * Retunrs the first available elevator for the given direction.
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
	 * Returns the collection of elevators traveling to the given direction.
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
	 * Checks if all elevators are in stopped state.
	 * 
	 * @return
	 */
	private boolean isAllLiftStall(){
		return lifts.size()==getLiftsByDirection(Direction.STALL).size(); 
	}
	


	/**
	 * This class is a consumer class of waitingQ BlockingQueue
	 * LiftController#sendRequest method will publish waiting request to waitingQ.
	 * The run method will consume and locate the STALL lift and assign to it.
	 * 
	 * {@link LiftController#sendRequest(String)}
	 * {@link LiftRunner#run()}
	 * 
	 *  @author Saji Venugopalan
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
					int[] Lvls = waitReqQ.take();
					
					Lift lift = null;
				    
					while ((lift = getNearestAvailableLift(Lvls[0],	Direction.STALL)) == null) {
						TimeUnit.MILLISECONDS.sleep(1000);
				    }
				    	
			    	Direction direction = (Lvls[0] > Lvls[1]) ? DOWN : UP;
			    	Direction secDirection = direction;
			    	
			    	if(direction==DOWN){
			    		if(lift.getCurrLevel() < Lvls[0]){direction = UP; secDirection=DOWN;}
			    	}else{
			    		if(lift.getCurrLevel() > Lvls[0]){direction = DOWN;	secDirection=UP;}
			    	}
				    	
					if (lift.getCurrLevel() == Lvls[0]) { lift.setReqLevel(-1);	}
					else {lift.setReqLevel(Lvls[0]);}

					lift.addStop(Lvls[1]);
					lift.setDirection(direction);
					lift.setSecDirection(secDirection==null ? direction : secDirection);
					direction.getQ().add(Lvls[0]);
					liftNotifierQ[lift.getLiftNum()].offer(Lvls[0]);
				    	
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	 
	}
	
	
	
	
	
	/**
	 * Main method.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		LiftController lc = LiftController.getInstance();
		try {
			
			Scanner keyboard = new Scanner(System.in);
			
			int[] levels = readLiftLevels(keyboard);			
			ArrayList<String> reqDestLevels = readReqDestLevels(keyboard);
			
			lc.setup(levels);

			ExecutorService service1 = Executors.newCachedThreadPool();
			service1.submit(new LiftRequestor(reqDestLevels));
			service1.shutdown();
			
		} catch (ProcessingException e) {
			e.printStackTrace();
			System.exit(1);
		}

	
	}
	
	
	/**
	 * To read the intial lift levels from the user
	 * 
	 * @param keyboard
	 * @return
	 */
	private static int[] readLiftLevels(Scanner keyboard){
		sop("Enter the current stages of the lifts: MAX floor["+Lift.MAX_FLOOR+"] and MIN floor["+Lift.MIN_FLOOR+"]. Format[12,5,3..]:>");
		String temp = keyboard.nextLine();
		String[] lvls = temp.split("[,]");
		int[] levels = new int[lvls.length];
		for (int i = 0; i < lvls.length; i++) {
			levels[i] = Integer.parseInt(lvls[i]);
		}
		return levels;
	}
	
	
	/**
	 * To read the req/destination levels from the user
	 * @param keyboard
	 * @return
	 */
	private static ArrayList<String> readReqDestLevels(Scanner keyboard){
		ArrayList<String> reqDestLevels = new ArrayList<String>();
		
		while(true){
			sop("Enter request/destination levels : Format [12,3]:>");
			String temp = keyboard.nextLine();
			if("c".equalsIgnoreCase(temp)){ break;}
			else{reqDestLevels.add(temp);}
			sop("Enter 'c' for Complete");
		}
		return reqDestLevels;
	}
	
	

	/**
	 * Utility method to print messages
	 */
	private static void sop(String msg){
		System.out.println("[CONT]["+msg+"]");
	}
	
}
