package ctc5.main;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.process.ImageConverter;

public class test implements PlugIn
{
	int fixed_width = 9109;
	int fixed_height = 9156;
	int moving_width = 12708;
	int moving_height = 12465;
	
	double fixed_area;
	double moving_area;
	
	double temp_value;
	
	double scaling_factor;
	
	public void run(String arg0)
	{
		IJ.log("Test Has Started");
		
		fixed_area = fixed_width*fixed_height;
		moving_area = moving_width*moving_height;
		
		//temp_value = fixed_area/moving_area;
		
		scaling_factor = Math.sqrt(fixed_area/moving_area);
		
		IJ.log("fixed_area = "+fixed_area);
		IJ.log("moving_area = "+moving_area);
		IJ.log("temp_value = "+temp_value);
		IJ.log("scaling_factor = "+scaling_factor);
		
		testThresholding();
	}	 
	
	public void testThresholding()
	{
		ImagePlus imp_1 = new ImagePlus("C:\\test folder\\MCF7-1\\Raw Fluorescence\\Stitched\\BF1_ROI.tif");
		ImagePlus imp_2 = new ImagePlus("C:\\test folder\\MCF7-1\\Raw Fluorescence\\Stitched\\BF2_ROI.tif");
		
		ImagePlus[] imps = {imp_1, imp_2};
		
		for(int i = 0; i < 2; i++)
		{
			imps[i].show();
			IJ.showMessage("Should be able to see the image");
			
			new ImageConverter(imps[i]).convertToGray8();
			IJ.showMessage("image should be greyscale");
			
			IJ.run(imps[i], "Despeckle", "");
			IJ.showMessage("image should be despeckled");
			
			IJ.run(imps[i], "Auto Threshold", "method=Moments ignore_black ignore_white white");
			IJ.showMessage("Pores only should be visible.");
		}
	}
}
