package com.saji.lift.exercises;

import java.util.ArrayList;
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
	private final ArrayList<String> args;

	/**
	 * 
	 * @param lc
	 */
	public LiftRequestor(ArrayList<String> args) {
		this.args = args;
	}

	@Override
	public void run() {

		try {
			for (String input : args) {

				if (input != null && !input.trim().equals("")) {

					System.out.println("Requested [" + input + "]");
					lc.sendRequest(input);

				}

				TimeUnit.SECONDS.sleep(3);

			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
///********************************************************************		
		Scanner keyboard = new Scanner(System.in);
		while (true) {
			String command = keyboard.nextLine();
			if ("q".equalsIgnoreCase(command)) {
				System.exit(0);
			} else {
				System.err.println("Unknown command [" + command + "]");
			}
		}

	}

}
