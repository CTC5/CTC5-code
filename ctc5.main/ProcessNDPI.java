package ctc5.main;

import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import edfgui.ExtendedDepthOfField;
import edfgui.Parameters;
import fr.in2p3.imnc.ndpitools.NDPIToolsPreviewPlugin;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.OvalRoi;
import ij.gui.Roi;

/**
 * Automates the extraction, focusing and stitching of tif data from NDPIfiles. This is dependent on the three ImageJ Plugins: 1) NDPItools, 2)ExtendDeptOfField, 3) Grid/Collection Stitching.
 * 
 * @author  Brendan Ffrench: Royal College of Surgeons in Ireland.
 * @date    10 August 2017.
 * @version 2.0.0
 */
public class ProcessNDPI {
	
	private File directory;
	
	public ProcessNDPI(File f){
		directory = f;
	}

	public void extractFromNDPI(String as, Roi r, double totalFiles, double currFile, String[] focalPlanes, int mag) throws InterruptedException
	{
		if(currFile == 0){
			CTC5Analysis.graphic_progress_update_frame.extractNDPI.UpdateProgress(1, as);
		}
		else
		{
			CTC5Analysis.graphic_progress_update_frame.extractNDPI.UpdateProgress(((currFile/totalFiles)*100)+1, as);
		}
		
		new Thread(new Runnable(){
			public void run(){	
				CTC5Analysis.graphic_progress_update_frame.extractNDPI.repaint();
			}
		}).start();
				
	    ImagePlus im = new ImagePlus(); //will be used to hold the currently opened image.
	    String removeExt = as.substring(0, as.lastIndexOf("."));
	    String arg;
	    
	    ArrayList<String> allPlanesAsString;
	    String[] focus;
	    Double[] allPlanesAsDouble;
	    int[] allPlanesAsInt;
	    ArrayList<Integer> noDuplicates;
	    
	    if(!isWindows(System.getProperty("os.name").toLowerCase()))
	    {
	    	IJ.open(directory.getPath()+"\\"+removeExt+"\\Raw Giemsa\\"+as);
	    	IJ.run("Preview NDPI...", "ndpitools=["+directory.getPath()+"\\"+removeExt+"\\Raw Giemsa\\"+as+"]");
	    }
	    else
	    {
	    	NDPIToolsPreviewPlugin CTC5ndpiPreview = new NDPIToolsPreviewPlugin();
	    	CTC5ndpiPreview.run(directory.getPath()+"\\"+removeExt+"\\Raw Giemsa\\"+as);
	    }
	    
	    im = WindowManager.getCurrentImage();
	    
	    int x = r.getBounds().x;
	    int y = r.getBounds().y;
	    int w = r.getBounds().width;
	    int h = r.getBounds().height;
	      
	    OvalRoi newROI = new OvalRoi(x-((w*0.05)/2), y-((h*0.05)/2), w+((w*0.05)), h+((h*0.05)));
	      
	    im.setRoi(newROI);
	   
	    String dims = (String)im.getProperty("PreviewOfNDPIAvailableDimensions");
	    
	    long[] mosaicDims = calculateMosaicDims(dims);
	    
	    
	    IJ.log("mosaicDims = "+mosaicDims[0]+" x "+mosaicDims[1]);
	    
	    int[] previewDims = im.getDimensions();
	    
	    Rectangle roiRect = newROI.getBounds();
	    double[] roiRatio = {((double)roiRect.width)/((double)previewDims[0]), ((double)roiRect.height)/((double)previewDims[1])}; 
	    double mosaicWidth = mosaicDims[0]*roiRatio[0]*0.5*1.05; //making four quadrants that overlap by 5 %.
		double mosaicHeight = mosaicDims[1]*roiRatio[1]*0.5*1.05;
		int widthpx = (int) mosaicWidth;
		int heightpx = (int) mosaicHeight;
		    
		arg = "label="+removeExt+" format_of_split_images=[TIFF with LZW compression] make_mosaic=[always] mosaic_pieces_format=[TIFF with LZW compression] requested_jpeg_compression=100 mosaic_pieces_overlap=5.000000 mosaic_pieces_overlap_unit=% size_limit_on_each_mosaic_piece=0 width_of_each_mosaic_piece_in_pixels="+widthpx+" height_of_each_mosaic_piece_in_pixels="+heightpx+" extract_images_at_magnification_"+mag+"x";
		
	    allPlanesAsString = new ArrayList<String>();
	      
	    for (int k = 0; k < 4; k++) 
	    {
	      focus = focalPlanes[k].split(",");
	        
	      for (int l = 0; l < focus.length; l++) 
	      {
	        focus[l] = focus[l].replaceAll("\\s+", "");
	        allPlanesAsString.add(focus[l]); //builds a list of required focal planes for extraction.
	      }
	    }
	      
	    allPlanesAsDouble = new Double[allPlanesAsString.size()];
	    allPlanesAsInt = new int[allPlanesAsString.size()];
	      
	    for (int k = 0; k < allPlanesAsString.size(); k++) 
	    {
	       allPlanesAsDouble[k] = Double.valueOf(Double.parseDouble((String)allPlanesAsString.get(k)));
	       allPlanesAsInt[k] = allPlanesAsDouble[k].intValue();
	    }
	      
	    noDuplicates = removeDuplicates(allPlanesAsInt);
	      
	    for (int j = 0; j < noDuplicates.size(); j++) 
	    {
	       arg = arg + " extract_images_with_z-offset_" + ((Integer)noDuplicates.get(j)).intValue(); //removes all duplicates from the same NDPI file each focal plane only needs to be extracted once.																					 
	    }
	   
	    IJ.log("NDPITools arg = "+arg);
	    
	    RunPlugin extractToTiff = new RunPlugin("Custom extract to TIFF / Mosaic...", arg);
	    Thread tiffExtractThread =  new Thread(extractToTiff);
	    tiffExtractThread.start();
	    IJ.log("Extracting Images from NDPI.");
	    tiffExtractThread.join();
	    IJ.log("NDPI Extraction completed.");
	    
	    im.close();
	   
	    im = null;
	    removeExt = null;
	    arg = null;
	    allPlanesAsString = null;
	    focus = null;
	    allPlanesAsDouble = null;
	    allPlanesAsInt = null;
	    noDuplicates = null;
	    System.gc();
	        
		if((currFile+1) >= totalFiles)
		{
			CTC5Analysis.graphic_progress_update_frame.extractNDPI.UpdateProgress(100, as);
		}
		else
		{
			CTC5Analysis.graphic_progress_update_frame.extractNDPI.UpdateProgress(((currFile+1)/totalFiles)*100, as);
		}
			
	    new Thread(new Runnable(){
			public void run(){	
				CTC5Analysis.graphic_progress_update_frame.extractNDPI.repaint();
			}
		}).start();
	}
	
