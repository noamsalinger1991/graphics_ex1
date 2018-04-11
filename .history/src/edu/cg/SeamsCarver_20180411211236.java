package edu.cg;

import java.awt.image.BufferedImage;
import java.nio.Buffer;


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
	private int[][] greyImg;
    private long[][] M_Matrix;
    private int[][] minPaths;
    private int[][] xIndices;
    private int[][] seams;
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
		this.minPaths = new int[this.inHeight][this.inWidth];
		this.seams = new int[this.numOfSeams][this.inHeight];
		this.greyImg = this.getGrayScaleIMG();
		this.findSeams();
		
	}
	
	private void findSeams() {
		for(; this.seamsRemoved < this.numOfSeams ;this.seamsRemoved++){
			calculateM_Matrix();
			removeMinSeam()
		}
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
		this.minPaths[y][x] = min.min_x_value;
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
	private BufferedImage reduceImageWidth() {
		//TODO: Implement this method, remove the exception.
		throw new UnimplementedMethodException("reduceImageWidth");
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
