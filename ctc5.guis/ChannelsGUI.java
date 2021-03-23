package ctc5.guis;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.GridLayout;
import java.awt.TextField;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ctc5.main.CTC5Data;
import ij.IJ;

public class ChannelsGUI extends JFrame {
	
	/**
	 *  UI to allow user to select the corresponding LUTs and alignment reference templates for creating the digital slide.
	 */
	private static final long serialVersionUID = 3324132144101546976L;
	
	public Checkbox[] channelsCheck;
	public String[] channels =  CTC5Data.return_all_fluorescent_channels(); 
	
	public TextField IDstring;
	public JButton ok;
	public JButton cancel;
	int type;
 	
	public ChannelsGUI(int t)
	{
		super("CTC-5");
		type = t;
	}
	
	public void init()
	{	
		IJ.log("initialising ChannelsGUI...");
		JPanel 		clp 	= makeChecklistPane();
		JPanel 		idp		= makeIDpane();
		JPanel 		bp 		= makeButtonPane();

		getContentPane().add(clp, BorderLayout.NORTH);
		getContentPane().add(idp, BorderLayout.CENTER);
		getContentPane().add(bp, BorderLayout.SOUTH);

		pack();
	}
	
	public JPanel makeChecklistPane()
	{
		JPanel l = new JPanel(new GridLayout(4, 1));
		
		int size = channels.length;
		boolean[] checks = {true, true, true, true, false, false, false};
		channelsCheck = new Checkbox[size];
		
		for(int i = 0; i<size; i++)
		{
			if(checks.length == size)
			{
				channelsCheck[i] = new Checkbox(channels[i], checks[i]);
			}
			else
			{
				channelsCheck[i] = new Checkbox(channels[i], true);
			}
			
			l.add(channelsCheck[i]);
		}
		
		return l;
	}
	
	public JPanel makeIDpane()
	{
		JPanel tID = new JPanel(new GridLayout(2,2));
		JLabel IDlabel;
		if(type == 6)
		{
			IDlabel = new JLabel("Positions File (e.g. positions.txt): ");
		}
		else
		{
		    IDlabel = new JLabel("BF templates (e.g. BF1, BF2) : ");
		}
		
		IDstring = new TextField("BF1, BF2", 8); 

		tID.add(IDlabel);
		tID.add(IDstring);
		
		return tID;
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
	
	public String returnIDString(){
		String s;
		s = IDstring.getText();
		
		return s;
	}

}