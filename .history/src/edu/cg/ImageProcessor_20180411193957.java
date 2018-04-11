package edu.cg;

import java.awt.Color;
import java.awt.font.GraphicAttribute;
import java.awt.image.BufferedImage;
import java.io.Console;

public class ImageProcessor extends FunctioalForEachLoops {
    
    //MARK: Fields
    public final Logger logger;
    public final BufferedImage workingImage;
    public final RGBWeights rgbWeights;
    public final int inWidth;
    public final int inHeight;
    public final int workingImageType;
    public final int outWidth;
    public final int outHeight;
    
    //MARK: Constructors
    public ImageProcessor(Logger logger, BufferedImage workingImage,
            RGBWeights rgbWeights, int outWidth, int outHeight) {
        super(); //Initializing for each loops...
        
        this.logger = logger;
        this.workingImage = workingImage;
        this.rgbWeights = rgbWeights;
        this.inWidth = workingImage.getWidth();
        inHeight = workingImage.getHeight();
        workingImageType = workingImage.getType();
        this.outWidth = outWidth;
        this.outHeight = outHeight;
        setForEachInputParameters();
    }
    
    public ImageProcessor(Logger logger,
            BufferedImage workingImage,
            RGBWeights rgbWeights) {
        this(logger, workingImage, rgbWeights,
                workingImage.getWidth(), workingImage.getHeight());
    }
    
    //MARK: Change picture hue - example
    public BufferedImage changeHue() {
        logger.log("Prepareing for hue changing...");
        
        int r = rgbWeights.redWeight;
        int g = rgbWeights.greenWeight;
        int b = rgbWeights.blueWeight;
        int max = rgbWeights.maxWeight;
        
        BufferedImage ans = newEmptyInputSizedImage();
        
        forEach((y, x) -> {
            Color c = new Color(workingImage.getRGB(x, y));
            int red = r*c.getRed() / max;
            int green = g*c.getGreen() / max;
            int blue = b*c.getBlue() / max;
            Color color = new Color(red, green, blue);
            ans.setRGB(x, y, color.getRGB());
        });
        
        logger.log("Changing hue done!");
        
        return ans;
    }
    
    
    public BufferedImage greyscale() {
        logger.log("creates a greyscale image.");
        
        BufferedImage ans = newEmptyInputSizedImage();
        forEach((y, x) -> {
            int grayLevel = GrayScalePixel(x, y);
            Color color = new Color(grayLevel, grayLevel, grayLevel);
            ans.setRGB(x, y, color.getRGB());
        });
        return ans;
    }

    
    public BufferedImage gradientMagnitude() {
        BufferedImage ans = newEmptyInputSizedImage();
        forEach((y, x) -> {
            int currentGrayPixel = GrayScalePixel(x, y);
            int NextVerticalGrayPixel = (y + 1 >= inHeight) ? GrayScalePixel(x, y - 1) : GrayScalePixel(x, y + 1);
            int NextHorizontalGrayPixel = (x + 1 >= inWidth) ? GrayScalePixel(x - 1, y) : GrayScalePixel(x + 1, y);
            int dx2 = (int) Math.pow(currentGrayPixel - NextVerticalGrayPixel, 2);
            int dy2 = (int) Math.pow(currentGrayPixel - NextHorizontalGrayPixel, 2);
            int gradientMagnitude = (int) Math.sqrt((dx2 + dy2) / 2);
            Color color = new Color(gradientMagnitude, gradientMagnitude, gradientMagnitude);
            ans.setRGB(x, y, color.getRGB());
        });
        return ans;
        
    }
    
