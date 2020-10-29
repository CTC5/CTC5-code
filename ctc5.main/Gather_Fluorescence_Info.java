package ctc5.main;

import ij.IJ;
import ij.plugin.PlugIn;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import plugin.GridType;
import ctc5.guis.ChannelsGUI;
import ctc5.guis.FluorDataGUI;
import ctc5.guis.IDChannelsGUI;

/**
 * Collects all the user input required to automate stitching of fluorescent mosaic datasets.
 * This is completely dependent on the ImageJ Plugin: 'Grid/Collection Stitching' authored by Stephen Preibisch.
 * 
 * @author  Brendan Ffrench: Royal College of Surgeons in Ireland.
 * @date    09 October 2017.
 * @version 1.0.0
 */
public class Gather_Fluorescence_Info implements  PlugIn, ActionListener 
{
	FluorDataGUI getDatasets;
	ChannelsGUI getChannels;
	IDChannelsGUI getFluorDataInfo;
	ArrayList<String> selectedChannels;
	ArrayList<String> selectedFiles;
	String[][] selectedFilesAdapted;
	String[] reference_templates;
	GridType gtGUI; /** Source from:- https://github.com/fiji/Stitching/blob/master/src/main/java/plugin/GridType.java  */
	String type_string = "";
	String order_string = "";
	int type;
	
	public Gather_Fluorescence_Info(ArrayList<String> als)
	{
		IJ.log("Starting ProcessFluoresence");
		selectedFiles = als;
	}
	
	/** Asks the user to select the grid orientation.  */
	public void run() throws InterruptedException
	{
		this.selectGridType();
		this.selectChannels();
	}
	
	public void selectGridType() throws InterruptedException
	{
		Thread getGT = new Thread(new Runnable(){
									public void run()
									{
										gtGUI = new GridType();
									}});
		getGT.start();
		getGT.join();
		
		type  = gtGUI.getType();
		int order = gtGUI.getOrder();
		
		typeOrderToStrings(type, order);
	}
	
	public void selectChannels()
	{
		IJ.log("type = "+type_string+"; order = "+order_string+";");
		
		getChannels = new ChannelsGUI(type);
		getChannels.init();
		getChannels.ok.addActionListener(this);
		getChannels.cancel.addActionListener(this);
		getChannels.setVisible(true);
	}
	
	public void selectDatasets()
	{
		getDatasets = new FluorDataGUI(selectedFiles, type, reference_templates);
		getDatasets.init();
		getDatasets.ok.addActionListener(this);
		getDatasets.cancel.addActionListener(this);
		getDatasets.setVisible(true);
	}
	
	public void idChannels()
	{
		getFluorDataInfo = new IDChannelsGUI(CTC5Analysis.data.returnSelectedChannels(), reference_templates);
		getFluorDataInfo.init();
		getFluorDataInfo.ok.addActionListener(this);
		getFluorDataInfo.cancel.addActionListener(this);
		getFluorDataInfo.setVisible(true);
	}
	
