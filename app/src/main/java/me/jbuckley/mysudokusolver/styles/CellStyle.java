package me.jbuckley.mysudokusolver.styles;

import android.content.Context;
import android.graphics.Typeface;

/** Cell style can vary depending on activity. This class defines each style property. */
public abstract class CellStyle
{
    protected Context context;

    public CellStyle(Context context)
    {
        this.context = context;
    }

    public abstract int getBackgroundSelected();

    public abstract int getBackgroundDefault();

    public abstract int getFontSelected();

    public abstract int getFontDefault();

    public abstract int getFontSize();

    public abstract int getGravity();

    public abstract Typeface getFontStyleSelected();

    public abstract Typeface getFontStyleDefault();

    public abstract int getFontStyleIncorrect();
}
