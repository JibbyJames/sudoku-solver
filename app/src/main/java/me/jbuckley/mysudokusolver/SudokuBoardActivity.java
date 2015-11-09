package me.jbuckley.mysudokusolver;

import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import me.jbuckley.mysudokusolver.styles.DefaultCellStyle;
import solver.dvo.OrderByDomain;
import solver.strategies.BasicBacktracking;
import solver.strategies.ForwardChecking;
import solver.strategies.GAC;
import solver.strategies.InitialGAC;
import solver.strategies.InitialSAC;
import solver.strategies.SAC;
import solver.strategies.SolveStrategy;

public class SudokuBoardActivity extends Activity
{
    private SudokuBoardView sudokuBoardView;

    private GridLayout digitsLayout;

    private CellView selectedCell;

    private Button solveButtonBench;
    private Button solveButtonPlay;
    private RelativeLayout playLayoutTop;
    private Button checkCellsButton;
    private RelativeLayout benchmarkLayoutTop;
    private Spinner strategySpinner;
    private RelativeLayout playLayoutBottom;
    private RelativeLayout benchmarkLayoutBottom;
    private Button resetButton;
    private TextView assignmentsValue;
    private TextView timeValue;

    private char[] cellValues;
    private Context context;
    private Button[] digits;

    private boolean benchmarkMode;
    private char[] startCellValues;
    private char[] cellSolutionValues;
    private ProgressBar progressBar;
    private AsyncSudokuSolver solver;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sudoku_board);

        playLayoutTop = (RelativeLayout) findViewById(R.id.play_layout_top);
        checkCellsButton = (Button) findViewById(R.id.check_cells_button);
        benchmarkLayoutTop = (RelativeLayout) findViewById(R.id.benchmark_layout_top);
        strategySpinner = (Spinner) findViewById(R.id.strategy_spinner);
        solveButtonBench = (Button) findViewById(R.id.solve_button_bench);
        solveButtonPlay = (Button) findViewById(R.id.solve_button_play);

        sudokuBoardView = (SudokuBoardView) findViewById(R.id.gameBoardLayout);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        playLayoutBottom = (RelativeLayout) findViewById(R.id.play_layout_bottom);
        digitsLayout = (GridLayout) findViewById(R.id.digits);
        benchmarkLayoutBottom = (RelativeLayout) findViewById(R.id.benchmark_layout_bottom);
        resetButton = (Button) findViewById(R.id.reset_button);
        assignmentsValue = (TextView) findViewById(R.id.ass_value_text);
        timeValue = (TextView) findViewById(R.id.time_value_text);

        context = this;

        initialize();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_sudoku_activity, menu);

        menu.findItem(R.id.benchmark_mode).setChecked(benchmarkMode);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.benchmark_mode:
                if (item.isChecked()) {
                    item.setChecked(false);
                    benchmarkMode = false;
                    initLayouts();
                } else {
                    item.setChecked(true);
                    benchmarkMode = true;
                    startCellValues = sudokuBoardView.getCellValues();
                    initLayouts();
                }
                break;
        }

        return true;
    }

    /** Initialise for the initial start up of the activity. */
    private void initialize()
    {
        benchmarkMode = false;

        initDigits();
        initSudokuBoardView();
        initLayouts();
    }

    /** Initialise the layouts present for the given mode. */
    private void initLayouts()
    {
        if (benchmarkMode) {

            playLayoutTop.setVisibility(View.GONE);
            playLayoutBottom.setVisibility(View.GONE);
            benchmarkLayoutTop.setVisibility(View.VISIBLE);
            benchmarkLayoutBottom.setVisibility(View.VISIBLE);

            ArrayAdapter<CharSequence> adapter =
                    ArrayAdapter.createFromResource(context, R.array.strategy_array,
                            R.layout.spinner_item);
            strategySpinner.setAdapter(adapter);
            strategySpinner.getBackground()
                    .setColorFilter(getResources().getColor(R.color.button_dark),
                            PorterDuff.Mode.SRC_ATOP);

            solveButtonBench.setOnClickListener(solveOnClick());
            resetButton.setOnClickListener(resetOnClick());

        } else {
            playLayoutTop.setVisibility(View.VISIBLE);
            playLayoutBottom.setVisibility(View.VISIBLE);
            benchmarkLayoutTop.setVisibility(View.GONE);
            benchmarkLayoutBottom.setVisibility(View.GONE);

            checkCellsButton.setOnClickListener(checkOnClick());
            solveButtonPlay.setOnClickListener(solveOnClick());
        }
    }

    /** Initialise the sudoku board. */
    private void initSudokuBoardView()
    {
        cellValues = getValuesFromIntent();
        cellSolutionValues = getSolutionValuesFromIntent();
        startCellValues = cellValues;

        sudokuBoardView.setCellValues(cellValues);
        sudokuBoardView.setCellStyle(new DefaultCellStyle(context));
        sudokuBoardView.showGridLines(true);
        for (CellView cell : sudokuBoardView.getCells()) {
            cell.setOnFocusChangeListener(cellOnFocus(cell));
            cell.setEnabled(true);
        }
        sudokuBoardView.setAsSolidCell();
    }

    /** Initialise the digits. */
    private void initDigits()
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

    /**
     * Get the strategy selected from the spinner.
     *
     * @return The selected strategy.
     */
    public SolveStrategy getSolveStrategy()
    {
        SolveStrategy result;

        if (benchmarkMode) {
            switch (strategySpinner.getSelectedItemPosition()) {
                case (0):
                    result = new BasicBacktracking();
                    break;
                case (1):
                    result = new ForwardChecking();
                    break;
                case (2):
                    result = new ForwardChecking(new OrderByDomain());
                    break;
                case (3):
                    result = new GAC();
                    break;
                case (4):
                    result = new InitialGAC();
                    break;
                case (5):
                    result = new SAC();
                    break;
                case (6):
                    result = new InitialSAC();
                    break;
                default:
                    result = new InitialSAC();
                    break;
            }
        } else {
            result = new InitialSAC();
        }

        return result;
    }

    /** Obtain the solution values passed when launching the activity. */
    private char[] getSolutionValuesFromIntent()
    {
        char[] result = new char[81];

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            result = extras.getCharArray(getResources().getString(R.string.intent_solution_cells));
        }

        return result;
    }

    /** Obtain the read cell values passed when launching the activity. */
    private char[] getValuesFromIntent()
    {
        char[] result = new char[81];

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            result = extras.getCharArray(getResources().getString(R.string.intent_cells));
        }

        return result;
    }

    public void setAssignmentsValue(String s)
    {
        assignmentsValue.setText(s);
    }

    public void setTimeValue(String s)
    {
        timeValue.setText(s);
    }

    public void setCellValues(char[] cellValues)
    {
        sudokuBoardView.setCellValues(cellValues);
    }

    /** Check button is pressed, inform user of incorrect/correct cells. */
    private View.OnClickListener checkOnClick()
    {
        return new View.OnClickListener()
        {
            public void onClick(View view)
            {
                boolean allCorrect = true;
                char[] currentValues = sudokuBoardView.getCellValues();

                for (int i = 0; i < currentValues.length; i++) {
                    if (currentValues[i] != ' ' && cellSolutionValues[i] != currentValues[i]) {
                        allCorrect = false;
                        sudokuBoardView.getCells().get(i).setAsIncorrect();
                    }
                }

                if (allCorrect) {
                    Toast.makeText(context, getResources().getString(R.string.all_correct),
                            Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    /** Solve button clicked, display progress wheel and show values and benchmark results. */
    private View.OnClickListener solveOnClick()
    {
        return new View.OnClickListener()
        {
            public void onClick(View view)
            {
                solver = new AsyncSudokuSolver(SudokuBoardActivity.this,
                        progressBar, sudokuBoardView.getCellValues());
                solver.execute();
            }
        };
    }

    /**
     * Reset button pressed, put cell values back to state when benchmark mode was started. Clear
     * previous assignments/time values.
     */
    private View.OnClickListener resetOnClick()
    {
        return new View.OnClickListener()
        {
            public void onClick(View view)
            {
                cellValues = startCellValues;
                sudokuBoardView.setCellValues(cellValues);
                assignmentsValue.setText("");
                timeValue.setText("");
            }
        };
    }

    /** Digit is pressed, set selected cell value as digit value. */
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

    /** Cell focus change, if focus lost, deselect cell. If gained, select cell. */
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

}
