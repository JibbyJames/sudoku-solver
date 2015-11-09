package me.jbuckley.mysudokusolver;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import me.jbuckley.mysudokusolver.camera.CameraActivity;
import solver.csp.Solution;
import solver.dvo.OrderByDomain;
import solver.strategies.ForwardChecking;
import solver.strategies.SolveStrategy;
import sudoku.SudokuGrid;

/** Class for solving sudoku puzzle in background. */
public class AsyncSudokuSolver extends AsyncTask<Void, Void, Void>
{
    private final ProgressBar progressBar;
    private final Context context;
    private final boolean fromCameraActivity;
    private final SolveStrategy strat;
    private final CameraActivity cameraActivity;
    private final SudokuBoardActivity sudokuBoardActivity;
    private final char[] finalCellValues;

    private Activity activity;
    private boolean isSolution = false;
    private boolean areEnoughCells = true;

    private SudokuGrid sudokuGrid;

    private Solution solution;

    /**
     * Solve a Sudoku puzzle as a background task, whilst updating the progress wheel.
     *
     * @param activity The activity which created the AsyncSudokuSolver.
     * @param progressBar The activity progress wheel.
     * @param finalCellValues The starting values of the Sudoku grid.
     */
    public AsyncSudokuSolver(Activity activity, ProgressBar progressBar,
                             char[] finalCellValues)
    {
        this.context = activity.getApplicationContext();
        this.activity = activity;
        this.progressBar = progressBar;
        this.finalCellValues = finalCellValues;

        fromCameraActivity = activity instanceof CameraActivity;
        if (fromCameraActivity) {
            cameraActivity = (CameraActivity) activity;
            sudokuBoardActivity = null;
            strat = new ForwardChecking(new OrderByDomain());
        } else {
            sudokuBoardActivity = (SudokuBoardActivity) activity;
            cameraActivity = null;
            strat = sudokuBoardActivity.getSolveStrategy();
        }
    }

    @Override
    /** Show progress wheel before starting the grid/value extraction. */
    protected void onPreExecute()
    {
        super.onPreExecute();

        isSolution = false;
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    /** Attempt to solve puzzle. Check if there is a solution. */
    protected Void doInBackground(Void... params)
    {
        // May be less than 12 cells, causing 1000s of possible solutions.
        areEnoughCells = areEnoughCells();

        if (areEnoughCells) {
            sudokuGrid = new SudokuGrid(finalCellValues, strat);
            solution = sudokuGrid.solve();

            isSolution = solution != null;
        }

        return null;
    }

    @Override
    /** If solution found, cells are valid, launch activity to play/solve sudoku if in Camera
     * Activity. Show values and update benchmark results if in SudokuBoardActivity. Display
     * toast messages informing user of errors.*/
    protected void onPostExecute(Void aVoid)
    {
        super.onPostExecute(aVoid);

        progressBar.setVisibility(View.INVISIBLE);

        if (fromCameraActivity) {

            if (areEnoughCells) {
                if (isSolution) {

                    // Pass the read values and solution values to the play/solve activity.
                    Intent intent = new Intent(activity.getApplicationContext(),
                            SudokuBoardActivity.class);
                    intent.putExtra(activity.getString(R.string.intent_cells), finalCellValues);
                    intent.putExtra(activity.getString(R.string.intent_solution_cells),
                            sudokuGrid.getCellValues());
                    activity.startActivity(intent);

                } else {
                    Toast.makeText(context, activity.getString(R.string.no_solution_possible),
                            Toast.LENGTH_SHORT).show();
                    cameraActivity.getTopText()
                            .setText(activity.getResources().getString(R.string.verify_cells));
                }
            } else {

                cameraActivity.getTopText().setText(
                        activity.getResources().getString(R.string.verify_cells));
            }
        } else {
            if (areEnoughCells) {
                if (isSolution) {

                    sudokuBoardActivity.setAssignmentsValue(String.valueOf(strat.getAssignments()));
                    sudokuBoardActivity.setTimeValue(String.valueOf(strat.getTimeTaken()) + "ms");
                    sudokuBoardActivity.setCellValues(sudokuGrid.getCellValues());

                } else {
                    Toast.makeText(context,
                            activity.getResources().getString(R.string.no_solution_possible),
                            Toast.LENGTH_SHORT).show();
                }
            } else {

                cameraActivity.getTopText().setText(
                        activity.getResources().getString(R.string.verify_cells));
            }
        }
    }

    @Override
    protected void onCancelled()
    {
        super.onCancelled();
        progressBar.setVisibility(View.GONE);
    }

    /**
     * Check if there are at least 12 cells detected. This is to prevent puzzles with 1000s of
     * possible solutions from being used.
     */
    private boolean areEnoughCells()
    {
        boolean result = false;
        int totalDigits = 0;

        for (char finalCellValue : finalCellValues) {
            if (finalCellValue != ' ') {
                totalDigits++;
                if (totalDigits > 11) {
                    result = true;
                    break;
                }
            }
        }

        return result;
    }
}
