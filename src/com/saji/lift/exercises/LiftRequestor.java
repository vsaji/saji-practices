package com.saji.lift.exercises;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * 
 * A simulator to send requests to LiftController Class 
 * 
 * @author Saji Venugopalan
 *
 */
public class LiftRequestor implements Runnable {

	
	private final LiftController lc = LiftController.getInstance();

	/**
	 * 
	 * @param lc
	 */
	public LiftRequestor() {
	}

	@Override
	public void run() {

		Scanner keyboard = new Scanner(System.in);
		String input = null;
		//String[] args = { "2,8", "14,7", "1,7", "5,15", "9,4" };
		//String[] args = { "1,10", "8,5", "3,15", "15,6", "7,1" };
		//String[] args = { "1,10", "6,9", "15,1", "9,6", "4,2" };
		String[] args = { "2,4", "12,5", "2,10", "13,9","1,10", "6,9", "15,1", "9,6", "4,2" };

		while (true) {
			System.out
					.println("Request Lift- FORMAT [current flr#,destination flr#]");

			for (int j = 0; j < args.length; j++) {

				input = args[j];// keyboard.nextLine();

				if (input != null && !input.trim().equals("")) {
					System.out.println("Requested [" + input + "]");
					lc.sendRequest(input);
				}

				try {
					TimeUnit.SECONDS.sleep(3);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			input = keyboard.nextLine();
		}

	}

}