package ctc5.main;
 
import java.util.ArrayList;

import ij.IJ;
 
public class AlignmentObject 
{
    public boolean isTransformed; /** identifies if the original image needs to be horizontally flipped */
    double rough_rotation; /** holds the initial coarse step rotation */
    public double refined_rotation; /** holds the final required rotation: a function of rough_rotation plus the 
                                      * additional rotation required as returned from the affine transform points */
    double rough_scale;  /** holds the initial scaling calculated from the relative areas of the fixed and moving ROIs */
    public double refined_scale;  /** holds the final required scaling factor:  a function of rough_scale times the 
                                    * additional scaling required as returned from the affine transform points */
    public String[] moving_images; /** holds the file paths of the images that have to be
                                     * have to be scaled and rotated by this object. */
    public double[] primary_translation;
    public double[] refinedTranslation; 
    
    public int[] original_image_dimensions; 
    public double[] original_image_center_point;
     
    public AlignmentObject(String image_key, int[] originalImgDims, boolean transformed, double initial_scale, double initial_rotation, double[] movingX, double[] movingY, int[] moving_anchor, double[] fixedX, double[] fixedY, int[] fixed_anchor, double[] moving_center, double[] initial_translation, boolean is_initial_alignment)
    {
        isTransformed = transformed;
        rough_rotation = initial_rotation;
        rough_scale = initial_scale;
        IJ.log("rough_scale = "+rough_scale);
        moving_images = getMovingImages(image_key);
        original_image_dimensions = originalImgDims;
        original_image_center_point = new double[2];
        
        for(int i = 0; i<original_image_dimensions.length; i++)
        {
        	original_image_center_point[i] = ((double) original_image_dimensions[i])/2.0;
        }
         
        double[] rotation_scale = calculateRotationAndScale(rough_rotation, movingX, movingY, fixedX, fixedY);
        refined_rotation = rotation_scale[0];
        
        /** the refinedTranslation algorithm is dependent on the rotation being between -180 and +180 */
        if(refined_rotation > 180.0)
        {
        	refined_rotation = refined_rotation - 360.0;
        }
        else if(refined_rotation < -180.0)
        {
        	refined_rotation = refined_rotation + 360.0;
        }
        
        refined_scale = rough_scale/rotation_scale[1];
        
        double[][] m_pts = new double[movingX.length][2];
        double[][] f_pts = new double[movingX.length][2];
        
        for(int i = 0; i<movingX.length; i++)
        {
        	m_pts[i][0] = movingX[i];
        	m_pts[i][1] = movingY[i];
        	f_pts[i][0] = fixedX[i];
            f_pts[i][1] = fixedY[i];
        }
        
        if(is_initial_alignment)
        {
           primary_translation = primaryTranslation(m_pts, f_pts, moving_center);
           
           for(int f = 0; f<primary_translation.length; f++)
           {
        	   IJ.log("primary_translation["+f+"] = " + primary_translation[f]);
           }    
        }
        else
        {
        	primary_translation = initial_translation;
        	refinedTranslation = calculateTranslation(m_pts, f_pts, moving_center, moving_anchor, fixed_anchor);
        	
        	for(int r = 0; r<refinedTranslation.length; r++)
            {
                IJ.log("refinedTranslation["+r+"] ="+refinedTranslation[r]);
            }
        }
        
        IJ.log("refined_scale ="+refined_scale);
        IJ.log("refined_rotation ="+refined_rotation);
    }
     