	public ArrayList<Integer> removeDuplicates(int[] raw)
	{
		ArrayList<Integer> noDups = new ArrayList<Integer>();
		HashSet<Integer> testerSet = new HashSet<Integer>();
  
		for (int i = 0; i < raw.length; i++)
		{
			Integer iToInt = new Integer(raw[i]);
    
			if (!testerSet.contains(iToInt)) {
				noDups.add(iToInt);
				testerSet.add(iToInt);
			}
		}
  
		testerSet = null;
		return noDups;
	}

	public long[] calculateMosaicDims(String s){
		
		IJ.log("##########  updated  ############");
		IJ.log("calculateMosaicDims(String s)");
		IJ.log("######################");
		
		IJ.log("String s = "+s);
		
		
		
		long[] widthAndHeight = new long[2];
		String[] heightByWidth = s.split(",");
		long[] area = new long[heightByWidth.length];
		String[][] temp = new String[heightByWidth.length][2];
		
		for(int i =0;i<heightByWidth.length;i++)
		{
			temp[i] = heightByWidth[i].split("x");
			IJ.log("temp[i][0] = "+temp[i][0]);
			IJ.log("temp[i][1] = "+temp[i][1]);
			area[i] = Long.parseLong(temp[i][0])*Long.parseLong(temp[i][1]);
			IJ.log("area[i] = "+area[i]);
		}
		
		int indexOfMax = getIndexOfMaxValInArray(area);
		IJ.log("indexOfMax = "+indexOfMax);
		
		widthAndHeight[0] = Integer.parseInt(temp[indexOfMax][0]);
		IJ.log("widthAndHeight[0] =  "+widthAndHeight[0]);
		widthAndHeight[1] = Integer.parseInt(temp[indexOfMax][1]);
		IJ.log("widthAndHeight[1] = "+widthAndHeight[1]);
		
		IJ.log("### returning widthAndHeight ###");
		return widthAndHeight;
	}
	
