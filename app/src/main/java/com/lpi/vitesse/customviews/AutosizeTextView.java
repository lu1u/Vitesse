package com.lpi.vitesse.customviews;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lpi.vitesse.R;

/**
 * Custom view pour afficher un texte dont la taille s'adapte automatiquement
 */
public class AutosizeTextView extends View
{
	private TextPaint _textPaintPrincipal;
	final Rect _rPrincipal = new Rect();			// Pour eviter des allocations dans onDraw
	final Rect _rText = new Rect();
	private String _texte = "";
	private boolean _calculerTaille = true;

	public AutosizeTextView(Context context)
	{
		super(context);
		init(null, 0);
	}

	private void init(AttributeSet attrs, int defStyle)
	{
		// Load attributes
		final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.AutosizeTextView, defStyle, 0);
		int couleurPrincipal = a.getColor(R.styleable.AutosizeTextView_AutosizeTextCouleur, Color.WHITE);
		_texte = a.getString(R.styleable.AutosizeTextView_AutosizeTextTexte);
		if (_texte == null)
			_texte = "";
		a.recycle();

		// Set up a default TextPaint object
		_textPaintPrincipal = new TextPaint();
		_textPaintPrincipal.setColor(couleurPrincipal);
		_textPaintPrincipal.setFlags(Paint.ANTI_ALIAS_FLAG);
		_textPaintPrincipal.setTextAlign(Paint.Align.LEFT);
	}


	public AutosizeTextView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(attrs, 0);
	}

	public AutosizeTextView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		init(attrs, defStyle);
	}

	/***********************************************************************************************
	 * Affichage
	 * @param canvas
	 */
	@Override protected void onDraw(@NonNull Canvas canvas)
	{
		super.onDraw(canvas);
		if (_texte != null)
		{
			if (_calculerTaille)
			{
				calculeTextPaint(_texte, _rPrincipal, _textPaintPrincipal);
				_calculerTaille = false;
			}

			drawMultilineText(canvas, _texte, _rPrincipal.exactCenterX() - _rText.exactCenterX(), _rPrincipal.exactCenterY() - _rText.exactCenterY(), _textPaintPrincipal);
		}
	}

	private void drawMultilineText(@NonNull final Canvas canvas, @NonNull final  String texte, float x, float y, @NonNull final TextPaint textPaint)
	{
		Rect rText = new Rect();
		getTextBounds(textPaint, texte, rText);
		x -= rText.width() /2 ;
		for (String line: texte.split("\n")) {
			canvas.drawText(line, x, y, textPaint);
			y += textPaint.descent() - textPaint.ascent();
		}
	}

	/***********************************************************************************************
	 * Change le texte a afficher
	 * Redessine le controle si le texte change
	 *
	 * @param texte Nouveau texte a afficher
	 */
	public void setText(@Nullable final String texte)
	{
		if (!_texte.equals(texte))
		{
			if (texte != null)
				_texte = texte;
			else
				_texte = "";
			_calculerTaille = true;
			invalidate();
		}
	}

	/***********************************************************************************************
	 * Change la couleur du texte
	 * @param couleur
	 */
	public void setTextColor(int couleur)
	{
		_textPaintPrincipal.setColor(couleur);
		invalidate();
	}

	/***********************************************************************************************
	 * Calcule un textPaint avec une taille de caractere qui permet d'afficher le texte entierement
	 */
	private void calculeTextPaint(@NonNull String texte, @NonNull Rect r, @NonNull TextPaint textPaint)
	{
		final int contentWidth = r.width();
		final int contentHeight = r.height();

		float texteMax = Math.min(r.width(), r.height());
		float texteMin = 1;
		float tailleTexte = texteMin + (texteMax - texteMin) / 2.0f;

		Rect rText = new Rect();

		// Recherche dichotomique de la taille
		while (texteMax > (texteMin + 1))
		{
			textPaint.setTextSize(tailleTexte);
			getTextBounds(textPaint, texte,  rText);

			if ((rText.width() >= contentWidth) || (rText.height() >= contentHeight))
				// Trop grand
				texteMax = tailleTexte;
			else
				// Trop petit
				texteMin = tailleTexte;

			tailleTexte = texteMin + (texteMax - texteMin) / 2.0f;
		}
	}

	/***
	 * Calcule de la taille en pixels d'un texte multiligne
	 * @param textPaint
	 * @param texte
	 * @param rText
	 */
	private void getTextBounds(TextPaint textPaint, String texte, Rect rText)
	{
		int largeur = Integer.MIN_VALUE;
		int hauteur = 0;

		Rect rLine = new Rect();

		for (String line: texte.split("\n"))
		{
			textPaint.getTextBounds(line, 0, line.length(), rLine);
			if ( rLine.width()>largeur)
				largeur = rLine.width();
			hauteur += textPaint.descent() - textPaint.ascent();
		}

		rText.top = 0;
		rText.bottom = hauteur;
		rText.left = 0;
		rText.right = largeur;
	}

	@Override protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		_rPrincipal.set(0, 0, getWidth(), getHeight());
		_rPrincipal.left += getPaddingLeft();
		_rPrincipal.right -= getPaddingRight();
		_rPrincipal.top += getPaddingTop();
		_rPrincipal.bottom -= getPaddingBottom();

		_calculerTaille = true;
		invalidate();
		super.onSizeChanged(w, h, oldw, oldh);
	}

	public void setFont(String fontPath)
	{
		try
		{
			Typeface t = Typeface.createFromFile(fontPath);
			if ( t!=null)
			{
				_textPaintPrincipal.setTypeface(t);
				_calculerTaille = true;
				invalidate();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}