package me.jbuckley.mysudokusolver.camera.imageprocessing;

import java.util.Arrays;
import java.util.Stack;

/** Class used to locate and isolate individual regions in binary images. */
public class RegionFinder
{
    // Total area of the maximum label.
    private int labelMaxTotal;

    // The label of the largest region.
    private int labelMax;

    // The values of the largest regions position.
    private int top, right, bottom, left;

    public RegionFinder()
    {
        top = Integer.MIN_VALUE;
        right = Integer.MAX_VALUE;
        bottom = Integer.MAX_VALUE;
        left = Integer.MIN_VALUE;
    }

    /**
     * After regions have been labelled, show only the largest one.
     *
     * @param image The original binary image of the captured grid.
     * @return The image with the largest region/blob singled out.
     */
    public int[][] showLargestRegion(int[][] image)
    {
        int[][] result = new int[image.length][image.length];
        int[][] labelledRegions = labelRegions(image);

        // Identify the largest region with 1 and all the others regions with 0.
        for (int x = 0; x < labelledRegions.length; x++) {
            for (int y = 0; y < labelledRegions.length; y++) {
                if (labelledRegions[y][x] == labelMax) {
                    result[y][x] = 1;
                    storeSizeData(x, y);
                } else {
                    result[y][x] = 0;
                }
            }
        }

        return result;
    }

    /**
     * Store the highest/lowest width and height values.
     *
     * @param x X co-ordinate.
     * @param y Y co-ordinate.
     */
    private void storeSizeData(int x, int y)
    {
        // Because it is an array, x is technically the y co-ordinate.
        if (y > top) {
            top = y;
        }
        if (x < right) {
            right = x;
        }
        if (y < bottom) {
            bottom = y;
        }
        if (x > left) {
            left = x;
        }
    }

    /**
     * Label the regions using flood filling.
     *
     * @param image The original binary image of the captured grid.
     * @return The binary image with numbered regions.
     */
    private int[][] labelRegions(int[][] image)
    {
        int length = image.length;

        int[][] result = new int[length][length];
        for (int i = 0; i < length; i++) {
            result[i] = Arrays.copyOf(image[i], length);
        }

        int label = 2;

        labelMax = 0;
        labelMaxTotal = 0;

        // label all the regions
        for (int x = 0; x < length; x++) {
            for (int y = 0; y < length; y++) {
                if (result[x][y] == 1) {
                    result = floodFill(x, y, result, label);
                    label++;
                }
            }
        }

        return result;
    }

    /**
     * The flood fill technique. Obtained from "Principles of Digital Image Processing: Core
     * Algorithms" by Burger and Burge.
     */
    private int[][] floodFill(int startX, int startY, int[][] image, int label)
    {
        int width = image[0].length;
        int height = image.length;

        int currentLabelTotal = 0;

        Stack<int[]> stack = new Stack();
        int[] next;

        stack.push(new int[]{startX, startY});

        while (!stack.isEmpty()) {

            currentLabelTotal++;
            next = stack.pop();

            int x = next[0];
            int y = next[1];

            image[x][y] = label;

            if (y - 1 >= 0 && image[x][y - 1] == 1) {
                stack.push(new int[]{x, y - 1});
            }

            if (y + 1 < height && image[x][y + 1] == 1) {
                stack.push(new int[]{x, y + 1});
            }

            if (x - 1 >= 0 && image[x - 1][y] == 1) {
                stack.push(new int[]{x - 1, y});
            }

            if (x + 1 < width && image[x + 1][y] == 1) {
                stack.push(new int[]{x + 1, y});
            }

            if (x - 1 >= 0 && y - 1 >= 0 && image[x - 1][y - 1] == 1) {
                stack.push(new int[]{x - 1, y - 1});
            }

            if (x + 1 < width && y - 1 >= 0 && image[x + 1][y - 1] == 1) {
                stack.push(new int[]{x + 1, y - 1});
            }

            if (x - 1 >= 0 && y + 1 < height && image[x - 1][y + 1] == 1) {
                stack.push(new int[]{x - 1, y + 1});
            }

            if (x + 1 < width && y + 1 < height && image[x + 1][y + 1] == 1) {
                stack.push(new int[]{x + 1, y + 1});
            }
        }

        if (currentLabelTotal > labelMaxTotal) {
            labelMaxTotal = currentLabelTotal;
            labelMax = label;
        }

        return image;
    }

    public int[] getLargestRegionPosition()
    {
        // Values flipped to give standard top/right/bottom/left representation, instead of max.
        return new int[]{bottom, left, top, right};
    }

    public int getLabelMaxTotal()
    {
        return labelMaxTotal;
    }
}
