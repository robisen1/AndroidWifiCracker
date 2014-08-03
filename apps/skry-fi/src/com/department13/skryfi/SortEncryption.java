package com.department13.skryfi;

public class SortEncryption  extends SortingNetworkObject
{

	public int compare(Network arg0, Network arg1) 
	{
		if((arg0 != null)&(arg1 != null))
		{
			return arg0.getLastSurvey().security.compareToIgnoreCase(arg1.getLastSurvey().security);
		}
		return 0;
	}

}
