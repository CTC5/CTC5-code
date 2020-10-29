package ctc5.main;

import ij.IJ;

public class RunPlugin implements Runnable{
	
	private final String pluginID;
	private final String pluginArg;
	
	public RunPlugin(String a, String b)
	{
		pluginID = a;
		pluginArg = b;
	}
	
	@Override
	public void run() 
	{
		IJ.run(pluginID, pluginArg);
	}

}