package me.jbuckley.mysudokusolver;

import android.content.Context;
import android.widget.TextView;

import me.jbuckley.mysudokusolver.styles.CellStyle;

/** View for displaying the sudoku grid cell value. */
public class CellView extends TextView
{
    private CellStyle style;

    public CellView(Context context, String value)
    {
        super(context);
        this.setText(value);
        this.setFocusable(true);
        this.setFocusableInTouchMode(true);
    }

    /** Cell has lost focus, change style accordingly. */
    public void deselect()
    {
        this.setBackgroundColor(style.getBackgroundDefault());
        this.setTypeface(style.getFontStyleDefault());
        this.setTextColor(style.getFontDefault());
    }

    /** Cell has gained focus, change style accordingly. */
    public void select()
    {
        this.setBackgroundColor(style.getBackgroundSelected());
        this.setTypeface(style.getFontStyleSelected());
        this.setTextColor(style.getFontSelected());
    }

    public void setStyle(CellStyle style)
    {
        this.style = style;

        deselect();
        this.setGravity(style.getGravity());
        this.setTextSize(style.getFontSize());
    }

    /** When checking cell values, if cell is incorrect, change style. */
    public void setAsIncorrect()
    {
        this.setTextColor(style.getFontStyleIncorrect());
    }
}
