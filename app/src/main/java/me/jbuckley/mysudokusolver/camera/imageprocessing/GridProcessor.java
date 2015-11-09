package me.jbuckley.mysudokusolver.camera.imageprocessing;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;

import me.jbuckley.mysudokusolver.R;
import me.jbuckley.mysudokusolver.SudokuBoardView;

/**
 * Class for handling the image processing required for reading numbers in the sudoku grid capture.
 */
public class GridProcessor
{
    private final int cameraRotation = 90;
    private final int imageSize = 512;

    private final byte[] data;
    private final Activity context;
    private final SudokuBoardView sudokuBoardView;

    private final ImageProcessor imageProcessor;

    // The warped bitmap as a 2D array of binary values.
    private int[][] warpedArray;

    /**
     * An object for processing the captured image of the sudoku grid.
     *
     * @param context The application context.
     * @param data The raw data from the camera capture.
     * @param sudokuBoardView The sudokuBoardView, used for dimensions.
     */
    public GridProcessor(Context context, byte[] data, SudokuBoardView sudokuBoardView)
    {
        this.context = ((Activity) context);
        this.data = data;
        this.sudokuBoardView = sudokuBoardView;
        this.imageProcessor = new ImageProcessor();
    }

    /**
     * Returns a 512x512 sized Bitmap that is cropped to the exact dimensions of the sudoku grid.
     * Grid is first located, then projected to the required size and orientation.
     */
    public Bitmap findGrid() throws Exception
    {
        Bitmap result;

        // Generate, scale, and crop the captured image.
        Bitmap sudokuBoardBitmap = generateBitmap(data);

        // Remove colour from bitmap.
        Bitmap monoGameBoardBitmap = imageProcessor.grayscaleBitmap(sudokuBoardBitmap);

        // Threshold the bitmap.
        int[][] binaryArray = imageProcessor.adaptiveThreshold(monoGameBoardBitmap);
        //Utils.saveToStorage(Utils.intToBitmap(binaryArray), context);

        // Find the grid corners.
        RegionFinder regionFinder = new RegionFinder();
        int[][] gameBoardBordersIntArray = regionFinder.showLargestRegion(binaryArray);
        float corners[] = findCornersOfGrid(gameBoardBordersIntArray);

        if (cornersAreValid(corners)) {
            result = androidPoly(sudokuBoardBitmap, binaryArray, corners);
        } else {
            result = null;
        }

        if (result == null) {
            throw new Exception(context.getString(R.string.project_to_gameboard_exception));
        }

        return result;
    }

    /**
     * Raw data is the photo of the entire device screen. We need to scale, rotate, and crop the
     * image so that it is what is seen from inside the guidelines when capturing the photo.
     *
     * @param data The raw data from the camera capture.
     * @return The image from inside the guidelines.
     */
    private Bitmap generateBitmap(byte[] data)
    {
        Bitmap result;

        // Decode data into a bitmap image.
        BitmapFactory.Options options = new BitmapFactory.Options();
        result = BitmapFactory.decodeByteArray(data, 0, data.length, options);

        // Get screen dimensions.
        Point screenSize = new Point();
        context.getWindowManager().getDefaultDisplay().getRealSize(screenSize);

        // Create scaling and rotation matrix.
        Matrix m = new Matrix();
        m.setScale((float) screenSize.y / result.getWidth(),
                (float) screenSize.x / result.getHeight());
        m.postRotate(cameraRotation);

        // Rotate and scale bitmap using the matrix.
        result = Bitmap.createBitmap(result, 0, 0, result.getWidth(), result.getHeight(), m, true);

        // Get co-ordinates of the grid guidelines.
        int startX = Math.round(sudokuBoardView.getX());
        int startY = Math.round(sudokuBoardView.getY()
                + context.getResources().getDimension(R.dimen.activity_vertical_margin));
        int gameBoardSize = Math.round(sudokuBoardView.getWidth());

        // Create final bitmap as cropped image containing just the game board grid.
        result = Bitmap.createBitmap(result, startX, startY, gameBoardSize, gameBoardSize);

        // Compress the bitmap to 512x512 for faster image processing computations.
        Bitmap bmwork = Bitmap.createBitmap(imageSize, imageSize, Bitmap.Config.RGB_565);
        Rect destRect = new Rect(0, 0, imageSize, imageSize);
        Rect origRect = new Rect(0, 0, result.getWidth(), result.getHeight());
        Canvas canvas = new Canvas(bmwork);
        canvas.drawBitmap(result, origRect, destRect, null);

        result = Bitmap.createBitmap(bmwork, 0, 0, imageSize, imageSize);

        return result;
    }

    /**
     * Checks that the detected grid corners form a square.
     *
     * @param corners The corner positions.
     * @return True, if corners resemble a square.
     */
    private boolean cornersAreValid(float[] corners)
    {
        boolean result = true;

        for (int i = 0; i < corners.length && result; i++) {
            if (!(corners[i] > 0)) {
                result = false;
            }
        }

        if (result) {

            double angle;
            float angleThreshold = 15;
            float[][] points = new float[4][];

            // Check top-left, top-right, bottom-right.
            points[0] = new float[]{corners[0], corners[1], corners[2], corners[3], corners[4],
                    corners[5]};

            // Check top-right, bottom-right, bottom-left.
            points[1] = new float[]{corners[2], corners[3], corners[4], corners[5], corners[6],
                    corners[7]};

            // Check bottom-right, bottom-left, top-left.
            points[2] = new float[]{corners[4], corners[5], corners[6], corners[7], corners[0],
                    corners[1]};

            // Check bottom-left, top-left, top-right.
            points[3] = new float[]{corners[6], corners[7], corners[0], corners[1], corners[2],
                    corners[3]};

            for (int i = 0; i < points.length && result; i++) {
                angle = findAngle(points[i]);
                if (angle > 90 + angleThreshold || angle < 90 - angleThreshold) {
                    result = false;
                }
            }
        }

        return result;
    }