    /**
     * calculateRotation takes three points shared between both images.
     * it sets one point as the anchor point and measures the distance and angle from the horizontal to each of the other points.
     * 1] by comparing changes in distances (and averaging) between the fixed and moving images it determines how to scale the moving image.
     * 2] by comparing changes in angle from the horizontal (and averaging) between the fixed and moving images it determines how to rotate the moving image.
     * @param initial
     * @param fixed_XY
     * @param moving_XY
     * @return
     */
    public double[] calculateRotationAndScale(double initial, double[] mX, double[] mY, double[] fX, double[] fY)
    {
        double[] rotationAndScale = {0.0, 0.0};
        double[] moving_dist      =  new double[2];
        double[] moving_opp       =  new double[2];
        double[] moving_angles    =  new double[2];
        double[] fixed_dist       =  new double[2];
        double[] fixed_opp        =  new double[2];
        double[] fixed_angles     =  new double[2];
        double[] delta_angles     =  new double[2];
        double[] scale            =  new double[2];
         
        for(int n = 0; n < moving_dist.length; n++)
        {
            moving_dist[n] = Math.hypot(Math.abs(mX[0]-mX[n+1]), Math.abs(mY[0]-mY[n+1]));
            IJ.log("moving_dist["+n+"] = "+moving_dist[n]);
            moving_opp[n] = mY[0]-mY[n+1];
            fixed_dist[n] = Math.hypot(Math.abs(fX[0]-fX[n+1]), Math.abs(fY[0]-fY[n+1]));
            IJ.log("fixed_dist["+n+"] = "+fixed_dist[n]);
            fixed_opp[n] = fY[0]-fY[n+1];
             
            moving_angles[n] = Math.toDegrees(Math.asin(moving_opp[n]/moving_dist[n]));
            fixed_angles[n] = Math.toDegrees(Math.asin(fixed_opp[n]/fixed_dist[n]));
            scale[n] = moving_dist[n]/fixed_dist[n];
             
            delta_angles[n] = moving_angles[n]-fixed_angles[n];
             
            IJ.log("moving_angles["+n+"] = "+moving_angles[n]);
            IJ.log("fixed_angles["+n+"] = "+fixed_angles[n]);
            IJ.log("scale["+n+"] = "+scale[n]);
        }
        
        double mov_dist_p2_p3 = Math.hypot(Math.abs(mX[1]-mX[2]), Math.abs(mY[1]-mY[2]));
        IJ.log("mov_dist_p2_p3 = "+mov_dist_p2_p3);
        double mov_opp_3 = mY[1]-mY[2];
        IJ.log("mov_opp_3 = "+mov_opp_3);
        double fix_dist_p2_p3 = Math.hypot(Math.abs(fX[1]-fX[2]), Math.abs(fY[1]-fY[2]));
        IJ.log("fix_dist_p2_p3 = "+fix_dist_p2_p3);
        double fix_opp_3 = fY[1]-fY[2];
        IJ.log("fix_opp_3 = "+fix_opp_3);
        
        double moving_angle_3 = Math.toDegrees(Math.asin(mov_opp_3/mov_dist_p2_p3));
        IJ.log("moving_angle_3 = "+moving_angle_3);
        double fixed_angle_3 = Math.toDegrees(Math.asin(fix_opp_3/fix_dist_p2_p3));
        IJ.log("fixed_angle_3 = "+fixed_angle_3);
        double scale_3 = mov_dist_p2_p3/fix_dist_p2_p3;
        IJ.log("scale_3 = "+scale_3);
        double delta_angle_3 = moving_angle_3 - fixed_angle_3;
      
        
        IJ.log("initial rotation = "+initial+"º; extra rotation = "+(((-1.0*delta_angles[0])+delta_angles[1]+delta_angle_3)/3)+"º");
         
        rotationAndScale[0] = initial+(((-1.0*delta_angles[0])+delta_angles[1]+delta_angle_3)/3);
        IJ.log("rotationAndScale[0] = "+rotationAndScale[0]);
        rotationAndScale[1] = ((scale[0]+scale[1]+scale_3)/3);
        IJ.log("rotationAndScale[1] = "+rotationAndScale[1]);
         
        return rotationAndScale;
    }
     