	@Override
	public void actionPerformed(ActionEvent ae) {
		if(ae.getSource() == getChannels.ok)
		{
			getChannels.setVisible(false);
			
			CTC5Analysis.data.processSelectedChannels(getChannels.channelsCheck);
			selectedChannels = CTC5Analysis.data.returnSelectedChannels();
			
			for(int t = 0; t<selectedChannels.size(); t++){
				IJ.log("Selected Channel: "+selectedChannels.get(t));
			}
			
			reference_templates = getChannels.returnIDString().split(",");
			
			for(int k=0; k<reference_templates.length; k++){
				reference_templates[k] = reference_templates[k].replaceAll("\\s+", "");
				IJ.log("reference_templates["+k+"] = "+reference_templates[k]);
			}
			
			if(CTC5_QC_User_Input.check_if_brightfield_templates_are_unique(reference_templates))
			{
				IJ.log("All template IDs are unique");
			} 
			else
			{
				IJ.showMessage("Template IDs are not completely unique.");
			}
			
		    this.selectDatasets();
		}
		else if(ae.getSource() == getChannels.cancel)
		{
			IJ.log("Fluorescence processing cancelled by user.");
		}
		else if(ae.getSource() == getDatasets.ok)
		{
			getDatasets.setVisible(false);
			
			CTC5Analysis.data.processSelectionOfFluorData(getDatasets.datasetsCheck, getDatasets.datasets, getDatasets.refTemplates, getDatasets.x, getDatasets.y, getDatasets.pc, getDatasets.getID, getDatasets.getIndex);
			
			this.idChannels();
		}
		else if(ae.getSource() == getChannels.cancel)
		{
			IJ.log("Fluorescence processing cancelled by user.");
		}
		else if(ae.getSource() == getFluorDataInfo.ok)
		{
			 getFluorDataInfo.setVisible(false);

			 CTC5Analysis.data.process_fluorescent_channel_naming_and_templates(getFluorDataInfo.radioGroup, getFluorDataInfo.channelID, getFluorDataInfo.templates, getFluorDataInfo.refTemplates);
		
			 IJ.log("counterstain_index = "+CTC5Analysis.data.returnCounterStainChanelIndex()+"("+CTC5Data.return_all_fluorescent_channels()[CTC5Analysis.data.returnCounterStainChanelIndex()]+")");
			 
			 for(int i = 0; i<CTC5Analysis.data.returnSelectedChannels().size(); i++)
			 {
				 IJ.log("Channel "+i+" ("+CTC5Data.return_all_fluorescent_channels()[i]+") = "+CTC5Analysis.data.returnChannelID_BFtemplate_pairs().get(i)[0]
						 + ", ["+CTC5Analysis.data.returnChannelID_BFtemplate_pairs().get(i)[1]+"]");
			 }
			 
			 CTC5Analysis.data.moveSelectedFluorescentFiles(CTC5Analysis.data.selected_fluorescent_dataset_list);
			  
			 ArrayList<String[]> tif_files = new ArrayList<String[]>();
			 File[] temp_file_list = null;
			 
			 for(int k = 0; k < CTC5Analysis.data.organised_fluor_dataset_folder.length; k++)
			 {
				 String[] temp_tif_filenames =  new String[CTC5Analysis.data.selected_fluorescent_dataset_list.get(k).length-1];
				 String[] template_name =  new String[CTC5Analysis.data.selected_fluorescent_dataset_list.get(k).length-1];
				 
				 /** Use this boolean array to determine when all templates have identified an appropriate file name. */ 
				 Boolean[] filename_match_found = new Boolean[template_name.length];
				 Arrays.fill(filename_match_found, Boolean.FALSE);
				 
				 /** check to make sure that the XXX_fluor directory has actually been moved to the */
				 if(CTC5Analysis.data.organised_fluor_dataset_folder[k].exists())
				 {
					 temp_file_list = CTC5Analysis.data.organised_fluor_dataset_folder[k].listFiles();
				 
					 for(int z = 0; z < temp_file_list.length; z++)
					 {	 
						 String temp_filename =  temp_file_list[z].getName();
						 
						 for(int l=0; l<template_name.length; l++)
						 {
							 template_name[l] = CTC5Analysis.data.selected_fluorescent_dataset_list.get(k)[l+1];
							 
							 if(temp_filename.endsWith(".tif") && temp_filename.contains(template_name[l]) && !(filename_match_found[l]))
							 {
								 filename_match_found[l] = true;
								 temp_tif_filenames[l] = temp_filename;
							 }
						 }
						 tif_files.add(temp_tif_filenames);
					 }
				 }
				 else
				 {
					 IJ.log("ERROR: files could not be moved to "+CTC5Analysis.data.organised_fluor_dataset_folder[k].getPath()+". Plugin terminated.");
				 }
			 }

			 int start_index = CTC5Analysis.data.fluorescent_mosaic_tile_ID.indexOf("{"); 
			 int end_index = CTC5Analysis.data.fluorescent_mosaic_tile_ID.indexOf("}");
			 int number_of_digits = end_index - start_index - 1;
			 
			 IJ.log("number_of_digits = "+number_of_digits);
			 
			 String temp_digit_builder = "";
			 for(int k = 0; k < number_of_digits; k ++){
				  temp_digit_builder = temp_digit_builder + "\\d";
			 }			 
			 
			 /** 
			  * String query_tile_ID takes the mosaic_tile_ID user input and converts it into a form that is compatible with RegEx based comparison to file names 
			  * This allows for the creation of String[] generic_template_filename which holds the 'FIJI (Grid/Collection Stitching)' compatible version of the template filename that needs to be stitched. 
			  */ 
			 String query_tile_ID = CTC5Analysis.data.fluorescent_mosaic_tile_ID.substring(0,start_index)+temp_digit_builder+CTC5Analysis.data.fluorescent_mosaic_tile_ID.substring(end_index+1,CTC5Analysis.data.fluorescent_mosaic_tile_ID.length());
			 
			 IJ.log("query_tile_ID =" +query_tile_ID);
			 
			 
			 IJ.log("CTC5Analysis.data.fluorescent_mosaic_tile_ID = "+CTC5Analysis.data.fluorescent_mosaic_tile_ID);
			 for(int k = 0; k < tif_files.size(); k++)
			 {	 
				 String[] temp_generic_names = new String[tif_files.get(k).length];
				 
		
				 for(int h = 0; h < tif_files.get(k).length; h++)
				 {
					 IJ.log("tif_files.get("+k+")["+h+"] = "+tif_files.get(k)[h]);
					 temp_generic_names[h] = tif_files.get(k)[h].replaceAll(query_tile_ID, CTC5Analysis.data.fluorescent_mosaic_tile_ID);
				 }
				 
				 /** This is the final step in generating the 'FIJI (Grid/Collection Stitching)' compatible version of the template filename  */
				 CTC5Analysis.data.return_generic_template_filename().add(temp_generic_names);
			 }
			 
			 Thread extractTiffFromNDPI =  new Thread(new Runnable(){
				public void run()
					{
						try 
						{
							CTC5Analysis.startProcessing(CTC5Analysis.data.return_selected_ndpi_files());
							Stitch_Fluorescenct_Data.run();
						} 
						catch (InterruptedException e) 
						{
							e.printStackTrace();
						}
					}});
			 
			 extractTiffFromNDPI.start();
		}
		else if(ae.getSource() == getFluorDataInfo.cancel)
		{
			 getFluorDataInfo.setVisible(false);
		}
		else
		{
			IJ.log("ERROR: Unknown source of ActionEvent ae");
		}
	}

	@Override
	public void run(String arg0) 
	{
		try 
		{
			this.run();
		} 
		catch (InterruptedException e) 
		{
			e.printStackTrace();
		}	
	}

	private void typeOrderToStrings(int type_code, int order_code){
		String[]   type_strings  = GridType.choose1;
		String[][] order_strings = GridType.choose2;
		
		type_string = type_strings[type_code];
		order_string = order_strings[type_code][order_code];
	}
	
	public String returnTypeString(){
		return type_string;
	}
	
	public String returnOrderString(){
		return order_string;
	}
}