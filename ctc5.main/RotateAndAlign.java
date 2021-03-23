package ctc5.main;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import ij.IJ;
import ij.ImagePlus;
//import ij.Prefs;
import ij.WindowManager;
import ij.gui.OvalRoi;
import ij.measure.ResultsTable;
import ij.plugin.ImageCalculator;
import ij.plugin.RGBStackMerge;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

/**
 * Scales rotates and aligns all the stitched images using an iterative affine transform and output verification approach. 
 * 
 * @author  Brendan Ffrench: Royal College of Surgeons in Ireland.
 * @date    10 August 2017.
 * @version 2.0.0
 */

public class RotateAndAlign 
{
	double tracker_one = 0.0;
	double tracker_two = 0.0;
	double tracker_three = 0.0;
	double tracker_four = 0.0;
	
	int counter_stain_index = CTC5Analysis.data.counterstain_channel_index+1; /** add 1 to the index as the Giemsa channel is going to go in front of this. */
	
	File parent_file_location;
	int[] freq_table_x;
	int[] freq_table_y;
	
	int[][][] original_image_dimensions; /** int[number of datasets][number of pores only images][dimensions width*height] */
	int[][][] anchor_coords; /** holds the top left anchor positions for all ROIs of ndpi and BF images for each dataset 
	 						  *  int [] for each dataset
	 						  *  int [][] for each ndpi/BF templatr in data set
	 						  *  int [][][] {X,Y} top left anchor for each ndpi/BF template. */ 
	double[][][] center_coords; /** same as anchor_coords except identifies the center of each stitched image */
	
	ArrayList<double[][]> sourceCoordList = new ArrayList<double[][]>();
	ArrayList<double[][]> targetCoordList = new ArrayList<double[][]>();
	
	double[] mX;
	double[] mY;
	double[] fX;
	double[] fY;
	
	/** identify the index position of the fixed template/images in als */
	int fixed_index = 1;
	int[][] fixed_index_image_dimensions;
	
	/** File f: parent directory for .ndpi files and _fluor sub folders ; 
	 * ArrayList<String[]> datasets: ndpi [0] and BF template [n+1] filenames
	 * @throws InterruptedException */
	public RotateAndAlign(File f, ArrayList<String[]> datasets) throws InterruptedException
	{
		IJ.log("RotateAndAlign(File f, ArrayList<String[]> datasets)");
		
		parent_file_location = f;
		String ROI_location;
		
		int num_of_datasets = datasets.size();  /** dataset: equates to number of physical slides scanned */
		original_image_dimensions = new int[num_of_datasets][][];  
		anchor_coords = new int[num_of_datasets][][];                                              
		center_coords = new double[num_of_datasets][][];   
		
		fixed_index_image_dimensions =  new int[datasets.size()][2];
		
		/** nested loop [int i and k] generates pores only images for all ndpi and BF templates for all datasets
		  *   this will be used as input for the affine transforms to align the images */
		for(int i = 0; i < num_of_datasets; i++)
		{	
			/** update the graphics panel. */
			if(i == 0)
			{
				CTC5Analysis.graphic_progress_update_frame.feature_analysis.UpdateProgress(1, datasets.get(i)[0]+"- Giemsa");
			}
			else
			{
				CTC5Analysis.graphic_progress_update_frame.feature_analysis.UpdateProgress((((double)i/(double)num_of_datasets)*100.0)+1, datasets.get(i)[0]+"- Giemsa");
			}
			
			new Thread(new Runnable(){
				public void run(){	
					CTC5Analysis.graphic_progress_update_frame.feature_analysis.repaint();
				}
			}).start();
			
			
			int num_of_BF_templates = datasets.get(i).length-1; 													  
			IJ.log("num_of_BF_templates = "+num_of_BF_templates);
			String[] poresOnly_filenames = new String[num_of_BF_templates+1];
			original_image_dimensions[i] = new int[num_of_BF_templates+1][2];
			anchor_coords[i] = new int[num_of_BF_templates+1][2];
			center_coords[i] = new double[num_of_BF_templates+1][2];
			
			ROI_location = ROI_for_NDPI(datasets.get(i)[0],i);
			poresOnly_filenames[0] = pores_only_ROI(ROI_location,true);
			
			for(int k = 1; k < poresOnly_filenames.length; k++)
			{
				/** set the original fixed image dimensions to facilitate extending the canvas prior to translations */
				if(k==fixed_index)
				{
					IJ.log("Attempting to make fixed_imp: "+datasets.get(i)[k]);
					
					IJ.log("Fixed image = "+CTC5Analysis.data.final_adjusted_stitched_images[i][k]);
					ImagePlus fixed_imp = new ImagePlus(CTC5Analysis.data.final_adjusted_stitched_images[i][k]); 
					
					fixed_index_image_dimensions[i][0] = fixed_imp.getWidth();
					fixed_index_image_dimensions[i][1] = fixed_imp.getHeight();
				}
				ROI_location = ROI_for_BF_templates(datasets.get(i)[k], datasets.get(i)[0],k,i);
				
				poresOnly_filenames[k] = pores_only_ROI(ROI_location, false);
				
				/** update the graphics panel. */
				IJ.log("poresOnly_filenames["+k+"] = "+poresOnly_filenames[k]);
				int last_index = poresOnly_filenames[k].lastIndexOf("\\");
				
				if(last_index == -1)
				{
					last_index = poresOnly_filenames[k].lastIndexOf("/");
				}
				else if(last_index < poresOnly_filenames[k].lastIndexOf("/"))
				{
					last_index = poresOnly_filenames[k].lastIndexOf("/");
				}
				
				CTC5Analysis.graphic_progress_update_frame.feature_analysis.UpdateProgress((( (double)i + ((double)k/(double)poresOnly_filenames.length) / (double) num_of_datasets) * 100.0) + 1, datasets.get(i)[0]+poresOnly_filenames[k].substring(last_index));
				new Thread(new Runnable(){
					public void run(){	
						CTC5Analysis.graphic_progress_update_frame.feature_analysis.repaint();
					}
				}).start();
				
				IJ.log("k ="+k);
			}
			
			IJ.log("poresOnly_filenames.length = "+poresOnly_filenames.length);
			/** add to a ROI_poresOnly version of the "CTC5Analysis.data.return_selected_fluorescent_dataset_list()" ArrayList. */
			CTC5Analysis.data.ROI_pores_only_dataset.add(poresOnly_filenames);
			
			/** update the graphics panel. */
			CTC5Analysis.graphic_progress_update_frame.feature_analysis.UpdateProgress((( (double)i + 1 / (double) num_of_datasets) * 100.0), datasets.get(i)[0]);
			new Thread(new Runnable(){
				public void run(){	
					CTC5Analysis.graphic_progress_update_frame.feature_analysis.repaint();
				}
			}).start();
			
		}
			
		/** passes the ROI_poresOnly datasets to EugenesAlgo
         *  which generates a set of Alignment Objects for all moving images in each data set */
		AlignmentObject[][] dataset_alignments = alignment_algorithm(CTC5Analysis.data.ROI_pores_only_dataset); 
			
		IJ.log("dataset_alignments.length = "+dataset_alignments.length);
		
		ImagePlus current_image_for_adjustment;
		for(int j=0; j<dataset_alignments.length;j++)
		{
			IJ.log("j = "+j);
			IJ.log("dataset_alignments[j].length = "+dataset_alignments[j].length);
			for(int t =0; t<dataset_alignments[j].length; t++)
			{
				for(int y = 0; y<dataset_alignments[j][t].moving_images.length; y++)
				{				
					IJ.log("t = "+t);
					/** produce the final fully processed and aligned slide:
					 *   1) identify the image pairs to be aligned. */
					IJ.log("selected_ndpi_files.get("+j+") ="+CTC5Analysis.data.selected_ndpi_files.get(j));
					String remove_extension = CTC5Analysis.data.selected_ndpi_files.get(j).substring(0, CTC5Analysis.data.selected_ndpi_files.get(j).lastIndexOf("."));
					if(t==0)
					{
						IJ.log("		Image to be adjusted = "+CTC5Analysis.data.initial_NDPI_directory.getPath()+"\\"+remove_extension+"\\Final Processed\\"+remove_extension+"-Giemsa.tif");
						current_image_for_adjustment = new ImagePlus(CTC5Analysis.data.initial_NDPI_directory.getPath()+"\\"+remove_extension+"\\Final Processed\\"+remove_extension+"-Giemsa.tif");
					}
					else
					{
						IJ.log("		Image to be adjusted = "+CTC5Analysis.data.initial_NDPI_directory.getPath()+"\\"+remove_extension+"\\Raw Fluorescence\\Stitched\\"+dataset_alignments[j][t].moving_images[y]+".tif");	   
						current_image_for_adjustment = new ImagePlus(CTC5Analysis.data.initial_NDPI_directory.getPath()+"\\"+remove_extension+"\\Raw Fluorescence\\Stitched\\"+dataset_alignments[j][t].moving_images[y]+".tif");
					}  
			
					/**   2) scale the moving image. */
					IJ.log("dataset_alignments.get("+j+")["+t+"].refined_scale = " +dataset_alignments[j][t].refined_scale);
					IJ.run(current_image_for_adjustment, "Size...", "width="+current_image_for_adjustment.getWidth()*dataset_alignments[j][t].refined_scale+" height="+current_image_for_adjustment.getHeight()*dataset_alignments[j][t].refined_scale+" constrain average interpolation=Bilinear");
				  
					
					/**   3) rotate the scaled image  */
					IJ.log("dataset_alignments.get("+j+")["+t+"].isTransformed = " +dataset_alignments[j][t].isTransformed);
					if(dataset_alignments[j][t].isTransformed)
					{
						IJ.run(current_image_for_adjustment, "Flip Horizontally", "");
					}
					else
					{
						IJ.log("     Image does not need to be flipped horizontaly.");
					}
					
					IJ.log("dataset_alignments.get("+j+")["+t+"].refined_rotation = " +dataset_alignments[j][t].refined_rotation);
					IJ.run(current_image_for_adjustment, "Rotate... ", "angle="+dataset_alignments[j][t].refined_rotation+" grid=1 interpolation=Bilinear");
				  
				/**   4) translate the scaled image. */
				IJ.log("fixed_index_image_dimensions["+j+"][0] = " +fixed_index_image_dimensions[j][0]);
				IJ.log("fixed_index_image_dimensions["+j+"][1] = " +fixed_index_image_dimensions[j][1]);
				
				IJ.run(current_image_for_adjustment, "Canvas Size...", "width="+fixed_index_image_dimensions[j][0]+" height="+fixed_index_image_dimensions[j][1]+" position=Top-Left");
				
				
				IJ.log("dataset_alignments.get("+j+")["+t+"].refinedTranslation[0] = " +dataset_alignments[j][t].refinedTranslation[0]);
				IJ.log("dataset_alignments.get("+j+")["+t+"].refinedTranslation[1] = " +dataset_alignments[j][t].refinedTranslation[1]);

				IJ.run(current_image_for_adjustment, "Translate...", "x="+dataset_alignments[j][t].refinedTranslation[0]+" y="+dataset_alignments[j][t].refinedTranslation[1]+" interpolation=None");
				
				
				if(t==0)
				{
					IJ.saveAs(current_image_for_adjustment, "Tiff", CTC5Analysis.data.initial_NDPI_directory.getPath()+"\\"+remove_extension+"\\Final Processed\\Adjusted\\"+current_image_for_adjustment.getTitle());
					
					/** this is an ndpi image so save to position 0 in CTC5Analysis.data.initial_NDPI_directory.getPath() */
					CTC5Analysis.data.final_adjusted_stitched_images[j][t] = CTC5Analysis.data.initial_NDPI_directory.getPath()+"\\"+remove_extension+"\\Final Processed\\Adjusted\\"+current_image_for_adjustment.getTitle();
				}
				else
				{
					IJ.saveAs(current_image_for_adjustment, "Tiff", CTC5Analysis.data.initial_NDPI_directory.getPath()+"\\"+remove_extension+"\\Raw Fluorescence\\Stitched\\adjusted_"+current_image_for_adjustment.getTitle());
					                                                					
					int index_of_channel = -1; 
					for(int ti = 0; ti<CTC5Analysis.data.channelID_BFtemplate_pairs.size(); ti++)
					{
						IJ.log("dataset_alignments["+j+"]["+t+"].moving_images["+y+"] = "+dataset_alignments[j][t].moving_images[y]);
						IJ.log("CTC5Analysis.data.channelID_BFtemplate_pairs.get("+ti+")["+0+"] = "+CTC5Analysis.data.channelID_BFtemplate_pairs.get(ti)[0]);
						
						if(dataset_alignments[j][t].moving_images[y].equals(CTC5Analysis.data.channelID_BFtemplate_pairs.get(ti)[0]))
						{
							index_of_channel = ti+1;
							IJ.log("index_of_channel = "+index_of_channel);
						}
					}
					/** what is the name of the channel */
					/** where is the list of the selected channel */
					/** the index_of_channel is the position of the name in the selected channel list */
					
					IJ.log("CTC5Analysis.data.final_adjusted_stitched_images["+j+"]["+index_of_channel+"] = "+CTC5Analysis.data.final_adjusted_stitched_images[j][index_of_channel]);
					IJ.log("CTC5Analysis.data.final_adjusted_stitched_images["+j+"]["+index_of_channel+"] = "+CTC5Analysis.data.initial_NDPI_directory.getPath()+"\\"+remove_extension+"\\Raw Fluorescence\\Stitched\\"+current_image_for_adjustment.getTitle());
					CTC5Analysis.data.final_adjusted_stitched_images[j][index_of_channel] = CTC5Analysis.data.initial_NDPI_directory.getPath()+"\\"+remove_extension+"\\Raw Fluorescence\\Stitched\\"+current_image_for_adjustment.getTitle();
				}
				
				  /**   5) crop to match the fixed image.*/  
				 
				  /**   7) combine each image with the appropriate counterstain. */
				  /**   8) stack all into a digital stack and save. */
				
                IJ.log(""); 
				}
            }
		}
		
		construct_digital_slide(CTC5Analysis.data.selected_ndpi_files); //ArrayList<String> CTC5Analysis.data.selected_ndpi_files
	}
	
