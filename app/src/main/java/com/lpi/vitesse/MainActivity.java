package com.lpi.vitesse;

import android.Manifest;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.lpi.vitesse.customviews.BoussoleRondeView;
import com.lpi.vitesse.dialogues.AdvancedParametersDialog;
import com.lpi.vitesse.dialogues.DialogAPropos;
import com.lpi.vitesse.dialogues.FontPickerDialog;

import java.util.Objects;

/***
 * Affichage de la vitesse et la direction GPS, en plein ecran et mode Picture in Picture
 */
public class MainActivity extends AppCompatActivity implements LocationListener
{
	// Couleurs par defaut
	public static final int COULEUR_TEXTE_DEFAUT = Color.WHITE;
	public static final int COULEUR_FOND_DEFAUT = 0x88000000;

	// Permissions
	public static final @NonNull String[] PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
	private static final int PERMISSIONS_REQUEST_CODE = 12458;

	// Parametrage par defaut du GPS
	public static final int MIN_TIME_S = 0;
	public static final int MIN_DISTANCE_M = 0;

	// Gestion du GPS
	private @Nullable LocationManager _locationManager;
	private @Nullable Location _precedente;

	// Controles de l'interface utilisateur
	ViewGroup _fullscreenLayout, _pipLayout;
	private TextView _atvAltitude;
	private TextView _atvVitesse;
	private BoussoleRondeView _boussoleView;
	private View _vFond;
	private boolean _useSpeed, _useBearing;

	// Handler pour remettre la vitesse a zero si on n'a pas recu de nouvelle position depuis un certain temps
	final Handler _handlerRemiseZero = new Handler();
	final Runnable _runnableRemiseAZero = new Runnable()
	{
		@Override public void run()
		{
			// Delai ecoule depuis la derniere position, on considere que la vitesse est 0
			if (_atvVitesse != null)
				if (isInPictureInPictureMode())
					_atvVitesse.setText("-");
		}
	};

