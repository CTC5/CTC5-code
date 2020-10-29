package ctc5.main;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Stitch_Fluorescenct_Data {
	
	public static RotateAndAlign raa;
	
	public static void run() throws InterruptedException 
	{
		 IJ.log("// STARTING Stitch_Fluorescenct_Data");
		 int start_index = CTC5Analysis.data.fluorescent_mosaic_tile_ID.indexOf("{"); 
		 int end_index = CTC5Analysis.data.fluorescent_mosaic_tile_ID.indexOf("}");
		 int number_of_digits = end_index - start_index - 1;
		 String temp_digit_builder = "";
		
		 for(int k = 0; k < number_of_digits; k ++)
		 {
			  temp_digit_builder = temp_digit_builder + "\\d";
		 }
		 
		 /** 
		  * <String query_tile_ID> takes the mosaic_tile_ID user input and converts it into a form that is compatible with RegEx based comparison to file names 
		  * This allows for the creation of String[] generic_template_filename which holds the 'FIJI (Grid/Collection Stitching)' PlugIn compatible version of 
		  * the template filename that needs to be stitched. 
		  */ 
		 String query_tile_ID = CTC5Analysis.data.fluorescent_mosaic_tile_ID.substring(0,start_index)+temp_digit_builder+CTC5Analysis.data.fluorescent_mosaic_tile_ID.substring(end_index+1,CTC5Analysis.data.fluorescent_mosaic_tile_ID.length());
		 int num_of_channels_for_final_images = CTC5Analysis.data.channelID_BFtemplate_pairs.size()+1;
		 
		 /** the below five lines are trying to initialise the CTC5Analysis.data.final_adjusted_stitched_images to the correct size. This works here but somehow fails when trying to populate it. */ 
		 IJ.log("        CTC5Analysis.data.selected_fluorescent_dataset_list.size() = "+CTC5Analysis.data.selected_fluorescent_dataset_list.size());
		 IJ.log("        CTC5Analysis.data.channelID_BFtemplate_pairs.size() = "+CTC5Analysis.data.channelID_BFtemplate_pairs.size());
		 CTC5Analysis.data.final_adjusted_stitched_images = new String[CTC5Analysis.data.selected_fluorescent_dataset_list.size()][num_of_channels_for_final_images];
		 IJ.log("        CTC5Analysis.data.final_adjusted_stitched_images.length = "+CTC5Analysis.data.final_adjusted_stitched_images.length);
		 IJ.log("        CTC5Analysis.data.final_adjusted_stitched_images[0].length = "+CTC5Analysis.data.final_adjusted_stitched_images[0].length);
		 
		 
		 /** loop 'x' iterates through all of the NDPI/fluor datasets that have be selected for processing. */
		 for(int x = 0; x < CTC5Analysis.data.selected_fluorescent_dataset_list.size(); x++)
		 {
			 String dsName = CTC5Analysis.data.selected_fluorescent_dataset_list.get(x)[0]+"(fluor)";
			 int totalFiles = CTC5Analysis.data.selected_fluorescent_dataset_list.size();
			 
			    if(x == 0)
			    {
					CTC5Analysis.graphic_progress_update_frame.processFluorescence.UpdateProgress(1, dsName);
				}
				else
				{
					CTC5Analysis.graphic_progress_update_frame.processFluorescence.UpdateProgress(((x/totalFiles)*100)+1, dsName);
				}
				
				new Thread(new Runnable(){
					public void run(){	
						CTC5Analysis.graphic_progress_update_frame.processFluorescence.repaint();
					}
				}).start();
			 
			new File(CTC5Analysis.data.organised_fluor_dataset_folder[x].getPath()+"\\Stitched").mkdir();
			File[] temp_file_list = CTC5Analysis.data.organised_fluor_dataset_folder[x].listFiles();
			 
			/** loop 'd' iterates through all of the BF_templates that need to be stitched for each dataset */
			for(int d = 0; d < (CTC5Analysis.data.selected_fluorescent_dataset_list.get(x).length-1); d++)
			{
				IJ.run("Grid/Collection stitching", "type=[" + CTC5Analysis.process_fluoresence.type_string + "] order=[" + CTC5Analysis.process_fluoresence.order_string + "] "
					 	+ "grid_size_x="+ CTC5Analysis.data.fluorescent_mosaic_dimensions_x_y_pc.get(x)[d][0] + " grid_size_y=" + CTC5Analysis.data.fluorescent_mosaic_dimensions_x_y_pc.get(x)[d][1] 
					 	+ " tile_overlap="+ CTC5Analysis.data.fluorescent_mosaic_dimensions_x_y_pc.get(x)[d][1] + " first_file_index_i="+CTC5Analysis.data.index_of_first_tile
					 	+ " directory=["+ CTC5Analysis.data.organised_fluor_dataset_folder[x].getPath()+"] file_names="+CTC5Analysis.data.return_generic_template_filename().get(x)[d]
						+ " output_textfile_name="+CTC5Analysis.data.selected_fluorescent_dataset_list.get(x)[d+1]+ "_GCS.txt"
					 	+ " fusion_method=[Linear Blending] regression_threshold=0.30 max/avg_displacement_threshold=2.50 absolute_displacement_threshold=3.50"
						+ " compute_overlap subpixel_accuracy computation_parameters=[Save memory (but be slower)] image_output=[Fuse and display]");

				
			    IJ.saveAs("Tiff", CTC5Analysis.data.organised_fluor_dataset_folder[x].getPath()+ "\\Stitched\\"+CTC5Analysis.data.selected_fluorescent_dataset_list.get(x)[d+1]);
			    IJ.log("SAVING: "+CTC5Analysis.data.organised_fluor_dataset_folder[x].getPath()+ "\\Stitched\\"+CTC5Analysis.data.selected_fluorescent_dataset_list.get(x)[d+1]);
			    IJ.wait(1000); /** preventing the image window from closing too early [could not put it in its own thread and .join() as x could not be 'final'] */
			    
			    /** Grid/Collect Stitching places unstitched tiles at the 0,0 position in the *.registered.txt file
			     *  This can cause issues for calculation translations at end. I need to account for this by removing all 0,0 positions
			     *  and adding back in the cropped area if 'stitched from file' image does not match dimensions of original 'BF template' image.*/
			    ImagePlus BF_template_image = WindowManager.getCurrentImage();
			    int [] template_dims = new int[2];
			    template_dims[0] = BF_template_image.getWidth();
			    template_dims[1] = BF_template_image.getHeight();
			    
			    CTC5Analysis.closeAllImages();
			    System.gc();
			
			    String template = "EMPTY";
			 	
			 	String txt_file_location = CTC5Analysis.data.organised_fluor_dataset_folder[x].getPath()+"\\"+CTC5Analysis.data.selected_fluorescent_dataset_list.get(x)[d+1]+ "_GCS.registered.txt";  
			 	IJ.log("text_file_location = "+ txt_file_location);
			 	
			 	/** loop 'u' iterates through all of selected_fluor_channels, if the a channel matches the current BF template it adapts the 
			 	 * [template].registered.txt file and stitches that channel 'from  file'. */
			 	for(int u = 0; u < CTC5Analysis.data.returnChannelID_BFtemplate_pairs().size(); u++)
			 	{ 
			 		/** If the BF_template stitched in loop 'd' matches the current 'u' channel, then stitch the current channel 'from file'. */
			 		if(CTC5Analysis.data.selected_fluorescent_dataset_list.get(x)[d+1].equals(CTC5Analysis.data.returnChannelID_BFtemplate_pairs().get(u)[1]))
			 		{	
			 			try 
			 			{
			 				template = readFile(txt_file_location);
			 			} 
			 			catch (IOException e) 
			 			{
			 				IJ.log("I/O ERROR: Cannot access the '[template].registered.txt' file to stitch the additional wavelengths.");
			 				e.printStackTrace();
			 			}
			 			
			 			IJ.log("Current wavelenght = "+CTC5Analysis.data.returnChannelID_BFtemplate_pairs().get(u)[0]);
			 			IJ.log("Current template = " + template);
	
			 			if(d == CTC5Analysis.data.index_of_reference_BF_template)
			 			{			 				
			 				stitch_fluorescent_data_from_file(CTC5Analysis.data.returnChannelID_BFtemplate_pairs().get(u)[0], temp_file_list, template, number_of_digits, query_tile_ID, start_index, x, true, u, template_dims);
			 			}
			 			else
			 			{
			 				stitch_fluorescent_data_from_file(CTC5Analysis.data.returnChannelID_BFtemplate_pairs().get(u)[0], temp_file_list, template, number_of_digits, query_tile_ID, start_index, x, false, u, template_dims);
			 			}
			 			
			 			int NumOfChannels = CTC5Analysis.data.returnChannelID_BFtemplate_pairs().size();
			 			
			 			CTC5Analysis.graphic_progress_update_frame.processFluorescence.UpdateProgress((int)(((((double)x+(((double)u+1.0)/(double)NumOfChannels))/(double)totalFiles)*100.0)), dsName);
						
						new Thread(new Runnable(){
							public void run(){	
								CTC5Analysis.graphic_progress_update_frame.processFluorescence.repaint();
							}
						}).start();
			 		}
			 	}
			 	
			 	IJ.log("// ENDING Stitch_Fluorescenct_Data");
			} 
			
			if((x+1) >= totalFiles)
			{
				CTC5Analysis.graphic_progress_update_frame.processFluorescence.UpdateProgress(100, dsName);
			}
			else
			{
				CTC5Analysis.graphic_progress_update_frame.processFluorescence.UpdateProgress(((x+1)/totalFiles)*100, dsName);
			}
				
		    new Thread(new Runnable(){
				public void run(){	
					CTC5Analysis.graphic_progress_update_frame.processFluorescence.repaint();
				}
			}).start();
		 } 

		 IJ.log("CTC5Analysis.data.return_selected_fluorescent_dataset_list().size() = "+CTC5Analysis.data.return_selected_fluorescent_dataset_list().size());
		 raa = new RotateAndAlign(CTC5Analysis.data.return_initial_NDPI_directory(), CTC5Analysis.data.return_selected_fluorescent_dataset_list());		  
		 CTC5Analysis.exitProgram("Processing has completed:");
	}
	
	/** 
	 * This method is passed a wavelenght_ID and a folder to process (among other arguments.):
	 * 	1) it identifies a .tif file that contains the wavelength_ID in the filename.
	 *  2) it then splits the [template].registered.txt file into individual lines so that they can be edited to be applied for the stitching of the current file.
	 *  3) it then saves a new file called [wavelengthID]_GCS.txt that will be passed to 'Grid/Collection Stitching' plugin as a 'from file' argument.
	 * 	
	 * */
	private static void stitch_fluorescent_data_from_file(String wavelength_ID, File[] temp_file_list, String template, int number_of_digits, String query_tile_ID, int start_index, int x, boolean save_to_final, int index_of_wavelength, int[] template_dimensions)
	{
		IJ.log("index_of_wavelength = "+index_of_wavelength);
		Pattern tile_ID_finder = Pattern.compile(query_tile_ID);
	
		String temp_filename = null;
	 	
	 	for(int q = 0; q<temp_file_list.length; q++)
	 	{
	 		temp_filename =  temp_file_list[q].getName();
			 
			if(temp_filename.endsWith(".tif") && temp_filename.contains(wavelength_ID))
			{
				q = temp_file_list.length+1;
				IJ.log("temp_filename = "+temp_filename);
				IJ.log("query_tile_ID = "+query_tile_ID);
			}
	 	}
	 	
	 	String[] stitching_registered_output = template.split(System.getProperty("line.separator"));
	 	String wavelength_formatted_filename = null;
	 	
	 	for(int t = 0; t<stitching_registered_output.length; t++)
	 	{	
	 		wavelength_formatted_filename = "placeholder.tif";
	 		
	 		if(stitching_registered_output[t].contains(";"))
	 		{
	 			IJ.log("stitching_registered_output["+t+"] = "+stitching_registered_output[t]);
	 			Matcher matcher = tile_ID_finder.matcher(stitching_registered_output[t]);

	 			while(matcher.find())
	 			{
	 				wavelength_formatted_filename = temp_filename.replaceAll(query_tile_ID,matcher.group());
	 				IJ.log("wavelength_formatted_filename = "+wavelength_formatted_filename);
	 				stitching_registered_output[t] = wavelength_formatted_filename + stitching_registered_output[t].substring(stitching_registered_output[t].indexOf(";"),stitching_registered_output[t].length());
	 				IJ.log("stitching_registered_output["+t+"] = "+stitching_registered_output[t]);
	 			}
	 		}
	 	}
	 	
	 	PrintWriter out = null;
		try 
		{
			out = new PrintWriter(new File(CTC5Analysis.data.organised_fluor_dataset_folder[x].getPath()+"\\"+wavelength_ID+"_GCS.txt"));
		} catch (FileNotFoundException e) {
			IJ.log("ERROR: could not write the edited registered.txt file. Will not be able to stitch additional wavelengths.");
			e.printStackTrace();
		}
	 
	 	for(int y = 0; y<stitching_registered_output.length; y++)
	 	{
	 		out.println(stitching_registered_output[y]);
	 	}
	 	
	 	out.close();
	 	
	 	IJ.log("#### Stitching: "+wavelength_ID+" ####");
	 	
	 	IJ.run("Grid/Collection stitching", "type=[Positions from file] order=[Defined by TileConfiguration]"
	 	+ " directory=["+ CTC5Analysis.data.organised_fluor_dataset_folder[x].getPath()+"] layout_file="+wavelength_ID+"_GCS.txt"
	 	+ " fusion_method=[Linear Blending] regression_threshold=0.30 max/avg_displacement_threshold=2.50 absolute_displacement_threshold=3.50 subpixel_accuracy "
	 	+ "computation_parameters=[Save memory (but be slower)] image_output=[Fuse and display]");
		
	 	ImagePlus stitched_from_file_image = WindowManager.getCurrentImage();
	    int [] stitched_dims = new int[2];
	    stitched_dims[0] = stitched_from_file_image.getWidth();
	    stitched_dims[1] = stitched_from_file_image.getHeight();
	    
	    /** correcting stitched dimensions to account for any (0,0) bug coming out of the *.registerted.txt file */
	    if((stitched_dims[0] != template_dimensions[0])||(stitched_dims[1] != template_dimensions[1]))
	    {
	    	IJ.log("Stitched from file image adjusted to account for (0,0) bug.");
	    	IJ.run(stitched_from_file_image,"Canvas Size...", "width="+template_dimensions[0]+" height="+template_dimensions[1]+" position=Bottom-Right");
	    }

		IJ.saveAs("Tiff", CTC5Analysis.data.organised_fluor_dataset_folder[x].getPath()+ "\\Stitched\\"+wavelength_ID);
		IJ.log("        SAVING: "+CTC5Analysis.data.organised_fluor_dataset_folder[x].getPath()+ "\\Stitched\\"+wavelength_ID);

		/** if these images are stitched from the 'fixed' (reference) BF_template, save their final position for use later. */
		if(save_to_final)
		{
			int temp_index = index_of_wavelength+1;

			IJ.log("CTC5Analysis.data.final_adjusted_stitched_images.length = "+ CTC5Analysis.data.final_adjusted_stitched_images.length+". x = "+ x);
			IJ.log("CTC5Analysis.data.final_adjusted_stitched_images[0].length = "+ CTC5Analysis.data.final_adjusted_stitched_images[0].length+". temp_index = "+ temp_index);
			IJ.log("CTC5Analysis.data.organised_fluor_dataset_folder.length = "+CTC5Analysis.data.organised_fluor_dataset_folder.length+". x = "+x);
			
			IJ.log("      TRYING TO ADD: '"+CTC5Analysis.data.organised_fluor_dataset_folder[x].getPath()+ "\\Stitched\\"+wavelength_ID+".tif"+"' to CTC5Analysis.data.final_adjusted_stitched_images["+x+"]["+temp_index+"]");
			
			/** These images are already at the reference alignment position and do not need to be scaled or rotated. Thus save their location directly to the final position for merging later */
			CTC5Analysis.data.final_adjusted_stitched_images[x][temp_index] = CTC5Analysis.data.organised_fluor_dataset_folder[x].getPath()+ "\\Stitched\\"+wavelength_ID+".tif";
		}
		
		IJ.wait(1000); /** preventing the image window from closing too early [could not put it in its own thread and .join() as x could not be 'final'] */
		
		CTC5Analysis.closeAllImages();
		System.gc();
	}
	
	private static String readFile(String file) throws IOException {
	    BufferedReader reader = new BufferedReader(new FileReader (file));
	    String         line = null;
	    StringBuilder  stringBuilder = new StringBuilder();
	    String         ls = System.getProperty("line.separator");

	    try {
	        while((line = reader.readLine()) != null) 
	        {
	        	if(line.contains("(0.0, 0.0)")) 
	        	{
	        		/** this is to control for a bug in the .registered.txt file output of grid collection stitching. 
	        		 *  images that cannot be mapped get defaulted to 0,0 in the file even if nothing has been mapped to 0,0 
	        		 *  As such all 0,0 references will be removed from the text file.*/
	        	}
	        	else
	        	{
	        		stringBuilder.append(line);
	        		stringBuilder.append(ls);
	        	}
	        }

	        return stringBuilder.toString();
	    } finally {
	        reader.close();
	    }
	}

}