	public String ROI_for_NDPI(String ndpi_name, int dataset_number)
	{
		IJ.log("ROI_for_NDPI(String ndpi_name, int dataset_number)");
		
		String removeExt = ndpi_name.substring(0, ndpi_name.lastIndexOf("."));
		
		/**
		 * Reopen the extracted and focused ndpi image to save the ROI for pore detection.
		 */
		ImagePlus imp = new ImagePlus(parent_file_location.getPath() + "\\" + removeExt + "\\Final Processed\\" + removeExt + "-Giemsa.tif");
	
		/**
		 *  Find the relative scale of the ROI to place on the focused ndpi image.
		 */
		int[] current_dims  = new int[2];
		
		current_dims[0] = imp.getWidth();
		current_dims[1] = imp.getHeight();
		
		/**
		 *  Make and set the new scaled ROI.
		 *  This can be done by taking advantage of the knowledge that the extracted_focused_ndpi file is 5 % bigger than the original ROI used for extraction.
		 */
		int x = (int)((current_dims[0]*0.025)+0.5);
	    int y = (int)((current_dims[1]*0.025)+0.5);
	    int w = (int)((current_dims[0]*0.95)+0.5);
	    int h = (int)((current_dims[1]*0.95)+0.5); /** 
	    											*  the cast (int) function does not round but rather drops everything after the decimal. 
	    											*  by adding 0.5 to every number it simulates rounding by the cast (int) function.  
	    											*/
	    
	    int[] temp_anchor = new int[2];
	    temp_anchor[0] = x;
	    temp_anchor[1] = y;
	    
	    double[] temp_center =  new double[2];
	    temp_center[0] = ((double)w)/2.0;
	    temp_center[1] = ((double)h)/2.0;
	    
	    original_image_dimensions[dataset_number][0] = current_dims;
	    anchor_coords[dataset_number][0] = temp_anchor;
	    center_coords[dataset_number][0] = temp_center;
	
	    for(int g = 0; g< center_coords[dataset_number][0].length; g++)
		{
			  IJ.log("(NDPI) center_coords["+dataset_number+"][0]["+g+"]= "+center_coords[dataset_number][0][g]);
		}
		
	    OvalRoi newROI = new OvalRoi(x, y, w, h);
	    imp.setRoi(newROI);
		
		/** 
	    *   Set the outside to background
	    */   
	    boolean isDark = measureMean(imp);
	    
	    if(isDark)
	    {
	    	IJ.setBackgroundColor(0, 0, 0);
	    }
	    else
	    {
	    	IJ.setBackgroundColor(255, 255, 255);
	    }
	    
	    IJ.run(imp, "Clear Outside", "");
	    imp = imp.crop();
	    
		/** 
		 * Save the output.
		 */
	    String save_path = parent_file_location.getPath() + "\\" + removeExt + "\\Raw Giemsa\\Giemsa_ROI";
		IJ.saveAsTiff(imp, save_path);
		
		/**
		 * Close image and clean.
		 */
		
		CTC5Analysis.closeAllImages();
		imp = null;
	    removeExt = null;
	    newROI = null;
	    System.gc();
	    
	    return save_path;
	}
	
	public ImagePlus return_BF_template(String BF_template_name, String ndpi_name)
	{
		IJ.log("// return_BF_template(String BF_template_name, String ndpi_name) //");
		ImagePlus BF_template = null;
		
		String removeExt	= ndpi_name.substring(0, ndpi_name.lastIndexOf("."));
		String file_path 	= parent_file_location.getPath()+"/"+removeExt+"/Raw Fluorescence/Stitched/";
		
		 /** ArrayList<String[]> channelID_BFtemplate_pairs; holds pairs that link each fluorescent channel to a stitching template. 
		   *  channelID_BFtemplate_pairs[0] = the_flour_channel_name, channelID_BFtemplate_pairs[1] = the_respective_BF_template
		   *  the primary channel will be the first match.
		   */
		   for(int i = 0; i<CTC5Analysis.data.channelID_BFtemplate_pairs.size(); i++)
		   {
				if(CTC5Analysis.data.channelID_BFtemplate_pairs.get(i)[1] == BF_template_name)
				{
					 IJ.log("//      BF_template path = "+file_path+CTC5Analysis.data.channelID_BFtemplate_pairs.get(i)[1]+".tif");
					 BF_template    = new ImagePlus(file_path+CTC5Analysis.data.channelID_BFtemplate_pairs.get(i)[1]+".tif");
					
				     i = CTC5Analysis.data.channelID_BFtemplate_pairs.size()+1; /** End the loop */
				}
			}
		
		IJ.log("//      BF_template = "+BF_template.toString());   
		IJ.log("// return_BF_template(String BF_template_name, String ndpi_name) //");
		return BF_template;
	}
	
