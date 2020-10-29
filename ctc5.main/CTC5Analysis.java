package ctc5.main;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.WaitForUserDialog;
import ij.plugin.PlugIn;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.lang.Runnable; 
import java.lang.Thread;

import javax.swing.JTextField;

import org.apache.commons.io.FileUtils;

import ctc5.guis.*;

/**
 * This is the main class in the CTC5 program. It constructs the other objects in the program and calls their methods to carry out the image processing.
 * 
 * @author  Brendan Ffrench
 * @date    29 OCT 2020.
 * @version 2.1.0
 * 
 * Version Info:
 * 1) Includes new modules to incorporate stitching of fluorescent images
 * // 2) Includes key pairs to save the previously used settings.
 * // 3) Fixed a bug that prevented CTC5 from being run correctly twice in a row.
 * // 4) Made substantial improvements to code readability.
 * // 5) Improved Javadoc annotations.
 */

public class CTC5Analysis implements PlugIn, ActionListener, FocusListener
{
	  static Select_NDPI_Files_GUI ndpi_selection_frameGUI;			/** Used to access the {@code Select_NDPI_Files_GUI} class, which requests user input on which files to process.*/
	  public static CTC5Data data;									/** Used to store all the data required by classes the ctc5.main package */
	  static Gather_Fluorescence_Info process_fluoresence;			/** Class instance that will gather user input and use it to stitch the templates and immunofluorescent data. */  			
	  static RotateAndAlign process_rotation_and_alignment; 		/** Class instance that will use the processed NDPI, immunofluorescent and common_points_analysis data to rotate and align the digital slides. */
	  static FocalPlanesGUI focal_plane_input_frameGUI;				/** Used to access the {@code FocalPlanesGUI} class, which requests user input on which focal planes are required to get a fully focused image post processing.*/
	  final static ProgressFrame graphic_progress_update_frame = new ProgressFrame(); /** Frame for creating and displaying progress updates to the user during the long processing times.*/
	  boolean CTC5_installed_correctly = false;
	  
	  //---------------------------------------------------------------------//
	  //- Methods that exert over-arching control to the flow of processing -//
	  //---------------------------------------------------------------------//
	
	/**
	 * The method initially called by ImageJ when the Plugin is started.
	 * This method initiates the user input to select location of and which of the .ndpi file should be processed.
	 */
	 public void run(String arg0)
	 {
		IJ.log("CTC5 has started.");
		data = new CTC5Data();  
		data.startTime();
		
		if(isWindows(System.getProperty("os.name").toLowerCase()))
		{
			CTC5_installed_correctly = data.quality_control.check_plugin_dependencies();
		}
		else
		{
			CTC5_installed_correctly = true;
		}
		
		if(CTC5_installed_correctly)
		{
			data.getDirectory("", "Please select folder containing .ndpi files: ");
	
			if (data.return_initial_NDPI_directory() != null)
			{
				this.ask_for_selected_NDPI_files(data.return_all_ndpi_filenames(), "");
			}
			else 
			{
				CTC5Analysis.exitProgram("Processing was cancelled [0x001]");
			}	
		}
		else
		{
			IJ.showMessage("[0x002] ERROR: the following plugins are missing:"+System.lineSeparator()+System.lineSeparator()+data.quality_control.return_missing_plugins()+System.lineSeparator()+System.lineSeparator()+"Please install and try again.");
			CTC5Analysis.exitProgram("Processing was cancelled:  [0x002]");
		}
	  }
		
