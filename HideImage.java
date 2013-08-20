/*
 * Author: Charles Guan
 * Last Modified: 2013-08-12
 * -------------------------
 * HideImage.java
 * Takes an image and encrypts it using bit offsets in a larger image.
 */

// imports
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.imageio.ImageIO;

public class HideImage {
	
	public static void main(String args[]) {
		String BACKGROUND_IMAGE = "C:/Users/guanchar/Pictures/debug.png";
		String MSG_IMAGE = "C:/Users/guanchar/Pictures/msg.png";
		String OUTPUT_IMAGE = "C:/Users/guanchar/Pictures/msg_output.png";

		BufferedImage bgd = readImage(BACKGROUND_IMAGE);
		BufferedImage evenedPixelImage = evenOutPixels(bgd);
		saveImage(evenedPixelImage, "C:/Users/guanchar/Pictures/debug_output.png");
		BufferedImage imageToHide = readImage(MSG_IMAGE);
		BufferedImage encryptedImage = hideImage(evenedPixelImage, imageToHide);
		saveImage(encryptedImage, "C:/Users/guanchar/Pictures/intermediate_debug.png");
		BufferedImage recoveredImage = retrieveImage(encryptedImage);
		saveImage(recoveredImage, OUTPUT_IMAGE);
		testDifference(OUTPUT_IMAGE, MSG_IMAGE);
	}

	public static BufferedImage evenOutPixels(BufferedImage image) {
		int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage evenedPixelImage = new BufferedImage(width, height, image.getType());
        for (int x = 0; x < width; x++) {
        	for (int y = 0; y < height; y++) {
        		int pixel =  evenPixel(image.getRGB(x,y));
        		evenedPixelImage.setRGB(x, y, pixel);
        	}
        }
        return evenedPixelImage;
	}
	
	/*
	 * hideImage encrypts a message photo into the bytes of an even pixel-valued image.
	 * Parameter: bgd -> The background image with even pixel ARGB values.
	 * Parameter: msg -> An 8+ times smaller message image to encode.
	 * Return Value: Encoded image that is visually similar to bgd.
	 */
	public static BufferedImage hideImage(BufferedImage bgd, BufferedImage msg) { 
		int x = 0, y = 0;
		int pixelCode = 0, i = 0;
		byte[] pngByteArray = createByteArray(msg, new ByteArrayOutputStream());
		System.out.println("Length: " + 	pngByteArray.length);
		
		for (byte by : pngByteArray) {
	    	for (int mask = 0x80; mask > 0; mask >>= 1) {
	    		pixelCode <<= 8;
	    		int bit = by & mask;
	    		pixelCode |= (bit > 0) ? 1 : 0;
	    		i++;
	    		if (i >= 3) {
	    			bgd.setRGB(x, y, bgd.getRGB(x, y) | pixelCode);
	    			pixelCode = 0;
	    			i = 0;
	    			y++;
	    			if (y == bgd.getHeight()) {
	    				x++;
	    				y = 0;
	    			}
	    		}
	    	}
	    }
	    return bgd;
	}
	
	/*
	 * retrieveImage uses a keyImg as a bit map. Each even byte in the keyImg is off in the hiddenImg, and
	 * each odd byte is on. The method creates a byte array from this bitmap and converts it to an image.
	 * Parameter: keyImg -> the large image in which the message image is hidden.
	 * Return Value: 
	 */
	public static BufferedImage retrieveImage(BufferedImage keyImg) {
		int width = keyImg.getWidth();
		int height = keyImg.getHeight();
		//Each byte of the hidden image requires 8/3 pixels to extract information.
		byte[] buffer = new byte[(height * width * 3 + 7) / 8];
		int byteCount = 0;
		byte by = 0;
		int bitCount = 0;
		
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int pixel = keyImg.getRGB(x, y);
				
				for (int mask = 1 << 16; mask != 0; mask >>= 8) {
					int bit = ((pixel & mask) == 0) ? 0 : 1;					
					by <<= 1;
					by |= bit;
					bitCount++;
					assert (by >> 8) == 0;
					if (bitCount >= 8) {
						buffer[byteCount] = by;
						by = 0;
						bitCount = 0;
						byteCount++;
					}
				}
			}
		}
		int length;
		for (length = buffer.length - 1; buffer[length] == 0 && length > 0; length--);
		byte[] shortBuffer = new byte[length+1];
		System.arraycopy(buffer, 0, shortBuffer, 0, length);
		System.out.println(length + " bytes");
		return readImage(new ByteArrayInputStream(shortBuffer));
	}
	
	public static void printPixel(int pixel) {
		System.out.println(((pixel >>24) & 0xff) + " " + (pixel >> 16 & 0xff) + " " + (pixel >>8 & 0xff) + " " + (pixel & 0xff));
	}
	
	/*
	 * evenPixel rounds a pixels RGB characteristics down to the closest even number.
	 * Parameter: pixel -> an 8-byte pixel where alpha = (pixel >> 24) & 0xff [alpha use is deprecated]
	 * 	, red = (pixel >> 16) & 0xff, green = (pixel >> 8) & 0xff, blue = (pixel) & 0xff
	 * Return Value: performing & with 0xfffefefe emulates the reverse operation 
	 */
	public static int evenPixel(int pixel) {
		return pixel & 0xfffefefe;
	}
 
    /*
     * This method reads an image from the file
     * @param fileLocation -- > eg. "C:/testImage.png"
     * @return BufferedImage of the file read
     */
    public static BufferedImage readImage(String fileLocation) {
        BufferedImage img = null;
        try {
            img = ImageIO.read(new File(fileLocation));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return img;
    }
    
    public static BufferedImage readImage(InputStream in) {
    	BufferedImage img = null;
    	try {
    		img = ImageIO.read(in);
		} catch (IOException e) {
            e.printStackTrace();
    	}
    	return img;
    }
    
    public static void saveImage(BufferedImage img, String fileLocation) {
    	try {
    		ImageIO.write(img, "png", new File(fileLocation));
		} catch (IOException e) {
            e.printStackTrace();
        }
    }
        
    public static byte[] createByteArray(BufferedImage img, ByteArrayOutputStream baos) {
    	byte pngByteArray[] = null;
    	try {
			ImageIO.write(img, "png", baos);
			baos.flush();
		    pngByteArray = baos.toByteArray();
		    baos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return pngByteArray;
    }
    
    public static void testDifference(String file1, String file2) {
    	BufferedImage img1 = readImage(file1);
    	BufferedImage img2 = readImage(file2);
    	if (!(img1.getHeight() == img2.getHeight() && img1.getWidth() == img2.getWidth())) System.out.println("Mismatch in size!");
    	for (int x = 0; x < img1.getWidth(); x++) {
    		for (int y = 0; y < img1.getHeight(); y++) {
    			if (img1.getRGB(x, y) != img2.getRGB(x, y)) {
    				System.out.println("Mismatch at: " + x + ", " + y);
    				printPixel(img1.getRGB(x,y));
    				printPixel(img2.getRGB(x, y));
    			}
    		}
    	}
    }
}
