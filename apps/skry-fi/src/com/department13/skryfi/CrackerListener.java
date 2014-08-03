package com.department13.skryfi;

public interface CrackerListener {
	/** called when there is a cracker state change */
	public void crack_progress(Cracker cracker);
	public void crack_complete(Cracker cracker);
	 
}
