package me.jbuckley.mysudokusolver.camera.imageprocessing;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Contains general image processing methods.
 */
public class Utils
{
    /**
     * Converts the int array into a viewable bitmap.
     *
     * @param image The binary image as a 2D array.
     * @return A Bitmap of the binary array. White for 0s, black for 1s.
     */
    public static Bitmap intToBitmap(int[][] image)
    {
        Bitmap result = Bitmap.createBitmap(image.length, image.length, Bitmap.Config.RGB_565);

        int row[];

        for (int i = 0; i < image.length; i++) {
            row = Arrays.copyOf(image[i], image[i].length);
            for (int j = 0; j < row.length; j++) {
                if (row[j] == 0) {
                    row[j] = Color.rgb(255, 255, 255);
                } else {
                    row[j] = Color.rgb(0, 0, 0);
                }
            }
            result.setPixels(row, 0, image.length, 0, i, row.length, 1);
        }

        return result;
    }

    // Save the passed bitmap to the external storage.
    public static String saveToStorage(Bitmap bitmapImage, Context context)
    {
        String result;

        File file = createImageFile();

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 90, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Show in gallery to allow test images to be seen.
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);

        result = file.getAbsolutePath();

        return result;
    }

    // Create a file of the image we'll be saving here.
    private static File createImageFile()
    {
        File result = null;

        String imageFileName = "currentGrid";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);

        try {
            result = File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }
}