	public String ROI_for_BF_templates(String BF_template_name, String ndpi_name, int BF_template_index, int dataset_number) throws InterruptedException
	{
		IJ.log("ROI_for_BF_templates(String BF_template_name, String ndpi_name)");
		
		String save_path;
		String removeExt		= ndpi_name.substring(0, ndpi_name.lastIndexOf("."));
		String file_path 		= parent_file_location.getPath()+"/"+removeExt+"/Raw Fluorescence/Stitched/";
		String fluor_file_path 	= "";
		String BF_file_path 	= "";
		
		 /** ArrayList<String[]> channelID_BFtemplate_pairs; holds pairs that link each fluorescent channel to a stitching template. 
		  *  channelID_BFtemplate_pairs[0] = the_flour_channel_name, channelID_BFtemplate_pairs[1] = the_respective_BF_template
		  *  the primary channel will be the first match.
		  */
		for(int i = 0; i<CTC5Analysis.data.channelID_BFtemplate_pairs.size(); i++)
		{
			if(CTC5Analysis.data.channelID_BFtemplate_pairs.get(i)[1] == BF_template_name)
			{
			    fluor_file_path = file_path+CTC5Analysis.data.channelID_BFtemplate_pairs.get(i)[0];
			    BF_file_path    = file_path+CTC5Analysis.data.channelID_BFtemplate_pairs.get(i)[1]+".tif";
			
				i = CTC5Analysis.data.channelID_BFtemplate_pairs.size()+1; /** End the loop */
			}
		}
		
		String flour_pores = pores_only_ROI(fluor_file_path,false);
		
		ImagePlus imp = new ImagePlus(flour_pores);
			
		configure_for_analyse_particles(imp);
		IJ.run(imp, "Set Measurements...", "area centroid redirect=None decimal=3");

		RunProcess analyze_three = new RunProcess(imp, "Analyze Particles...", "circularity=0.0-1.00 show=Nothing display exclude clear include");
		Thread analyze =  new Thread(analyze_three);
		analyze.start();
	    
		try
	    {
			analyze.join();
		} 
		catch (InterruptedException e) 
		{
			StackTraceElement[] ste = e.getStackTrace();
			
			for(int i = 0; i < ste.length; i++)
			{
				IJ.log(ste[i].toString());
			}
		}
		
		IJ.log("Analyzing_3");
		
		ResultsTable results = ResultsTable.getResultsTable();
		
		//This for loop is trying to debug issues with the Results table.
		String[] headings = results.getHeadings();
		IJ.log("Results has "+headings.length+" columns.");
		for(int i = 0; i<headings.length; i++)
		{
			IJ.log("Results ("+headings[i]+") has "+results.getColumnAsDoubles(results.getColumnIndex(headings[i])).length+" data points.");
		}
		
		
		int x_index 	= results.getColumnIndex("X");
		int y_index 	= results.getColumnIndex("Y");
		
		double[] pores_x = results.getColumnAsDoubles(x_index);
		double[] pores_y = results.getColumnAsDoubles(y_index);
		
		int dim = pores_x.length;
		int n_bins = dim/30;
		double cutoff = (double)n_bins/(double)100.0;
		IJ.log("dim = "+dim);
		IJ.log("n_bins = "+n_bins);
		IJ.log("n_bins = "+n_bins);
		
		int[] bins_x = new int[n_bins+1];
		int[] bins_y = new int[n_bins+1];
		
		double max_x = maxDouble(pores_x);
		double max_y = maxDouble(pores_y);
		
		for(int b = 0; b<bins_x.length; b++)
		{
			bins_x[b] = (int) (b*(max_x/n_bins));
			bins_y[b] = (int) (b*(max_y/n_bins));
		}
		
		freq_table_x = new int[n_bins];
		freq_table_y = new int[n_bins];
		
		populate_freq_table(bins_x, bins_y, pores_x, pores_y);
		
		double[] freq_table_x_smooth = smooth(freq_table_x);
		double[] freq_table_y_smooth = smooth(freq_table_y);
		
		for(int t = 0; t<bins_x.length; t++)
		{
			IJ.log("bins_x["+t+"]: "+bins_x[t]);
			IJ.log("bins_y["+t+"]: "+bins_y[t]);
		}
		
		for(int t = 0; t<freq_table_x.length; t++)
		{
			IJ.log("freq_table: ("+freq_table_x[t]+", "+freq_table_y[t]+")");
		}
		
		for(int t = 0; t<freq_table_x_smooth.length; t++)
		{
			IJ.log("smooth_table: ("+freq_table_x_smooth[t]+", "+freq_table_y_smooth[t]+")");
		}
		
		double[] makeOval = new double[4]; /** stores values for IJ.makeOval(x,y,width,height) */
		
		for(int x = 0; x<freq_table_x_smooth.length; x++)
		{
			if(freq_table_x_smooth[x]>=cutoff)
			{
				IJ.log("[x] Cutoff = "+cutoff+"; freq_table_x_smooth["+x+"]"+freq_table_x_smooth[x]);
				
				makeOval[0] = (bins_x[x]+bins_x[x+1])/2;
				x=freq_table_x_smooth.length;
			}
		}
		for(int y = 0; y<freq_table_y_smooth.length; y++)
		{
			if(freq_table_y_smooth[y]>=cutoff)
			{
				IJ.log("[y] Cutoff = "+cutoff+"; freq_table_y_smooth["+y+"]"+freq_table_y_smooth[y]);
				
				makeOval[1] = (bins_y[y]+bins_y[y+1])/2;
				y=freq_table_y_smooth.length;
			}
		}
		for(int w = freq_table_x_smooth.length-1; w>=0; w--)
		{
			
			
			if(freq_table_x_smooth[w]>=cutoff)
			{
				IJ.log("[w] Cutoff = "+cutoff+"; freq_table_x_smooth["+w+"]"+freq_table_x_smooth[w]);
				
				makeOval[2] = ((bins_x[w]+bins_x[w+1])/2)-makeOval[0];
				w = -1;
			}
		}
		for(int h = freq_table_y_smooth.length-1; h>=0; h--)
		{
			
			
			if(freq_table_y_smooth[h]>=cutoff)
			{
				IJ.log("[h] Cutoff = "+cutoff+"; freq_table_y_smooth["+h+"]"+freq_table_y_smooth[h]);
				
				makeOval[3] = ((bins_y[h]+bins_y[h+1])/2)-makeOval[1];
				h = -1;
			}
		}

		 int[] temp_anchor = new int[2];
		 temp_anchor[0] = (int) (makeOval[0]+0.5);
		 temp_anchor[1] = (int) (makeOval[1]+0.5); /** 
													*  the cast (int) function does not round but rather drops everything after the decimal. 
													*  by adding 0.5 to every number it simulates rounding by the cast (int) function.  
													*/
		 
		 double[] temp_center = new double[2];
		 temp_center[0] = ((double)makeOval[2])/2.0;
		 temp_center[1] = ((double)makeOval[3])/2.0;
		 
		 anchor_coords[dataset_number][BF_template_index] = temp_anchor;
		 center_coords[dataset_number][BF_template_index] = temp_center;   
		
		for(int g = 0; g< center_coords[dataset_number][BF_template_index].length; g++)
		{
			  IJ.log("(BF) center_coords["+dataset_number+"]["+BF_template_index+"] ["+g+"]= "+center_coords[dataset_number][BF_template_index][g]);
		}
		
		OvalRoi newROI = new OvalRoi(makeOval[0], makeOval[1], makeOval[2], makeOval[3]);
	    IJ.log("OvalRoi newROI = new OvalRoi("+makeOval[0]+", "+makeOval[1]+", "+makeOval[2]+", "+makeOval[3]+");");
		
	    CTC5Analysis.closeAllImages();
		imp = new ImagePlus(BF_file_path);
	    imp.setRoi(newROI);
	    
	    int[] current_dims =  new int[2];
	    current_dims[0] = imp.getWidth();
	    current_dims[1] = imp.getHeight();
	    
	    original_image_dimensions[dataset_number][BF_template_index] = current_dims;
	    
	    IJ.setBackgroundColor(0, 0, 0);
	    IJ.setForegroundColor(0, 0, 0);
	    ImageProcessor ip = imp.getProcessor();
	    ip.fillOutside(imp.getRoi());
	    imp.setProcessor(ip);
	    imp = imp.crop();
		
		save_path = parent_file_location.getPath()+"/"+removeExt+"/Raw Fluorescence/Stitched/"+BF_template_name+"_ROI";
		IJ.saveAsTiff(imp, save_path);
		
		return save_path;
	}
	