    public BufferedImage nearestNeighbor() {
        BufferedImage ans = newEmptyOutputSizedImage();
        // BufferedImage paddedImg = padImage(workingImage);
        double heightRatio = (inHeight - 1 )/ ((double) outHeight - 1) ;
        double widthRatio = (inWidth - 1) / ((double) outWidth - 1 );
        setForEachOutputParameters();
        forEach((y, x) -> {
            ans.setRGB(x,y,workingImage.getRGB(((int)Math.round(x * widthRatio)), (int)Math.round(y * heightRatio)));
        });
        return ans;
    }
    
    
    public BufferedImage bilinear() {
        BufferedImage ans = newEmptyOutputSizedImage();
        double heightRatio = (inHeight -1 )/ ((double) outHeight - 1) ;
        double widthRatio = (inWidth - 1) / ((double) outWidth - 1 );
        setForEachOutputParameters();
        forEach((y, x) -> {
				if((x < outWidth - 1) && (y < outHeight - 1)){
                double Vx = (double) x * widthRatio;
                double Vy = (double) y * heightRatio;
                Color SE_Pixel =new Color(workingImage.getRGB((int)( Vx) + 1,(int)( Vy) + 1));
                Color SW_Pixel =new Color(workingImage.getRGB((int)( Vx),(int)( Vy) + 1));
                Color NW_Pixel =new Color(workingImage.getRGB((int)( Vx),(int)( Vy)));
                Color NE_Pixel =new Color(workingImage.getRGB((int)( Vx) + 1,(int)( Vy)));
                double u = Vx - (int)(Vx);
                double v = Vy - (int)(Vy);
                double SuRed = SE_Pixel.getRed() * u + SW_Pixel.getRed() * (1 - u);
                double SuGreen = SE_Pixel.getGreen() * u + SW_Pixel.getGreen() * (1 - u);
                double SuBlue = SE_Pixel.getBlue() * u + SW_Pixel.getBlue() * (1 - u);
                double NuRed = NE_Pixel.getRed() * u + NW_Pixel.getRed() * (1 - u);
                double NuGreen = NE_Pixel.getGreen() * u + NW_Pixel.getGreen() * (1 - u);
                double NuBlue = NE_Pixel.getBlue() * u + NW_Pixel.getBlue() * (1 - u);
                double VRed = NuRed * v + SuRed * (1 -v );
                double VGreen = NuGreen * v + SuGreen * (1 -v );
                double VBlue = NuBlue * v + SuBlue * (1 -v );
                Color V = new Color((int)VRed,(int)VGreen,(int)VBlue);
				ans.setRGB(x , y ,V.getRGB() );
				}
		});
		
        setForEachInputParameters();
        return ans;
    }

    
    //MARK: Utilities
    public final void setForEachInputParameters() {
        setForEachParameters(inWidth, inHeight);
    }
    
    public final void setForEachOutputParameters() {
        setForEachParameters(outWidth, outHeight);
    }
    
    public final BufferedImage newEmptyInputSizedImage() {
        return newEmptyImage(inWidth, inHeight);
    }
    
    public final BufferedImage newEmptyOutputSizedImage() {
        return newEmptyImage(outWidth, outHeight);
    }
    
    public final BufferedImage newEmptyImage(int width, int height) {
        return new BufferedImage(width, height, workingImageType);
    }
    
    public final BufferedImage duplicateWorkingImage() {
        BufferedImage output = newEmptyInputSizedImage();
        
        forEach((y, x) ->
        output.setRGB(x, y, workingImage.getRGB(x, y))
        );
        
        return output;
    }
    
    private BufferedImage cropIMG(BufferedImage originalImg) {
        BufferedImage output = newEmptyOutputSizedImage();
        forEach((y, x) ->
        output.setRGB(x, y, originalImg.getRGB(x, y))
        );
        return output;
    }
    
    private BufferedImage padImage(BufferedImage originalImg){
        BufferedImage output = newEmptyImage(originalImg.getWidth() + 1, originalImg.getHeight()+ 1);
        
        forEach((y, x) ->
        output.setRGB(x, y, workingImage.getRGB(x, y))
        );
        
        return output;
    }

    private int GrayScalePixel(int x ,int y ){
        int r = rgbWeights.redWeight;
        int g = rgbWeights.greenWeight;
        int b = rgbWeights.blueWeight;
        
        Color c = new Color(workingImage.getRGB(x, y));
        int red = rgbWeights.redWeight*c.getRed();
        int green = rgbWeights.greenWeight*c.getGreen();
        int blue = rgbWeights.blueWeight*c.getBlue();
        return (red + green + blue) / (r + g + b);
    }

}