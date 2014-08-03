package com.department13.skryfi;


public class SortSignalStrength extends SortingNetworkObject
{

	public int compare(Network arg0, Network arg1) 
	{
		if((arg0!=null)&&(arg1!=null))
		{
			return ((Integer)arg0.last_survey.level).compareTo(((Integer)arg1.last_survey.level));
		}
		return 0;
	}

}
