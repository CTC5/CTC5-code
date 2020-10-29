package ctc5.main;

import fiji.util.gui.GenericDialogPlus;
import fr.in2p3.imnc.ndpitools.NDPIToolsPreviewPlugin;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.TextField;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JTextField;

import org.apache.commons.io.FileUtils;

import ctc5.guis.FocalPlanesGUI;

/**
 * Gathers and carries out quality control on all of the data required to carry out image analysis.
 * 
 * @author  Brendan Ffrench (Royal College of Surgeons in Ireland)
 * @date    12 December 2017.
 * @version 2.1.0
 * 
 */
public class CTC5Data 
{
	public long runtimeStart; /** Holds the program start-time to enable calculation of total processing time */ 
	public long runtimeEnd; /** Holds the program end-time to enable calculation of total processing time */ 
	
	public CTC5_QC_User_Input quality_control;
	
	public File initial_NDPI_directory = null; /** Stores the directory info for the user selected NDPI files*/
	public String[] all_ndpi_filenames; /** Stores all the filenames of the NDPI files in the selected initial_NDPI_directory*/
	public ArrayList<String> selected_ndpi_files; /** Stores the filenames of those NDPI files selected by the user for processing*/
	
	public String[][] final_adjusted_stitched_images; /** this holds the path names (String) to the final scaled, rotated and translated images that have yet to have LUT applied and be merged with counterstains. */
	
	public int index_of_reference_BF_template = 0; /** this holds the index of the BF template that all images are going to be aligned to. */
	
	public ArrayList<Roi> ndpi_regions_for_extraction = null; /** Stores the user selected 'ndpi_regions_for_extraction of interest' for extraction from {@code .ndpi} to {@code .tif}.*/
	public ArrayList<String> availableZOffsets = new ArrayList<String>(); /** Stores allowed ndpi_infocus_focal_planes to control what the user is able to enter*/
	public String[][] ndpi_infocus_focal_planes; /** Stores the user inputed focal planes required for creating a single focal plane from z-stacked NDPI files.*/
	public ArrayList<Integer> ndpi_maximum_magnification_list = new ArrayList<Integer>(); /** Stores the maximumMagnification for each of the selected_ndpi_files. */
	
	public static String[] all_fluorescent_channels = {"Blue","Green","Yellow","Red", "Grey","Magenta","Cyan"}; /** List of fluorescent channels that can be easily represented in FIJI */
	public ArrayList<String> selected_fluorescent_channels; /** Stores the fluorescent channels selected by the user */
	public File[] organised_fluor_dataset_folder; /** Stores the full path for the directory containing the raw fluorescence data after it has been moved by CTC5 program to relevant folder*/
	public ArrayList<String[]> selected_fluorescent_dataset_list; /** Stores the fluorescent datasets selected by the user.
																	* selected_fluorescent_dataset_list.get(n)[0] = the ndpi filename
																	* selected_fluorescent_dataset_list.get(n)[1-i] = the string identifier(s) for the brightfield reference templates for each NDPI/Fluor dataset.
																	*/
	
	public ArrayList<String[]> ROI_pores_only_dataset = new ArrayList<String[]>(); /** Stores the PORES only ROI gated version of all the Giemsa and BF images to facilitate automatic alignment.
	 																				*  selected_fluorescent_dataset_list.get(n)[0] = the "ndpi_ROI_poresOnly" filename
	 																				*  selected_fluorescent_dataset_list.get(n)[1-i] = the string identifier(s) for the brightfield reference templates "ROI_poresOnly" for each NDPI/Fluor dataset.
	 																				*/ 
	public ArrayList<String> x; /** Stores the x dimension for fluorescent tiles. input by the user for each channel template of each dataset. */
	public ArrayList<String> y; /** Stores the y dimension for fluorescent tiles. input by the user for each channel template of each dataset. */
	public ArrayList<String> pc; /** Stores the percentage overlap between fluorescent tiles. input by the user for each channel template of each dataset. */
	public ArrayList<int[][]> fluorescent_mosaic_dimensions_x_y_pc; /** Stores a processed form {@code int[]} of the above x, y and pc {@code ArrayList<String>} variables for each channel template of each dataset. */
	 