	public String pores_only_ROI(String file, boolean isNDPI_image) throws InterruptedException
	{
		/** open the ROI_file. */
		ImagePlus imp = new ImagePlus(file + ".tif");
		
		IJ.log("pores_only_ROI(String file, boolean isNDPI_image) file = "+imp.getTitle());
		if(isNDPI_image)
		{
			IJ.log("isNDPI_image = TRUE");
		}
		else
		{
			IJ.log("isNDPI_image = FALSE");
		}
		
		/** 
		 * Set the threshold. 
		 * If it is an NDPI image use "Default" threshold settings
		 * If it is a fluorescent image invert the image (particles need to be black to "Analyze Particles...") and use "Triangle" threshold settings. 
		 */
		
		new ImageConverter(imp).convertToGray8();
		IJ.run(imp, "Despeckle", "");
		
		if(isNDPI_image)
		{ 
			IJ.run(imp, "Auto Threshold", "method=Moments ignore_black ignore_white white");
			//Prefs.set("blackBackground", ""+false);
			IJ.run(imp, "Make Binary","");
		}
		else
		{
			IJ.run(imp, "Auto Threshold", "method=Moments ignore_black ignore_white white"); 
			//Prefs.set("blackBackground", ""+false);
			IJ.run(imp, "Make Binary","");
		}
		
		/** 
		 * "Analyze Particles..." and identify pores:
		 * 	1) "Analyze Particles..." from 0-Infinity; circularity 0.7-1.00.
		 * 		1.1) Including holes can sometimes lead to poor results in dimly lit BF images. As such, holes will be excluded on first pass and a Mask of Mask 
		 * 			 approach will be used as a more robust method of obtaining 'pores only images' with holes included.
		 *  2) Remove the small noise/debris by getting rid of the smallest particles so long as they continue to decrease in frequency. CALL THIS temp_minPore
		 *  3) "Analyze Particles..." from temp_minPore-Infinity; circularity 0.7-1.00.
		 *  4) Remove extreme large particles by only selecting the smallest 95 % of particles. CALL THIS temp_maxPore
		 *  5) "Analyze Particles..." from temp_minPore-temp_maxPore; circularity 0.7-1.00.
		 *  6) Identify the median and SD. Use +/- 0.75*SD to set a strict criteria for pores.
		 */
		
		/* STEP 1 */
		boolean inverted_one = configure_for_analyse_particles(imp);
		IJ.run(imp, "Set Measurements...", "area centroid redirect=None decimal=3");
		RunProcess analyze_one = new RunProcess(imp, "Analyze Particles...", "  circularity=0.70-1.00 display exclude clear");
		Thread analyze =  new Thread(analyze_one);
		analyze.start();
	    analyze.join();
		
		IJ.log("Analyzing_1");

		ResultsTable results = ResultsTable.getResultsTable();
		
		//This for loop is trying to debug issues with the Results table.
		String[] headings = results.getHeadings();
		IJ.log("Results has "+headings.length+" columns.");
		for(int i = 0; i<headings.length; i++)
		{
			IJ.log("Results ("+headings[i]+") has "+results.getColumnAsDoubles(results.getColumnIndex(headings[i])).length+" data points.");
		}
		//End of resultsTable debug
		
		int area = results.getColumnIndex("Area");
		double[] areas = results.getColumnAsDoubles(area);
		
		/* STEP 2 */
		Arrays.sort(areas);
		int[] frequencies_areas = all_frequencies(areas);
		double[] d_freq_areas = smooth(frequencies_areas);
		d_freq_areas = smooth(d_freq_areas);
		
		int temp_minPore = end_of_noise(d_freq_areas);
		IJ.log("("+file+") temp_minPore = "+temp_minPore);

		/* STEP 3 */
		ArrayList<Double> areas_two = new ArrayList<Double>();

		for(int t = 0; t<areas.length; t++)
		{
			if(areas[t]>temp_minPore)
			{
				areas_two.add(areas[t]);
			}
			
		}
		
		areas = new double[areas_two.size()];
		for(int i = 0; i<areas.length; i ++)
		{
			areas[i] = areas_two.get(i);
		}
		
		int temp_maxPore = removeLargeOutliers(areas);
		IJ.log("("+file+") temp_maxPore = "+temp_maxPore);
		
		int minPore = temp_minPore;
		int maxPore = temp_maxPore;
		
		boolean inverted_two = configure_for_analyse_particles(imp);
		IJ.run(imp, "Set Measurements...", "area centroid redirect=None decimal=3");
		
		RunProcess analyze_two = new RunProcess(imp, "Analyze Particles...", "size="+minPore+"-"+maxPore+" circularity=0.70-1.00 show=Masks display exclude clear include");
		analyze =  new Thread(analyze_two);
		analyze.start();
	    analyze.join();
		
		IJ.log("("+file+") IJ.run(Analyze Particles..., size="+minPore+"-"+maxPore+" circularity=0.70-1.00 show=Masks display exclude clear include);");
		IJ.log("("+file+") Analyzing_2");	
		
		/** Select the Mask of window. */
		imp = WindowManager.getImage("Mask of "+file+".tif");
		
		/** analyse particles again on this mask - to include holes for pores only image */
		IJ.run(imp, "Set Measurements...", "area centroid redirect=None decimal=3");
		
		RunProcess analyze_three = new RunProcess(imp, "Analyze Particles...", "circularity=0.70-1.00 show=Masks display exclude clear include");
		analyze =  new Thread(analyze_three);
		analyze.start();
	    analyze.join();
	    
	    
	    /** Reversing the LUT inversion so the image will have the correct LUT when finally collated */
		if(inverted_two)
		{
			IJ.run(imp, "Invert", "");
			
			if(inverted_one)
			{
				IJ.run(imp, "Invert", "");
			}
		}
		else if(inverted_one)
		{
			IJ.run(imp, "Invert", "");
		}
	    
	    
	    /** Select the 'Mask of Mask of' window. */
		imp = WindowManager.getImage("Mask of Mask of "+file+".tif");
	    
		IJ.log("SAVING: "+file+"_poresOnly");
		/** save to disk and store string location. */
		String save_path = file+"_poresOnly";
		IJ.saveAsTiff(imp, save_path);
		
		CTC5Analysis.closeAllImages();
		
		String full_path =  save_path+".tif";
		
		return full_path;
	}
	
	public int frequency(double[] d_array, double q)
	{	
	    int frequency = 0;
	      
	    for(int i=0; i < d_array.length; i++)
	    {
	    	if(q == d_array[i])
	    	{
	    	  frequency++;
	    	}
	    }
	      
	    return frequency;
	}
	
	public int removeLargeOutliers(double[] area_array)
	{
		IJ.log("removeLargeOutliers(double[] area_array)");
		
		Arrays.sort(area_array);
		
		
		int threshold_index = (int) ((area_array.length*0.98)+0.5);
		int outlier_threshold = (int) ((area_array[threshold_index])+0.5);
		IJ.log("threshold_index = "+threshold_index);
		IJ.log("outlier_threshold = "+outlier_threshold);
		
		return outlier_threshold;
	}
	
	public int median(double[] d_array)
	{
		IJ.log("median(double[] d_array)");
		int med;
		
		Arrays.sort(d_array);
		med =  (int) d_array[d_array.length/2];
		
		return med;
	}
	
	public int sd(double[] d_array)
	{
		IJ.log("sd(double[] d_array)");
		
		double sum_distances = 0.0;
		double sum = 0.0;
		int length = d_array.length;
		
		for(int i = 0; i<length; i++)
		{
			sum = sum + d_array[i];
		}
		
		double mean = sum/length;
		
		for(int k = 0; k<length; k++)
		{
			sum_distances = sum_distances + Math.pow(d_array[k]-mean, 2);
		}
		
		double variance =  sum_distances/length;
		double sd = Math.pow(variance, 0.5);
		int stdev = (int)sd;
		
		return stdev;
	}
	
	public void populate_freq_table(int[] bin_x, int[] bin_y, double[] x, double[] y)
	{
		int length = x.length;
		
		for(int i = 0; i<length; i++)
		{
			for(int b = 0; b<bin_x.length-1; b++)
			{		
				if(x[i]>bin_x[b] && x[i]<=bin_x[b+1])
				{
					freq_table_x[b]++;
				}
				if(y[i]>bin_y[b] && y[i]<=bin_y[b+1])
				{
					freq_table_y[b]++;
				}
			}
		}
	}
	
	/** this method smoothes frequency data by replacing bin frequency with the mean of a sliding window of width five */
	public double[] smooth(int[] input_array)
	{	
		double[] d_input_array = new double[input_array.length];
		for(int t = 0; t < d_input_array.length; t++ )
		{
			d_input_array[t] = (double) input_array[t]; 
		}
		
		double[] output = new double[input_array.length];
		
		for(int i = 0; i<d_input_array.length; i++)
		{			
			if(i<3)
			{
				output[i] = (d_input_array[0]+d_input_array[1]+d_input_array[2]+d_input_array[3]+d_input_array[4])/5.00;
			}
			else if((i+2)>=input_array.length-1)
			{
				output[i] = (d_input_array[d_input_array.length-5]+d_input_array[d_input_array.length-4]+d_input_array[d_input_array.length-3]+d_input_array[d_input_array.length-2]+d_input_array[d_input_array.length-1])/5.00;
			}
			else
			{
				output[i] = (d_input_array[i-2]+d_input_array[i-1]+d_input_array[i]+d_input_array[i+1]+d_input_array[i+2])/5.00;
			}
		}
		return output;
	}
	
	public double[] smooth(double[] input_array)
	{		
		double[] output = new double[input_array.length];
		
		for(int i = 0; i<input_array.length; i++)
		{			
			if(i<3)
			{
				output[i] = (input_array[0]+input_array[1]+input_array[2]+input_array[3]+input_array[4])/5.00;
			}
			else if((i+2)>=input_array.length-1)
			{
				output[i] = (input_array[input_array.length-5]+input_array[input_array.length-4]+input_array[input_array.length-3]+input_array[input_array.length-2]+input_array[input_array.length-1])/5.00;
			}
			else
			{
				output[i] = (input_array[i-2]+input_array[i-1]+input_array[i]+input_array[i+1]+input_array[i+2])/5.00;
			}
		}
		
		return output;
	}
	
	public double maxDouble(double[] d){
		
		double max = d[0];
		
		for(int i = 0; i<d.length; i++)
		{
			if(d[i]>max)
			{
				max = d[i];
			}
		}
		
		return max;
	}
	
