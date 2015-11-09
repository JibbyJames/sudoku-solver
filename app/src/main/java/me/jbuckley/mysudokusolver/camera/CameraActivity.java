package me.jbuckley.mysudokusolver.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import me.jbuckley.mysudokusolver.AsyncSudokuSolver;
import me.jbuckley.mysudokusolver.CellView;
import me.jbuckley.mysudokusolver.R;
import me.jbuckley.mysudokusolver.SudokuBoardView;
import me.jbuckley.mysudokusolver.camera.imageprocessing.GridProcessor;
import me.jbuckley.mysudokusolver.styles.CameraCellStyle;

/** Take a photo of a sudoku grid and read the values for quick input. */
public class CameraActivity extends Activity
{
    Camera.PictureCallback pictureCallback = new Camera.PictureCallback()
    {
        @Override
        public void onPictureTaken(byte[] data, Camera camera)
        {
            extractGrid(data);
        }
    };

    private Button editButton;
    private Button saveButton;
    private Button captureButton;
    private Button[] digits;
    private CameraPreview cameraPreview;
    private SudokuBoardView sudokuBoardView;
    private ImageView backgroundImage;
    private GridLayout digitsLayout;
    private CellView selectedCell;
    private Context context;
    private ProgressBar progressBar;
    private TextView topText;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);

        editButton = (Button) findViewById(R.id.edit_button);
        saveButton = (Button) findViewById(R.id.save_button);
        captureButton = (Button) findViewById(R.id.capture_button);
        sudokuBoardView = (SudokuBoardView) findViewById(R.id.gameBoardLayout_camera);
        backgroundImage = (ImageView) findViewById(R.id.sudoku_background_image);
        cameraPreview = (CameraPreview) findViewById(R.id.my_camera_preview);
        digitsLayout = (GridLayout) findViewById(R.id.digits);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        topText = (TextView) findViewById(R.id.top_text);

        context = getApplicationContext();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        initialize();
    }

    /** Perform initial setup before an image has been captured. */
    private void initialize()
    {
        initSudokuBoard();
        initButtonsBeforeCapture();

        backgroundImage.setVisibility(View.INVISIBLE);
        topText.setText(getResources().getString(R.string.align_grid));
    }

    /** Initialise the sudoku board. */
    private void initSudokuBoard()
    {
        sudokuBoardView.setCellStyle(new CameraCellStyle(this));
        sudokuBoardView.setCellsVisibility(View.INVISIBLE);
        sudokuBoardView.resetCellValues();
        sudokuBoardView.showGridLines(false);

        sudokuBoardView.setEnabled(false);
        sudokuBoardView.setCellsEnabled(false);

        selectedCell = null;
    }

    /** Initialise the buttons before the image has been captured. */
    private void initButtonsBeforeCapture()
    {
        initCaptureButton();

        editButton.setOnClickListener(editOnClick());
        saveButton.setOnClickListener(saveOnClick());

        disableButton(editButton);
        disableButton(saveButton);

        digitsLayout.setVisibility(View.INVISIBLE);
    }

    /** Initialise the capture button. */
    private void initCaptureButton()
    {
        captureButton.setBackground(getResources().getDrawable(R.drawable.light_button));
        captureButton.setText(getResources().getString(R.string.capture_button));
        captureButton.setOnClickListener(captureOnClick());
        captureButton.setTextColor(getResources().getColor(R.color.black));
    }

    /** Initialise the buttons after the image has been captured. */
    private void initButtonsAfterCapture()
    {
        initDiscardButton();

        enableButton(editButton);
        enableButton(saveButton);
    }

    /** Initialise the discard button. */
    private void initDiscardButton()
    {
        captureButton.setText(getResources().getString(R.string.discard_button));
        captureButton.setBackground(getResources().getDrawable(R.drawable.red_button));
        captureButton.setOnClickListener(discardOnClick());
        captureButton.setTextColor(getResources().getColor(R.color.white));
    }

    /** Initialise the digits once the edit button has been pressed. */
    private void initDigitsOnEdit()
    {
        if (digits == null) {
            digits = new Button[10];

            for (int i = 0; i < digits.length; i++) {
                digits[i] = new Button(this);
                digits[i].setTextSize(21);
                digits[i].setGravity(Gravity.CENTER);
                digits[i].setText(Integer.toString(i + 1));
                digits[i].setOnClickListener(digitOnClick(digits[i]));
                digits[i].setPadding(0, 0, 0, 0);

                digits[i].setBackground(getResources().getDrawable(R.drawable.digit_light_button));

                digitsLayout.addView(digits[i]);
            }

            digits[9].setText(String.valueOf(' '));
        }

        digitsLayout.setVisibility(View.VISIBLE);
    }

    /** Initialise the cells once the edit button has been pressed. */
    private void initCellsOnEdit()
    {
        sudokuBoardView.setEnabled(true);

        for (CellView cell : sudokuBoardView.getCells()) {
            cell.setOnFocusChangeListener(cellOnFocus(cell));
            cell.setEnabled(true);
        }
    }

    /**
     * Take the captured camera data, and extract the grid. Perform in background.
     *
     * @param data The raw camera captured image data.
     */
    private void extractGrid(byte[] data)
    {
        GridProcessor gridFinder = new GridProcessor(this, data, sudokuBoardView);
        AsyncGridValueExtractor extractor = new AsyncGridValueExtractor(gridFinder);

        extractor.execute();
    }

    /**
     * Display the read values on the sudoku grid.
     *
     * @param cellValues The read values.
     * @param warpedBitmap The grid stretched to fill the dimensions of the sudoku grid.
     */
    private void showCapturedValues(char[] cellValues, Bitmap warpedBitmap)
    {
        //Send the cell values to the SudokuBoardLayout
        sudokuBoardView.setCellValues(cellValues);

        // Set background backgroundImage of the sudoku board as the warped bitmap.
        setBackgroundImage(warpedBitmap);

        topText.setText(getResources().getString(R.string.verify_cells));

        // Make them visible.
        sudokuBoardView.setCellsVisibility(View.VISIBLE);
        backgroundImage.setVisibility(View.VISIBLE);

        // Change the Capture button to a Discard button.
        initButtonsAfterCapture();
    }

    /**
     * Sets the background image of the sudokuBoardView.
     *
     * @param warpedBitmap The image.
     */
    private void setBackgroundImage(Bitmap warpedBitmap)
    {
        Bitmap resized = Bitmap.createScaledBitmap(warpedBitmap, sudokuBoardView.getMeasuredWidth(),
                sudokuBoardView.getMeasuredHeight(), true);
        backgroundImage.setImageBitmap(resized);
    }

    /**
     * Disable the button, change its opacity.
     *
     * @param button The button to disable.
     */
    private void disableButton(Button button)
    {
        button.setEnabled(false);
        button.getBackground().setAlpha(getResources().getInteger(R.integer.button_disabled_alpha));
    }

    /**
     * Enable the button, change its opacity.
     *
     * @param button The button to enable.
     */
    private void enableButton(Button button)
    {
        button.setEnabled(true);
        button.getBackground().setAlpha(getResources().getInteger(R.integer.button_enabled_alpha));
    }

    /** The capture button is pressed, so find grid and read values. */
    private View.OnClickListener captureOnClick()
    {
        return new View.OnClickListener()
        {
            public void onClick(View view)
            {
                // Disable the capture button.
                disableButton(captureButton);

                // Take the photo.
                cameraPreview.takePicture(null, null, null,
                        pictureCallback); // Make third param show loading bar
            }
        };
    }

    /** The edit button is pressed, so enable cells and display digits. */
    private View.OnClickListener editOnClick()
    {
        return new View.OnClickListener()
        {
            public void onClick(View view)
            {
                initDigitsOnEdit();
                initCellsOnEdit();
            }
        };
    }

    /** The discard button is pressed, clear read values and image, start camera preview. */
    private View.OnClickListener discardOnClick()
    {
        return new View.OnClickListener()
        {
            public void onClick(View view)
            {
                initialize();
                cameraPreview.startPreview();
            }
        };
    }

    /** Save is clicked, verifiy cells are correct, before launching new activity. */
    private View.OnClickListener saveOnClick()
    {
        return new View.OnClickListener()
        {
            public void onClick(View view)
            {
                topText.setText(getResources().getString(R.string.verifying_cells));
                AsyncSudokuSolver solver = new AsyncSudokuSolver(CameraActivity.this, progressBar,
                        sudokuBoardView.getCellValues());
                solver.execute();
            }
        };
    }

    /** Digit has been pressed, set selected cell value as digit value. */
    private View.OnClickListener digitOnClick(final Button digit)
    {
        return new View.OnClickListener()
        {
            public void onClick(View view)
            {
                if (selectedCell != null) {
                    selectedCell.setText(String.valueOf(digit.getText()));
                }
            }
        };
    }

    /** If cell is focused, set as selected cell. If lost focus, deselect cell. */
    private View.OnFocusChangeListener cellOnFocus(final CellView cell)
    {
        return new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                if (hasFocus) {
                    cell.select();
                    selectedCell = cell;
                } else {
                    cell.deselect();
                }
            }
        };
    }

    public TextView getTopText()
    {
        return topText;
    }

    /** Class for performing the grid value extracting on another thread. */
    private class AsyncGridValueExtractor extends AsyncTask<Void, Void, Void>
    {
        private final GridProcessor gridProcessor;

        private boolean successfulCapture;

        private Bitmap warpedBitmap;

        private char[] cellValues;

        private int errorMessage;

        private AsyncGridValueExtractor(GridProcessor gridProcessor)
        {
            this.gridProcessor = gridProcessor;
            this.cellValues = new char[81];
        }

        @Override
        /** Show progress wheel before starting the grid/value extraction. */
        protected void onPreExecute()
        {
            super.onPreExecute();

            successfulCapture = true;
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        /** Find grid and read values. */
        protected Void doInBackground(Void... params)
        {
            // Find grid. Display error message if exception thrown (failed to find grid).
            try {
                warpedBitmap = gridProcessor.findGrid();
            } catch (Exception e) {
                Log.e("CaptureGrid", e.getMessage());
                warpedBitmap = null;
                successfulCapture = false;
                errorMessage = R.string.grid_not_found_toast;
            }

            // Grid found.
            if (successfulCapture) {

                // Read values. Display error message if no values read.
                try {
                    cellValues = gridProcessor.readValues();
                } catch (Exception e) {
                    Log.e("GetValues", e.getMessage());
                    successfulCapture = false;
                    errorMessage = R.string.failed_to_read_digits_toast;
                }
            }

            return null;
        }

        @Override
        /** Hide progress wheel. Display captured values or inform of failure via toast. */
        protected void onPostExecute(Void aVoid)
        {
            super.onPostExecute(aVoid);
            progressBar.setVisibility(View.GONE);
            enableButton(captureButton);

            if (!successfulCapture) {
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
                cameraPreview.startPreview();
            } else {
                Toast.makeText(context, R.string.capture_success_toast, Toast.LENGTH_SHORT).show();
                showCapturedValues(cellValues, warpedBitmap);
            }
        }
    }
}