    /** 
     * this method calculates the final translation required to align the scaled and rotated points with the target points.
     * 	it does this via three major steps:
     * 		1) map the 'source points' in the roughly aligned and roughly rotated image obtained from the TurboReg plugin 
     * 			back to their corresponding points in the original image. 
     * 		2) then using the refined rotation and refined scaling values transform the original points to the correct scale
     * 			and rotation. 
     * 		3) finally, calculate the average of the required translations to map each of these points back to their corresponding
     * 			'target points' in the target image.
     * 
     * @param moving_points
     * @param fixed_points
     * @param m_center
     * @param m_anchor
     * @param f_anchor
     * @return
     */
    public double[] calculateTranslation(double[][] moving_points, double[][] fixed_points, double[] m_center, int[] m_anchor, int[] f_anchor)
    {
    	IJ.log("// calculateTranslation(double rotation, double scale, double[] mX, double[] mY, int[] m_anchor, double[] fX, double[] fY, int[] f_anchor, int[] m_center)");
    	

    	IJ.log("	initial: m_center[0] = "+m_center[0]);
    	IJ.log("	initial: m_center[1] = "+m_center[1]);
    	IJ.log("	initial: m_anchor[0] = "+m_anchor[0]);
    	IJ.log("	initial: m_anchor[1] = "+m_anchor[1]);
    	IJ.log("	initial: f_anchor[0] = "+f_anchor[0]);
    	IJ.log("	initial: f_anchor[1] = "+f_anchor[1]);
    	IJ.log("	initial: original_image_dimensions[0] = "+original_image_dimensions[0]);
    	IJ.log("	initial: original_image_dimensions[1] = "+original_image_dimensions[1]);
    	IJ.log("	initial: original_image_center_point[0] = "+original_image_center_point[0]);
    	IJ.log("	initial: original_image_center_point[1] = "+original_image_center_point[1]);
    	IJ.log("	rough_scale 	 = "+rough_scale);
    	IJ.log("	rough_rotation 	 = "+rough_rotation);
    	IJ.log("	refined_scale	 = "+refined_scale);
    	IJ.log("	refined_rotation = "+refined_rotation);
    	
    	for(int i = 0; i< moving_points.length; i++)
    	{
    		for(int k = 0; k< moving_points[i].length; k++)
        	{
        		IJ.log("	initial: moving_points["+i+"]["+k+"] = "+moving_points[i][k]);
        		IJ.log("	initial: fixed_points["+i+"]["+k+"] = "+fixed_points[i][k]);
        	}
    	}
    	
    	double[][] rough_translations = new double[3][2];
        double[] refined_translation = new double[2];
         
        double[][] m_points = new double[3][2];
        double[][] f_points = new double[3][2];
         
        
        /** Reverse the initial primary translation */
        m_points = reverse_translation(moving_points, primary_translation);
        for(int i = 0; i< m_points.length; i++)
    	{
    		for(int k = 0; k< m_points[i].length; k++)
        	{
        		IJ.log("	reverse primary translation: m_points["+i+"]["+k+"] = "+m_points[i][k]);
        	}
    	}
        
        /** 
         * invertY to prepare for rotation 
         * Math functions operate with the Cartesian coordinations 
         * rather than screen index coordinate systems.
         */
        m_points = invertY(m_points, m_center[1]*2*rough_scale);
        for(int i = 0; i< m_points.length; i++)
    	{
    		for(int k = 0; k< m_points[i].length; k++)
        	{
        		IJ.log("	invertY: m_points["+i+"]["+k+"] = "+m_points[i][k]);
        	}
    	}
        
        /** reverse the rough rotation*/
        m_points = rotatePoints(-1*rough_rotation, m_center, m_points, rough_scale);
        for(int i = 0; i< m_points.length; i++)
    	{
    		for(int k = 0; k< m_points[i].length; k++)
        	{
        		IJ.log("	reverse rotation: m_points["+i+"]["+k+"] = "+m_points[i][k]);
        	}
    	}
        
        /** invertY*/
        m_points = invertY(m_points, m_center[1]*2*rough_scale);
        for(int i = 0; i< m_points.length; i++)
    	{
    		for(int k = 0; k< m_points[i].length; k++)
        	{
        		IJ.log("	invertY: m_points["+i+"]["+k+"] = "+m_points[i][k]);
        	}
    	}
       
        if(isTransformed)
        {
    	  /** flip horizontally to get back to same state as original image */
    	  m_points = flip_horizontally(m_points, m_center[0]*rough_scale);
    	  for(int i = 0; i< m_points.length; i++)
      	  {
      		for(int k = 0; k< m_points[i].length; k++)
          	{
          		IJ.log("	flip horizontally: m_points["+i+"]["+k+"] = "+m_points[i][k]);
          	}
      	  }
        }
        
        /** reverse the rough scaling */
        m_points = scalePoints(m_points, (1/rough_scale));
        for(int i = 0; i< m_points.length; i++)
    	{
    		for(int k = 0; k< m_points[i].length; k++)
        	{
        		IJ.log("	reverse scaling: m_points["+i+"]["+k+"] = "+m_points[i][k]);
        	}
    	}
        
        /** 
         * account for the anchor points in both the 'moving' and 'fixed' points. 
         * The boolean at the end distinguishes between adding anchor points to the
         * moving image versus the fixed image. For the moving image the primary_translation
         * will also need to be accounted for.
         */
        m_points = addAnchorPoints(m_points, m_anchor);
        f_points = addAnchorPoints(fixed_points, f_anchor);
        
        for(int i = 0; i< m_points.length; i++)
    	{
    		for(int k = 0; k< m_points[i].length; k++)
        	{
        		IJ.log("	add anchor points: m_points["+i+"]["+k+"] = "+m_points[i][k]);
        		IJ.log("	add anchor points: f_points["+i+"]["+k+"] = "+f_points[i][k]);
        	}
    	}
      
        if(isTransformed)
        {
    	  /** flip horizontally to recreate the post affine transform conditions. */
    	  m_points = flip_horizontally(m_points, original_image_center_point[0]); 
    	  for(int i = 0; i< m_points.length; i++)
      	  {
      		for(int k = 0; k< m_points[i].length; k++)
          	{
          		IJ.log("	flip horizontally: m_points["+i+"]["+k+"] = "+m_points[i][k]);
          	}
      	  }
    	  
        }
       
        /** apply refined scaling to recreate the post affine transform conditions. */
        m_points = scalePoints(m_points, refined_scale);
        for(int i = 0; i< m_points.length; i++)
    	  {
    		for(int k = 0; k< m_points[i].length; k++)
        	{
        		IJ.log("	refine scale: m_points["+i+"]["+k+"] = "+m_points[i][k]);
        	}
    	  }
       
        /** invertY to prepare for rotation. */
        m_points = invertY(m_points, original_image_dimensions[1]*refined_scale);   
        for(int i = 0; i< m_points.length; i++)
    	{
    	    for(int k = 0; k< m_points[i].length; k++)
        	{
        		IJ.log("	invertY: m_points["+i+"]["+k+"] = "+m_points[i][k]);
        	}
    	  }
        
        /** apply the refined rotation to recreate the post affine transform conditions. */
        m_points = rotatePoints(refined_rotation, original_image_center_point, m_points, refined_scale);
        for(int i = 0; i< m_points.length; i++)
    	{
    	   for(int k = 0; k< m_points[i].length; k++)
           {
        		IJ.log("	refine rotation: m_points["+i+"]["+k+"] = "+m_points[i][k]);
           }
    	}
        
        /** invertY*/
        m_points = invertY(m_points, original_image_dimensions[1]*refined_scale);
        
        for(int i = 0; i< m_points.length; i++)
    	{
    	    for(int k = 0; k< m_points[i].length; k++)
        	{
        		IJ.log("	invertY: m_points["+i+"]["+k+"] = "+m_points[i][k]);
        	}
    	}
       
        /** calculate and average the translations and return as refined translation */
        for(int rt  = 0;  rt<rough_translations.length; rt++)
        {
            rough_translations[rt][0] = f_points[rt][0] - m_points[rt][0];
            rough_translations[rt][1] = f_points[rt][1] - m_points[rt][1];
            
            IJ.log("	rough_translations["+rt+"][0] ="+rough_translations[rt][0]);
            IJ.log("	rough_translations["+rt+"][1] ="+rough_translations[rt][1]);
        }
         
        refined_translation[0] = (rough_translations[0][0]+rough_translations[1][0]+rough_translations[2][0])/3;
        refined_translation[1] = (rough_translations[0][1]+rough_translations[1][1]+rough_translations[2][1])/3;
         
        IJ.log("	refined_translation[0] ="+refined_translation[0]);
        IJ.log("	refined_translation[1] ="+refined_translation[1]);
        
        IJ.log("// calculateTranslation(double rotation, double scale, double[] mX, double[] mY, int[] m_anchor, double[] fX, double[] fY, int[] f_anchor, int[] m_center)");
    	
        return refined_translation;
    }
     
