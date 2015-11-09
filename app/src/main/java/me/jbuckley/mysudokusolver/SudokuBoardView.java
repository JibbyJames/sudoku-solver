package me.jbuckley.mysudokusolver;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.GridLayout;

import java.util.ArrayList;

import me.jbuckley.mysudokusolver.styles.CellStyle;
import me.jbuckley.mysudokusolver.styles.SolidCellStyle;

/** Grid for displaying  sudoku cells. */
public class SudokuBoardView extends GridLayout
{
    private int boardDimension;
    private int cellSize;
    private Context context;
    private int boardSize;
    private ArrayList<CellView> cellViews;
    private int totalColumns;
    private boolean showGridLines;
    private Paint thinLine;
    private int borderThickness;
    private Paint thickLine;

    public SudokuBoardView(Context context)
    {
        super(context);
        initialize(context);
    }

    public SudokuBoardView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initialize(context);
    }

    public SudokuBoardView(Context context, AttributeSet attrs, int defstyle)
    {
        super(context, attrs, defstyle);
        initialize(context);
    }

    /** Initialise appearance of grid and cells. */
    private void initialize(Context context)
    {
        this.context = context;

        this.totalColumns = this.getResources().getInteger(R.integer.sudoku_total_columns);
        this.setColumnCount(totalColumns);

        this.showGridLines = false;
        this.boardSize = totalColumns * totalColumns;

        borderThickness = (int) getResources().getDimension(R.dimen.sudoku_border_width);

        thinLine = new Paint();
        thinLine.setColor(getResources().getColor(R.color.sudoku_border));
        thinLine.setStrokeWidth(getResources().getDimension(R.dimen.sudoku_gridline_width));

        thickLine = new Paint();
        thickLine.setColor(getResources().getColor(R.color.sudoku_border));
        thickLine.setStrokeWidth(borderThickness);

        this.setPadding(borderThickness, borderThickness, borderThickness, borderThickness);

        this.setFocusable(true);
        this.setFocusableInTouchMode(true);

        initializeCellViews();
    }

    /** Create and add cells to grid layout. */
    private void initializeCellViews()
    {
        cellViews = new ArrayList<>(boardSize);

        for (int i = 0; i < boardSize; i++) {
            CellView cell = new CellView(context, " ");
            cell.setOnClickListener(new CellViewClick());
            cellViews.add(cell);
            this.addView(cell);
        }
    }

    @Override
    /** Set size to be the largest square possible on the screen. */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        if (width > height) {
            boardDimension = height;
        } else {
            boardDimension = width;
        }

        for (int i = 0; i < getChildCount(); i++) {
            GridLayout.LayoutParams params = (GridLayout.LayoutParams) getChildAt(
                    i).getLayoutParams();

            // Set width and height of cells to stretch across entire view space.
            float newWidth = Math.round(((getWidth() - (borderThickness * 2)) / getColumnCount())
                    - params.rightMargin - params.leftMargin);
            params.width = (int) newWidth;
            params.height = params.width;
            cellSize = params.height;

            getChildAt(i).setLayoutParams(params);
        }

        setMeasuredDimension(boardDimension, boardDimension);
    }

    @Override
    protected void dispatchDraw(Canvas canvas)
    {
        super.dispatchDraw(canvas);

        // Draw Grid Lines on top of cells.
        if (showGridLines) {
            drawGridLines(canvas);
        }
    }

    /**
     * Draw the gridlines. Two thicknesses. One for border and to highlight 3x3 groups, other to
     * separate cells.
     */
    private void drawGridLines(Canvas canvas)
    {
        int thickOffset = Math.round(borderThickness / 2);
        int thinOffset = Math.round(getResources().getDimension(R.dimen.sudoku_gridline_width) * 2);

        int[] thickIndexes = new int[]{3, 6};
        int[] thinIndexes = new int[]{1, 2, 4, 5, 7, 8};

        // Thick lines.
        for (int thickIndex : thickIndexes) {
            // Horizontal.
            canvas.drawLine(0, thickOffset + cellSize * thickIndex, getWidth(),
                    thickOffset + cellSize * thickIndex, thickLine);
            // Vertical
            canvas.drawLine(thickOffset + cellSize * thickIndex, 0,
                    thickOffset + cellSize * thickIndex, getHeight(), thickLine);
        }

        // Thin lines.
        for (int thinIndex : thinIndexes) {
            // Horizontal.
            canvas.drawLine(0, thinOffset + cellSize * thinIndex, getWidth(),
                    thinOffset + cellSize * thinIndex, thinLine);
            // Vertical.
            canvas.drawLine(thinOffset + cellSize * thinIndex, 0,
                    thinOffset + cellSize * thinIndex, getHeight(), thinLine);
        }
    }

    /** Cell is marked as value obtained from capture. */
    public void setAsSolidCell()
    {
        CellStyle style = new SolidCellStyle(context);

        for (CellView cell : cellViews) {
            if (String.valueOf(cell.getText()).charAt(0) != ' ') {
                cell.setStyle(style);
            }
        }
    }

    public char[] getCellValues()
    {
        char[] result = new char[81];

        for (int i = 0; i < cellViews.size(); i++) {
            result[i] = cellViews.get(i).getText().charAt(0);
        }

        return result;
    }

    public void setCellValues(char[] cellValues)
    {
        for (int i = 0; i < cellValues.length; i++) {
            cellViews.get(i).setText(String.valueOf(cellValues[i]));
        }
    }

    public void resetCellValues()
    {
        for (CellView cell : cellViews) {
            cell.setText(String.valueOf(' '));
        }
    }

    public void setCellStyle(CellStyle cellStyle)
    {
        for (CellView cell : cellViews) {
            cell.setStyle(cellStyle);
        }
    }

    public void setCellsVisibility(int visibility)
    {
        for (CellView cell : cellViews) {
            cell.setVisibility(visibility);
            cell.clearFocus();
        }
    }

    public void showGridLines(boolean showGridLines)
    {
        this.showGridLines = showGridLines;
    }

    public ArrayList<CellView> getCells()
    {
        return cellViews;
    }

    public void setCellsEnabled(boolean value)
    {
        for (CellView cell : cellViews) {
            cell.setEnabled(value);
        }
    }

    public class CellViewClick implements View.OnClickListener
    {
        @Override
        public void onClick(View v)
        {
            // Do nothing. Focus change in parent activity will handle selected ID changes.
        }
    }
}
