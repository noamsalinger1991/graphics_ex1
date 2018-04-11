package edu.cg;

import java.awt.image.BufferedImage;
import java.nio.Buffer;
import java.util.*;


import java.awt.Color;


public class SeamsCarver extends ImageProcessor {
	private class MinPixelTupple{
		long minPixelValue;
		int min_x_value;
		MinPixelTupple(long minPixelValue, int min_x_value){
			this.min_x_value = min_x_value;
			this.minPixelValue = minPixelValue;
		}
	}
	//MARK: An inner interface for functional programming.
	@FunctionalInterface
	interface ResizeOperation {
		BufferedImage apply();
	}
	
	//MARK: Fields
	private int numOfSeams;
	private ResizeOperation resizeOp;
	//workingImg
	private int[][] greyImg;
	//M matrix for backtrack
	private long[][] M_Matrix;
	//from which pixel did I get here
	private int[][] minPathsArray;
	//TODO: add xIndices init(original pixel position)
	private int[][] xIndices;
	//seams list for addition or subtruction of pixels
	ArrayList<Integer[]> seamsList;
	//number of seams removed so far
    private int seamsRemoved;
	
	//MARK: Constructor
	public SeamsCarver(Logger logger, BufferedImage workingImage,
			int outWidth, RGBWeights rgbWeights) {
		super(logger, workingImage, rgbWeights, outWidth, workingImage.getHeight()); 
		
		numOfSeams = Math.abs(outWidth - inWidth);
		
		if(inWidth < 2 | inHeight < 2)
			throw new RuntimeException("Can not apply seam carving: workingImage is too small");
		
		if(numOfSeams > inWidth/2)
			throw new RuntimeException("Can not apply seam carving: too many seams...");
		
		//Sets resizeOp with an appropriate method reference
		if(outWidth > inWidth)
			resizeOp = this::increaseImageWidth;
		else if(outWidth < inWidth)
			resizeOp = this::reduceImageWidth;
		else
			resizeOp = this::duplicateWorkingImage;
		this.greyImg = getGrayScaleIMG();
		this.M_Matrix = new long[this.inHeight][this.inWidth];
		this.minPathsArray = new int[this.inHeight][this.inWidth];
		this.seamsList = new ArrayList<Integer[]>();
		for(; this.seamsRemoved < this.numOfSeams ;this.seamsRemoved++){
			calculateM_Matrix();
			removeSeam();
		}		
	}
	
	//remove the seam from the working img to avoid seams bieng picked more then once
	private void removeSeam() {
		int minRow= findMinRow();
		Integer[] seam = new Integer[this.inHeight]; 
		for(int y = this.inHeight - 1; y > -1; --y){
			seam[y] = this.xIndices[y][minRow];
			shiftRowLeft(y , minRow + 1);
			minRow = this.minPathsArray[y][minRow];
		}
		seamsList.add(seam);
	}

	private void shiftRowLeft(int y, int startIndex) {
		for (int x = startIndex; x < inWidth - seamsRemoved; x++) {
			xIndices[y][x - 1] = xIndices[y][x];
			greyImg[y][x - 1] = greyImg[y][x];
		}
	}

	private int findMinRow() {
		int minRow = 0;
		for (int y = 0; y < inWidth - seamsRemoved; y++) {
			if(M_Matrix[inHeight - 1][y] <  M_Matrix[inHeight - 1][minRow]){
				minRow = y;
			}
		}
		return minRow;
	}

	private void calculateM_Matrix() {
        this.pushForEachParameters();
        this.setForEachWidth(this.inWidth - this.seamsRemoved);
        this.forEach((y, x) -> {
			this.setMinPath(y ,x);
		});
	}

	private void setMinPath(Integer x, Integer y) {
		MinPixelTupple min = this.findMinPath(x , y);
		this.M_Matrix[y][x] = (long) this.getMagnitude(x , y) + min.minPixelValue;
		this.minPathsArray[y][x] = min.min_x_value;
	}

	private long getMagnitude(Integer y, Integer x) {
		int neighborX = x < this.inWidth - this.seamsRemoved ? x + 1 : x -1;
        return Math.abs(this.greyImg[y][neighborX] - this.greyImg[y][x]);
	}

	private MinPixelTupple findMinPath(int x, int y) {
		long leftSum = M_Matrix[y - 1][x - 1];
		long rightSum = M_Matrix[y - 1][x];
		long verticalSum = M_Matrix[y - 1][x + 1];
		long cLeft , cVertical , cRight;

		if(y == 0){
			return new MinPixelTupple(0, x);
		}

		cLeft = cVertical = cRight = (x > 0 & x + 1 < this.inWidth - this.seamsRemoved) ?
		getMagnitudeDiff(y , x - 1 , y , x + 1):
		//TODO: check impact of 0 vs 250 or max
		0;
		// add posible new neighbors(subtrection is done to avoid overflow of long)
		cLeft = (x > 0) ? 
		cLeft + getMagnitudeDiff(y - 1 , x , y , x + 1) :
		Long.MAX_VALUE -  M_Matrix[y - 1][x - 1]; 

		cRight = (x + 1 < this.inWidth - this.seamsRemoved) ?
		cRight + getMagnitudeDiff(y - 1 , x , y , x + 1) :
		Long.MAX_VALUE - M_Matrix[y - 1][x]; 

		leftSum += cLeft;
		rightSum += cRight;
		verticalSum += cVertical; 
		//return mininal path 
		MinPixelTupple min = new MinPixelTupple(verticalSum , x);
		if(leftSum < verticalSum){
			if(leftSum < rightSum){
				min = new MinPixelTupple(leftSum, x - 1);
			}
			else{
				min = new MinPixelTupple(rightSum, x + 1);
			}
		}
		return min;
	}

