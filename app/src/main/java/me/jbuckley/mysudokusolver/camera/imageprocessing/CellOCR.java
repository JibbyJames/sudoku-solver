package me.jbuckley.mysudokusolver.camera.imageprocessing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import me.jbuckley.mysudokusolver.R;

/**
 * Used for performing optical character recognition on a given cell image.
 */
public class CellOCR
{
    private final Context context;

    private Bitmap[][] train;

    public CellOCR(Context context)
    {
        this.context = context;

        loadTrainingData();
    }

    /**
     * Execute OCR to extract the value contained within the cell.
     *
     * @return The digit value, or ' ' representing an empty cell, as a character.
     */
    public char computeDigit(CellImage cellImage)
    {
        char result = ' ';

        // Find the exact location of the digit.
        RegionFinder rFinder = new RegionFinder();
        rFinder.showLargestRegion(cellImage.getBinaryArray());
        int[] positions = rFinder.getLargestRegionPosition();

        // Check if cell contains a digit.
        if (containsDigit(rFinder, positions, cellImage.getBinaryImage())) {

            // Crop to the exact dimensions of the digit.
            Bitmap croppedMono = cropToDigitSize(cellImage.getBinaryImage(), positions);

            // Can identify if value is one by height/width ratio of digit.
            if (isOne(croppedMono)) {
                result = '1';
            } else {
                // Scale the digit to a 5x5 image to average the pixel density of each segment.
                Bitmap fiveByFive = Bitmap.createScaledBitmap(croppedMono, 5, 5, true);

                // Find the closest matching digit using training images.
                result = computeZoning(fiveByFive);
            }
        }

        return result;
    }

    /**
     * Confirms that the cell image contains a digit or is empty.
     *
     * @param rFinder Used to store information about the largest region in the image (the digit).
     * @param positions The detected dimensions of the largest region.
     * @param binaryImage The cell image as a binary array.
     * @return True, if the cell contains a digit.
     */
    private boolean containsDigit(RegionFinder rFinder, int[] positions, Bitmap binaryImage)
    {
        boolean result;

        int digitThreshold = 200;

        result = rFinder.getLabelMaxTotal() > digitThreshold;
        result = result && digitCorrectSize(positions, binaryImage);

        return result;
    }

    /**
     * Checks that the detected digit size is what would be expected for a digit.
     *
     * @param positions The detected dimensions of the largest region.
     * @param binaryImage The cell image as a binary array.
     * @return True, if the digit dimensions match that of a typical digit.
     */
    private boolean digitCorrectSize(int[] positions, Bitmap binaryImage)
    {
        boolean result = true;

        int maxWidth = (int) (binaryImage.getWidth() * 0.9);
        int minWidth = (int) (binaryImage.getWidth() * 0.1);
        int maxHeight = (int) (binaryImage.getHeight() * 0.9);
        int minHeight = (int) (binaryImage.getWidth() * 0.1);

        int width = positions[1] - positions[3];
        int height = positions[2] - positions[0];

        for (int i = 0; i < positions.length && result; i++) {
            if (positions[i] < 0) {
                result = false;
            }
        }

        // Check size isn't too big or too small.
        result = result && width > minWidth && width < maxWidth;
        result = result && height > minHeight && height < maxHeight;

        return result;
    }

    /**
     * Compare each pixel of the 5x5 image with the training 5x5 images. The image that has the
     * smallest total difference of pixel grey values is selected as the chosen digit.
     *
     * @param fiveByFive The cell binary image scaled down to a 5x5 image.
     * @return The digit value of the closest matching digit.
     */
    private char computeZoning(Bitmap fiveByFive)
    {
        char result = ' ';

        int minDifferenceOne = Integer.MAX_VALUE;
        int currentDifference;

        for (int y = 0; y < train.length; y++) {
            for (int x = 0; x < train[y].length; x++) {

                currentDifference = getBitmapDifference(fiveByFive, train[y][x]);

                if (currentDifference < minDifferenceOne) {
                    minDifferenceOne = currentDifference;

                    // train[0] contains only images of 1s, hence the y + 1.
                    result = String.valueOf(y + 1).charAt(0);
                }
            }
        }

        return result;
    }

    /**
     * Calculate the total difference of pixel values of the two provided 5x5 images.
     *
     * @param test The cell image from the captured grid.
     * @param train The training cell being compared against.
     * @return The total difference of pixel values for all 25 pixels.
     */
    private int getBitmapDifference(Bitmap test, Bitmap train)
    {
        int result = 0;

        int[] trainRow = new int[train.getWidth()];
        int[] testRow = new int[test.getWidth()];

        int testValue;
        int trainValue;

        for (int i = 0; i < train.getHeight(); i++) {
            test.getPixels(testRow, 0, testRow.length, 0, i, testRow.length, 1);
            train.getPixels(trainRow, 0, trainRow.length, 0, i, trainRow.length, 1);
            for (int x = 0; x < testRow.length; x++) {
                testValue = Color.red(testRow[x]);
                trainValue = Color.red(trainRow[x]);
                result += Math.abs(testValue - trainValue);
            }
        }

        return result;
    }

    /**
     * Crop the image to match the dimensions of the digit.
     *
     * @param monoImage The original cell image.
     * @param positions The positions of the digit.
     * @return The cropped image.
     */
    private Bitmap cropToDigitSize(Bitmap monoImage, int[] positions)
    {
        Bitmap result;

        int startX = positions[3];
        int startY = positions[0];
        int width = positions[1] - startX;
        int height = positions[2] - startY;

        result = Bitmap.createBitmap(monoImage, startX, startY, width, height);

        return result;
    }

