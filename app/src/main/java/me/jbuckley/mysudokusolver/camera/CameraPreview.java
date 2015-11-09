package me.jbuckley.mysudokusolver.camera;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback
{
    public static Camera camera;
    private final int cameraRotation = 90;
    private SurfaceHolder surfaceHolder;
    private Camera.Size optimalPictureSize;
    private Camera.Size optimalPreviewSize;

    public CameraPreview(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        camera.startPreview();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        try {
            camera = safeCameraOpen();
            setCamera();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        stopPreviewAndFreeCamera();
    }

    private Camera safeCameraOpen()
    {
        Camera tempCamera = null;

        try {
            stopPreviewAndFreeCamera();
            tempCamera = Camera.open();
        } catch (Exception e) {
            Log.e("Camera Preview", "Failed to open camera");
            e.printStackTrace();
        }

        return tempCamera;
    }

    private void stopPreviewAndFreeCamera()
    {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    public void setCamera()
    {
        if (camera != null) {
            requestLayout();

            try {
                camera.setPreviewDisplay(surfaceHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Camera.Parameters parameters = camera.getParameters();

            camera.setDisplayOrientation(cameraRotation);

            List<Camera.Size> supportedPictureSizes = camera.getParameters()
                    .getSupportedPictureSizes();
            List<Camera.Size> supportedPreviewSizes = camera.getParameters()
                    .getSupportedPreviewSizes();

            getOptimalSizes(supportedPreviewSizes, supportedPictureSizes);

            if (optimalPictureSize != null) {
                parameters.setPreviewSize(optimalPreviewSize.width, optimalPreviewSize.height);
                parameters.setPictureSize(optimalPictureSize.width, optimalPictureSize.height);
            }

            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            camera.setParameters(parameters);

            camera.startPreview();
        }
    }

    /**
     * This finds the optimal preview/picture size pairing based on sizes available and a matching
     * aspect ratio.
     */
    private void getOptimalSizes(List<Camera.Size> supportedPreviewSizes,
                                 List<Camera.Size> supportedPictureSizes)
    {

        // I'm concerned that this is not a sufficient way to do this.
        // In any case I'm checking for null before I assign these sizes.

        float previewRatio;
        float pictureRatio;

        for (Camera.Size previewSize : supportedPreviewSizes) {
            for (Camera.Size pictureSize : supportedPictureSizes) {
                previewRatio = (float) previewSize.width / previewSize.height;
                pictureRatio = (float) pictureSize.width / pictureSize.height;

                if (Math.abs(previewRatio - pictureRatio) < 0.0001) {
                    optimalPreviewSize = previewSize;
                    optimalPictureSize = pictureSize;
                    return;
                }
            }
        }
    }

    public void startPreview()
    {
        camera.startPreview();
    }

    public void takePicture(Camera.ShutterCallback x, Camera.PictureCallback y,
                            Camera.PictureCallback z, Camera.PictureCallback a)
    {
        camera.takePicture(x, y, z, a);
    }
}