	private long getMagnitudeDiff(int y1, int x1, int y2, int x2) {
		return (long)Math.abs(this.greyImg[y1][x1] - this.greyImg[y2][x2]);
	}

	public int[][] getGrayScaleIMG(){
		BufferedImage graySacle = this.greyscale();
		int[][] grayScaleArrayImg = new int[inHeight][inWidth];
		this.forEach((y,x) ->{
			grayScaleArrayImg[y][x] = new Color(graySacle.getRGB(x.intValue(), y.intValue())).getRed();
		});
		return grayScaleArrayImg;
	}

	
	//MARK: Methods
	public BufferedImage resize() {
		return resizeOp.apply();
	}
	
	//MARK: Unimplemented methods
	// private BufferedImage reduceImageWidth() {
	// 	seamsList.forEach((seam) ->{
	// 		forEachHeight((y) -> { 
	// 			int x = seam[y];
	// 			img[y][x] = 
	// 		});
	// 	});

	private BufferedImage reduceImageWidth() {                                             
        this.logger.log("reduces image width by " + this.numOfSeams + " pixels.");         
        int[][] image = this.duplicateWorkingImageAs2DArray();                             
			seamsList.forEach((seam) ->{                                         
            this.forEachHeight((y) -> {                                                    
                int x = seam[y.intValue()];                                                
                int rgbMid = image[y.intValue()][x];                                       
                int rgbRight;                                                              
                int rgbMix;                                                                
                if (x > 0) {                                                               
                    rgbRight = image[y.intValue()][x - 1];                                 
                    rgbMix = mixRGB(rgbMid, rgbRight);                                     
                    image[y.intValue()][x - 1] = rgbMix;                                   
                }                                                                          
                                                                                           
                if (x + 1 < this.inWidth) {                                                
                    rgbRight = image[y.intValue()][x + 1];                                 
                    rgbMix = mixRGB(rgbMid, rgbRight);                                     
                    image[y.intValue()][x + 1] = rgbMix;                                   
                }                                                                          
                                                                                           
			});   
		});                                                                          
                                                                                          
                                                                                           
        BufferedImage ans = this.newEmptyOutputSizedImage();                               
        this.pushForEachParameters();                                                      
        this.setForEachWidth(this.outWidth);                                               
        this.forEach((y, x) -> {                                                           
            int originalX = this.xIndices[y.intValue()][x.intValue()];                     
            int rgb = image[y.intValue()][originalX];                                      
            Color c = new Color(getRed(rgb), getGreen(rgb), getBlue(rgb));                 
            ans.setRGB(x.intValue(), y.intValue(), c.getRGB());                            
        });                                                                                
        this.popForEachParameters();                                                       
        return ans;                                                                        
    }                                                                                      
                                                                                           
    private int[][] duplicateWorkingImageAs2DArray() {                                     
        int[][] image = new int[this.inHeight][this.inWidth];                              
        this.forEach((y, x) -> {                                                           
            Color c = new Color(this.workingImage.getRGB(x.intValue(), y.intValue()));     
            int r = c.getRed();                                                            
            int g = c.getGreen();                                                          
            int b = c.getBlue();                                                           
            image[y.intValue()][x.intValue()] = getRGB(r, g, b);                           
        });                                                                                
        return image;                                                                      
    }                                                                                      
                                                                                           
    private static int getRGB(int r, int g, int b) {                                       
        return r << 16 | g << 8 | b;                                                       
    }                                                                                      
                                                                                           
    private static int mixRGB(int rgb1, int rgb2) {                                        
        int r = (getRed(rgb1) + getRed(rgb2)) / 2;                                         
        int g = (getGreen(rgb1) + getGreen(rgb2)) / 2;                                     
        int b = (getBlue(rgb1) + getBlue(rgb2)) / 2;                                       
        return getRGB(r, g, b);                                                            
    }                                                                                      
                                                                                           
    private static int getRed(int rgb) {                                                   
        return rgb >> 16;                                                                  
    }                                                                                      
                                                                                           
    private static int getGreen(int rgb) {                                                 
        return rgb >> 8 & 255;                                                             
    }                                                                                      
                                                                                           
    private static int getBlue(int rgb) {                                                  
        return rgb & 255;                                                                  
    }                                                                                      
                                                                                                                                                  
																							 
	//	throw new UnimplementedMethodException("reduceImageWidth");
	
	
	private BufferedImage increaseImageWidth() {
		//TODO: Implement this method, remove the exception.
		throw new UnimplementedMethodException("increaseImageWidth");
	}
	
	public BufferedImage showSeams(int seamColorRGB) {
		//TODO: Implement this method (bonus), remove the exception.
		throw new UnimplementedMethodException("showSeams");
	}
}