	public int getIndexOfMaxValInArray(long[] array){
		int index = 0;
		long temp = array[0];
		IJ.log("### getIndexOfMaxValInArray(int[] array) ###");
		IJ.log("temp = "+temp);
		
		for(int i =0;i<array.length;i++)
		{
			if(array[i]>temp){
				temp = array[i];
				index=i;
			}
		}
		IJ.log("returning index = "+index);
		return index;
	}

	public void focusNDPI(String as, String[] fp, double totalFiles, double currFile, int mag, Roi r) throws InterruptedException
	{
		if(currFile == 0)
		{
			CTC5Analysis.graphic_progress_update_frame.focusNDPI.UpdateProgress(1, as);
		}
		else
		{
			CTC5Analysis.graphic_progress_update_frame.focusNDPI.UpdateProgress(((currFile/totalFiles)*100)+1, as);
		}
		
		new Thread(new Runnable(){
			public void run(){	
				CTC5Analysis.graphic_progress_update_frame.focusNDPI.repaint();
			}
		}).start();

	    IJ.log("FocusGiemsa "+(currFile+1)+" has started");
	    
	    String maxMagNoX = ""+mag;
	    String removeExt = null;
	    String quadCode = null;
	    String[] quadrantPlanes = null;
	    Double planeDouble = null;
	    
	    removeExt = as.substring(0, as.lastIndexOf(".")); //gets the current file name.
	    quadCode = ""; //primes the quad code variable.
	      
	    for (int k = 0; k < 4; k++) 
	    {
	        quadrantPlanes = fp[k].split(","); // splits each user entered plane into the appropriate number of strings.
	        
	        for (int l = 0; l < quadrantPlanes.length; l++) 
	        {
	        	
	          quadrantPlanes[l] = quadrantPlanes[l].replaceAll("\\s+", ""); //removes white spaces
	          planeDouble = Double.valueOf(Double.parseDouble(quadrantPlanes[l]));
	          int planeInt = planeDouble.intValue();
	          
	          if(!(r==null))
	          {
	        	  	if (k == 0) 
		          	{
		        	  quadCode = "i1j1";
		            	IJ.open(directory.getPath() + "\\" + removeExt + "\\Raw Giemsa\\" + removeExt + "_x" + maxMagNoX + "_z" + planeInt + "_" + removeExt + "_" + quadCode + ".tif");
		          	} 
		          	else if (k == 1) 
		          	{
		        	  quadCode = "i1j2";
		            	IJ.open(directory.getPath() + "\\" + removeExt + "\\Raw Giemsa\\" + removeExt + "_x" + maxMagNoX + "_z" + planeInt + "_" + removeExt + "_" + quadCode + ".tif");
		          	} 
		          	else if (k == 2) 
		          	{
		        	  quadCode = "i2j1";
		            	IJ.open(directory.getPath() + "\\" + removeExt + "\\Raw Giemsa\\" + removeExt + "_x" + maxMagNoX + "_z" + planeInt + "_" + removeExt + "_" + quadCode + ".tif");
		          	} 
		          	else if (k == 3) 
		          	{
		        	  quadCode = "i2j2";
		            	IJ.open(directory.getPath() + "\\" + removeExt + "\\Raw Giemsa\\" + removeExt + "_x" + maxMagNoX + "_z" + planeInt + "_" + removeExt + "_" + quadCode + ".tif");
		          	} 
		          	else 
		          	{
		        	  IJ.log("Unexpected error: more than 4 quadrants.");
		          	} 
	          } 
	          else
	          {
	        	  if (k == 0) 
	          	  {
	        		  quadCode = "i1j1";
	        		  IJ.open(directory.getPath() + "\\" + removeExt + "\\Raw Giemsa\\" + removeExt + "_x" + maxMagNoX + "_z" + planeInt + "_" + quadCode + ".tif");
	          	  } 
	          	  else if (k == 1) 
	          	  {
	          		  quadCode = "i1j2";
	          		  IJ.open(directory.getPath() + "\\" + removeExt + "\\Raw Giemsa\\" + removeExt + "_x" + maxMagNoX + "_z" + planeInt + "_" + quadCode + ".tif");
	          	  } 
	          	  else if (k == 2) 
	          	  {
	          		  quadCode = "i2j1";
	          		  IJ.open(directory.getPath() + "\\" + removeExt + "\\Raw Giemsa\\" + removeExt + "_x" + maxMagNoX + "_z" + planeInt + "_" + quadCode + ".tif");
	          	  } 
	          	  else if (k == 3) 
	          	  {
	          		  quadCode = "i2j2";
	          		  IJ.open(directory.getPath() + "\\" + removeExt + "\\Raw Giemsa\\" + removeExt + "_x" + maxMagNoX + "_z" + planeInt + "_" + quadCode + ".tif");
	          	  } 
	          	  else 
	          	  {
	          		  IJ.log("Unexpected error: more than 4 quadrants.");
	          	  }
	          }
	        }
	        
	          if (quadrantPlanes.length > 1) 
	          {
	        	IJ.run("Images to Stack");
	        	Boolean zetreneStackerInstalled = false; 
	          
	        	if(zetreneStackerInstalled)
	        	{
	        		// place holder for potential method to automate the selection and focusing of the focal planes. 
	        	}else
	        	{		
	        		IJ.log("Preping pameters for file: " +as+ "; quadrant #"+(k+1));
	        		
	        		Parameters EDFpara = new Parameters();
	        		EDFpara.color = true;
	        		EDFpara.showTopology = false;
	        		EDFpara.show3dView = false;
	        		EDFpara.outputColorMap = Parameters.COLOR_RGB;
	        		EDFpara.colorConversionMethod = 0;
	        		EDFpara.setQualitySettings(1);
	        		EDFpara.setTopologySettings(0);
	        		
	        		ImagePlus imp = WindowManager.getCurrentImage();
	        		
	        		ExtendedDepthOfField edf = new ExtendedDepthOfField(imp, EDFpara);
	        		edf.process();
	        	   		
	        		IJ.wait(1000);
	        	  	IJ.selectWindow("Output");
	        	}
	           }
	        
	        if (quadCode.equalsIgnoreCase("i1j1")) 
	        {
	          IJ.saveAs("Tiff", directory.getPath() + "\\" + removeExt + "\\Raw Giemsa\\Focused\\Focused_00");
	          
	          //Update the progressPane
	          double range = ((currFile+1)/totalFiles)-((currFile)/totalFiles);
	          IJ.log("range = "+range);
	          IJ.log("progress = "+(((currFile)/totalFiles)+(range/4))*100);
	          
	          CTC5Analysis.graphic_progress_update_frame.focusNDPI.UpdateProgress((((currFile)/totalFiles)+(range/4))*100, as); 
	          
	          new Thread(new Runnable(){
	   		   public void run(){	
	   			   CTC5Analysis.graphic_progress_update_frame.focusNDPI.repaint();
	   			}
	   	   	  }).start();
	        } 
	        else if (quadCode.equalsIgnoreCase("i1j2")) 
	        {
	          IJ.saveAs("Tiff", directory.getPath() + "\\" + removeExt + "\\Raw Giemsa\\Focused\\Focused_01");
	          
	          //Update the progressPane
	          double range = ((currFile+1)/totalFiles)-((currFile)/totalFiles);
	          IJ.log("range = "+range);
	          IJ.log("progress = "+(((currFile)/totalFiles)+(2*(range/4)))*100);
	          
	          CTC5Analysis.graphic_progress_update_frame.focusNDPI.UpdateProgress((((currFile)/totalFiles)+(2*(range/4)))*100, as); 
	          
	          new Thread(new Runnable(){
	   		   public void run(){	
	   			   CTC5Analysis.graphic_progress_update_frame.focusNDPI.repaint();
	   			}
	   	   	  }).start();
	        } 
	        else if (quadCode.equalsIgnoreCase("i2j1")) 
	        {
	          IJ.saveAs("Tiff", directory.getPath() + "\\" + removeExt + "\\Raw Giemsa\\Focused\\Focused_02");
	          
	          //Update the progressPane
	          double range = ((currFile+1)/totalFiles)-((currFile)/totalFiles);
	          IJ.log("range = "+range);
	          IJ.log("progress = "+(((currFile)/totalFiles)+(3*(range/4)))*100);
	          
	          CTC5Analysis.graphic_progress_update_frame.focusNDPI.UpdateProgress((((currFile)/totalFiles)+(3*(range/4)))*100, as); 
	          
	          new Thread(new Runnable(){
	   		   public void run(){	
	   			   CTC5Analysis.graphic_progress_update_frame.focusNDPI.repaint();
	   			}
	   	   	  }).start();  
	        } 
	        else if (quadCode.equalsIgnoreCase("i2j2")) 
	        {
	          IJ.saveAs("Tiff", directory.getPath() + "\\" + removeExt + "\\Raw Giemsa\\Focused\\Focused_03");
	        }
	        
	        CTC5Analysis.closeAllImages(); //The above few lines saves and closes all the focused quadrants making them available for stitching next.
	    } 
	    maxMagNoX = null;
	    removeExt = null;
	    quadCode = null;
	    quadrantPlanes = null;
	    planeDouble = null;
	    System.gc();
	    
	    if((currFile+1) >= totalFiles)
		{
			CTC5Analysis.graphic_progress_update_frame.focusNDPI.UpdateProgress(100, as);
		}
		else
		{
			CTC5Analysis.graphic_progress_update_frame.focusNDPI.UpdateProgress(((currFile+1)/totalFiles)*100, as);
		}
			
	   new Thread(new Runnable(){
		   public void run(){	
			CTC5Analysis.graphic_progress_update_frame.focusNDPI.repaint();
			}
	   }).start();
	}