	  /** Reports back when user has completed interactions with the various GUIs produced.
	   *  Depending on the source of the {@code ActionEvent} different methods will be called as appropriate.
	   */
	 public void actionPerformed(ActionEvent e) 
	 {
		if(e.getSource() == ndpi_selection_frameGUI.ok)
		{
			ndpi_selection_frameGUI.setVisible(false);
			data.processSelectedFiles(ndpi_selection_frameGUI.ndpi_filename_checkbox_array, ndpi_selection_frameGUI.files);
			
			if (data.return_selected_ndpi_files().size() <= 0 && data.return_selected_ndpi_files()!= null)
			{
				 this.ask_for_selected_NDPI_files(data.return_all_ndpi_filenames(), "ERROR: Select at least one file.");
			}
			else
			{
				boolean run = false; /** this controls for the generation of the new file structure. 
    				 			 	  *  If for any reason the program cannot create the file tree, the program will exit with a I/O error. 
    				 			 	  */
				try
				{	
					this.organiseFileStructure(); /** generates file structure and moves the ndpi files to the desired location. 
											   * May open a user dialog if files are in use and cannot be used. */
					run = true;
				}
				catch (Exception ex)
				{
					run = false;
				    IJ.showMessage("Error: cannot create file-tree at this location.");
				}
				
				if (run)
				{	 
					Thread collect_ndpi_extraction_data = new Thread(new Runnable(){
								public void run()
								{
									data.getROIs(data.return_selected_ndpi_files()); //opens NDPIpreview for each file selected and prompts the user to select the ROI to be extracted.
															
									if (data.return_ndpi_regions_for_extraction() != null)
									{
										get_focal_planes_for_NDPI_focusing(data.return_selected_ndpi_files()); //generates a user interface to request the desired focal planes. This is controlled for user errors.
									}
									else 
									{
										IJ.showMessage("Processing has terminated, no Regions of Interest were detected."); //this should only happen if getROIs returns null.
									}				
								}});	
					
					collect_ndpi_extraction_data.start();
				}
				else 
				{
					IJ.showMessage("Processing has terminated with a read/write error [0x001].");
				}
			}	
		}
		else if(e.getSource() == ndpi_selection_frameGUI.cancel)
		{
			ndpi_selection_frameGUI.setVisible(false);
			ndpi_selection_frameGUI = null;
			exitProgram("CTC-5 processing was exited by the user");
		}
		else if(e.getSource() == focal_plane_input_frameGUI.ok)
		{
			focal_plane_input_frameGUI.setVisible(false);
			
			Thread getFluorData = new Thread(new Runnable(){
												public void run(){
													process_fluoresence = new Gather_Fluorescence_Info(data.return_selected_ndpi_files()); 
													try {
														process_fluoresence.run();
													} catch (InterruptedException e1) {
														e1.printStackTrace();
												    }
												}});
			getFluorData.start();
			
		}
		else if(e.getSource() == focal_plane_input_frameGUI.cancel)
		{
			focal_plane_input_frameGUI.setVisible(false);
			exitProgram("CTC-5 processing was exited by the user");
		}
		else
		{
			IJ.log("e.getSource() ="+e.getSource());
			IJ.log("Unexpected error: when attempting to retrieve files for processing.");
		}
	  }

	  /**
	   * Restructures the file structures in the target initial_NDPI_directory in preparation for all the processing that will be conducted.
	   */
	 public void organiseFileStructure()
	  {
		ArrayList<String> selectedFiles = data.return_selected_ndpi_files();
		File directory = data.return_initial_NDPI_directory();
		  
	    for (int i = 0; i < selectedFiles.size(); i++)
	    {
	      //Makes a set of directories; one for the Raw Giemsa focused and stitched images; one for the RawFluoresence and one for the final processed.	
	      String removeExt = ((String)selectedFiles.get(i)).substring(0, ((String)selectedFiles.get(i)).lastIndexOf("."));
	      new File(directory.getPath() + "\\" + removeExt + "\\Final Processed").mkdirs();
	      new File(directory.getPath()+"\\"+removeExt+"\\Final Processed\\Adjusted").mkdirs();
	      new File(directory.getPath() + "\\" + removeExt + "\\Raw Giemsa\\Focused").mkdirs();
	    }
	    
	    for (int i = 0; i < selectedFiles.size(); i++) 
	    {
	      String removeExt = ((String)selectedFiles.get(i)).substring(0, ((String)selectedFiles.get(i)).lastIndexOf("."));
	      
	      try
	      {
	        FileUtils.moveFile(new File(directory.getPath() + "\\" + (String)selectedFiles.get(i)), new File(directory.getPath() + "\\" + removeExt + "\\Raw Giemsa" + "\\" + (String)selectedFiles.get(i)));
	      }
	      catch (IOException e)
	      {
	        WaitForUserDialog fileWFU = new WaitForUserDialog("CTC-5", "ERROR: Unable to move the files selected for processing. \n Please close other programs and press OK.");
	        fileWFU.show();
	      }
	    }
	  }

	  /**
	   * This method is called after the user has entered all of requested focal planes.
	   * It then initiates a quality control method on the user input from the {@code FocalPlanesGUI}, before proceeding to extract, focus and stitch and save as tiff.
	   * 
	   * @param as An {@code ArrayList<String>} containing all of the filenames for the user selected .ndpi files to be processed.
	   * @throws InterruptedException 
	   */
	 public static void startProcessing(ArrayList<String> as) throws InterruptedException
	  {
		  final int size = as.size();
		  final String[][] planes =  new String[size][4]; 
		  
		  for (int i = 0; i < size; i++) 
		  {
		      for (int k = 0; k < 4; k++) 
		      {
		          planes[i][k] = focal_plane_input_frameGUI.focalPlanes[i][k].getText();
		      }
		  }
		  
		  data.updateFocalPlanes(planes);
		  
		  Boolean b = data.qcFocalPlanes(planes, size, data.availableZOffsets);

		  if(b)
		  {
			  graphic_progress_update_frame.init();
			  graphic_progress_update_frame.setSize(480,450);
			  graphic_progress_update_frame.setAlwaysOnTop(true);
			  graphic_progress_update_frame.setVisible(true);
			  
			  ProcessNDPI pNDPI = new ProcessNDPI(data.return_initial_NDPI_directory());			  	
		
			  for(int i =0; i<size; i++)
			  {				
			  	pNDPI.extractFromNDPI(data.return_selected_ndpi_files().get(i), data.return_ndpi_regions_for_extraction().get(i), (double)size, (double)i, planes[i], data.returnMaxMag(i));
			  }
			 
			  for(int i =0; i<size; i++)
			  {				
			  	pNDPI.focusNDPI(data.return_selected_ndpi_files().get(i), planes[i], (double)size, (double)i, data.returnMaxMag(i), data.return_ndpi_regions_for_extraction().get(i)); 
			  }
			  
			  for(int i =0; i<size; i++)
			  {
				closeAllImages();
				pNDPI.stitchNDPI(data.return_selected_ndpi_files().get(i), (double)size, (double)i);
			  }
		  }
		  else{
			  IJ.log("ERROR: incorrect focal planes entered.");
		  }
	  }
	  