    /**
     * Checks if the digit contains a 1, using the width/height ratio.
     *
     * @param digit The cell image cropped to the digit dimensions.
     * @return True, if the digit contains a 1.
     */
    private boolean isOne(Bitmap digit)
    {
        return digit.getWidth() < (digit.getHeight() * 0.5);
    }

    /**
     * Get the training images from the drawable resources. Places images of 1s in the first row, 2s
     * in the second, etc. so their values are known.
     */
    private void loadTrainingData()
    {
        train = new Bitmap[9][5];

        // Get original raw 5x5 image. No scaling.
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inScaled = false;

        train[0][0] = BitmapFactory.decodeResource(context.getResources(), R.drawable.one_a, o);
        train[0][1] = BitmapFactory.decodeResource(context.getResources(), R.drawable.one_b, o);
        train[0][2] = BitmapFactory.decodeResource(context.getResources(), R.drawable.one_c, o);
        train[0][3] = BitmapFactory.decodeResource(context.getResources(), R.drawable.one_d, o);
        train[0][4] = BitmapFactory.decodeResource(context.getResources(), R.drawable.one_e, o);

        train[1][0] = BitmapFactory.decodeResource(context.getResources(), R.drawable.two_a, o);
        train[1][1] = BitmapFactory.decodeResource(context.getResources(), R.drawable.two_b, o);
        train[1][2] = BitmapFactory.decodeResource(context.getResources(), R.drawable.two_c, o);
        train[1][3] = BitmapFactory.decodeResource(context.getResources(), R.drawable.two_d, o);
        train[1][4] = BitmapFactory.decodeResource(context.getResources(), R.drawable.two_e, o);

        train[2][0] = BitmapFactory.decodeResource(context.getResources(), R.drawable.three_a, o);
        train[2][1] = BitmapFactory.decodeResource(context.getResources(), R.drawable.three_b, o);
        train[2][2] = BitmapFactory.decodeResource(context.getResources(), R.drawable.three_c, o);
        train[2][3] = BitmapFactory.decodeResource(context.getResources(), R.drawable.three_d, o);
        train[2][4] = BitmapFactory.decodeResource(context.getResources(), R.drawable.three_e, o);

        train[3][0] = BitmapFactory.decodeResource(context.getResources(), R.drawable.four_a, o);
        train[3][1] = BitmapFactory.decodeResource(context.getResources(), R.drawable.four_b, o);
        train[3][2] = BitmapFactory.decodeResource(context.getResources(), R.drawable.four_c, o);
        train[3][3] = BitmapFactory.decodeResource(context.getResources(), R.drawable.four_d, o);
        train[3][4] = BitmapFactory.decodeResource(context.getResources(), R.drawable.four_e, o);

        train[4][0] = BitmapFactory.decodeResource(context.getResources(), R.drawable.five_a, o);
        train[4][1] = BitmapFactory.decodeResource(context.getResources(), R.drawable.five_b, o);
        train[4][2] = BitmapFactory.decodeResource(context.getResources(), R.drawable.five_c, o);
        train[4][3] = BitmapFactory.decodeResource(context.getResources(), R.drawable.five_d, o);
        train[4][4] = BitmapFactory.decodeResource(context.getResources(), R.drawable.five_e, o);

        train[5][0] = BitmapFactory.decodeResource(context.getResources(), R.drawable.six_a, o);
        train[5][1] = BitmapFactory.decodeResource(context.getResources(), R.drawable.six_b, o);
        train[5][2] = BitmapFactory.decodeResource(context.getResources(), R.drawable.six_c, o);
        train[5][3] = BitmapFactory.decodeResource(context.getResources(), R.drawable.six_d, o);
        train[5][4] = BitmapFactory.decodeResource(context.getResources(), R.drawable.six_e, o);

        train[6][0] = BitmapFactory.decodeResource(context.getResources(), R.drawable.seven_a, o);
        train[6][1] = BitmapFactory.decodeResource(context.getResources(), R.drawable.seven_b, o);
        train[6][2] = BitmapFactory.decodeResource(context.getResources(), R.drawable.seven_c, o);
        train[6][3] = BitmapFactory.decodeResource(context.getResources(), R.drawable.seven_d, o);
        train[6][4] = BitmapFactory.decodeResource(context.getResources(), R.drawable.seven_e, o);

        train[7][0] = BitmapFactory.decodeResource(context.getResources(), R.drawable.eight_a, o);
        train[7][1] = BitmapFactory.decodeResource(context.getResources(), R.drawable.eight_b, o);
        train[7][2] = BitmapFactory.decodeResource(context.getResources(), R.drawable.eight_c, o);
        train[7][3] = BitmapFactory.decodeResource(context.getResources(), R.drawable.eight_d, o);
        train[7][4] = BitmapFactory.decodeResource(context.getResources(), R.drawable.eight_e, o);

        train[8][0] = BitmapFactory.decodeResource(context.getResources(), R.drawable.nine_a, o);
        train[8][1] = BitmapFactory.decodeResource(context.getResources(), R.drawable.nine_b, o);
        train[8][2] = BitmapFactory.decodeResource(context.getResources(), R.drawable.nine_c, o);
        train[8][3] = BitmapFactory.decodeResource(context.getResources(), R.drawable.nine_d, o);
        train[8][4] = BitmapFactory.decodeResource(context.getResources(), R.drawable.nine_e, o);
    }
}