	public void stitchNDPI(String as, double totalFiles, double currFile){
		
		if(currFile == 0)
		{
			CTC5Analysis.graphic_progress_update_frame.stitchNDPI.UpdateProgress(1, as);
		}
		else
		{
			CTC5Analysis.graphic_progress_update_frame.stitchNDPI.UpdateProgress(((currFile/totalFiles)*100)+1, as);
		}
		
		new Thread(new Runnable(){
			public void run(){	
				CTC5Analysis.graphic_progress_update_frame.stitchNDPI.repaint();
			}
		}).start();
		
		String removeExt = null;
		String targetPath = null;
		
		removeExt = as.substring(0, as.lastIndexOf("."));
	    targetPath = directory.getPath() + "\\" + removeExt + "\\Raw Giemsa\\Focused";
	      
	    //This seems OK, as the CTC-5 program sets the order. Therefore, it is safe to make these assumptions.
	    //might use the "use virtual memory" option it may get around some of the problems.
	    //down sampling.
	    IJ.run("Grid/Collection stitching", "type=[Grid: row-by-row] order=[Right & Down                ] grid_size_x=2 grid_size_y=2 tile_overlap=5 first_file_index_i=0 initial_NDPI_directory=[" + targetPath + "] file_names=Focused_{ii}.tif output_textfile_name=TileConfiguration.txt fusion_method=[Linear Blending] regression_threshold=0.30 max/avg_displacement_threshold=2.50 absolute_displacement_threshold=3.50 compute_overlap subpixel_accuracy computation_parameters=[Save memory (but be slower)] image_output=[Fuse and display]");
	    IJ.run("RGB Color");
	    
	    IJ.saveAs("Tiff", directory.getPath() + "\\" + removeExt + "\\Final Processed\\" + removeExt + "-Giemsa");
	      
	    CTC5Analysis.closeAllImages();
	    System.gc();
		
		if((currFile+1) >= totalFiles)
		{
			CTC5Analysis.graphic_progress_update_frame.stitchNDPI.UpdateProgress(100, as);
		}
		else
		{
			CTC5Analysis.graphic_progress_update_frame.stitchNDPI.UpdateProgress(((currFile+1)/totalFiles)*100, as);
		}
			
	   new Thread(new Runnable(){
		   public void run(){	
			CTC5Analysis.graphic_progress_update_frame.stitchNDPI.repaint();
			}
	   }).start();
		
	}
	
	public static boolean isWindows(String s) 
	{
		IJ.log("OS name: "+s);
		return (s.indexOf("win") >= 0);
	}
}