	public AlignmentObject[][] alignment_algorithm(ArrayList<String[]> als)
	{
		IJ.log("Starting Eugene's Algorithm");
		
		/** holds the initial pass at aligning the images.
		 *  this gets passed through affineTransform() one 
		 *  last time before generating the final alignmentObject (described below)   */
		AlignmentObject temp_alignment;
		double additional_scale; /** used to tweak the the scale of the temp_alignment */
		double additional_rotation; /** used to tweak the rotation of the temp_alignment */
		
		/** holds a list of objects containing scale, rotation and translation information */
		AlignmentObject[][] all_alignments = new AlignmentObject[als.size()][]; 
		
		double rotationStep = 9.0; 
		
		/** rotate to +/- 180 */
		int number_of_steps = (int)(180.0/rotationStep);
		
		IJ.log("als.size() = "+als.size());
		
		for(int d = 0; d<als.size(); d++)
		{
			/** update the graphics panel. */
			if(d == 0)
			{
				CTC5Analysis.graphic_progress_update_frame.ScaleAndAlign.UpdateProgress(1, als.get(d)[0]+"- Giemsa");
			}
			else
			{
				CTC5Analysis.graphic_progress_update_frame.ScaleAndAlign.UpdateProgress((((double)d/(double)als.size())*100.0)+1, als.get(d)[0]+"- Giemsa");
			}
			
			new Thread(new Runnable(){
				public void run(){	
					CTC5Analysis.graphic_progress_update_frame.ScaleAndAlign.repaint();
				}
			}).start();
			
			/** the number of images that need to be aligned to 'fixed' is set up using int numOfMoving */
			int numOfMoving = als.get(d).length-1;
			all_alignments[d] = new AlignmentObject[numOfMoving];
			
			for(int test = 0; test< als.get(d).length; test++)
			{
				IJ.log("als.get("+d+")["+test+"] = "+als.get(d)[test]);
			}
			
			double[] areaScoreTracker =  new double[2];
			for(int a = 0; a< areaScoreTracker.length; a++)
			{
				/** 
				 * set the default areaScore to 1.0
				 * less than 0.75 will be considered aligned. 
				 */
				
				//TODO: might also be worth including a criteria that the refined rotation cannot be greater than step distance. Occasionally, a large rotation can get a crude match that is close but not quite perfect. A refined rotation limit may generate more stable alignments.
				
				areaScoreTracker[a] = 1.0; 
			}	
			
			IJ.log("fixed_index = "+fixed_index+"; Stepping rotation = "+rotationStep+"°; number_of_steps = "+number_of_steps+"; numOfMoving ="+numOfMoving);
			
			/** Set up all the ImagePlus objects that will be required. */
			ImagePlus fixed = new ImagePlus(als.get(d)[fixed_index]);
			ImagePlus output;
	        ImagePlus[] moving =  new ImagePlus[numOfMoving];
			ImagePlus[][] movingTransformations =  new ImagePlus[numOfMoving][4];
			int[] moving_index_tracker =  new int[numOfMoving];
			
			double fixed_area = fixed.getWidth()*fixed.getHeight();
			IJ.log("fixed.getWidth() = " +fixed.getWidth());
			IJ.log("fixed.getHeight() = " +fixed.getHeight());
			IJ.log("fixed_area = " +fixed_area);
			double[] scale_factor = new double[numOfMoving];
			int moving_counter = 0;
			
			IJ.log("als.get("+d+").length = "+als.get(d).length);
			
			for(int m = 0; m < als.get(d).length; m++)
			{
				if(m != fixed_index)
				{
					CTC5Analysis.graphic_progress_update_frame.ScaleAndAlign.UpdateProgress((((double)d+((double)moving_counter/((double)als.get(d).length-1))/(double)als.size())*100.0)+1, als.get(d)[0]+"- Giemsa");
					new Thread(new Runnable()
					{
						public void run()
						{	
							CTC5Analysis.graphic_progress_update_frame.ScaleAndAlign.repaint();
						}
					}).start();
					
					IJ.log("m = "+m+"; moving_counter = "+moving_counter);
					moving[moving_counter] = new ImagePlus(als.get(d)[m]);
					double moving_area = moving[moving_counter].getWidth()*moving[moving_counter].getHeight();
					IJ.log("moving_area = "+moving_area);
					scale_factor[moving_counter] = Math.sqrt(fixed_area/moving_area);
					IJ.log("scale_factor["+moving_counter+"] = "+scale_factor[moving_counter]);
					
					if(fixed_area > moving_area)
					{
						int width  = (int) Math.round(fixed.getWidth()/scale_factor[moving_counter]);
						int height = (int) Math.round(fixed.getHeight()/scale_factor[moving_counter]);
						IJ.run(fixed, "Size...", "width="+width+" height="+height+" constrain average interpolation=Bilinear");
					}
					else
					{
						int width  = (int) Math.round(moving[moving_counter].getWidth()*scale_factor[moving_counter]);
						int height = (int) Math.round(moving[moving_counter].getHeight()*scale_factor[moving_counter]);
						IJ.run(moving[moving_counter], "Size...", "width="+width+" height="+height+" constrain average interpolation=Bilinear");
					}
				
	                movingTransformations[moving_counter][0] = new ImagePlus("posRotation", moving[moving_counter].getProcessor().duplicate());
	                movingTransformations[moving_counter][0].show();
	                IJ.log("first: movingTransformations["+moving_counter+"][0] = "+movingTransformations[moving_counter][0].toString());
	                movingTransformations[moving_counter][1] = new ImagePlus("negRotation", moving[moving_counter].getProcessor().duplicate());
	                movingTransformations[moving_counter][1].show();
	                movingTransformations[moving_counter][2] = new ImagePlus("posRotation_t", moving[moving_counter].getProcessor().duplicate());
	                movingTransformations[moving_counter][2].getProcessor().flipHorizontal();
	                movingTransformations[moving_counter][2].updateAndRepaintWindow();
	                movingTransformations[moving_counter][2].show();
	                movingTransformations[moving_counter][3] = new ImagePlus("negRotation_t", moving[moving_counter].getProcessor().duplicate());
	                movingTransformations[moving_counter][3].getProcessor().flipHorizontal();
	                movingTransformations[moving_counter][3].updateAndRepaintWindow();	
	                movingTransformations[moving_counter][3].show();
	                
	                CTC5Analysis.hideAllImages();	
	                moving_index_tracker[moving_counter] = m;
	                moving_counter++;
				}
			}
			
			for(int mov =0; mov  <moving.length; mov++)
			{
				IJ.log("mov = "+mov);
				int moving_index;
				
				if(mov >= fixed_index)
				{
					moving_index = mov+1;
				}
				else
				{
					moving_index = mov;
				}
			    IJ.log("moving["+mov+"] dims = ("+moving[mov].getWidth()+", "+moving[mov].getHeight()+")");
			    IJ.log("fixed dims = ("+fixed.getWidth()+", "+fixed.getHeight()+")");
	            double originalArea = calculateArea(moving[mov], fixed, mov,-1);
	            IJ.log("originalArea = " + originalArea);
			    double area;
			    double tempScore;
			    double areaScore = 1.0;
			   
			    rotation_steps_loop:
			    for(int i = 0; i<=number_of_steps; i++)
			    {
			    	if(i==0)
			    	{
			    		IJ.log("0° rotation - affine");
			    		IJ.log("second: movingTransformations["+mov+"]["+0+"] = "+movingTransformations[mov][0].toString());
			    		IJ.log("fixed = "+fixed.toString());
			    		fixed.show();
			    		output = affineTransform(fixed, movingTransformations[mov][0], 0.0);
			    		IJ.log("affine output = " +output.toString());
			    		output.show();
			    		area = calculateArea(output, fixed, mov,i);
			    		IJ.log("area = "+ area);
			    	    tempScore = area/originalArea;
			    	    IJ.log("tempScore = "+ tempScore);
		    			IJ.log("areaScore = "+ areaScore);
			    	    
			    	    if(tempScore < areaScore)
			    	    {
			    	    	areaScore = tempScore;
			    	    	IJ.log("TempScore was less than areaScore and areaScore has been replaced.");
			    	    }
			    	    /** this needs to be reset for each moving template */
			    	    
			    	    if(areaScore <= 0.85)
			    	    {
			    	    	IJ.log("AreaScore is below the threshold and Escape Has initialised.");
			    	    	
			    	    	areaScoreTracker[mov] = areaScore;
			    	    	if(mov == 0)
			    	    	{
			    	    		temp_alignment = new AlignmentObject("Giemsa", original_image_dimensions[d][moving_index], false, scale_factor[mov], 0.0, mX, mY, anchor_coords[d][mov], fX, fY, anchor_coords[d][fixed_index], center_coords[d][moving_index],new double[]{0.0,0.0},true); 
			    	    		
			    	    		additional_scale = temp_alignment.refined_scale / scale_factor[mov];
			    	    		additional_rotation = temp_alignment.refined_rotation - temp_alignment.rough_rotation;
			    	    		
			    	    		
			    	    		IJ.log("Refined Affine: ");
			    	    		IJ.log("scale_factor["+mov+"] = "+scale_factor[mov]);
			    	    		IJ.log("initial rotation = 0.0");
			    	    		IJ.log("additional_scale = "+additional_scale);
			    	    		IJ.log("additional_rotation = "+additional_rotation);
			    	    		IJ.log("temp_alignment.primary_Translation[0] = "+temp_alignment.primary_translation[0]);
			    	    		IJ.log("temp_alignment.primary_Translation[1] = "+temp_alignment.primary_translation[1]);
			    	    					    	    		
			    	    		refined_AffineTransform(fixed, movingTransformations[mov][0], additional_rotation, additional_scale, temp_alignment.primary_translation);
			    	    		all_alignments[d][mov] = new AlignmentObject("Giemsa", original_image_dimensions[d][moving_index], false, temp_alignment.refined_scale, temp_alignment.refined_rotation, mX, mY, anchor_coords[d][mov], fX, fY, anchor_coords[d][fixed_index], center_coords[d][moving_index],temp_alignment.primary_translation, false);		    	    						
			    	    	
			    	    		IJ.log("final scale: all_alignments["+d+"]["+mov+"].refined_scale = "+all_alignments[d][mov].refined_scale);
			    	    		IJ.log("final rotation: all_alignments["+d+"]["+mov+"].refined_rotation = "+all_alignments[d][mov].refined_rotation);
			    	    	}
			    	    	else
			    	    	{
			    	    		temp_alignment = new AlignmentObject(get_template_name(als.get(d)[moving_index]), original_image_dimensions[d][moving_index], false, scale_factor[mov], 0.0, mX, mY, anchor_coords[d][mov], fX, fY, anchor_coords[d][fixed_index], center_coords[d][moving_index], new double[]{0.0,0.0},true); 
			    	    		
			    	    		additional_scale = temp_alignment.refined_scale / scale_factor[mov];
			    	    		additional_rotation = temp_alignment.refined_rotation - temp_alignment.rough_rotation;
			    	    		
			    	    		IJ.log("Refined Affine: ");
			    	    		IJ.log("scale_factor["+mov+"] = "+scale_factor[mov]);
			    	    		IJ.log("initial rotation = 0.0");
			    	    		IJ.log("additional_scale = "+additional_scale);
			    	    		IJ.log("additional_rotation = "+additional_rotation);
			    	    		IJ.log("temp_alignment.primary_Translation[0] = "+temp_alignment.primary_translation[0]);
			    	    		IJ.log("temp_alignment.primary_Translation[1] = "+temp_alignment.primary_translation[1]);
			    	    					    	    		
			    	    		refined_AffineTransform(fixed, movingTransformations[mov][0], additional_rotation, additional_scale, temp_alignment.primary_translation);
			    	    		all_alignments[d][mov] = new AlignmentObject(get_template_name(als.get(d)[moving_index]), original_image_dimensions[d][moving_index], false, temp_alignment.refined_scale, temp_alignment.refined_rotation, mX, mY, anchor_coords[d][moving_index], fX, fY, anchor_coords[d][fixed_index], center_coords[d][moving_index], temp_alignment.primary_translation, false);	    	
			    	    	
			    	    		IJ.log("final scale: all_alignments["+d+"]["+mov+"].refined_scale = "+all_alignments[d][mov].refined_scale);
			    	    		IJ.log("final rotation: all_alignments["+d+"]["+mov+"].refined_rotation = "+all_alignments[d][mov].refined_rotation);
			    	    	}
			    	    	IJ.log("areaScore = "+ areaScore);
			    	    	break rotation_steps_loop;
			    	    }
						
					    /** TRANSFORM NO ROTATION */
					    IJ.log("0° rotation (transformed) - affine");
			            output = affineTransform(fixed, movingTransformations[mov][2], 0.0);
			    	    area = calculateArea(output, fixed, mov, i);
			    	    IJ.log("area = "+ area);
			    	    tempScore = area/originalArea;
			    	    IJ.log("tempScore = "+ tempScore);
		    			IJ.log("areaScore = "+ areaScore);
			    	    
			    	    if(tempScore < areaScore)
			    	    {
			    	    	areaScore = tempScore;
			    	    }
			    	    
			    	    IJ.log("## Testing areaScore.... current areaScore = " + areaScore);
			    	    if(areaScore <= 0.85)
			    	    {
			    	    	areaScoreTracker[mov] = areaScore;
			    	    	
			    	    	if(mov == 0)
			    	    	{	
			    	    		temp_alignment = new AlignmentObject("Giemsa", original_image_dimensions[d][moving_index], true, scale_factor[mov], 0.0, mX, mY, anchor_coords[d][mov], fX, fY, anchor_coords[d][fixed_index], center_coords[d][moving_index], new double[]{0.0,0.0},true); 
			    	    		
			    	    		additional_scale = temp_alignment.refined_scale / scale_factor[mov];
			    	    		additional_rotation = temp_alignment.refined_rotation - temp_alignment.rough_rotation;
			    	    		
			    	    		IJ.log("Refined Affine: ");
			    	    		IJ.log("scale_factor["+mov+"] = "+scale_factor[mov]);
			    	    		IJ.log("initial rotation = 0.0");
			    	    		IJ.log("additional_scale = "+additional_scale);
			    	    		IJ.log("additional_rotation = "+additional_rotation);
			    	    		IJ.log("temp_alignment.primary_Translation[0] = "+temp_alignment.primary_translation[0]);
			    	    		IJ.log("temp_alignment.primary_Translation[1] = "+temp_alignment.primary_translation[1]);
			    	    					    	    	
			    	    		refined_AffineTransform(fixed, movingTransformations[mov][2], additional_rotation, additional_scale, temp_alignment.primary_translation);
			    	    		all_alignments[d][mov] = new AlignmentObject("Giemsa", original_image_dimensions[d][moving_index], true, temp_alignment.refined_scale, temp_alignment.refined_rotation, mX, mY, anchor_coords[d][mov], fX, fY, anchor_coords[d][fixed_index], center_coords[d][moving_index], temp_alignment.primary_translation, false);	
			    	    	
			    	    		IJ.log("final scale: all_alignments["+d+"]["+mov+"].refined_scale = "+all_alignments[d][mov].refined_scale);
			    	    		IJ.log("final rotation: all_alignments["+d+"]["+mov+"].refined_rotation = "+all_alignments[d][mov].refined_rotation);
			    	    	}
			    	    	else
			    	    	{
			    	    		temp_alignment = new AlignmentObject(get_template_name(als.get(d)[moving_index]), original_image_dimensions[d][moving_index], true, scale_factor[mov], 0.0, mX, mY, anchor_coords[d][mov], fX, fY, anchor_coords[d][fixed_index], center_coords[d][moving_index], new double[]{0.0,0.0},true); 
			    	    		
			    	    		additional_scale = temp_alignment.refined_scale / scale_factor[mov];
			    	    		additional_rotation = temp_alignment.refined_rotation - temp_alignment.rough_rotation;
			    	    		
			    	    		IJ.log("Refined Affine: ");
			    	    		IJ.log("scale_factor["+mov+"] = "+scale_factor[mov]);
			    	    		IJ.log("initial rotation = 0.0");
			    	    		IJ.log("additional_scale = "+additional_scale);
			    	    		IJ.log("additional_rotation = "+additional_rotation);
			    	    		IJ.log("temp_alignment.primary_Translation[0] = "+temp_alignment.primary_translation[0]);
			    	    		IJ.log("temp_alignment.primary_Translation[1] = "+temp_alignment.primary_translation[1]);
			    	    					    	    		
			    	    		refined_AffineTransform(fixed, movingTransformations[mov][2], additional_rotation, additional_scale, temp_alignment.primary_translation);
			    	    		all_alignments[d][mov] = new AlignmentObject(get_template_name(als.get(d)[moving_index]),  original_image_dimensions[d][moving_index], true, temp_alignment.refined_scale, temp_alignment.refined_rotation, mX, mY, anchor_coords[d][moving_index], fX, fY, anchor_coords[d][fixed_index], center_coords[d][moving_index], temp_alignment.primary_translation, false);
				    	    
			    	    		IJ.log("final scale: all_alignments["+d+"]["+mov+"].refined_scale = "+all_alignments[d][mov].refined_scale);
			    	    		IJ.log("final rotation: all_alignments["+d+"]["+mov+"].refined_rotation = "+all_alignments[d][mov].refined_rotation);
			    	    	}
			    	    	
			    	    	IJ.log("areaScore = "+ areaScore);
			    	    	break rotation_steps_loop;
			    	    }
			    	}
			    	else
			    	{			
			    		int posNeg; /** Controls positive vs negative rotations */
			    		boolean transformed;
					
			    		for(int t = 0; t < 4; t ++)
			    		{
			    			IJ.log("TESTING: t = "+t+"; rotation = "+(i*rotationStep));
			    			//if((mov == 0 && i == 10) || (mov== 1 && i == 18))    //TODO: this 'if' is just included to speed up the testing process. It needs to be removed once everything is complete. 
			    			//{			  			  							   //TODO: remember to adjust the 'output = affineTransform()' call.
			    									 
			    			IJ.log("TESTING: This loop should now be executed");
			    			if(t==0)
			    			{
			    				posNeg = 1;
			    				transformed = false;
			    				IJ.log((posNeg*i*rotationStep)+"° rotation - affine");
			    			}
			    			else if(t==1)
			    			{
			    				posNeg = -1;
			    				transformed = false;
			    				IJ.log((posNeg*i*rotationStep)+"° rotation - affine");
			    			}
			    			else if(t==2)
			    			{
			    				posNeg = 1;
			    				transformed = true;
			    				IJ.log((posNeg*i*rotationStep)+"° rotation (transformed) - affine");
			    			}
			    			else
			    			{
			    				posNeg = -1;
			    				transformed = true;
			    				IJ.log((posNeg*i*rotationStep)+"° rotation (transformed) - affine");
			    			}
			    			
			    			IJ.log("output = affineTransform("+fixed.toString()+", "+movingTransformations[mov][t].toString()+", ("+(posNeg*i*rotationStep)+"));");	    		
						
			    			//output = affineTransform(fixed, movingTransformations[mov][t], (posNeg*i*rotationStep)); //TODO: This is just a time saver for debugging. Need to use the line 
			    																									   //	   below for actual program and delete this line.
			    			
			    			output = affineTransform(fixed, movingTransformations[mov][t], (posNeg*rotationStep)); //TODO: use this line for actual program.
			    			
			    			IJ.log("third; movingTransformations["+mov+"]["+t+"]"+movingTransformations[mov][t].toString());
			    			if(t==0)
			    			{
			    				tracker_one = tracker_one+(posNeg*rotationStep);
			    				IJ.log("tracker_one = "+tracker_one);
			    			}
			    			else if(t==1)
			    			{
			    				tracker_two = tracker_two+(posNeg*rotationStep);
			    				IJ.log("tracker_two = "+tracker_two);
			    			}
			    			else if(t==2)
			    			{
			    				tracker_three = tracker_three+(posNeg*rotationStep);
			    				IJ.log("tracker_three = "+tracker_three);
			    			}
			    			else if(t==3)
			    			{
			    				tracker_four = tracker_four+(posNeg*rotationStep);
			    				IJ.log("tracker_four = "+tracker_four);
			    			}
			    			area = calculateArea(output, fixed, mov, i);
			    			IJ.log("area = "+ area);
			    			tempScore = area/originalArea;
			    			
			    			IJ.log("tempScore = "+ tempScore);
			    			IJ.log("areaScore = "+ areaScore);
				    	
			    			if(tempScore < areaScore)
			    			{
			    				areaScore = tempScore;
			    			}
				    	
			    			if(areaScore <= 0.85)
			    			{
			    				areaScoreTracker[mov] = areaScore;
			    				
			    				if(mov == 0)
				    	    	{
			    					temp_alignment = new AlignmentObject("Giemsa", original_image_dimensions[d][moving_index], transformed, scale_factor[mov], (posNeg*i*rotationStep), mX, mY, anchor_coords[d][mov], fX, fY, anchor_coords[d][fixed_index], center_coords[d][moving_index], new double[]{0.0,0.0},true); 
				    	    		
			    					additional_scale = temp_alignment.refined_scale / scale_factor[mov];
			    					additional_rotation = temp_alignment.refined_rotation - temp_alignment.rough_rotation;
				    	    		
				    	    		IJ.log("Refined Affine: ");
				    	    		IJ.log("scale_factor["+mov+"] = "+scale_factor[mov]);
				    	    		IJ.log("initial rotation = "+(posNeg*i*rotationStep));
				    	    		IJ.log("additional_scale = "+additional_scale);
				    	    		IJ.log("additional_rotation = "+additional_rotation);
				    	    		IJ.log("temp_alignment.primary_Translation[0] = "+temp_alignment.primary_translation[0]);
				    	    		IJ.log("temp_alignment.primary_Translation[1] = "+temp_alignment.primary_translation[1]);
				    	    		
				    	    		refined_AffineTransform(fixed, movingTransformations[mov][t], additional_rotation, additional_scale, temp_alignment.primary_translation);
				    	    		all_alignments[d][mov] = new AlignmentObject("Giemsa", original_image_dimensions[d][moving_index], transformed, temp_alignment.refined_scale, temp_alignment.refined_rotation, mX, mY, anchor_coords[d][mov], fX, fY, anchor_coords[d][fixed_index], center_coords[d][moving_index], temp_alignment.primary_translation, false);		
				    	    	
				    	    		IJ.log("final scale: all_alignments["+d+"]["+mov+"].refined_scale = "+all_alignments[d][mov].refined_scale);
				    	    		IJ.log("final rotation: all_alignments["+d+"]["+mov+"].refined_rotation = "+all_alignments[d][mov].refined_rotation);
				    	    	
				    	    	}
				    	    	else
				    	    	{
				    	    		temp_alignment = new AlignmentObject(get_template_name(als.get(d)[moving_index]), original_image_dimensions[d][moving_index], transformed, scale_factor[mov], (posNeg*i*rotationStep), mX, mY, anchor_coords[d][mov], fX, fY, anchor_coords[d][fixed_index], center_coords[d][moving_index], new double[]{0.0,0.0},true); 
				    	    		
				    	    		additional_scale = temp_alignment.refined_scale / scale_factor[mov];
				    	    		additional_rotation = temp_alignment.refined_rotation - temp_alignment.rough_rotation;
				    	    		
				    	    		IJ.log("Refined Affine: ");
				    	    		IJ.log("scale_factor["+mov+"] = "+scale_factor[mov]);
				    	    		IJ.log("initial rotation = "+(posNeg*i*rotationStep));
				    	    		IJ.log("additional_scale = "+additional_scale);
				    	    		IJ.log("additional_rotation = "+additional_rotation);
				    	    		IJ.log("temp_alignment.primary_Translation[0] = "+temp_alignment.primary_translation[0]);
				    	    		IJ.log("temp_alignment.primary_Translation[1] = "+temp_alignment.primary_translation[1]);
				    	    		
				    	    		refined_AffineTransform(fixed, movingTransformations[mov][t], additional_rotation, additional_scale, temp_alignment.primary_translation);
				    	    		all_alignments[d][mov] = new AlignmentObject(get_template_name(als.get(d)[moving_index]), original_image_dimensions[d][moving_index], transformed, temp_alignment.refined_scale, temp_alignment.refined_rotation, mX, mY, anchor_coords[d][moving_index], fX, fY, anchor_coords[d][fixed_index], center_coords[d][moving_index], temp_alignment.primary_translation, false);
				    	    	
				    	    		IJ.log("final scale: all_alignments["+d+"]["+mov+"].refined_scale = "+all_alignments[d][mov].refined_scale);
				    	    		IJ.log("final rotation: all_alignments["+d+"]["+mov+"].refined_rotation = "+all_alignments[d][mov].refined_rotation);
				    	    	}
			    				
			    				IJ.log("areaScore = "+ areaScore);
			    				break rotation_steps_loop;
			    			}
			    		//}
			    		//else //TODO: need to get rid of this else bracket once finished testing.
			    		//{
			    	//		IJ.log("Skipping this loop as it is not needed for testing.");
			    	//	}
			    		}
			    	}
			    }
			}
			
			CTC5Analysis.graphic_progress_update_frame.ScaleAndAlign.UpdateProgress((((double)d+1/(double)als.size())*100.0), als.get(d)[0]);
			new Thread(new Runnable()
			{
				public void run()
				{	
					CTC5Analysis.graphic_progress_update_frame.ScaleAndAlign.repaint();
				}
			}).start();
		}
		
		for(int i =0; i < all_alignments.length; i++)
		{
			for(int t =0; t < all_alignments[i].length; t++){
				IJ.log("Testing null alignment_objects: all_alignments["+i+"]["+t+"] = "+all_alignments[i][t]); 
			
				for(int y =0; y < all_alignments[i][t].moving_images.length; y++)
				{
					IJ.log("Testing null alignment_objects: all_alignments["+i+"]["+t+"].moving_images["+y+"] = "+all_alignments[i][t].moving_images[y]);
				}
			}
		}
		
		return all_alignments;
	}
	
