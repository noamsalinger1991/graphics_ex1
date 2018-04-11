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
	private int[][] greyScaleImage;
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
		this.greyScaleImage = getGrayScaleIMG();
		this.M_Matrix = new long[this.inHeight][this.inWidth];
		this.minPaths = new int[this.inHeight][this.inWidth];
		this.seams = new int[this.numOfSeams][this.inHeight];
		this.greyScaleImage = this.getGrayScaleIMG();
		this.findSeams();
		//TODO: Initialize your additional fields and apply some preliminary calculations:
		
	}
	
	private void findSeams() {
		for(; this.seamsRemoved < this.numOfSeams ;this.seamsRemoved++){
			calculateM_Matrix();

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
		MinPixelTupple min = this.findMinCost(x , y);
		this.M_Matrix[y][x] = (long) this.getMagnitude(x , y) + min.minPixelValue;
		this.minPaths[y][x] = min.min_x_value;
	}

	private long getMagnitude(Integer y, Integer x) {
		int neighborX = x < this.inWidth - this.seamsRemoved ? x + 1 : x -1;
        return Math.abs(this.greyScaleImage[y][neighborX] - this.greyScaleImage[y][x]);
	}

	private MinPixelTupple findMinCost(int x, int y) {
		long minPixelValue  = 0;
		int min_x_value = x;
		if(y <= 0){
			return new MinPixelTupple(minPixelValue, min_x_value);
		}
		float minL = M_Matrix[][]

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