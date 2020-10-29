package ctc5.guis;

import java.awt.GridLayout;
import javax.swing.JFrame;

/**
 * Generates a JFrame populated with JPanels that graphically report the overall progress of the image processing.
 * 
 * @author  Brendan Ffrench: Royal College of Surgeons in Ireland.
 * @date    10 August 2017.
 * @version 1.0.0
 */

public class ProgressFrame extends JFrame
{
	private static final long serialVersionUID = -748664141855160118L;

	public ProgressPanel extractNDPI;
	public ProgressPanel focusNDPI;
	public ProgressPanel stitchNDPI;
	public ProgressPanel processFluorescence;
	public ProgressPanel feature_analysis;
	public ProgressPanel ScaleAndAlign;
	
	public ProgressFrame()
	{
		super("CTC5: Processing Images..."); 
	}
	
	public void init() 
	{
		super.setLayout(new GridLayout(2, 4, 3, 3));
		
		extractNDPI = new ProgressPanel("Extracting .tif files:",1);
		focusNDPI = new ProgressPanel("Focusing:",0);
		stitchNDPI = new ProgressPanel("Stitching:",0);
		processFluorescence = new ProgressPanel("Processing fluorescence:",0);
		feature_analysis = new ProgressPanel("Feature Analysis:",0);
		ScaleAndAlign = new ProgressPanel("Calculate Scale and Alignment:",0);
		
		getContentPane().add(extractNDPI);
		getContentPane().add(focusNDPI);
		getContentPane().add(stitchNDPI);
		getContentPane().add(processFluorescence);
		getContentPane().add(feature_analysis);
		getContentPane().add(ScaleAndAlign);
			
		pack();
		this.setVisible(true);
	}
}