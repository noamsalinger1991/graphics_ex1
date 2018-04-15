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
		this.initXIndices();
		for(; this.seamsRemoved < this.numOfSeams ;this.seamsRemoved++){
			calculateM_Matrix();
			removeSeam();
		}		
	}
	//before changes set indicates to original position
    private void initXIndices() {                                                 
        this.xIndices = new int[this.inHeight][this.inWidth];                      
        this.forEach((y, x) -> {                                                   
            this.xIndices[y][x] = x;              
        });                                                                        
    }                                                                              

	private void calculateM_Matrix() {
		this.pushForEachParameters();
		this.setForEachHeight(this.inHeight);
        this.setForEachWidth(this.inWidth - this.seamsRemoved);
        this.forEach((y, x) -> {
			this.setMinPath(y ,x);
		});
	}

	private void setMinPath( Integer y ,Integer x) {
		if(y == inHeight){
			return;
		}
		MinPixelTupple min = this.findMinPath(y , x);
		this.M_Matrix[y][x] = (long) this.getMagnitude(y , x) + min.minPixelValue;
		this.minPathsArray[y][x] = min.min_x_value;
	}

	private long getMagnitude(Integer y, Integer x) {
		int neighborX = x < this.inWidth - 1 - this.seamsRemoved ? x + 1 : x -1;
        return Math.abs(this.greyImg[y][neighborX] - this.greyImg[y][x]);
	}

	private MinPixelTupple findMinPath(int y, int x) {
		if(y == 0 ){
			return new MinPixelTupple(0, x);
		}

		long leftSum = (x > 0) ? M_Matrix[y - 1][x - 1] : 0;
		long rightSum = M_Matrix[y - 1][x];
		long verticalSum = (x + 1 < inWidth - this.seamsRemoved) ?
		M_Matrix[y - 1][x + 1] :
		0;
		long cLeft , cVertical , cRight;

		cLeft = cVertical = cRight = (x > 0 & x + 1 < this.inWidth - this.seamsRemoved) ?
		getMagnitudeDiff(y , x - 1 , y , x + 1):
		//TODO: check impact of 0 vs 250 or maxValue
		0;
		// add posible new neighbors(subtrection is done to avoid overflow of long)
		cLeft = (x > 0) ? 
		cLeft + getMagnitudeDiff(y - 1 , x , y , x - 1) :
		Long.MAX_VALUE ; 

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
		if(rightSum < verticalSum){
			min = new MinPixelTupple(rightSum, x + 1);
		}
		return min;
	}

	private long getMagnitudeDiff(int y1, int x1, int y2, int x2) {
		return (long)Math.abs(this.greyImg[y1][x1] - this.greyImg[y2][x2]);
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
	
	// MARK: Unimplemented methods
	//TODO:countinue from here
	private BufferedImage reduceImageWidth() {
		seamsList.forEach((seam) ->{

		});
		return null;
	}
	
	private BufferedImage increaseImageWidth() {
		//TODO: Implement this method, remove the exception.
		throw new UnimplementedMethodException("increaseImageWidth");
	}
	
	public BufferedImage showSeams(int seamColorRGB) {
		//TODO: Implement this method (bonus), remove the exception.
		throw new UnimplementedMethodException("showSeams");
	}
}