	public double calculateArea(ImagePlus output, ImagePlus fixed, int index, int loop)
	{
		IJ.run("Clear Results");
		
		IJ.log("// Start calculateAndClean() //");
		double area;
		IJ.log("	output = "+output.toString());
		IJ.log("	fixed = "+fixed.toString());
		
		IJ.log("	'output' stack size = "+output.getStackSize());
		output.setSlice(0);
		ImagePlus data = new ImagePlus("Data", output.getProcessor().duplicate());
		//Prefs.set("blackBackground", ""+false);
		IJ.run(data, "Make Binary", "");
		
		ImageCalculator ic = new ImageCalculator();
		ImagePlus merge = ic.run("Max create", fixed, data);
		IJ.log("	merge = "+merge.toString());
		IJ.log("	merge (title) = "+merge.getTitle());

		IJ.run(merge, "Set Measurements...", "area centroid area_fraction redirect=None decimal=3");
		IJ.run(merge,"Measure", "");
		
		ResultsTable results = ResultsTable.getResultsTable();
		int area_index 	= results.getColumnIndex("%Area");
		IJ.log("	area_index = "+area_index);
		double[] area_raw = results.getColumnAsDoubles(area_index);
		for(int i = 0; i< area_raw.length; i++)
		{
			IJ.log("	area_raw["+i+"] = "+area_raw[i]);
		}
		
		area = area_raw[0];
		IJ.log("	area = "+area);
		
		CTC5Analysis.hideAllImages();
		IJ.run("Clear Results");
		IJ.log("// End calculateAndClean() //");
		
		return area;
	}
	