    /** 
     * this method calculates the primary_translation that is fed into the 'second pass' refined_affineTransform 
     * it calculates the primary translation by applying the refined_scale and refined_rotation to the moving points 
     * then calculates the average translation required to make each pair of moving and fixed points overlap. 
	 *
     * @param moving_points
     * @param fixed_points
     * @param m_center
     * @return
     */
    public double[] primaryTranslation(double[][] moving_points, double[][] fixed_points, double[] m_center)
    {
    	IJ.log("// primaryTranslation(double[][] moving_points, double[][] fixed_points, double[] m_center)");
    	IJ.log("	initial: m_center[0] = "+m_center[0]);
    	IJ.log("	initial: m_center[1] = "+m_center[1]);
    	IJ.log("	initial: original_image_dimensions[0] = "+original_image_dimensions[0]);
    	IJ.log("	initial: original_image_dimensions[1] = "+original_image_dimensions[1]);
    	IJ.log("	initial: original_image_center_point[0] = "+original_image_center_point[0]);
    	IJ.log("	initial: original_image_center_point[1] = "+original_image_center_point[1]);
    	IJ.log("	rough_scale 	 = "+rough_scale);
    	IJ.log("	rough_rotation 	 = "+rough_rotation);
    	IJ.log("	refined_scale	 = "+refined_scale);
    	IJ.log("	refined_rotation = "+refined_rotation);
    	
    	for(int i = 0; i< moving_points.length; i++)
    	{
    		for(int k = 0; k< moving_points[i].length; k++)
        	{
        		IJ.log("	initial: moving_points["+i+"]["+k+"] = "+moving_points[i][k]);
        		IJ.log("	initial: fixed_points["+i+"]["+k+"] = "+fixed_points[i][k]);
        	}
    	}
    	
    	double[][] rough_translations = new double[3][2];
        double[] initial_translation = new double[2];
         
        double[][] m_points = new double[3][2];
                
        /** 
         * invertY to prepare for rotation 
         * Math functions operate with the Cartesian coordinations 
         * rather than screen index coordinate systems.
         */
        m_points = invertY(moving_points, m_center[1]*2*rough_scale);
        for(int i = 0; i< m_points.length; i++)
    	{
    		for(int k = 0; k< m_points[i].length; k++)
        	{
        		IJ.log("	invertY: m_points["+i+"]["+k+"] = "+m_points[i][k]);
        	}
    	}
        
        /** add the extra rotation to the moving points to allow primary translation to be calculated. */
        double extra_rotation = refined_rotation - rough_rotation;
        m_points = rotatePoints(extra_rotation, m_center, m_points, rough_scale);
        for(int i = 0; i< m_points.length; i++)
    	{
    		for(int k = 0; k< m_points[i].length; k++)
        	{
        		IJ.log("	rotated points: m_points["+i+"]["+k+"] = "+m_points[i][k]);
        	}
    	}
        
        /** invertY*/
        m_points = invertY(m_points, m_center[1]*2*rough_scale);
        for(int i = 0; i< m_points.length; i++)
    	{
    		for(int k = 0; k< m_points[i].length; k++)
        	{
        		IJ.log("	invertY: m_points["+i+"]["+k+"] = "+m_points[i][k]);
        	}
    	}
        
        /** add the extra scaling to allow primary translation to be calculated. */
        double extra_scale = refined_scale/rough_scale;
        m_points = scalePoints(m_points, extra_scale);
        for(int i = 0; i< m_points.length; i++)
    	{
    		for(int k = 0; k< m_points[i].length; k++)
        	{
        		IJ.log("	EXTRA scaling: m_points["+i+"]["+k+"] = "+m_points[i][k]);
        	}
    	}
       
        /** calculate and average the translations and return as primary_translation */
        for(int rt  = 0;  rt<rough_translations.length; rt++)
        {
            rough_translations[rt][0] = fixed_points[rt][0] - m_points[rt][0];
            rough_translations[rt][1] = fixed_points[rt][1] - m_points[rt][1];
            
            IJ.log("	rough_translations["+rt+"][0] ="+rough_translations[rt][0]);
            IJ.log("	rough_translations["+rt+"][1] ="+rough_translations[rt][1]);
        }
         
        initial_translation[0] = (rough_translations[0][0]+rough_translations[1][0]+rough_translations[2][0])/3;
        initial_translation[1] = (rough_translations[0][1]+rough_translations[1][1]+rough_translations[2][1])/3;
         
        IJ.log("	initial_translation[0] ="+initial_translation[0]);
        IJ.log("	initial_translation[1] ="+initial_translation[1]);
        IJ.log("//END// primaryTranslation(double[][] moving_points, double[][] fixed_points, double[] m_center)");
    	
        return initial_translation;
    }
    
