package ctc5.guis;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

/**
 * Generates individual JPanels that graphically report the progress of each aspect of the image Processing.
 * 
 * @author  Brendan Ffrench: Royal College of Surgeons in Ireland.
 * @date    10 August 2017.
 * @version 1.0.0
 */
public class ProgressPanel extends JPanel
{
	private static final long serialVersionUID = 9036549211449937331L;
	private double progress = 0;
	String title = "";
	String currentFile = "pending...";
	JProgressBar progressBar;
	
	public ProgressPanel(String arg,int i)
	{
		title = arg;
		progress = i;
		
		progressBar = new JProgressBar();
		progressBar.setIndeterminate(true);
		this.setLayout(new BorderLayout());
		this.add(progressBar, BorderLayout.SOUTH);
		this.repaint();
	}
	
	public void UpdateProgress(double progress_value, String file)
	{
		progress 	= progress_value;
		currentFile = file;
	}

	@Override
	public void paint(Graphics g)
	{	
		this.setSize(150,210);
		
		super.paint(g);
		Graphics2D g2 = (Graphics2D)g;
		
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON );
		g2.translate(this.getWidth()/2,this.getWidth()/2);
		g2.rotate(Math.toRadians(270));
		
		if((int)progress == 0)
		{
			progressBar.setVisible(false);
			
			Arc2D.Float arc = new Arc2D.Float(Arc2D.PIE);
			Ellipse2D circle = new Ellipse2D.Float(0,0,55,55);
			arc.setFrameFromCenter(new Point(0,0), new Point(60,60));
			circle.setFrameFromCenter(new Point(0,0), new Point(55,55));
			arc.setAngleStart(1);
			arc.setAngleExtent(360);
		
			g2.setColor(Color.LIGHT_GRAY);
			g2.draw(arc);
			g2.fill(arc);
			g2.setColor(Color.GRAY);
			g2.draw(circle);
			g2.fill(circle);
			g2.setColor(Color.LIGHT_GRAY);

			g2.rotate(Math.toRadians(90));
			g.setFont(new Font("Arial", Font.PLAIN,12));
			FontMetrics fm = g2.getFontMetrics();
			Rectangle2D r =fm.getStringBounds("Pending...", g);
			int x = (0-(int)r.getWidth()/2);
			int y= (0-(int)r.getHeight()/2+fm.getAscent());
			g2.drawString("Pending...", x,y );	
			
			g2.setColor(Color.BLACK);
			g2.translate((this.getWidth()/-2)+3,(this.getWidth()/2)+5);
			g.setFont(new Font("Arial", Font.PLAIN,12));
			r =fm.getStringBounds(title, g);
			y = (0-(int)r.getHeight()/2+fm.getAscent());
			g2.drawString(title, 0,(y+5));
			g2.translate(0,(y+5));
		}
		else if((int)progress == 100)
		{
			progressBar.setVisible(false);
			
			Arc2D.Float arc = new Arc2D.Float(Arc2D.PIE);
			Ellipse2D circle = new Ellipse2D.Float(0,0,55,55);
			arc.setFrameFromCenter(new Point(0,0), new Point(60,60));
			circle.setFrameFromCenter(new Point(0,0), new Point(55,55));
			arc.setAngleStart(1);
			arc.setAngleExtent(360);
		
			g2.setColor(Color.GREEN);
			g2.draw(arc);
			g2.fill(arc);
			g2.setColor(Color.WHITE);
			g2.draw(circle);
			g2.fill(circle);
			g2.setColor(Color.GREEN);

			g2.rotate(Math.toRadians(90));
			g.setFont(new Font("Arial", Font.PLAIN,12));
			FontMetrics fm = g2.getFontMetrics();
			Rectangle2D r =fm.getStringBounds("Done", g);
			int x = (0-(int)r.getWidth()/2);
			int y= (0-(int)r.getHeight()/2+fm.getAscent());
			g2.drawString("Done", x,y );
			
			g2.setColor(Color.BLACK);
			g2.translate((this.getWidth()/-2)+3,(this.getWidth()/2)+5);
			g.setFont(new Font("Arial", Font.PLAIN,12));
			r =fm.getStringBounds(title, g);
			y = (0-(int)r.getHeight()/2+fm.getAscent());
			g2.drawString(title, 0,(y+5));
			g2.translate(0,(y+5));
		}
		else
		{
			progressBar.setVisible(true);
			
			Arc2D.Float arc = new Arc2D.Float(Arc2D.PIE);
			Ellipse2D circle = new Ellipse2D.Float(0,0,55,55);
			arc.setFrameFromCenter(new Point(0,0), new Point(60,60));
			circle.setFrameFromCenter(new Point(0,0), new Point(55,55));
			arc.setAngleStart(1);
			arc.setAngleExtent(-progress*3.6);
		
			g2.setColor(Color.RED);
			g2.draw(arc);
			g2.fill(arc);
			g2.setColor(Color.WHITE);
			g2.draw(circle);
			g2.fill(circle);
			g2.setColor(Color.RED);

			g2.rotate(Math.toRadians(90));
			g.setFont(new Font("Arial", Font.PLAIN,12));
			FontMetrics fm = g2.getFontMetrics();
			Rectangle2D r =fm.getStringBounds((int)progress+"%", g);
			int x = (0-(int)r.getWidth()/2);
			int y= (0-(int)r.getHeight()/2+fm.getAscent());
			g2.drawString((int)progress+"%", x,y );
			
			g2.setColor(Color.BLACK);
			g2.translate((this.getWidth()/-2)+3,(this.getWidth()/2)+5);
			g.setFont(new Font("Arial", Font.PLAIN,12));
			r =fm.getStringBounds(title, g);
			y = (0-(int)r.getHeight()/2+fm.getAscent());
			g2.drawString(title, 0,(y+5));
			g2.translate(0,(y+5));
			r =fm.getStringBounds(currentFile, g);
			y = (0-(int)r.getHeight()/2+fm.getAscent());
			g.setFont(new Font("Arial", Font.PLAIN,10));
			g2.drawString(currentFile, 0,(y+5));
		}
	}
}