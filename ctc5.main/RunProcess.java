package ctc5.main;

import ij.IJ;
import ij.ImagePlus;

public class RunProcess implements Runnable{
	
	private final ImagePlus imageID;
	private final String 	processID;
	private final String 	processArg;
	
	public RunProcess(ImagePlus i, String a, String b)
	{
		imageID		= i; 	
		processID 	= a;
		processArg 	= b;
	}
	
	@Override
	public void run() 
	{
		IJ.run(imageID, processID, processArg);
	}

}