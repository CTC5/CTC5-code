package ctc5.guis;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.LineBorder;

/**
 * Generates a check list of all appropriate files found and allows the user to select which to process.
 * 
 * @author  Brendan Ffrench (Royal College of Surgeons in Ireland)
 * @date    12 December 2017.
 * @version 2.1.0
 * 
 */
public class Select_NDPI_Files_GUI extends JFrame 
{
	private static final long serialVersionUID = -5875802220279619220L;
  
    String error;
    public String[] files;
    JScrollPane sp;
    public JButton ok;
	public JButton cancel;
	public Checkbox[] ndpi_filename_checkbox_array;

    public Select_NDPI_Files_GUI(String[] f, String e) 
    {
    	super("CTC-5");
    	error = e;
    	files = f;
    }

    public void init() 
    {
    	JPanel 		label 	= makeLabelPane();
    				sp 		= makeScrollPane();
    	JPanel 		bp 		= makeButtonPane();
    	
    	getContentPane().add(label, BorderLayout.NORTH);
    	getContentPane().add(sp, BorderLayout.CENTER);
    	getContentPane().add(bp, BorderLayout.SOUTH);
    	
    	pack();
    }
    
    public JPanel makeLabelPane(){
    	
    	JPanel l = new JPanel(new GridLayout(2, 1));
    	JLabel a = new JLabel(error);
    	a.setForeground(Color.RED);
    	JLabel b = new JLabel("Select the files for extraction.");
    	
    	l.add(a);
    	l.add(b);
  	
    	return l;
    }
    
    public JScrollPane makeScrollPane()
    {
    	JScrollPane scrollpane; 
    	JPanel p = new JPanel();
    	Color c = p.getBackground();
    	LineBorder border = new LineBorder(c, 10);
    	p.setBorder(border);
    	p.setLayout(new GridLayout(0, 2, 3, 3));
	
    	ndpi_filename_checkbox_array = new Checkbox[files.length];
    	
    	for (int i = 0; i < files.length; i++) 
    	{
    		ndpi_filename_checkbox_array[i] = new Checkbox(files[i], true);
    		p.add(ndpi_filename_checkbox_array[i]);
    	}
	
    	p.validate();
    	scrollpane = new JScrollPane(p);
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