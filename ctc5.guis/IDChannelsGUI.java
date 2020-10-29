package ctc5.guis;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.TextField;
import java.util.ArrayList;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import ij.IJ;

/**
 * @author  Brendan Ffrench (Royal College of Surgeons in Ireland)
 * @date    03 January 2018.
 * @version 2.1.0
 *  
 * Generates a graphical user interface to allow the user to:
 *  1) select which channel should be used as the counterstain channel (JRadioButon[])
 *  2) input labels that identify the filenames of the individual channels (TextField[])
 *  3) select which stitching template matches each channel (JComboBox[])
 */
public class IDChannelsGUI extends JFrame 
{
	private static final long serialVersionUID = 3848238382841092830L;
	
	public ButtonGroup radioGroup =  new ButtonGroup();
	public JRadioButton[] radioArray;
	public TextField[] channelID;
	public String[] selectedChannels;
	@SuppressWarnings("rawtypes")
	public JComboBox[] templates;
	public String[] refTemplates;
	public int numOfChannels;
	public JButton ok;
	public JButton cancel;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public IDChannelsGUI(ArrayList<String> selChannels, String[] rt)
	{
		super("CTC-5");
		numOfChannels = selChannels.size();
		selectedChannels = new String[numOfChannels];
		radioArray = new JRadioButton[numOfChannels];
		channelID = new TextField[numOfChannels];
		templates = new JComboBox[numOfChannels];
		refTemplates = rt;
		
		for(int i = 0; i < numOfChannels; i++)
		{
			selectedChannels[i] = selChannels.get(i);
			radioArray[i] =  new JRadioButton();
			radioGroup.add(radioArray[i]);
			channelID[i] = new TextField("_w0000", 8);
			templates[i] = new JComboBox(refTemplates);
		}
	}
	
	public void init()
	{	
		IJ.log("initialising IDchannelsGUI...");
		JPanel 		hp 	= makeHeaderPane();
		JPanel 		ip	= makeInterfacePane();
		JPanel 		bp 	= makeButtonPane();

		getContentPane().add(hp, BorderLayout.NORTH);
		getContentPane().add(ip, BorderLayout.CENTER);
		getContentPane().add(bp, BorderLayout.SOUTH);

		pack();
	}
	
	public JPanel makeHeaderPane()
	{
		JPanel l = new JPanel(new GridLayout(1, 4));
		String[] stringHeaders = {"Channels","Counter-Stain","Channel ID","Stitching Template"};
		JLabel[] headers = new JLabel[4];

		for(int i = 0; i < headers.length; i++)
		{
			headers[i] = new JLabel(stringHeaders[i]);
			l.add(headers[i]);
		}
		
		return l;
	}
	
	public JPanel makeInterfacePane()
	{
		JPanel chID = new JPanel(new GridLayout(0,4));
		
		for(int i = 0; i<numOfChannels; i++)
		{
			chID.add(new JLabel(selectedChannels[i]));
			chID.add(radioArray[i]);
			chID.add(channelID[i]);
			chID.add(templates[i]);
		}
			
		return chID;
	}
	
	public JPanel makeButtonPane()
	{
		JPanel buttons 	= new JPanel(new GridLayout(0, 2, 5, 5));
    	ok 		= new JButton("OK");
    	cancel 	= new JButton("Cancel");
    	
    	buttons.add(ok);
    	buttons.add(cancel);
    	
    	return buttons;
	}
}