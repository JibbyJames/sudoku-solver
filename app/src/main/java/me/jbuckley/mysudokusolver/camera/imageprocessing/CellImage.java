package me.jbuckley.mysudokusolver.camera.imageprocessing;

import android.graphics.Bitmap;

/**
 * An image of a cell from the captured sudoku grid. Contains both the Bitmap image and the integer
 * array representing the binary image.
 */
public class CellImage
{
    private final Bitmap binaryImage;

    private int[][] binaryArray;

    /**
     * Create a cell image for storing information required to read the digit value.
     *
     * @param binaryArray The cell image as a binary array.
     * @param binaryImage The cell binary image.
     */
    public CellImage(int[][] binaryArray, Bitmap binaryImage)
    {
        this.binaryArray = binaryArray;
        this.binaryImage = binaryImage;
    }

    public int[][] getBinaryArray()
    {
        return binaryArray;
    }

    public Bitmap getBinaryImage()
    {
        return binaryImage;
    }
}