	 public static void closeAllImages()
	  {
		ImagePlus ip;  
		
	    while (WindowManager.getCurrentImage() != null) 
	    {
	      ip = WindowManager.getCurrentImage();
	      ip.changes = false;
	      ip.close();
	    }
	    
	    ip = null;
	  }
	 
	 public static void hideAllImages()
	  {
		ImagePlus ip;  
		
	    while (WindowManager.getCurrentImage() != null) 
	    {
	      ip = WindowManager.getCurrentImage();
	      ip.hide();
	    }
	  }

	@Override
	public void focusGained(FocusEvent fe) 
	{
		JTextField tf = (JTextField) fe.getSource();
	    for (int i = 0; i < focal_plane_input_frameGUI.focalPlanes.length; i++)
	      for (int j = 0; j < 4; j++)
	        if (focal_plane_input_frameGUI.focalPlanes[i][j] == tf)
	        {
	          String[] currFileInFocus = {(String)focal_plane_input_frameGUI.fileList.get(i),(String)data.availableZOffsets.get(i)};
	          data.updateAllowedFocalPlanes(currFileInFocus);
	        }	
	}

	@Override
	public void focusLost(FocusEvent arg0) 
	{
		// The only places that focus can be lost in the program are other TextFields (which updates the panel) or the OK/Cancel buttons which render the frame invisible.
	}

	public static void exitProgram(String s){
		IJ.log(s);
		
		data.runtimeEnd = System.currentTimeMillis();
	    IJ.log("Elapsed time = " + (data.runtimeEnd - data.runtimeStart) / 60000L + " min");  
	    IJ.selectWindow("Log");
	    IJ.saveAs("Text", data.initial_NDPI_directory.getAbsolutePath()+"\\CTC5_log");
	    
	    if(s.equalsIgnoreCase("Successful Completion"))
	    {
	    	String completion_message = "Program has completed successfully."+System.lineSeparator()+"Aligned images can be found in: <dataset>\\Final Processed\\Adjusted";
	    	
	    	IJ.showMessage(completion_message);
	    }
	    
	    
	    ndpi_selection_frameGUI = null;
		data = null;
		process_fluoresence = null;
		process_rotation_and_alignment = null;
		focal_plane_input_frameGUI = null;
	    
		System.gc();
	}
	
	  //--------------------------------------------//
	  //- Methods for creating GUIs for user input -//
	  //--------------------------------------------//
	  
	  /** Used to access the {@code Select_NDPI_Files_GUI} class, which requests user input on which NDPI files to process.*/
	  public void ask_for_selected_NDPI_files(String[] list_of_all_ndpi_files, String error)
	  {
	    ndpi_selection_frameGUI = new Select_NDPI_Files_GUI(list_of_all_ndpi_files, error);
	    ndpi_selection_frameGUI.init();
	    ndpi_selection_frameGUI.ok.addActionListener(this);
	    ndpi_selection_frameGUI.cancel.addActionListener(this);
	    ndpi_selection_frameGUI.setVisible(true);
	  }
	  
	  /**
	   * Generates a {@code FocalPlanesGUI} dialog to ask the user to enter the necessary focal planes to focus each of the NDPI files passed to the method in the {@code ArrayList<String> as} argument.
	   * 
	   * @param as An {@code ArrayList<String>} containing all of the filenames for the user selected .ndpi files to be processed.
	   */
	  public void get_focal_planes_for_NDPI_focusing(ArrayList<String> as)
	  {
		focal_plane_input_frameGUI = new FocalPlanesGUI(as);
		focal_plane_input_frameGUI.init();
		
		for(int i= 0; i < focal_plane_input_frameGUI.focalPlanes.length;i++){
			for(int k = 0; k<4; k++){
				focal_plane_input_frameGUI.focalPlanes[i][k].addFocusListener(this);
			}
		}
		
		focal_plane_input_frameGUI.ok.addActionListener(this);
		focal_plane_input_frameGUI.cancel.addActionListener(this);
		focal_plane_input_frameGUI.setVisible(true);
	  }
	  
	  /**
	   * @returns The current state of the {@code FocalPlanesGUI focal_plane_input_frameGUI} so that it can be mined for data by other classes.
	   */
	  public static FocalPlanesGUI return_focal_plane_input_frameGUI()
	  {
		  return focal_plane_input_frameGUI;
	  }
	  
	  public static boolean isWindows(String s) 
	  {
		IJ.log("OS name: "+s);
		return (s.indexOf("win") >= 0);
	  }
	
}