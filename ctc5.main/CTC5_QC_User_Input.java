package ctc5.main;

import java.io.File;

public class CTC5_QC_User_Input 
{
	String missing_plugins = "";
	String[] dependencies = {"Extended_Depth_Field","ndpi2tiff.exe", "ndpisplit.exe","ndpisplit-m.exe","ndpisplit-mJ.exe", "ndpisplit-s.exe", "ndpisplit-s-m.exe", "ndpisplit-s-mJ.exe", "NDPITools_", "TurboReg_", "Stitching_"};
	File plugin_directory = new File("."+File.separator+"plugins");
	String installed_plugins = "";
	
	public CTC5_QC_User_Input()
	{
		File[] installed_plugin_files = plugin_directory.listFiles();
		
		for(int i = 0; i < installed_plugin_files.length; i++)
		{
			if(installed_plugin_files[i].isFile())
			{
				installed_plugins = installed_plugins + "\n" + installed_plugin_files[i];
			}
		}
	}
	
	public static boolean check_if_brightfield_templates_are_unique(String[] template_IDs){
		
		boolean are_unique = true;
		
		for(int i = 0; i < template_IDs.length; i++)
		{
			for(int k = 0; k<template_IDs.length; k++)
			{
				if(!(k==i))
				{
					if(template_IDs[i].toLowerCase().contains(template_IDs[k].toLowerCase()) || template_IDs[k].toLowerCase().contains(template_IDs[i].toLowerCase()))
					{
						are_unique = false;
					}
				}
			}
		}
		return are_unique;
	}
	
	public boolean check_plugin_dependencies()
	{
		boolean complete_install = false;
		
		for(int i = 0; i < dependencies.length; i++)
		{
			if(!(installed_plugins.contains(dependencies[i])))
			{
				missing_plugins =  missing_plugins + "\n" + dependencies[i];
			}
		}
		
		if(missing_plugins == "")
		{
			complete_install = true;
		}
		
		return complete_install;
	}
	
	public String return_missing_plugins()
	{
		return missing_plugins;
	}
}