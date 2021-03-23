package ctc5.guis;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

/**
 * Generates a form into which the user can enter the focal planes required for processing.
 * 
 * @author Brendan Ffrench: Royal College of Surgeons in Ireland.
 * @date   10 August 2017.
 * @version 1.0.0
 */
public class FocalPlanesGUI extends JFrame 
{
	private static final long serialVersionUID = -5875802220279619220L;
	
	public JTextField[][] focalPlanes;
	public JButton ok;
	public JButton cancel;
	public String[][] planes;
	
	public JScrollPane sp;
	public JPanel 		label;
	public String error = "";
	int fileNumber;
	public ArrayList<String> fileList;

	public FocalPlanesGUI(ArrayList<String> as) 
    {
    	super("CTC-5");
    	fileList = as;
    	fileNumber = as.size();
    	planes = new String[fileNumber][4];  //There will only be four quadrants. That need to be focused. However, there is no limit on the number of slices selected for each quadrant.
    }
	
    public FocalPlanesGUI(ArrayList<String> as, String e, JTextField [][] jtf) 
    {
    	super("CTC-5");
    	focalPlanes = jtf;
    	error = e;
    	fileList = as;
    	fileNumber = as.size();
    	planes = new String[fileNumber][4];  //There will only be four quadrants. That need to be focused. However, there is no limit on the number of slices selected for each quadrant.
    }
    
    public void init() 
    {
    	String[] empty = {"",""};
    	
    				label 	= makeLabelPane("", empty);
    				sp 		= makeScrollPane();
    	JPanel 		bp 		= makeButtonPane();
    	
    	getContentPane().add(label, BorderLayout.NORTH);
    	getContentPane().add(sp, BorderLayout.CENTER);
    	getContentPane().add(bp, BorderLayout.SOUTH);
    	
    	pack();
    }
    
    public JPanel makeLabelPane(String msg, String [] availableFocalPlanes){
    	error = msg;
    	
    	JPanel l = new JPanel(new GridLayout(1, 2));
    	ImageIcon quadrantsImg = new ImageIcon(FocalPlanesGUI.class.getResource("/Resources/quadrants.png"));
    	
    	JPanel left = new JPanel(new GridLayout(3,1));
    	JLabel img = new JLabel("", quadrantsImg, JLabel.CENTER);
    	JLabel a = new JLabel(msg);
    	a.setForeground(Color.RED);
    	JLabel b = new JLabel("Enter focal planes to be extracted.");
    	left.add(img);
    	left.add(a);
    	left.add(b);
    	
    	
    	JPanel promptFocalPlanes = new JPanel(new GridLayout(0, 1));
    	
    	JLabel fileName = new JLabel("Allowed Focal Planes ("+availableFocalPlanes[0]+"):");
    	promptFocalPlanes.add(fileName);
    	
    	String[] planesArray = availableFocalPlanes[1].split(",");
    	JLabel[] planes = new JLabel[planesArray.length];
    	
    	for(int i = 0; i<planesArray.length; i++){
    		planes[i] = new JLabel(planesArray[i]);
    		promptFocalPlanes.add(planes[i]);
    	}
    	
    	JScrollPane right = new JScrollPane(promptFocalPlanes);
    	
    	l.add(left);
    	l.add(right);
    
    	return l;
    }
    
    public JScrollPane makeScrollPane()
    {
    	JScrollPane scrollpane; 
    	
    	JPanel inputPanel = new JPanel();
    	Color c = inputPanel.getBackground();
    	LineBorder border = new LineBorder(c, 10);
    	inputPanel.setBorder(border);
    	
        inputPanel.setBackground(new Color(238, 238, 238));
        inputPanel.setLayout(new GridLayout(fileNumber + 1, 5, 5, 5));
        inputPanel.add(new JLabel(""));
        inputPanel.add(new JLabel("1", 0));
        inputPanel.add(new JLabel("2", 0));
        inputPanel.add(new JLabel("3", 0));
        inputPanel.add(new JLabel("4", 0));
	
        if(focalPlanes == null) 
        {
        	focalPlanes = new JTextField[fileNumber][4];
        	for (int i = 0; i < fileNumber; i++)
        	{
        		inputPanel.add(new JLabel((String)fileList.get(i)));
          
        		focalPlanes[i][0] = new JTextField("-6000, -4000, -2000", 10);
        		inputPanel.add(focalPlanes[i][0]);
          
        		focalPlanes[i][1] = new JTextField("-6000, -4000, -2000, 0", 10);
        		inputPanel.add(focalPlanes[i][1]);
          
        		focalPlanes[i][2] = new JTextField("-8000,-6000", 10);
        		inputPanel.add(focalPlanes[i][2]);
          
        		focalPlanes[i][3] = new JTextField("-8000,-6000, -4000", 10);
        		inputPanel.add(focalPlanes[i][3]);
        	}
        }
        else
        {
        	for (int i = 0; i < fileNumber; i++)
            {
              inputPanel.add(new JLabel((String)fileList.get(i)));
              inputPanel.add(focalPlanes[i][0]);
              inputPanel.add(focalPlanes[i][1]);
              inputPanel.add(focalPlanes[i][2]);
              inputPanel.add(focalPlanes[i][3]);
            }
        }
	
        inputPanel.validate();
    	scrollpane = new JScrollPane(inputPanel);
    	scrollpane.validate();

    	return scrollpane;
    }
 
    public JScrollPane updateScrollPane(JTextField[][] jtf)
    {
    	focalPlanes = jtf;
    	JScrollPane scrollpane; 
    	
    	JPanel inputPanel = new JPanel();
    	Color c = inputPanel.getBackground();
    	LineBorder border = new LineBorder(c, 10);
    	inputPanel.setBorder(border);
    	
        inputPanel.setBackground(new Color(238, 238, 238));
        inputPanel.setLayout(new GridLayout(fileNumber + 1, 5, 5, 5));
        inputPanel.add(new JLabel(""));
        inputPanel.add(new JLabel("1", 0));
        inputPanel.add(new JLabel("2", 0));
        inputPanel.add(new JLabel("3", 0));
        inputPanel.add(new JLabel("4", 0));
	
        for (int i = 0; i < fileNumber; i++)
        {
            inputPanel.add(new JLabel((String)fileList.get(i)));
            inputPanel.add(jtf[i][0]);
            inputPanel.add(jtf[i][1]);
            inputPanel.add(jtf[i][2]);
            inputPanel.add(jtf[i][3]);
        }
        	
        inputPanel.validate();
    	scrollpane = new JScrollPane(inputPanel);
    	scrollpane.validate();

    	return scrollpane;
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
    
    public int getPreferredWidth()
    {
    	int w = (int) sp.getPreferredSize().getWidth();
    	return w;
    }
}