	public ImagePlus affineTransform(ImagePlus fixed, ImagePlus moving, double rotation)
	{
	    IJ.log("// Start affingTransform() //");
	    if(rotation != 0.0)
	    {
	    	moving.getProcessor().setBackgroundValue(0);
	    	moving.getProcessor().rotate(rotation);
	    	moving.updateAndRepaintWindow();
	    } 
	    
		fixed.show();
		moving.show();
		

		int [][] moving_points = getPoints(moving.getWidth(), moving.getHeight(), 0.25);
		int [][] fixed_points = getPoints(fixed.getWidth(), fixed.getHeight(), 0.25);
		
		//https://github.com/fiji-BIG/TurboReg/blob/master/src/main/java/TurboReg_.java
		IJ.runPlugIn("TurboReg_", "-align -window "+moving.getTitle()+" 0 0 "+moving.getWidth()+" "+moving.getHeight()+
				  " -window "+fixed.getTitle()+" 0 0 "+fixed.getWidth()+" "+fixed.getHeight()+
				  " -affine "+moving_points[0][0]+" "+moving_points[0][1]+" "+fixed_points[0][0]+" "+fixed_points[0][1]+" "   //source&target topLeft
				  			 +moving_points[1][0]+" "+moving_points[1][1]+" "+fixed_points[1][0]+" "+fixed_points[1][1]+" "   //source&target topRight
				  			 +moving_points[2][0]+" "+moving_points[2][1]+" "+fixed_points[2][0]+" "+fixed_points[2][1]+	  //source&target botRight
				  " -showOutput");
		
		ResultsTable results = ResultsTable.getResultsTable();
		int sX_index 	= results.getColumnIndex("sourceX");
		int sY_index 	= results.getColumnIndex("sourceY");
		int tX_index 	= results.getColumnIndex("targetX");
		int tY_index 	= results.getColumnIndex("targetY");
		mX = results.getColumnAsDoubles(sX_index); /** moving image x position */
		mY = results.getColumnAsDoubles(sY_index); /** moving image y position */
		fX = results.getColumnAsDoubles(tX_index); /** fixed image x position */
		fY = results.getColumnAsDoubles(tY_index); /** fixed image y position */
		
		for(int i = 0; i<mX.length; i++)
		{
			IJ.log(i+"] "+mX[i]+","+mY[i]+","+fX[i]+","+fY[i]+".");
		}
		
		IJ.selectWindow("Output");
		ImagePlus output = new ImagePlus("Output", WindowManager.getCurrentImage().getProcessor().duplicate());
		CTC5Analysis.hideAllImages();
		
		output.show();
		IJ.log("// End affingTransform() //");
		return output;
	}
	
