package me.jbuckley.mysudokusolver.styles;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;

import me.jbuckley.mysudokusolver.R;

/** Style of cell when it is a read value from the camera activity shown on the playing activity. */
public class SolidCellStyle extends DefaultCellStyle
{
    public SolidCellStyle(Context context)
    {
        super(context);
    }


    @Override
    public int getBackgroundSelected()
    {
        return context.getResources().getColor(R.color.solid_cell_selected);
    }

    @Override
    public int getBackgroundDefault()
    {
        return context.getResources().getColor(R.color.solid_cell_default);
    }

    @Override
    public int getFontSelected()
    {
        return context.getResources().getColor(R.color.cell_font_default);
    }

    @Override
    public int getFontDefault()
    {
        return context.getResources().getColor(R.color.cell_font_default);
    }

    @Override
    public int getFontSize()
    {
        return 25;
    }

    @Override
    public int getGravity()
    {
        return Gravity.CENTER;
    }

    @Override
    public Typeface getFontStyleSelected()
    {
        return Typeface.DEFAULT_BOLD;
    }

    @Override
    public Typeface getFontStyleDefault()
    {
        return Typeface.DEFAULT_BOLD;
    }
}
