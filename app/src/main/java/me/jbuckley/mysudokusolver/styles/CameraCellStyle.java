package me.jbuckley.mysudokusolver.styles;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;

import me.jbuckley.mysudokusolver.R;

/** Cell style for camera activity. */
public class CameraCellStyle extends CellStyle
{
    public CameraCellStyle(Context context)
    {
        super(context);
    }

    @Override
    public int getBackgroundSelected()
    {
        return context.getResources().getColor(R.color.trans_light_blue);
    }

    @Override
    public int getBackgroundDefault()
    {
        return context.getResources().getColor(R.color.trans);
    }

    @Override
    public int getFontSelected()
    {
        return context.getResources().getColor(R.color.camera_cell_font_selected);
    }

    @Override
    public int getFontDefault()
    {
        return context.getResources().getColor(R.color.camera_cell_font_default);
    }

    @Override
    public int getFontSize()
    {
        return 20;
    }

    @Override
    public int getGravity()
    {
        return Gravity.BOTTOM | Gravity.LEFT;
    }

    @Override
    public Typeface getFontStyleSelected()
    {
        return Typeface.DEFAULT_BOLD;
    }

    @Override
    public Typeface getFontStyleDefault()
    {
        return Typeface.DEFAULT;
    }

    @Override
    public int getFontStyleIncorrect()
    {
        return getFontDefault();
    }
}
