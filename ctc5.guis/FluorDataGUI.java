package ctc5.guis;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.GridLayout;
import java.awt.TextField;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class FluorDataGUI extends JFrame{

	private static final long serialVersionUID = -1533049211881590083L;
	
	public ArrayList<String> datasets;
	public Checkbox[][] datasetsCheck;
	public TextField[][] x;
	public TextField[][] y;
	public TextField[][] pc;
	public String[] refTemplates;
	
	public TextField getID;
	public TextField getIndex;
	
	public JButton ok;
	public JButton cancel;
		   JPanel  tidp;
		   
		   int numberOfFilters;
		   int numRef;
	
	public boolean positionsFromFile;
 	
	public FluorDataGUI(ArrayList<String> als, int b, String[] rT)
	{
		super("CTC-5");
		datasets = als;	
		refTemplates = rT;
		
		if(b == 6){
			positionsFromFile =  true;
		}else{
			positionsFromFile =  false;
		}
	}
	
	public void init()
	{	
		JPanel 		tfp 	= makeTextFieldPane();
		
		if(!positionsFromFile)
		{
			tidp	= makeTileID();
		}
		
		JPanel 		bp 		= makeButtonPane();
		getContentPane().add(tfp, BorderLayout.NORTH);
		
		if(!positionsFromFile)
		{
			getContentPane().add(tidp, BorderLayout.CENTER);
		}
		
		getContentPane().add(bp, BorderLayout.SOUTH);
		pack();
	}
	
	public JPanel makeTextFieldPane()
	{
		JPanel l = new JPanel(new GridLayout(0, 4));
		
		String[] headers = {"Datasets","x","y","Overlap (%)"};
		JLabel[] hl = new JLabel[4];
		
		for(int t = 0; t<headers.length; t++){
			hl[t] = new JLabel(headers[t]);
			l.add(hl[t]);
		}
		
		numberOfFilters = datasets.size();
		numRef = refTemplates.length;
		
		datasetsCheck = new Checkbox[numberOfFilters][numRef];
		x 	= new TextField[numberOfFilters][numRef];
		y 	= new TextField[numberOfFilters][numRef];
		pc 	= new TextField[numberOfFilters][numRef];
		
		for(int i = 0; i<numberOfFilters; i++)
		{
			for(int k = 0; k<numRef; k++)
			{
				datasetsCheck[i][k] = new Checkbox(""+datasets.get(i)+" ("+refTemplates[k]+")", true);
			
				if(!positionsFromFile)
				{
					x[i][k] = new TextField("11",3); 
					y[i][k] = new TextField("11",3); 
					pc[i][k] = new TextField("5",3); 
				}
			
				l.add(datasetsCheck[i][k]);
			
				if(!positionsFromFile)
				{
					l.add(x[i][k]);
					l.add(y[i][k]);
					l.add(pc[i][k]);	
				}
			}
		}
		
		return l;
	}
	
	public JPanel makeTileID()
	{
		JPanel tID = new JPanel(new GridLayout(2,2));
		
		JLabel IDlabel = new JLabel("Tile ID");
		JLabel index   = new JLabel("i = ");
		getID = new TextField("_m{iiii}", 8); 
		getIndex = new TextField("0", 3); 
		
		tID.add(IDlabel);
		tID.add(getID);
		tID.add(index);
		tID.add(getIndex);
		
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
}