    public String[] getMovingImages(String key)
    {
    	IJ.log("// getMovingImages(String key)");
    	IJ.log("	key = "+key);
        ArrayList<String>  images_list = new ArrayList<String>();
        
        if(key.equals("Giemsa"))
        {
        	IJ.log("	... adding Giemsa.");
        	images_list.add("Giemsa");
        }
        else
        {
		      /** CTC5.data.channelID_BFtemplate_pairs.get(n) holds bf to channel reference IDs. [0] holds file name. [1] holds template */
		      for(int q = 0; q <CTC5Analysis.data.channelID_BFtemplate_pairs.size(); q++)
		      {
		    	  IJ.log("	CTC5Analysis.data.channelID_BFtemplate_pairs.get("+q+")[0] = "+CTC5Analysis.data.channelID_BFtemplate_pairs.get(q)[0]);
		    	  IJ.log("	CTC5Analysis.data.channelID_BFtemplate_pairs.get("+q+")[1] = "+CTC5Analysis.data.channelID_BFtemplate_pairs.get(q)[1]);
		    	  
		    	  if(key.equals(CTC5Analysis.data.channelID_BFtemplate_pairs.get(q)[1]))
		          {
		        	  IJ.log("	... adding "+CTC5Analysis.data.channelID_BFtemplate_pairs.get(q)[0]);
		              images_list.add(CTC5Analysis.data.channelID_BFtemplate_pairs.get(q)[0]);
		          }
		      }
        }
        
        String[] images = new String[images_list.size()];
        IJ.log("	... converting image_list to array["+images.length+"]");
         
        for(int i = 0; i<images.length; i++)
        {
        	IJ.log("	images_list.get("+i+") = "+images_list.get(i));
            images[i] = images_list.get(i);
        }
        
        IJ.log("// getMovingImages(String key)");
        return images;
    }