    /**
     * Finds the angle between three points.
     *
     * @param points The three points as x1, y1, x2, y2, x3, y3.
     * @return The acute angle formed by the points.
     */
    private double findAngle(float[] points)
    {
        float Ax = points[0];
        float Ay = points[1];
        float Bx = points[2];
        float By = points[3];
        float Cx = points[4];
        float Cy = points[5];

        double a = Math.pow(Bx - Ax, 2) + Math.pow(By - Ay, 2);
        double b = Math.pow(Bx - Cx, 2) + Math.pow(By - Cy, 2);
        double c = Math.pow(Cx - Ax, 2) + Math.pow(Cy - Ay, 2);

        return Math.acos((a + b - c) / Math.sqrt(4 * a * b)) * (180 / Math.PI);
    }

    /**
     * Search diagonally from each corner of the image, find the corners of the captured grid.
     *
     * @param binaryArray The image as a binary array. Will contain just the largest region.
     * @return The grid corner co-ordinates.
     */
    private float[] findCornersOfGrid(int[][] binaryArray)
    {
        float[] result = new float[8];
        int length = binaryArray.length;
        boolean found;

        // Top left
        found = false;
        for (int i = 0; i < length && !found; i++) {
            for (int j = 0; j <= i && !found; j++) {
                int k = i - j;
                if (binaryArray[j][k] == 1) {
                    result[0] = k;
                    result[1] = j;
                    found = true;
                }
            }
        }

        // Top right.
        found = false;
        for (int i = 0; i < length && !found; i++) {
            int ii = length - 1 - i;
            for (int j = 0; j <= i && !found; j++) {
                int k = ii + j;
                if (binaryArray[j][k] == 1) {
                    result[2] = k;
                    result[3] = j;
                    found = true;
                }
            }
        }

        // Bottom right.
        found = false;
        for (int i = 0; i < length && !found; i++) {
            int ii = length - 1 - i;
            for (int j = 0; j <= i && !found; j++) {
                int jj = length - 1 - j;
                int k = ii + j;
                if (binaryArray[jj][k] == 1) {
                    result[4] = k;
                    result[5] = jj;
                    found = true;
                }
            }
        }

        // Bottom left.
        found = false;
        for (int i = 0; i < length && !found; i++) {
            for (int j = 0; j <= i && !found; j++) {
                int jj = length - 1 - j;
                int k = i - j;
                if (binaryArray[jj][k] == 1) {
                    result[6] = k;
                    result[7] = jj;
                    found = true;
                }
            }
        }

        return result;
    }

    /**
     * Using Androids setPolyToPoly method, project the grid to the sudoku board dimensions.
     *
     * @param sudokuBoardBitmap Image of grid inside guidelines.
     * @param binaryArray The image as a binary array.
     * @param corners The grid corners.
     * @return The projected Bitmap, in colour, in the dimensions of the sudoku board.
     */
    private Bitmap androidPoly(Bitmap sudokuBoardBitmap, int[][] binaryArray, float[] corners)
    {
        warpedArray = new int[512][512];

        float width = binaryArray.length;

        Bitmap result = Bitmap.createBitmap(Math.round(width), Math.round(width),
                Bitmap.Config.RGB_565);

        Matrix matrix = new Matrix();

        float[] src = {0, 0, width - 1, 0, width - 1, width - 1, 0, width - 1};
        matrix.setPolyToPoly(src, 0, corners, 0, 4);

        float[] points = new float[9];
        matrix.getValues(points);

        double a = points[0];
        double b = points[1];
        double c = points[2];
        double d = points[3];
        double e = points[4];
        double f = points[5];
        double g = points[6];
        double h = points[7];

        Double warpedX;
        Double warpedY;
        int warpedPixel, warpedBinaryValue;

        for (int y = 0; y < width; y++) {
            for (int x = 0; x < width; x++) {

                warpedX = ((a * x) + (b * y) + c) / ((g * x) + (h * y) + 1);
                warpedY = ((d * x) + (e * y) + f) / ((g * x) + (h * y) + 1);

                if (warpedY >= 0 && warpedY < width && warpedX >= 0 && warpedX < width) {

                    // Set the warped pixel from the bitmap image for the warped image.
                    warpedPixel = sudokuBoardBitmap
                            .getPixel(warpedX.intValue(), warpedY.intValue());
                    result.setPixel(x, y, warpedPixel);

                    // Set the warped value from the 2D array representation of the image.
                    warpedBinaryValue = binaryArray[warpedY.intValue()][warpedX.intValue()];
                    warpedArray[y][x] = warpedBinaryValue;

                }
            }
        }

        return result;
    }

    /**
     * Executed after the grid has been found. Read the values contained within each cell.
     *
     * @return The cell values.
     */
    public char[] readValues()
    {
        char[] result;

        CellValueExtractor cellValueExtractor = new CellValueExtractor(context);
        result = cellValueExtractor.extract(warpedArray);

        return result;
    }
}
