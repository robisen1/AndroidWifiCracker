package com.department13.skryfi;

public class SortName extends SortingNetworkObject
{

	public int compare(Network arg0, Network arg1) {
		// TODO Auto-generated method stub
		
		if((arg0.name != null) || (arg1.name !=null) )
		{
			return arg0.name.compareTo(arg1.name);
		}
		else if(arg0.name != null)
		{
			return 0;
		}
		else
		{
			return 1;
		}
	}

}