	/***
	 * Creation de l'activity et de son contenu
	 * @param savedInstanceState
	 */
	@Override protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);

		// Initialiser l'activity si les permissions sont accordees
		if (checkPermissions())
		{
			initActivity();
		}
	}


	/***
	 * Verifie que les permisions Android sont accordees, les demande sinon
	 * @return true si les permissions sont accordees
	 */
	private boolean checkPermissions()
	{
		boolean permissions = true;
		for (String p : PERMISSIONS)
			if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED)
			{
				permissions = false;
				break;
			}

		if (permissions)
			return true;

		requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
		return false;
	}

	/***
	 * Retour de la demande de permissions Android
	 * @param requestCode
	 * @param permissions
	 * @param grantResults
	 */
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
	{
		if (requestCode == PERMISSIONS_REQUEST_CODE)
		{
			if (checkPermissions())
				initActivity();
		}

		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	/***********************************************************************************************
	 * Initialisation des controles de l'activity
	 */
	private void initActivity()
	{
		final Preferences preferences = Preferences.getInstance(this);
		_useSpeed = preferences.getBoolean(Preferences.USE_SPEED, false);
		_useBearing = preferences.getBoolean(Preferences.USE_BEARING, false);

		////////////////////////////////////////////////////////////////////////////////////////////
		// Listener pour ajuster la hauteur des affichages vitesse/cap
		//View.OnLayoutChangeListener layoutListener = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
		//{
		//	{
		//		final float ratio = preferences.getFloat(Preferences.RATIO_VITESSE_CAP, 0.9f);
		//		// Changer la taille de la vue Direction pour qu'elle occupe le tiers de la hauteur et
		//		// de la largeur
		//		ViewGroup.LayoutParams params = _atvVitesse.getLayoutParams();
		//		params.height = (int) ((bottom - top) * ratio);
		//		_atvVitesse.setLayoutParams(params);
		//	}
		//
		//	{
		//		// Changer la taille de la vue Altitude pour qu'elle occupe 0.3 de la largeur
		//		// et 0.2 de la hauteur
		//		ViewGroup.LayoutParams params = _atvAltitude.getLayoutParams();
		//		params.height = (int) ((bottom - top) * 0.25);
		//		params.width = (int) ((right - left) * 0.4);
		//		_atvAltitude.setLayoutParams(params);
		//	}
		//};
		////////////////////////////////////////////////////////////////////////////////////////////
		// Layout fullscreen
		_fullscreenLayout = findViewById(R.id.layoutFullScreen);
		//_fullscreenLayout.addOnLayoutChangeListener(layoutListener);

		////////////////////////////////////////////////////////////////////////////////////////////
		// Layout Picture in Picture
		_pipLayout = findViewById(R.id.layoutPip);
		//_pipLayout.addOnLayoutChangeListener(layoutListener);


		////////////////////////////////////////////////////////////////////////////////////////////
		// Bouton "Fonte"
		{
			Button bFonte = findViewById(R.id.bFonte);
			bFonte.setOnClickListener(view ->
					{
						FontPickerDialog.start(MainActivity.this, new FontPickerDialog.Listener()
						{
							@Override public void onOK(@NonNull final String fontPath)
							{
								preferences.setString(Preferences.FONTE, fontPath);
								//_atvVitesse.setFont(fontPath);
								setTypeFace( _atvVitesse, fontPath);
								setTypeFace( _atvAltitude, fontPath);
							}

							@Override public void onCancel()
							{
							}
						});
					}
			);
		}

		////////////////////////////////////////////////////////////////////////////////////////////
		// Bouton "Mode incrusté"
		Button b = findViewById(R.id.bSwitch);
		b.setOnClickListener(view ->
		{
			PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
			{
				builder = builder.setSeamlessResizeEnabled(false);
			}
			PictureInPictureParams params = builder.build();
			// restaurer l'ancienne position : pas possible, non documenté
			MainActivity.this.enterPictureInPictureMode(params);
		});


		////////////////////////////////////////////////////////////////////////////////////////////
		// Fragment Couleur de texte
		{
			ColorFragment fragmentCouleurTexte = (ColorFragment) getSupportFragmentManager().findFragmentById(R.id.frCouleurTexte);

			fragmentCouleurTexte.setTitre(getString(R.string.texte));
			fragmentCouleurTexte.setCouleur(preferences.getInt(Preferences.COULEUR_TEXTE, COULEUR_TEXTE_DEFAUT));
			fragmentCouleurTexte.setListener(couleur ->
			{
				_atvVitesse.setTextColor(couleur);
				_boussoleView.setTextColor(couleur);
				preferences.setInt(Preferences.COULEUR_TEXTE, couleur);
			});
		}

		////////////////////////////////////////////////////////////////////////////////////////////
		// Fragment Couleur de fond
		{
			ColorFragment fragmentCouleurFond = (ColorFragment) getSupportFragmentManager().findFragmentById(R.id.frCouleurFond);
			Objects.requireNonNull(fragmentCouleurFond).setTitre(getString(R.string.fond));
			fragmentCouleurFond.setCouleur(preferences.getInt(Preferences.COULEUR_FOND, COULEUR_FOND_DEFAUT));
			fragmentCouleurFond.setListener(couleur ->
			{
				_vFond.setBackgroundColor(couleur);
				preferences.setInt(Preferences.COULEUR_FOND, couleur);
			});
		}

		////////////////////////////////////////////////////////////////////////////////////////////
		// Bouton A propos
		{
			ImageButton ibAbout = findViewById(R.id.ibAbout);
			ibAbout.setOnClickListener(view -> DialogAPropos.start(MainActivity.this));
		}

		////////////////////////////////////////////////////////////////////////////////////////////
		// Parametres avances
		{
			ImageButton ibSettings = findViewById(R.id.ibSettings);
			ibSettings.setOnClickListener(view -> AdvancedParametersDialog.start(MainActivity.this, new AdvancedParametersDialog.Listener()
			{
				@Override public void onOK()
				{
					_useSpeed = preferences.getBoolean(Preferences.USE_SPEED, false);
					_useBearing = preferences.getBoolean(Preferences.USE_BEARING, false);
					stopGPS();
					startGPS();
				}

				@Override public void onCancel()
				{
				}
			}));
		}
		if (isInPictureInPictureMode())
			switchToPip();
		else
			switchToFullScreen();

		startGPS();
	}


	/***********************************************************************************************
	 * Le systeme nous averti du passage au mode Picture in Picture
	 * @param isInPictureInPictureMode
	 * @param newConfig
	 */
	@Override
	public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig)
	{
		if (isInPictureInPictureMode)
			switchToPip();
		else
			switchToFullScreen();
	}

	/***********************************************************************************************
	 * Passer en mode Activite en plein ecran
	 */
	private void switchToFullScreen()
	{
		_fullscreenLayout.setVisibility(View.VISIBLE);
		_pipLayout.setVisibility(View.GONE);
		_atvVitesse = findViewById(R.id.atvVitesse);
		_atvAltitude = findViewById(R.id.atvAltitude);
		_boussoleView = findViewById(R.id.boussoleView);
		_vFond = findViewById(R.id.vFond);

		Preferences preferences = Preferences.getInstance(this);
		_atvVitesse.setTextColor(preferences.getInt(Preferences.COULEUR_TEXTE, COULEUR_TEXTE_DEFAUT));
		setTypeFace( _atvVitesse,  preferences.getString(Preferences.FONTE, ""))  ;
		setTypeFace( _atvAltitude, preferences.getString(Preferences.FONTE, ""));

		_boussoleView.setTextColor(preferences.getInt(Preferences.COULEUR_TEXTE, COULEUR_TEXTE_DEFAUT));
		_vFond.setBackgroundColor(preferences.getInt(Preferences.COULEUR_FOND, COULEUR_FOND_DEFAUT));
	}

	private void setTypeFace(TextView view, String fontpath)
	{
		try
		{
			Typeface tf = Typeface.createFromFile(fontpath);
			view.setTypeface(tf);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/***********************************************************************************************
	 * Passer en mode Activite en Picture in Picture
	 */
	private void switchToPip()
	{
		_fullscreenLayout.setVisibility(View.GONE);
		_pipLayout.setVisibility(View.VISIBLE);
		_atvVitesse = findViewById(R.id.atvVitessePip);
		_atvAltitude = findViewById(R.id.atvAltitudePip);
		_boussoleView = findViewById(R.id.boussoleViewPip);
		_vFond = findViewById(R.id.vFondPip);

		Preferences preferences = Preferences.getInstance(this);
		_atvVitesse.setTextColor(preferences.getInt(Preferences.COULEUR_TEXTE, COULEUR_TEXTE_DEFAUT));
		setTypeFace(_atvVitesse, preferences.getString(Preferences.FONTE, ""));
		setTypeFace(_atvAltitude, preferences.getString(Preferences.FONTE, ""));
		_boussoleView.setTextColor(preferences.getInt(Preferences.COULEUR_TEXTE, COULEUR_TEXTE_DEFAUT));
		_vFond.setBackgroundColor(preferences.getInt(Preferences.COULEUR_FOND, COULEUR_FOND_DEFAUT));
	}

	/***********************************************************************************************
	 * Demarre le GPS, proposer l'acces a l'ecran de parametres GPS s'il est inaccessible
	 */
	private void startGPS()
	{
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
		{
			// Les permissions ont ete demandées a l'ouverture de l'application, donc ce code est juste pour faire plaisir a Android Studio
			return;
		}

		try
		{
			Preferences preferences = Preferences.getInstance(this);

			// Lissage du cap
			_boussoleView.setNbValeursLissage(preferences.getInt(Preferences.DELAI_LISSAGE_CAP_SECONDES, 10));

			if (_locationManager != null)
				_locationManager.removeUpdates(this);

			_locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

			// Choisir un fournisseur de localisation
			String provider = preferences.getString(Preferences.PROVIDER, LocationManager.GPS_PROVIDER);
			if ("".equals(provider))
				provider = GPSUtils.getBestProvider(_locationManager);

			final int minDistance = preferences.getInt(Preferences.DISTANCE_MIN, MIN_DISTANCE_M);
			final int minTemps = preferences.getInt(Preferences.TEMPS_MIN, MIN_TIME_S);
			_locationManager.requestLocationUpdates(provider, minTemps * 1000L, minDistance, this);
			Toast.makeText(this, getString(R.string.gps_provider, provider), Toast.LENGTH_SHORT).show();
		} catch (Exception e)
		{
			MessageBoxUtils.messageBox(this, R.string.no_gps_titre, R.string.no_gps, MessageBoxUtils.BOUTON_OK | MessageBoxUtils.BOUTON_CANCEL, () ->
					startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)));
		}
	}

	/***********************************************************************************************
	 * Arreter le GPS
	 */
	private void stopGPS()
	{
		if (_locationManager != null)
		{
			_locationManager.removeUpdates(this);
			_locationManager = null;
		}
	}


	/***********************************************************************************************
	 * Reception d'une nouvelle position du GPS
	 * @param position
	 */
	public void onLocationChanged(@NonNull Location position)
	{
		if (GPSUtils.isBetterLocation(position, _precedente))
		{
			// Vitesse
			final float vitesse = GPSUtils.getSpeed(position, _precedente, _useSpeed);
			String texte = formatSpeed(vitesse);
			_atvVitesse.setText(texte);

			// Direction
			float bearing = GPSUtils.getBearing(position, _precedente, _useBearing);
			bearing = limite(bearing, 0, 360);
			_boussoleView.setCap(bearing);

			// Altitude
			float altitude = GPSUtils.getAltitude(position);
			texte = formatAltitude(altitude);
			_atvAltitude.setText(texte);

			// Memoriser la derniere position pour le prochain calcul de vitesse
			_precedente = position;

			// Annuler le handler de remise a zero
			_handlerRemiseZero.removeCallbacks(_runnableRemiseAZero);

			// Remettre la vitesse a zero si on ne recoit pas de changement de position pendant un certain temps
			_handlerRemiseZero.postDelayed(_runnableRemiseAZero, 5000L * (1 + Preferences.getInstance(this).getInt(Preferences.TEMPS_MIN, MIN_TIME_S)));
		}
	}

	private String formatAltitude(float altitude)
	{
		return getString(R.string.formatAltitude, (int) altitude);
	}

	/***
	 * Limite la valeur donnee entre deux bornes
	 * @param valeur
	 * @param min
	 * @param max
	 * @return
	 */
	private float limite(float valeur, float min, float max)
	{
		final float step = max - min;
		while (valeur < min)
			valeur += step;

		while (valeur > max)
			valeur -= step;
		return valeur;
	}


	/***********************************************************************************************
	 * Formate la vitesse en metres par seconde
	 * @param speed
	 * @return
	 */
	private String formatSpeed(final float speed)
	{
//		if ( speed < 10)
//			return String.format(getResources().getConfiguration().getLocales().get(0),"%.1f", speed);
//		else
		// Pas de decimale au dessus de 20km/h
		return Integer.toString((int) speed);
	}

	/***********************************************************************************************
	 * Notre fournisseur de position est desactive, essayer d'en utiliser un autre
	 * @param provider
	 */
	@Override public void onProviderDisabled(@NonNull String provider)
	{
		Toast.makeText(this, R.string.gps_provider_disabled, Toast.LENGTH_SHORT).show();
		startGPS();
	}


	/***
	 * Arret de l'application
	 */
	@Override protected void onDestroy()
	{
		stopGPS();
		super.onDestroy();
	}

	/***
	 * Mise en pause de l'activity: arreter le GPS, sauf si on est en Pip
	 */
	@Override protected void onPause()
	{
		super.onPause();

		// on peut recevoir onPause quand on passe en mode PiP: ne pas arreter le GPS
		if (!isInPictureInPictureMode())
		{
			stopGPS();
		}
	}

	@Override protected void onResume()
	{
		super.onResume();
		startGPS();
	}
}