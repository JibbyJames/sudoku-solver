package me.jbuckley.mysudokusolver.camera.imageprocessing;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

/**
 * Created by James on 25/02/2015.
 */
public class ImageProcessor
{
    // Grayscale the image.
    public Bitmap grayscaleBitmap(Bitmap result)
    {
        Bitmap bmwork;
        bmwork = Bitmap.createBitmap(result.getWidth(), result.getHeight(), Bitmap.Config.RGB_565);
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        Paint paint = new Paint();
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(filter);
        Canvas drawingCanvas = new Canvas(bmwork);
        drawingCanvas.drawBitmap(result, 0, 0, paint);
        return bmwork;
    }

    public int[][] adaptiveThreshold(Bitmap bitmap)
    {
        int count;
        int sum;

        // Number of neighbours
        int s = 12;

        // Percentage threshold;
        int t = 35;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int[][] result = new int[height][width];
        int[][] originalImage = new int[height][width];

        for (int i = 0; i < width; i++) {
            int row[] = new int[width];
            bitmap.getPixels(row, 0, width, 0, i, width, 1);
            for (int j = 0; j < width; j++) {
                row[j] = Color.red(row[j]);
            }
            originalImage[i] = row.clone();
        }

        int[][] integral = getIntegralImage(originalImage);

        int x1, x2, y1, y2;

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {

                x1 = i - (s / 2);
                if (x1 < 1) {
                    x1 = 1;
                }

                x2 = i + (s / 2);
                if (x2 >= width) {
                    x2 = width - 1;
                }

                y1 = j - (s / 2);
                if (y1 < 1) {
                    y1 = 1;
                }

                y2 = j + (s / 2);
                if (y2 >= height) {
                    y2 = height - 1;
                }

                count = (x2 - x1) * (y2 - y1);

                sum = integral[y2][x2] - integral[y1 - 1][x2]
                        - integral[y2][x1 - 1] + integral[y1 - 1][x1 - 1];

                if (originalImage[j][i] * count <= sum * (100 - t) / 100) {
                    result[j][i] = 1; // Black
                } else {
                    result[j][i] = 0; // White
                }
            }
        }

        return result;
    }

    private int[][] getIntegralImage(int[][] originalImage)
    {
        int width = originalImage[0].length;
        int height = originalImage.length;

        int[][] result = new int[height][width];

        // integral at current position:
        int sum = 0;

        for (int i = 0; i < width; i++) {
            sum = 0;
            for (int j = 0; j < height; j++) {
                sum = sum + originalImage[j][i];
                if (i == 0) {
                    result[j][i] = sum;
                } else {
                    result[j][i] = result[j][i - 1] + sum;
                }
            }
        }

        return result;
    }

    // Binarize the image.
    public int[][] binarize(Bitmap bitmap, int threshold)
    {
        double newThreshold = threshold;
        newThreshold = newThreshold * 0.95;

        int red;
        int newPixel;

        int imageHeight = bitmap.getHeight();
        int imageWidth = bitmap.getWidth();

        int result[][] = new int[imageHeight][imageWidth];

        for (int i = 0; i < bitmap.getWidth(); i++) {
            int row[] = new int[bitmap.getWidth()];
            bitmap.getPixels(row, 0, imageWidth, 0, i, imageWidth, 1);
            for (int j = 0; j < imageWidth; j++) {

                // Get pixels
                red = Color.red(row[j]);
                if (red > newThreshold) {
                    newPixel = 0;
                } else {
                    newPixel = 1;
                }
                row[j] = newPixel;
            }
            result[i] = row.clone();
        }

        return result;
    }

    /**
     * Get binary threshold using Otsu's method.
     * <p/>
     * Algorithm obtained from: http://www.labbookpages.co.uk/software/imgProc/otsuThreshold.html
     */
    public int otsuThreshold(Bitmap bitmap)
    {
        int[] histogram = imageHistogram(bitmap);
        int total = bitmap.getHeight() * bitmap.getWidth();

        float sum = 0;
        for (int i = 0; i < 256; i++) {
            sum += i * histogram[i];
        }

        float sumB = 0;
        int wB = 0;
        int wF = 0;

        float varMax = 0;
        int threshold = 0;

        for (int i = 0; i < 256; i++) {
            wB += histogram[i];
            if (wB != 0) {
                wF = total - wB;

                if (wF == 0) {
                    break;
                }

                sumB += (float) (i * histogram[i]);
                float mB = sumB / wB;
                float mF = (sum - sumB) / wF;

                float varBetween = (float) wB * (float) wF * (mB - mF) * (mB - mF);

                if (varBetween > varMax) {
                    varMax = varBetween;
                    threshold = i;
                }
            }
        }

        return threshold;
    }

    // Return histogram of grayscale image
    public int[] imageHistogram(Bitmap bitmap)
    {
        int[] histogram = new int[256];

        for (int i = 0; i < histogram.length; i++) {
            histogram[i] = 0;
        }

        int row[] = new int[bitmap.getWidth()];
        for (int i = 0; i < bitmap.getWidth(); i++) {
            bitmap.getPixels(row, 0, bitmap.getWidth(), 0, i, bitmap.getHeight(), 1);
            for (int j = 0; j < bitmap.getHeight(); j++) {
                int red = Color.red(row[j]);
                histogram[red]++;
            }
        }

        return histogram;
    }
}