	public String fluorescent_mosaic_tile_ID; /** Stores the ID string that is used to count the order of raw fluorescent tiles. input by user, for example "_m{iiii}" */
	public int index_of_first_tile; /** Stores the index of the first fluorescent tile to be stitched. e.g. i=0 */
	public int counterstain_channel_index; /** Identifies which channel is to be used as a counterstain. This channel will be merged with all other channels when creating the final digital slide.*/
	public ArrayList<String[]> channelID_BFtemplate_pairs = new ArrayList<String[]>(); /** Holds pairs that link each fluorescent channel to a stitching template. This allows for fluorescent channels
	 																					*  to be stitched based on the template generated when stitching the brightfield template*/
	public ArrayList<String[]> generic_template_filename = new ArrayList<String[]>(); /** Holds the generic filename for the reference tif images (fluorescent) that need to be stitched. */
	
	//-- Constructor Method --//
	
	public CTC5Data()
	{
		quality_control = new CTC5_QC_User_Input();
	}
	
	
	//------------------------------------------------------//
	//  Methods to return the data collected by this class  //
	//------------------------------------------------------//
	
	public ArrayList<String> return_selected_ndpi_files(){
		return selected_ndpi_files;
	}
	public File return_initial_NDPI_directory(){
		return initial_NDPI_directory;
	}
	public String[] return_all_ndpi_filenames(){
		return all_ndpi_filenames;
	}
	public static String[] return_all_fluorescent_channels(){
		return all_fluorescent_channels;
	}
	public ArrayList<String> returnSelectedChannels(){
		return selected_fluorescent_channels;
	}
	public ArrayList<int[][]> return_fluorescent_mosaic_dimensions_x_y_pc(){
		return fluorescent_mosaic_dimensions_x_y_pc;
	}
	public ArrayList<String[]> return_selected_fluorescent_dataset_list(){
		return selected_fluorescent_dataset_list;
	}
	public ArrayList<Roi> return_ndpi_regions_for_extraction(){
		return ndpi_regions_for_extraction;
	}
	public void startTime(){
		runtimeStart = System.currentTimeMillis();
	}
	public String[] return_ndpi_infocus_focal_planes(int i){
		return ndpi_infocus_focal_planes[i];
	}
	public int returnMaxMag(int i)
	{
		return ndpi_maximum_magnification_list.get(i).intValue();
	}
	public String availableZOffsets(int i){
		return availableZOffsets.get(i);
	}
	public String returnMosaicTileID(){
		return fluorescent_mosaic_tile_ID;	
	}
	public int returnIndexOfFirstTile(){
		return index_of_first_tile;
	}
	public int returnCounterStainChanelIndex(){
		return counterstain_channel_index;
	}
	public ArrayList<String[]> returnChannelID_BFtemplate_pairs(){
		return channelID_BFtemplate_pairs;
	}
	