    /** The invert Y method inverts the Y coordinates. This is need to convert from programming coordinate systems to mathematical coordinate systems and back when prior to rotations */
    public double[][] invertY(double[][] points, double image_height)
    {
    	double[][] inverted = new double[points.length][2]; /** 3 positions: top_left, top_right, bottom_right, each position has an int[] representing x and y. x index = 0, y index = 1. */
    	
    	for(int i = 0; i<points.length; i++)
    	{
    		for(int k = 0; k<points[i].length; k++)
    		{
    			if(k == 0)
    			{
    				inverted[i][k] = points[i][k];
    			}
    			else if(k == 1)
    			{
    				inverted[i][k] = image_height - points[i][k];
    			}
    			else
    			{
    				IJ.log("ERROR: unexpected index in int[][] points [method: invertY] ");
    			}
    		}
    	}
    	
    	return inverted;
    }

    public double[][] rotatePoints(double rotation_angle, double[] origin, double[][] points, double scale)
    {
    	double[][] rotated 		= new double[points.length][2];
    	double[][] temp_points 	= new double[points.length][2];
    	double[][] temp_two_points 	= new double[points.length][2];
    	
    	for(int i = 0; i<points.length; i++)
        {
            /** 
             * translate the centre of rotation to the origin 
             * ImageJ rotates from the center of the image. so for the below x',y' 
             * equations to work the center of the image must be the mathematical origin => 0,0
             */
    		temp_points[i][0] = points[i][0]-(origin[0]*scale);
    		temp_points[i][1] = points[i][1]-(origin[1]*scale);
    		IJ.log("	temp_translation["+i+"][0] ="+temp_points[i][0]);
    		IJ.log("	temp_translation["+i+"][1] ="+temp_points[i][1]);
             
            /**
             * Rotate the points:
             * x' = x.cosA - y.sinA 
             * y' = x.sinA + y.cosA
             * 
             * use the negative of double rotation to account for programming vs math coordinate system.
             */
            temp_two_points[i][0] = (temp_points[i][0]*Math.cos(Math.toRadians(-1*rotation_angle)))-(temp_points[i][1]*Math.sin(Math.toRadians(-1*rotation_angle)));
            temp_two_points[i][1] = (temp_points[i][0]*Math.sin(Math.toRadians(-1*rotation_angle)))+(temp_points[i][1]*Math.cos(Math.toRadians(-1*rotation_angle)));
            IJ.log("	temp_rotation["+i+"][0] ="+temp_two_points[i][0]);
    		IJ.log("	temp_rotation["+i+"][1] ="+temp_two_points[i][1]);
            
            
            /** translate origin back to centre */
            rotated[i][0] = temp_two_points[i][0]+(origin[0]*scale);
            rotated[i][1] = temp_two_points[i][1]+(origin[1]*scale);
            
            IJ.log("	reverse_translation["+i+"][0] ="+rotated[i][0]);
    		IJ.log("	reverse_translation["+i+"][1] ="+rotated[i][1]);
        }
    	
    	return rotated;
    }

