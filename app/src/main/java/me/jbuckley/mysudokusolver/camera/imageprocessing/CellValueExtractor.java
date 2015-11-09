package me.jbuckley.mysudokusolver.camera.imageprocessing;

import android.content.Context;
import android.graphics.Bitmap;

/**
 * Used to find all the digits from the sudoku grid.
 */
public class CellValueExtractor
{
    private final Context context;

    /**
     * Create a new CellValueExtractor to read cell values.
     *
     * @param context The application context.
     */
    public CellValueExtractor(Context context)
    {
        this.context = context;
    }

    /**
     * Using the provided binary array representation of the grid, find the values in each cell.
     *
     * @param warpedArray Binary array representation of the warped sudoku grid image.
     * @return The cell values as characters. ' ' is an empty cell.
     */
    public char[] extract(int[][] warpedArray)
    {
        char[] result = new char[81];

        int startY, startX;

        // Crop 4 pixels off each edge for an array size divisible by 9. (56);
        int cellSize = (512 - 8) / 9;

        // Loop through each cell block, finding a value if it exists.
        StringBuilder sb = new StringBuilder();

        CellOCR cellOCR = new CellOCR(context);

        for (int y = 0; y < 9; y++) {

            startY = (y * cellSize) + 4;

            for (int x = 0; x < 9; x++) {
                startX = (x * cellSize) + 4;
                int[][] croppedCell = cropCell(startY, startX, cellSize, warpedArray);
                Bitmap croppedImage = Utils.intToBitmap(croppedCell);
                CellImage cellImage = new CellImage(croppedCell, croppedImage);
                result[(y * 9) + x] = cellOCR.computeDigit(cellImage);
                sb.append(result[(y * 9) + x]);
            }
        }

        result = sb.toString().toCharArray();

        return result;
    }

    /**
     * Crop the cell image in order to only process the area where the digit is likely to be.
     *
     * @param startY Starting Y co-ordinate.
     * @param startX Starting X co-ordinate.
     * @param cellSize New size of cell. Height and Width.
     * @param warpedArray Binary array representation of the warped sudoku grid image.
     * @return The binary array section that covers the cropped cell.
     */
    private int[][] cropCell(int startY, int startX, int cellSize, int[][] warpedArray)
    {
        int cropBorder = 6;
        int totalCrop = cropBorder * 2;

        int[][] result = new int[cellSize - totalCrop][cellSize - totalCrop];

        // To keep track of co-ordinates of entire grid, and co-ordinates of the cropped cell.
        int croppedY = 0;
        int croppedX = 0;

        for (int y = startY + cropBorder; y < startY + cellSize - cropBorder; y++) {
            for (int x = startX + cropBorder; x < startX + cellSize - cropBorder; x++) {
                result[croppedY][croppedX] = warpedArray[y][x];
                croppedX++;
            }
            croppedX = 0;
            croppedY++;
        }

        return result;
    }
}