	public ArrayList<String[]> return_generic_template_filename(){
		return generic_template_filename;
	}
	
	
	//------------------------------------------------------//
	//--- Methods to collect and QC the key data fields ----//
	//------------------------------------------------------//
	
	
	
	
	/**
	* Asks the user to navigate to the location containing the .ndpi files for processing.
	* Checks if the selected folder actually contains .ndpi files and if none are found prompts the user to select a valid initial_NDPI_directory. 
    * Using the {@code Properties} class, it will default to and update the last selected location, if applicable.
	*  
	* @param error 	A {@code String} containing an error message explaining why the selected initial_NDPI_directory could not be opened.
	* @param msg	A {@code String} containing a message asking the user to navigate to a desired initial_NDPI_directory.
	*/
	public void getDirectory(String error, String msg)
	{
		Properties defaultProps = new Properties();
		InputStream input = null;
		String defPath = null;

		try 
		{
			File file = new File("."+File.separator+"plugins"+File.separator+"config.CTC5");
			input = new FileInputStream(file);
			defaultProps.load(input);
			defPath = defaultProps.getProperty("DIRECTORY_PATH");
		}
		catch (FileNotFoundException e)
		{
			IJ.log("No default settings detected. Current settings will be set as defaults where applicable.");
		}
		catch (IOException ex) 
		{
			IJ.log("ERROR: could not load default settings from the config.CTC5 file. File may be corrupted.");
		} 
		finally 
		{
			if (input != null) 
			{
				try 
				{
					input.close();
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
			}
		}
		
	    GenericDialogPlus gdDir = new GenericDialogPlus("CTC-5");
	    gdDir.addMessage(error, null, Color.RED);
	    gdDir.addMessage(msg);

	    if(!(defPath==null))
	    {
	    	gdDir.addDirectoryField("Directory:", defPath, 50);
	    }else
	    {
	    	gdDir.addDirectoryField("Directory:", "", 50);
	    }
	    gdDir.setMinimumSize(new Dimension(500,180));
	    gdDir.showDialog();
	    if (gdDir.wasOKed())
	    {
	      defPath = gdDir.getNextString();
	      
	      File inputFolder = new File(defPath);
	      
	      if (inputFolder.exists()) 
	      {
	    	all_ndpi_filenames = getFileNames(inputFolder);
	    	
	    	if(all_ndpi_filenames[0] == "")
	    	{
	    		getDirectory("ERROR: No .ndpi files were detected in this folder.", "Please select folder containing .ndpi files: ");
	        	gdDir = null;
	    	}
	    	else
	    	{
	    		gdDir = null;
	    		FileOutputStream out = null;
		
	    		try 
	    		{
	    			File temp = new File("."+File.separator+"plugins"+File.separator+"config.CTC5");
	    			out = new FileOutputStream(temp);
	    			defaultProps.setProperty("DIRECTORY_PATH",inputFolder.getPath());
	    			defaultProps.store(out,null);
	    			out.close();
	    		} 
	    		catch (FileNotFoundException e) 
	    		{
	    			e.printStackTrace();
	    		} 
	    		catch (IOException e) 
	    		{
	    			e.printStackTrace();
	    		}
	    		finally 
	    		{
	    			if (out != null) 
	    			{
	    				try 
	    				{
	    					out.close();
	    				} 
	    				catch (IOException e) 
	    				{
	    					e.printStackTrace();
	    				}
	    			}
	    		} 
	  
	    		initial_NDPI_directory = inputFolder;
	    		IJ.log("initial_NDPI_directory = "+initial_NDPI_directory);
	    	}
	      }
	      else
	      {
	    	  getDirectory("ERROR: Not a folder.", "Please select folder containing .ndpi files: ");
	    	  gdDir = null;
	      }					 
	    }
	  }
	  
	/** Searches an initial_NDPI_directory passed to it as a {@code File} argument for any .ndpi files.
	 * 
	 * @param f The (@code File} representing the initial_NDPI_directory that is to be searched for .ndpi files.
	 * @return A {@code String[]} containing the names of all the .ndpi files found in the given initial_NDPI_directory.
	 */
	public String[] getFileNames(File f)
	{
	    File[] listOfFiles = f.listFiles(); // lists all the files in the initial_NDPI_directory
	    String builder = ""; // string that will hold all of the NDPI files in the initial_NDPI_directory.
	    
	    //builds comma separated string of all the NDPI file names
	    for (int i = 0; i < listOfFiles.length; i++) {
	      if (listOfFiles[i].getName().toLowerCase().endsWith(".ndpi")) {
	        builder = builder + listOfFiles[i].getName() + ",";
	      }
	    }
	    
	    String[] ndpiFiles = builder.split(","); //converts the builder sting into a string array, rather than having to deal with an open ended array.
	    for(int i = 0; i < ndpiFiles.length; i++)
	    {
	    	IJ.log("ndpiFiles["+i+"] = "+ndpiFiles[i]);
	    }
	    
	    listOfFiles = null; 
	    builder = null;
	    return ndpiFiles;
	}
	
	/**
	 * Retrieves the selected filenames from the {@code Select_NDPI_Files_GUI} user interface.
	 * @param cb: The {@code Checkbox[]} used in the {@code Select_NDPI_Files_GUI} user interface.
	 * @param fn: The {@code String []} containing all of the .ndpi files originally detected by the {@code getFileNames(File f)} method.
	 * @return An {@code ArrayList<String>} containing the filenames of all the .ndpi files checked for processing by the user.
	 */
	public void processSelectedFiles(Checkbox[] cb, String[] fn)
	{
	    ArrayList<String> selectedFileNames = new ArrayList<String>();
	    
	    for (int i = 0; i < fn.length; i++)
	    {
	      if (cb[i].getState()) 
	      {
	    	String temp = removeSpaces(fn[i]);
	        selectedFileNames.add(temp);
	        
	        try {
	        	if(!(fn[i].equalsIgnoreCase(temp)))
	        	{
	        		/** This removes any spaces in the filename. Spaces cause an error when trying focus the various z-slices from the 
	        		  * extracted ndpi file. Due to a output file naming bug in the NDPITools plugin. */
	        		FileUtils.moveFile(new File(initial_NDPI_directory.getPath() + "\\" +fn[i]), new File(initial_NDPI_directory.getPath() + "\\"+temp));
	        	}
			} 
	        catch (IOException e) 
	        {
				IJ.log("ERROR: CTC5 could not remove the spaces from this file name. [0x001]");
				e.printStackTrace();
			}
	      }
	      else
	      {
	    	  IJ.log("DESELECTED: "+fn[i]);
	      }
	    }
	    
	    selected_ndpi_files = selectedFileNames;
	    for(int i = 0; i < selected_ndpi_files.size(); i++)
	    {
	    	IJ.log("selected_ndpi_files.get("+i+") = "+selected_ndpi_files.get(i));
	    }
	}
	
	public void moveSelectedFluorescentFiles(ArrayList<String[]> selected_fluor_files)
	{
		/**
		 * ArrayList<String[]> selected_fluor_files: The file names are stored in the first of each String[n][0]. 
		 * 											 String[n][1 ... i] contains info on which brightfield template is to be used to stitch the files. i is determined by 
		 * 											 how many reference templates are used per dataset. The number of or value of the brightfield templates is not needed
		 * 											 in this method.
		 */
		
		IJ.log("preparing to move selected Fluorescent files.");
		organised_fluor_dataset_folder = new File[selected_fluor_files.size()];
		
		for (int i = 0; i < selected_fluor_files.size(); i++)
	    {
			IJ.log("moving: "+selected_fluor_files.get(i)[0]+"...");
			
			String removeExt = (selected_fluor_files.get(i)[0]).substring(0, (selected_fluor_files.get(i)[0]).lastIndexOf("."));
			String fluor_folder_name = removeExt + "_fluor";
				
			IJ.log("source = " + initial_NDPI_directory.getPath() + "\\" + fluor_folder_name);
			IJ.log("target = " + initial_NDPI_directory.getPath() + "\\" + removeExt + "\\Raw Fluorescence");
				
			organised_fluor_dataset_folder[i] = new File(initial_NDPI_directory.getPath() + "\\" + removeExt + "\\Raw Fluorescence");	
				
			try 
			{
				FileUtils.moveDirectory(new File(initial_NDPI_directory.getPath() + "\\" + fluor_folder_name),organised_fluor_dataset_folder[i]);
				IJ.log("Fluorescent datasets have been moved.");
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
				IJ.log("ERROR: could not move the fluorescent dataset.");
			}
		} 
	}
	
	public void processSelectedChannels(Checkbox[] cb)
	{
	    selected_fluorescent_channels = new ArrayList<String>();
	    
	    for (int i = 0; i < cb.length; i++)
	    {
	      if (cb[i].getState()) 
	      {
	        selected_fluorescent_channels.add(all_fluorescent_channels[i]);
	      }
	      else{}
	    }
	}
	
	public void processSelectionOfFluorData(Checkbox[][] cb, ArrayList<String> ndpi_filenames, String[] reference_templates, TextField[][] x_user_input, TextField[][] y_user_input, TextField[][] pc_user_input, TextField tile_ID, TextField first_index)
	{
		selected_fluorescent_dataset_list = new ArrayList<String[]>();
		ArrayList<int[]> temp_x_y_pc_arraylist = new ArrayList<int[]>(); 
		fluorescent_mosaic_dimensions_x_y_pc = new ArrayList<int[][]>(); /** If there is 'n' templates per filename this will be ArrayList<int[n][3]>
																	     with ArrayList<int[n][3]>.size() = number of filenames [i.e. selected_fluorescent_dataset_list.size()] */
		ArrayList<String> temporary_string_arraylist = new ArrayList<String>();
		String [] temporary_string_array;  
		int[] temporary_int_array; 
		
		/** 
		 * this nested for loop structure to makes sure that all the cb.getState() entries are assessed. The high level loop populates the number of entries in the ArrayList objects. 
		 * The low level for loop is used to decide the size of and to populate the individual String[] and int[][]'s that will be added to the respective ArrayLists.
		 */
		
		/** The first loop iterates through all of the files selected for ndpi extraction */
	    for (int i = 0; i < cb.length; i++)
	    {
	    	temporary_string_arraylist.add(ndpi_filenames.get(i));
	    	
	    	/** The second loop iterates through all of the BF templates */
	    	for(int k = 0; k < cb[i].length; k++) 
	    	{
	    		/** identify which ndpi files and reference templates are to be stitched.*/
	    		if (cb[i][k].getState()) 
	    		{ 
	    			temporary_string_arraylist.add(reference_templates[k]);
	    			
	    			/** gather all the x, y and pc values collected for each file type in an order that matches 'selected_fluorescent_dataset_list' */
	    			temporary_int_array =  new int[3]; 
	    			temporary_int_array[0] = Integer.parseInt(x_user_input[i][k].getText()); // #tiles in x dimension 
	    			temporary_int_array[1] = Integer.parseInt(y_user_input[i][k].getText()); // #tiles in y dimension
	    			temporary_int_array[2] = Integer.parseInt(pc_user_input[i][k].getText()); // % overlap of tiles
	    			temp_x_y_pc_arraylist.add(temporary_int_array);
	    			temporary_int_array=null;
	    		}
	    		else{IJ.log("ndpi_filename_checkbox_array["+i+"] = false" );}
	    	}
	    	
	    	temporary_string_array = new String[temporary_string_arraylist.size()];
	    	IJ.log("temporary_string_array.length = "+temporary_string_array.length);
	    	
	    	for(int t = 0; t<temporary_string_arraylist.size(); t++)
	    	{
	    		temporary_string_array[t] = temporary_string_arraylist.get(t);
	    		IJ.log("temporary_string_array["+t+"] = "+temporary_string_array[t]);
	    	}
	    	
	    	//temporary_string_array = temporary_string_arraylist.toArray(new String[0]);
	    	IJ.log("temporary_string_array.length = "+temporary_string_array.length);
	    	selected_fluorescent_dataset_list.add(temporary_string_array);
	    	temporary_string_arraylist = null;
	    	temporary_string_arraylist = new ArrayList<String>();
	    	
	    	int[][] dim_pc_int_array = new int[temp_x_y_pc_arraylist.size()][3];
	    	
	    	for(int a = 0; a<temp_x_y_pc_arraylist.size(); a++)
	    	{
	    		for(int b = 0; b<3; b++)
	    		{
	    			dim_pc_int_array[a][b] = temp_x_y_pc_arraylist.get(a)[b];
	    		}
	    	}
	    	
	    	fluorescent_mosaic_dimensions_x_y_pc.add(dim_pc_int_array);
	    }
	        
	    fluorescent_mosaic_tile_ID = tile_ID.getText();
		index_of_first_tile = Integer.parseInt(first_index.getText());
	}
	
	public void process_fluorescent_channel_naming_and_templates(ButtonGroup counterstain_selection, TextField[] channel_identifiers, @SuppressWarnings("rawtypes") JComboBox[] brightfield_templates, String[] reference_templates)
	{
		IJ.log("//STARTING process_fluorescent_channel_naming_and_templates()");
		int temp_index = 0;
		
		for(Enumeration<AbstractButton> radio_buttons = counterstain_selection.getElements(); radio_buttons.hasMoreElements();)
		{
			IJ.log("        temp_index = "+temp_index);
			if(radio_buttons.nextElement().isSelected())
			{
				counterstain_channel_index = temp_index;
			}
			temp_index++;
		}
		
		for(int i = 0; i<channel_identifiers.length; i++)
		{
			String[] temp_string_array =  new String[2];
			
			IJ.log("        # process_fluorescent_channel_naming_and_templates #: i ="+i);
			temp_string_array[0] = channel_identifiers[i].getText();
			temp_string_array[1] = reference_templates[brightfield_templates[i].getSelectedIndex()];
			
			IJ.log("        temp_string_array: "+temp_string_array[0]+", "+temp_string_array[1]);
			
			channelID_BFtemplate_pairs.add(temp_string_array);
		}
		
		IJ.log("        selected_fluorescent_dataset_list.size() = "+selected_fluorescent_dataset_list.size());
		IJ.log("        channel_identifiers.length = "+channel_identifiers.length);
		IJ.log("        channelID_BFtemplate_pairs.size() = "+channelID_BFtemplate_pairs.size());     
		
		
		for(int k = 0; k<channelID_BFtemplate_pairs.size(); k++)
		{
			IJ.log("        channelID_BFtemplate_pairs.get("+k+")"+channelID_BFtemplate_pairs.get(k)[0]+", "+channelID_BFtemplate_pairs.get(k)[1]);
		}
		
		IJ.log("//ENDING: process_fluorescent_channel_naming_and_templates()");
	}
	
	/** Opens a preview of each of the .ndpi files named in {@code ArrayList<String> as} and asks the user to define a ROI for each. 
	 * 
	 * @param as An {@code ArrayList<String>} containing all of the filenames for the user selected .ndpi files to be processed.
	 * @return An {@code ArrayList<Roi>} containing all of the user defined ROIs for each of the user selected files passed to this method as {@code ArrayList<String> as}.
	 */
	
	public void getROIs(ArrayList<String> as)
	{  
		IJ.run("Point Tool...", "type=Circle color=Red size=Tiny label show counter=0");
		IJ.setTool("oval");
	    ImagePlus im = null; //This will hold the current image.
	    ArrayList<Roi> roi = new ArrayList<Roi>(); 	//This is an array that will hold the yet to be defined number of Regions Of Interest.
	     											//This will set up new user dialogs as required to capture the ROIs
	    String wfuString;
	    WaitForUserDialog wfu;
	    
	    for (int i = 0; i < as.size(); i++) 
	    {
	      String removeExt = ((String)as.get(i)).substring(0, ((String)as.get(i)).lastIndexOf("."));
	   
	      final NDPIToolsPreviewPlugin CTC5ndpiPreview = new NDPIToolsPreviewPlugin();
	      CTC5ndpiPreview.run(initial_NDPI_directory.getPath() + "\\" + removeExt + "\\Raw Giemsa\\" + (String)as.get(i));
	      im = WindowManager.getCurrentImage();
	      IJ.run("Enhance Contrast", "saturated=0.35");
	      final String mags = (String)im.getProperty("PreviewOfNDPIAvailableMagnifications");  //gets all available magnifications
	      availableZOffsets.add((String)im.getProperty("PreviewOfNDPIAvailableZOffsets"));
	      getMaxMags(mags);
	     
	      wfuString = ""+(String)as.get(i) + ": Select ROI";
	      wfu = new WaitForUserDialog("CTC-5", wfuString);
	      wfu.show();
	
	      int[] prev_dims = new int[2];
	      
	      prev_dims[0] = im.getWidth();
	      prev_dims[1] = im.getHeight();
	      
	      //preview_dimensions.add(prev_dims);
	      roi.add(im.getRoi());
	      
	      im.close();
	    }
	    
	    if (roi.size() > 0)
	    {
	      wfu = null;
	      im = null;
	      ndpi_regions_for_extraction = roi; //only returns the roi if some have be added to the ArrayList.
	      IJ.log("roi.size() = "+roi.size());
	      for(int t = 0; t<roi.size();t++)
	      {
	    	  IJ.log("ndpi_regions_for_extraction.get("+t+")= "+ndpi_regions_for_extraction.get(t));
	      }
	    }
	    
	    wfu = null;
	    im = null;
	  }	  

	/** Performs quality control checks on the user inputed focal planes to make sure the are of an expected type.
	 * 
	 * @param testing A {@code String[][]} that is passed from {@code startProcessing(ArrayList<String> as)} that contains the user input from {@code FocalPlanesGUI}.
	 * @return {@code Boolean} returns {@code true} only if the input meets expected criteria. Otherwise it recalls FocalPlane in a recurrsive manner until desired input is obtained or processing is aborted.  
	 */
	public Boolean qcFocalPlanes(String[][] input, int s, ArrayList<String> zOffsets)
	{
		  Boolean b = false;
		 
		  /**
		   * JTextField[][] testing is used to build a new JTextBox[][] containing highlighted syntax errors so that the user can correct and resubmit focal planes.
		   */
		  JTextField[][] testing = new JTextField[s][4]; // holds the user input in a formated way for testing and re-presenting to the user if required
	      int errorCount = 0;// tracks the number of input errors
	      
	      for (int i = 0; i < input.length; i++) 
	      {
	        for (int k = 0; k < input[i].length; k++)
	        {
	          String[] testPlanes = input[i][k].split(",");
	          
	          for (int l = 0; l < testPlanes.length; l++)
	          {
	            testPlanes[l] = testPlanes[l].replaceAll("\\s+", "");
	            
	            if (arrayContains(testPlanes[l], zOffsets.get(i).split(",")))
	            {
	              errorCount++;
	              
	              StringBuilder fullInput = new StringBuilder();
	              for (int m = 0; m < testPlanes.length; m++) {
	                fullInput.append(testPlanes[m]);
	                if (m < testPlanes.length - 1) {
	                  fullInput.append(", ");
	                }
	              }
	              String joined = fullInput.toString();
	              
	              testing[i][k] = new JTextField(joined);
	              testing[i][k].setForeground(Color.RED); // the erroneous text is highlighted in red for the user.
	              
	              l++;
	            }
	            else 
	            {
	              StringBuilder fullInput = new StringBuilder();
	              for (int m = 0; m < testPlanes.length; m++) {
	                fullInput.append(testPlanes[m]);
	                if (m < testPlanes.length - 1) {
	                  fullInput.append(", ");
	                }
	              }
	              String joined = fullInput.toString();
	              
	              testing[i][k] = new JTextField(joined);
	              testing[i][k].setForeground(Color.BLACK); //The correct text remains black.
	            }
	          }
	        }
	      }
	      if (errorCount == 0)// if any errors are detected the interface is re-displayed with the errors highlighted. If user exits the method will return null and initiate a user controlled exit.
	      {
	    	b= true;
	    	return b;
	        
	      }
	      else if (errorCount >0)
	      {
	    	  FocalPlanesGUI fpG = CTC5Analysis.return_focal_plane_input_frameGUI();
	    	  
	    	  fpG.remove(fpG.label);
	    	  fpG.remove(fpG.sp);
	    	  
	    	  fpG.label =null;
	    	  fpG.sp = null;
	    	  
	    	  String[] empty = {"",""};
	    	  fpG.label = fpG.makeLabelPane("ERROR: All values must be numeric and separated by commas only.", empty);
	    	  fpG.sp   	= fpG.updateScrollPane(testing);

	    	  fpG.add(fpG.label, BorderLayout.NORTH);
	    	  fpG.add(fpG.sp, BorderLayout.CENTER);
	    	  
	    	  fpG.validate();
	    	  fpG.pack();
	    	  fpG.repaint();
	    	  fpG.setVisible(true);
	          
	          testing = null;
	          b=false;
	          return b;
	    	  
	      }
	      else
	      {
	    	  IJ.log("errorCount =" +errorCount);
	    	  IJ.log("ERROR: unexpected 'errorCount' value in qcFocalPlanes()");
	    	  b=false;
	          return b;
	      }
	}
	  
	public boolean isNumeric(String inputData) 
	{
		return inputData.matches("[-+]?\\d+(\\.\\d+)?");
	}
	  
	public void getMaxMags(String mags)
	{
		    String[] magArray = mags.split(","); // assumes that it will be a comma seperated string of numbers. 
		    int maxMag = getMaxString(magArray); //assumes the largest magnification will be first in the array.
		    ndpi_maximum_magnification_list.add(new Integer(maxMag));    
	}
	  
	public int getMaxString(String[] mags)
	{
		int max = -1; 
		int temp;
		double tempD;
			
		if(mags.length > 0)
		{
			for(int i = 0; i<mags.length; i++)
			{
				tempD = Double.parseDouble(mags[i].replaceAll("x", ""));
				temp = (int) tempD;
				if(temp>max)
				{
					max=temp;
				}
				else
				{
					//Do nothing, max is already biggest.
				}
			}
				
		}
		else
		{
			IJ.log("ERROR: could not determine the magnifications.");
			temp = -1;
			return temp;
		}	
		
		return max;		
	}
	
	public void updateFocalPlanes(String[][] planes){
		ndpi_infocus_focal_planes = planes;
	}

	public String removeSpaces(String s){
		  String noSpaces = s.replaceAll(" ","");
		  return noSpaces;
	  }
	  
	public void updateAllowedFocalPlanes(String[] currFileInFocus)
	{	
	  FocalPlanesGUI fpG = CTC5Analysis.return_focal_plane_input_frameGUI();
  	  
  	  fpG.remove(fpG.label);
  	  
  	  fpG.label =null;
  	  
  	  fpG.label = fpG.makeLabelPane(fpG.error, currFileInFocus);

  	  fpG.add(fpG.label, BorderLayout.NORTH);
  	  
  	  fpG.validate();
  	  fpG.pack();
  	  fpG.repaint();
  	  fpG.setVisible(true);
	}

	public boolean arrayContains(String candidate, String[] standard){
		Boolean b = true;
		
		for(int i =0; i<standard.length;i++){
			if(candidate.equalsIgnoreCase(standard[i])){
				b=false;
			}
		}
		return b;
	}
}