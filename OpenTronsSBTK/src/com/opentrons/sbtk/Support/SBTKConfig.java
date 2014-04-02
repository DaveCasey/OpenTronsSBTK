package com.opentrons.sbtk.Support;

import java.util.HashSet;
import java.util.Set;

public class SBTKConfig 
{
	private static final Set<SBTKType> sysVals = new HashSet<SBTKType>();
	
	public SBTKConfig()
	{	
		sysVals.add(new SBTKType("therm", "float")); 
		sysVals.add(new SBTKType("fdel", "float"));
	}
	
	public Set<SBTKType> getSys()
	{
		return sysVals;
	}
	
	public class SBTKType
	{
		public String name;
		public String type;
		
		private SBTKType(String name, String type)
		{
			this.name = name;
			this.type = type;
		}
	}
}
