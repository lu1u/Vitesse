package com.lpi.vitesse;

import android.graphics.Color;
import android.os.Bundle;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class ColorFragment extends Fragment
{
	private int _couleur;

	public interface Listener
	{
		void onNewColor(int couleur);
	}

	private SeekBar _sbR, _sbG, _sbB, _sbA;
	private TextView _tvTitre, _tvExemple;
	private ImageButton _ibContracte, _ibDilate;
	private @Nullable
	Listener _listener;

	public ColorFragment()
	{
		// Required empty public constructor
	}


	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// Inflate the layout for this fragment
		View v = inflater.inflate(R.layout.fragment_color, container, false);
		_sbA = v.findViewById(R.id.seekBarA2);
		_sbR = v.findViewById(R.id.seekBarR2);
		_sbG = v.findViewById(R.id.seekBarG2);
		_sbB = v.findViewById(R.id.seekBarB2);
		_tvTitre = v.findViewById(R.id.tvTitre);
		_tvExemple = v.findViewById(R.id.tvExemple);

		_ibContracte = v.findViewById(R.id.ibContracte);
		_ibDilate = v.findViewById(R.id.ibDilate);

		_sbA.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
		{
			@Override
			public void onProgressChanged(SeekBar seekBar, int composante, boolean b)
			{
				nouvelleCouleur(Color.argb(composante, Color.red(_couleur), Color.green(_couleur), Color.blue(_couleur)));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar)
			{

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar)
			{

			}
		});

		_sbR.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
		{
			@Override
			public void onProgressChanged(SeekBar seekBar, int composante, boolean b)
			{
				nouvelleCouleur(Color.argb(Color.alpha(_couleur), composante, Color.green(_couleur), Color.blue(_couleur)));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar)
			{

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar)
			{

			}
		});

		_sbG.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
		{
			@Override
			public void onProgressChanged(SeekBar seekBar, int composante, boolean b)
			{
				nouvelleCouleur(Color.argb(Color.alpha(_couleur), Color.red(_couleur), composante, Color.blue(_couleur)));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar)
			{

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar)
			{

			}
		});

		_sbB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
		{
			@Override
			public void onProgressChanged(SeekBar seekBar, int composante, boolean b)
			{
				nouvelleCouleur(Color.argb(Color.alpha(_couleur), Color.red(_couleur), Color.green(_couleur), composante));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar)
			{

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar)
			{

			}
		});

		_ibContracte.setOnClickListener(view -> contracte());

		_ibDilate.setOnClickListener(view -> dilate());
		contracte();

		_tvExemple.setOnClickListener(new View.OnClickListener()
		{
			@Override public void onClick(View view)
			{
				if  (_ibContracte.getVisibility() == View.VISIBLE)
					contracte();
				else
					dilate();
			}
		});
		return v;
	}

	/**
	 * "Ouvrir" le fragment
	 */
	private void dilate()
	{
		_ibDilate.setVisibility(View.GONE);
		_ibContracte.setVisibility(View.VISIBLE);

		View v = getView();
		if ( v!=null)
		{
			ViewGroup parent = (ViewGroup) v.getParent();
			if (parent != null)
			{
				Transition transition = new Slide();
				transition.setDuration(200);
				transition.addTarget(_sbA);
				transition.addTarget(_sbR);
				transition.addTarget(_sbG);
				transition.addTarget(_sbB);
				TransitionManager.beginDelayedTransition(parent, transition);
			}
		}

		_sbA.setVisibility(View.VISIBLE);
		_sbR.setVisibility(View.VISIBLE);
		_sbG.setVisibility(View.VISIBLE);
		_sbB.setVisibility(View.VISIBLE);
	}

	/**
	 * "Fermer" le fragment
	 */
	private void contracte()
	{
		_ibDilate.setVisibility(View.VISIBLE);
		_ibContracte.setVisibility(View.GONE);

		View v = getView();
		if ( v!=null)
		{
			ViewGroup parent = (ViewGroup) v.getParent();
			if (parent != null)
			{
				Transition transition = new Slide();
				transition.setDuration(200);
				transition.addTarget(_sbA);
				transition.addTarget(_sbR);
				transition.addTarget(_sbG);
				transition.addTarget(_sbB);
				TransitionManager.beginDelayedTransition(parent, transition);
			}
		}

		_sbA.setVisibility(View.GONE);
		_sbR.setVisibility(View.GONE);
		_sbG.setVisibility(View.GONE);
		_sbB.setVisibility(View.GONE);
	}

	/***
	 * Nouvelle couleur: modifier la cadre d'exemple et prevenir le parent par l'intermediaire du
	 * listener
	 * @param couleur Nouvelle couleur
	 */
	private void nouvelleCouleur(int couleur)
	{
		_couleur = couleur;
		_tvExemple.setBackgroundColor(couleur);
		_couleur = couleur;
		_tvExemple.setText(String.format("#%02X%02X%02X%02X", Color.alpha(couleur), Color.red(couleur), Color.green(couleur), Color.blue(couleur)));

		if (_listener != null)
			_listener.onNewColor(couleur);
	}

	public void setListener(@Nullable final Listener listener)
	{
		_listener = listener;
	}

	public void setCouleur(int couleur)
	{
		_couleur = couleur;
		_sbA.setProgress(Color.alpha(couleur));
		_sbR.setProgress(Color.red(couleur));
		_sbG.setProgress(Color.green(couleur));
		_sbB.setProgress(Color.blue(couleur));

		_tvExemple.setBackgroundColor(couleur);
		_tvExemple.setText(String.format("#%02X%02X%02X%02X", Color.alpha(couleur), Color.red(couleur), Color.green(couleur), Color.blue(couleur)));
	}

	public void setTitre(@NonNull final String titre)
	{
		_tvTitre.setText(titre);
	}
}