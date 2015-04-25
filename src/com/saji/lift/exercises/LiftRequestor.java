package com.saji.lift.exercises;

import java.util.Scanner;

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

		while (true) {
			System.out.println("Request Lift [Current Floor, Destination Floor] :>");
			String input = keyboard.nextLine();
			System.out.println("Requested ["+input+"]");
			lc.sendRequest(input);
		}
	}


}