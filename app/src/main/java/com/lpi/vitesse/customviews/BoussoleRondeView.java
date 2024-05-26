package com.lpi.vitesse.customviews;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.core.content.res.ResourcesCompat;

import com.lpi.vitesse.Preferences;
import com.lpi.vitesse.R;
import com.lpi.vitesse.ValeurPonderee;

/**
 * Customview pour afficher une boussole
 */
public class BoussoleRondeView extends View
{
	private static final String PROPERTY_VALEUR = "CapView.valeur";
	private float _valeurADessiner = 0;                    // La valeur actuellement affichee par l'animation

	// Attributs du CustomView
	float _ratioTaille = 1.0f;
	Drawable _dBoussole;
	float _cap = 0;                                    // Le cap actuel

	// Variables de travail
	final Rect _rBoussole = new Rect();                    // Pour eviter des allocations dans onDraw
	final Rect _rect = new Rect();
	private boolean _calculerTailleCap = true;
	ValueAnimator _animator;
	private float _valeurCible = 0;                    // La valeur a atteindre par l'animation
	private Paint _paintCapLissé;

	ValeurPonderee _capPondere;
	public BoussoleRondeView(Context context)
	{
		super(context);
		init(null, 0);
	}

	public BoussoleRondeView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(attrs, 0);
	}

	public BoussoleRondeView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		init(attrs, defStyle);
	}

	private void init(AttributeSet attrs, int defStyle)
	{
		_paintCapLissé = new Paint();
		int couleurPrincipal=Color.WHITE;// Load attributes
		try
		{

			final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.BoussoleRondeView, defStyle, 0);

			couleurPrincipal = a.getColor(R.styleable.BoussoleRondeView_VitesseTextCouleur, Color.WHITE);
			_cap = a.getFloat(R.styleable.BoussoleRondeView_VitesseCap, 0);
			_ratioTaille = a.getFloat(R.styleable.BoussoleRondeView_VitesseRatioTaille, 2.0f);

			_paintCapLissé.setStrokeWidth(a.getDimension(R.styleable.BoussoleRondeView_VitesseCapLisseLargeur, 8));
			_paintCapLissé.setColor(a.getColor(R.styleable.BoussoleRondeView_VitesseCapLisseCouleur, Color.RED));
			a.recycle();
		}
		catch (Exception e)
		{
		e.printStackTrace();
		}
		_valeurADessiner = _cap;

		_dBoussole = ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.ic_boussole, getContext().getTheme());
		if (_dBoussole != null)
		{
			_dBoussole.setCallback(null);
			_dBoussole.setTint(couleurPrincipal);
		}

		_paintCapLissé.setStyle(Paint.Style.FILL_AND_STROKE);
		_paintCapLissé.setAntiAlias(true);
		_calculerTailleCap = true;
		_capPondere = new ValeurPonderee(Preferences.getInstance(getContext()).getInt( Preferences.DELAI_LISSAGE_CAP_SECONDES, 10));
	}


	/**
	 * Dessiner le controle
	 *
	 * @param canvas
	 */
	@Override protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);
		if (_dBoussole == null)
			return;

		if (_calculerTailleCap)
		{
			_rBoussole.set(0, 0, getWidth(), getHeight());
			_rBoussole.left += getPaddingLeft();
			_rBoussole.right -= getPaddingRight();
			_rBoussole.top += getPaddingTop();
			_rBoussole.bottom -= getPaddingBottom();
			_calculerTailleCap = false;

			final float h = _rBoussole.height() * _ratioTaille;            // _ratioTaille est la part du rayon de la boussole qui contient les graduations
			_rect.top = _rBoussole.top;
			_rect.bottom = _rBoussole.top + (int) (h * 2.0f);
			_rect.left = _rBoussole.centerX() - (int) h;
			_rect.right = _rBoussole.centerX() + (int) h;
			_dBoussole.setBounds(_rect);
		}

		// Cap lissé
		if ( _capPondere.hasValue())
		{
			canvas.save();
			canvas.rotate(_capPondere.getValue() - _valeurADessiner, _rect.exactCenterX(), _rect.exactCenterY());
			canvas.drawRect(_rBoussole.exactCenterX(), _rBoussole.top, _rBoussole.exactCenterX(), _rBoussole.top + (_rBoussole.height() * 0.66f), _paintCapLissé);
			canvas.restore();
		}
		// Dessine le cap courant
		canvas.save();
		canvas.rotate(-_valeurADessiner, _rect.exactCenterX(), _rect.exactCenterY());
		_dBoussole.draw(canvas);
		canvas.restore();
	}

	/***
	 * Changer la vitesse
	 * @param cap
	 */
	public void setCap(float cap)
	{
		// Inverser le sens si la difference est superieure à 180 degre (par exemple de 10° à 350°)
		float difference = Math.abs(_valeurCible - cap);
		if (difference > 180)
			_valeurCible = cap - 360;
		else
			_valeurCible = cap;

		// Nouveau cap lissé
		_capPondere.ajouteValeur(cap);
		_calculerTailleCap = true;
		animer();
	}

	/***********************************************************************************************
	 * Change la couleur du texte
	 * @param couleur
	 */
	public void setTextColor(int couleur)
	{
		_dBoussole.setTint(couleur);
		invalidate();
	}

	/***********************************************************************************************
	 * Change le nombre de valeurs pour le lissage du cap
	 * @param valeur
	 */
	public void setNbValeursLissage(int valeur)
	{
		_capPondere.setDelai( valeur );
		invalidate();
	}

	/***
	 * Fait une animation a chaque changement des valeurs
	 */
	private void animer()
	{
		if (_animator != null)
		{
			// Animation deja en cours
			_animator.removeAllUpdateListeners();
			_animator.cancel();
			//return;
		}

		_animator = ValueAnimator.ofFloat(_cap, _valeurCible);
		_animator.setDuration(200);
		PropertyValuesHolder propertyRadius = PropertyValuesHolder.ofFloat(PROPERTY_VALEUR, _cap, _valeurCible);
		_animator.setValues(propertyRadius);
		_animator.setInterpolator(new AccelerateDecelerateInterpolator());
		_animator.addUpdateListener(animation ->
		{
			try
			{
				_valeurADessiner = (float) animation.getAnimatedValue(PROPERTY_VALEUR);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
			invalidate();
		});

		_animator.addListener(new Animator.AnimatorListener()
		{
			@Override public void onAnimationStart(final Animator animator)
			{
			}

			@Override public void onAnimationEnd(final Animator animator)
			{
				_cap = _valeurCible;
				_valeurADessiner = _valeurCible;
				_animator = null;
				invalidate();
			}

			@Override public void onAnimationCancel(final Animator animator)
			{
			}

			@Override public void onAnimationRepeat(final Animator animator)
			{
			}
		});
		_animator.start();
	}

	@Override protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		_calculerTailleCap = true;
		invalidate();
		super.onSizeChanged(w, h, oldw, oldh);
	}
}