    public double[][] flip_horizontally(double[][] points, double image_width)
    {
    	double[][] flipped = new double[points.length][2];
    	
    	for(int i = 0; i<points.length; i++)
    	{
    		for(int k = 0; k<points[i].length; k++)
    		{
    			if(k == 0)
    			{
    				flipped[i][k] = (image_width - points[i][k])+image_width;
    			}
    			else if(k == 1)
    			{
    				flipped[i][k] = points[i][k];
    			}
    			else
    			{
    				IJ.log("ERROR: unexpected index in int[][] points [method: flip_horizontally] ");
    			}
    		}
    	}
    	
    	return flipped;
    }
    
    public double[][] scalePoints(double[][] points, double scaling_factor)
    {
    	double[][] scaled =  new double[points.length][2];
    	
    	for(int i = 0; i<points.length; i++)
    	{
    		for(int k = 0; k<points[i].length; k++)
    		{
    			scaled[i][k] = points[i][k]*scaling_factor;
    		}
    	}	
    	
    	return scaled;
    }

    public double[][] addAnchorPoints(double[][] points, int[] anchor_points)
    {
    	double[][] anchored =  new double[points.length][2]; 
    	
    	for(int i = 0; i<points.length; i++)
        {
        	for(int k = 0; k<points[i].length; k++)
        	{
        		if(k == 0)
        		{
        			anchored[i][k] = points[i][k]+(double)anchor_points[0];
        		}
        		else if(k == 1)
        		{
        			anchored[i][k] = points[i][k]+(double)anchor_points[1];
        		}
        		else
        		{
        				IJ.log("ERROR: unexpected index in double[][] points [method: addAnchorPoints; class: AlignmentObject] ");
        		}
        	}
        }	

    	return anchored;
    }
    
    public double[][] reverse_translation(double[][] points, double[] offset)
    {
    	double[][] new_points = new double[3][2];
    	
    	for(int i = 0; i < points.length; i++)
    	{
    		for(int k = 0; k < points[k].length; k++)
    		{
    			new_points[i][k] = points[i][k]-offset[k];
    		}
    	}
    	
    	return new_points;
    }
}