	public ImagePlus refined_AffineTransform(ImagePlus fixed, ImagePlus moving, double additional_rotation, double additional_scale, double[] primary_translation) 
	{
	    IJ.log("// Start affingTransform() //");
	    if(additional_rotation != 0.0)
	    {
	    	moving.getProcessor().setBackgroundValue(0);
	    	moving.getProcessor().rotate(additional_rotation);
	    	/** the below line scales and resizes the window. */
	    	IJ.run(moving, "Size...", "width="+moving.getWidth()*additional_scale+" height="+moving.getHeight()*additional_scale+" constrain average interpolation=Bilinear");
	    	
	    	IJ.log("Peparing to Translate:");
	    	IJ.log("primary_translation[0] = "+primary_translation[0]);
	    	IJ.log("primary_translation[1] = "+primary_translation[1]);
	    	moving.getProcessor().translate(primary_translation[0], primary_translation[1]);	    	
	    	moving.updateAndRepaintWindow();
	    } 
	    
		fixed.show();
		moving.show();
		
		int [][] moving_points = getPoints(moving.getWidth(), moving.getHeight(), 0.2);
		int [][] fixed_points = getPoints(fixed.getWidth(), fixed.getHeight(), 0.2);
		
		//https://github.com/fiji-BIG/TurboReg/blob/master/src/main/java/TurboReg_.java
		IJ.runPlugIn("TurboReg_", "-align -window "+moving.getTitle()+" 0 0 "+moving.getWidth()+" "+moving.getHeight()+
				  " -window "+fixed.getTitle()+" 0 0 "+fixed.getWidth()+" "+fixed.getHeight()+
				  " -affine "+moving_points[0][0]+" "+moving_points[0][1]+" "+fixed_points[0][0]+" "+fixed_points[0][1]+" "   //source&target topLeft
				  			 +moving_points[1][0]+" "+moving_points[1][1]+" "+fixed_points[1][0]+" "+fixed_points[1][1]+" "   //source&target topRight
				  			 +moving_points[2][0]+" "+moving_points[2][1]+" "+fixed_points[2][0]+" "+fixed_points[2][1]+	  //source&target botRight
				  " -showOutput");
		
		ResultsTable results = ResultsTable.getResultsTable();
		int sX_index 	= results.getColumnIndex("sourceX");
		int sY_index 	= results.getColumnIndex("sourceY");
		int tX_index 	= results.getColumnIndex("targetX");
		int tY_index 	= results.getColumnIndex("targetY");

		mX = results.getColumnAsDoubles(sX_index); /** moving image x position */
		mY = results.getColumnAsDoubles(sY_index); /** moving image y position */
		fX = results.getColumnAsDoubles(tX_index); /** fixed image x position */
		fY = results.getColumnAsDoubles(tY_index); /** fixed image y position */
		
		for(int i = 0; i<mX.length; i++)
		{
			IJ.log(i+"] "+mX[i]+","+mY[i]+","+fX[i]+","+fY[i]+".");
		}
		
		IJ.selectWindow("Output");
		ImagePlus output = new ImagePlus("Output", WindowManager.getCurrentImage().getProcessor().duplicate());
		CTC5Analysis.hideAllImages();
		
		output.show();
		IJ.log("// End affingTransform() //");
		return output;
	}

	public String get_template_name(String s)
	{
		IJ.log("String s = "+s);
		String template;
		int start_index;
		int end_index;
		
		int temp_one = s.lastIndexOf("/")+1;
		int temp_two = s.lastIndexOf("\\")+1;
		
		if(temp_one>temp_two)
		{
			start_index = temp_one;
		}
		else
		{
			start_index = temp_two;
		}
		
		end_index = s.indexOf("_ROI_poresOnly.tif");
		IJ.log("start_index = "+start_index);
		IJ.log("end_index = "+end_index);
		template = s.substring(start_index, end_index);
		
		return template;
	}
	
	public void construct_digital_slide(ArrayList<String> selected_ndpi_files) //ArrayList<String> CTC5Analysis.data.selected_ndpi_files
	{
		int num_of_fluor_channels = CTC5Analysis.data.selected_fluorescent_channels.size();
		
		IJ.log("num_of_fluor_channels = "+num_of_fluor_channels);
		IJ.log("counter_stain_index = "+counter_stain_index);
		
		for(int t = 0; t<selected_ndpi_files.size(); t++)
		{
		   
			String removeExt = selected_ndpi_files.get(t).substring(0, selected_ndpi_files.get(t).lastIndexOf("."));
			String save_path = CTC5Analysis.data.initial_NDPI_directory+"\\"+removeExt+"\\Final Processed\\Adjusted\\";
			IJ.log("save_path ="+save_path);
			
			
			String[] channels = CTC5Analysis.data.final_adjusted_stitched_images[t];
			for(int testing = 0; testing<CTC5Analysis.data.final_adjusted_stitched_images[t].length; testing++)
			{
				IJ.log(" DIGITAL: CTC5Analysis.data.final_adjusted_stitched_images["+t+"] = "+CTC5Analysis.data.final_adjusted_stitched_images[t]);
			}
			
			ImagePlus[] layers = new ImagePlus[channels.length];
			
			for(int i = 0; i<channels.length; i++)
			{
				IJ.log("channels["+i+"] "+channels[i]);
				layers[i] =  new ImagePlus(channels[i]);
				
				if(layers[i] == null)
				{
					IJ.log("ERROR: layers["+i+"] was never initialised.");
				}
				
				if(i>0)
				{
					int temp_index = i-1;
					IJ.log("IJ.run(layers["+i+"],CTC5Analysis.data.selected_fluorescent_channels.get("+temp_index+"),'');");
					IJ.log("layers.length = "+layers.length);
					IJ.log("CTC5Analysis.data.selected_fluorescent_channels.size() = "+CTC5Analysis.data.selected_fluorescent_channels.size());

					IJ.run(layers[i],CTC5Analysis.data.selected_fluorescent_channels.get(temp_index),"");
				}
			}
			
			ImagePlus[] imp_array = new ImagePlus[2];
			/** i = 1 to skip the Giemsa channel */
			for(int i = 1; i<layers.length;i++)
			{
				if(i!=counter_stain_index)
				{
					IJ.log(i+"th iteration of the RGBStackMerge Loop.");
					
					imp_array[0] = layers[counter_stain_index];
					imp_array[1] = layers[i];	
					
					ImagePlus merged = RGBStackMerge.mergeChannels(imp_array, true); 
					
					IJ.saveAs(merged,  "Tiff", save_path+"merged_"+layers[i].getTitle());
				}
			}
		}
	}
	
	/** 
	 * configure_for_analyse_particles() ensures that images have the correct foreground and background colour settings
	 * to allow for automated pore detection.
	 */
	public boolean configure_for_analyse_particles(ImagePlus imp)
	{
		boolean wasInverted;
		
		if(imp.isInvertedLut())
		{
			IJ.log("image is using inverted LUT... switching back to normal LUT");
			IJ.run(imp, "Invert LUT", "");
		}
		else
		{
			IJ.log("image is NOT using inverted LUT... no action needed");
		}
		
		IJ.run("Colors...", "foreground=black background=white selection=red"); /** This sets it up so that background is white and allows sets a standardised start point for analyse particles. */
		
		IJ.run("Set Measurements...", "mean");
		IJ.run("Clear Results");
		IJ.run(imp, "Measure", "");
		ResultsTable results = ResultsTable.getResultsTable();
		
		int in = 0;
		while(results.columnExists(in)) 
		{
			IJ.log("col num"+in+" exists.");
		}
		int mean_index = results.getColumnIndex("Mean");
		double[] mean = results.getColumnAsDoubles(mean_index);

		if(mean[0] < 127.5)
		{
			IJ.log("LUT mean = "+mean[0]+"; inverting....");
			IJ.run(imp, "Invert", "");
			
			IJ.run("Clear Results");
			IJ.run(imp, "Measure", "");
			results = ResultsTable.getResultsTable();
			
			mean_index = results.getColumnIndex("Mean");
			mean = results.getColumnAsDoubles(mean_index);
			IJ.log("New Mean = "+mean[0]);
			
			wasInverted = true;
		}
		else
		{
			IJ.log("LUT mean = "+mean[0]+"; Proceed without inverting");
			wasInverted = false;
		}
		
		IJ.run("Clear Results");
		
		return wasInverted;
	}
	
	public boolean measureMean(ImagePlus ip)
	{
		boolean dark;
		
		IJ.run("Colors...", "foreground=black background=white selection=red"); /** This sets it up so that background is white and allows sets a standardised start point for analyse particles. */
		
		IJ.run("Set Measurements...", "mean");
		IJ.run("Clear Results");
		IJ.run(ip, "Measure", "");
		ResultsTable results = ResultsTable.getResultsTable();
		
		int in = 0;
		while(results.columnExists(in)) 
		{
			IJ.log("col num"+in+" exists.");
		}
		int mean_index = results.getColumnIndex("Mean");
		double[] mean = results.getColumnAsDoubles(mean_index);

		if(mean[0] < 127.5)
		{
			dark = true;
		}
		else
		{
			dark = false;
		}
		return dark;
	}
	
	/** This method uses the golden ratio approach to selecting landmark points for the TurboReg_ 
	 * call in the same manner as the TurboReg_ Plugin when run on automatic settings. */
	public int[][] getPoints(int width, int height, double factor)
	{
		int[][] points = new int[3][2]; /** three positions[3], times x and Y coordinates[2]. */
		double golden_ratio = 0.5*(Math.sqrt(5.0)-1);
		
		points[0][0] = (int)((width*golden_ratio)+0.5);
		points[0][1] = (int)((factor*height*golden_ratio)+0.5);
		points[1][0] = (int)((factor*width*golden_ratio)+0.5);
		points[1][1] = (int)((height-(factor*height*golden_ratio))+0.5);
		points[2][0] = (int)((width-(factor*width*golden_ratio))+0.5);
		points[2][1] = (int)((height-(factor*height*golden_ratio))+0.5);
		
		return points;
	}
	
	public int[] all_frequencies(double[] d)
	{
		IJ.log("d[(d.length-1)] = " +d[(d.length-1)]);
		int[] values_and_freqs = new int[(int) d[(d.length-1)]];
		
		for(int i = 0; i<values_and_freqs.length; i ++)
		{
			values_and_freqs[i] =  frequency(d, (double)(i+1));
		}
		return values_and_freqs;
	}
	
	public int end_of_noise(double[] d)
	{
		int index = 0;
		
		for(int i = 0; i < (d.length-1); i++ )
		{
			if(d[i]<d[i+1])
			{
				IJ.log("end_of_noise: d["+i+"] = "+d[i]);
				IJ.log("end_of_noise: d["+(i+1)+"] = "+d[i+1]);
				index = i;	
				
				i = d.length;
			}		
		}
		
		return index;
	}

}