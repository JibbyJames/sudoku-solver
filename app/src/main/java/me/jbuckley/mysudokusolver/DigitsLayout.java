package me.jbuckley.mysudokusolver;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridLayout;

/** Layout for storing digits used to change cell values. */
public class DigitsLayout extends GridLayout
{
    public DigitsLayout(Context context)
    {
        super(context);
    }

    public DigitsLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public DigitsLayout(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    @Override
    /** Resize children (digits) to fit across parent width evenly. */
    protected void onMeasure(int widthSpec, int heightSpec)
    {
        super.onMeasure(widthSpec, heightSpec);

        for (int i = 0; i < getChildCount(); i++) {
            GridLayout.LayoutParams params = (GridLayout.LayoutParams) getChildAt(
                    i).getLayoutParams();

            params.setMargins(5, 5, 5, 5);
            params.width =
                    ((getMeasuredWidth() - getPaddingRight() - getPaddingLeft()) / getColumnCount())
                            - params.rightMargin - params.leftMargin;

            getChildAt(i).setLayoutParams(params);
        }
    }
}
