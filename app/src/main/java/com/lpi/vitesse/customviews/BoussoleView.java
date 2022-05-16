package com.lpi.vitesse.customviews;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public abstract class BoussoleView extends View
{
	public BoussoleView(Context context)
	{
		super(context);
	}

	public BoussoleView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public BoussoleView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	public abstract void setTextColor(int Color);
	public abstract void setCap(float cap);
}
