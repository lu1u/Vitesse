package com.lpi.vitesse;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.lpi.vitesse.customviews.AutosizeTextView;
import com.lpi.vitesse.customviews.CapView;
import com.lpi.vitesse.dialogues.AdvancedParametersDialog;
import com.lpi.vitesse.dialogues.DialogAPropos;

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
	private AutosizeTextView _atvVitesse;
	private CapView _capView;
	private View _vFond;

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
			initActivity();

		if (isInPictureInPictureMode())
			switchToPip();
		else
			switchToFullScreen();
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

		////////////////////////////////////////////////////////////////////////////////////////////
		// Layout fullscreen
		_fullscreenLayout = findViewById(R.id.layoutFullScreen);
		_fullscreenLayout.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
		{
			// Changer la taille de la vue Direction pour qu'elle occupe le tiers de la hauteur et
			// de la largeur
			ViewGroup.LayoutParams params = _capView.getLayoutParams();
			params.height = (bottom-top)/6;
			//params.width = (right-left)/4;
			_capView.setLayoutParams(params);
		});

		////////////////////////////////////////////////////////////////////////////////////////////
		// Layout Picture in Picture
		_pipLayout = findViewById(R.id.layoutPip);
		_pipLayout.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
		{
			// Changer la taille de la vue Direction pour qu'elle occupe le tiers de la hauteur et
			// de la largeur
			ViewGroup.LayoutParams params = _capView.getLayoutParams();
			params.height = (bottom-top)/4;
			//params.width = (right-left)/4;
			_capView.setLayoutParams(params);
		});

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

			MainActivity.this.enterPictureInPictureMode(params);
		});


		////////////////////////////////////////////////////////////////////////////////////////////
		// Fragment Couleur de texte
		{
			ColorFragment fragmentCouleurTexte = (ColorFragment) getSupportFragmentManager().findFragmentById(R.id.frCouleurTexte);

			fragmentCouleurTexte.setTitre(getString(R.string.texte));
			fragmentCouleurTexte.setCouleur(preferences.getInt(Preferences.PREFERENCES_COULEUR_TEXTE, COULEUR_TEXTE_DEFAUT));
			fragmentCouleurTexte.setListener(couleur ->
			{
				_atvVitesse.setTextColor(couleur);
				_capView.setTextColor(couleur);
				preferences.setInt(Preferences.PREFERENCES_COULEUR_TEXTE, couleur);
			});
		}

		////////////////////////////////////////////////////////////////////////////////////////////
		// Fragment Couleur de fond
		{
			ColorFragment fragmentCouleurFond = (ColorFragment) getSupportFragmentManager().findFragmentById(R.id.frCouleurFond);
			Objects.requireNonNull(fragmentCouleurFond).setTitre(getString(R.string.fond));
			fragmentCouleurFond.setCouleur(preferences.getInt(Preferences.PREFERENCES_COULEUR_FOND, COULEUR_FOND_DEFAUT));
			fragmentCouleurFond.setListener(couleur ->
			{
				_vFond.setBackgroundColor(couleur);
				preferences.setInt(Preferences.PREFERENCES_COULEUR_FOND, couleur);
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
					stopGPS();
					startGPS();
				}

				@Override public void onCancel()
				{

				}
			}));
		}

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
		_capView = findViewById(R.id.capView);
		_vFond =findViewById(R.id.vFond);

		Preferences preferences = Preferences.getInstance(this);
		_atvVitesse.setTextColor(preferences.getInt(Preferences.PREFERENCES_COULEUR_TEXTE, COULEUR_TEXTE_DEFAUT));
		_capView.setTextColor(preferences.getInt(Preferences.PREFERENCES_COULEUR_TEXTE, COULEUR_TEXTE_DEFAUT));
		_vFond.setBackgroundColor(preferences.getInt(Preferences.PREFERENCES_COULEUR_FOND, COULEUR_FOND_DEFAUT));
	}

	/***********************************************************************************************
	 * Passer en mode Activite en Picture in Picture
	 */
	private void switchToPip()
	{
		_fullscreenLayout.setVisibility(View.GONE);
		_pipLayout.setVisibility(View.VISIBLE);
		_atvVitesse = findViewById(R.id.atvVitessePip);
		_capView = findViewById(R.id.capViewPip);
		_vFond =findViewById(R.id.vFondPip);

		Preferences preferences = Preferences.getInstance(this);
		_atvVitesse.setTextColor(preferences.getInt(Preferences.PREFERENCES_COULEUR_TEXTE, COULEUR_TEXTE_DEFAUT));
		_capView.setTextColor(preferences.getInt(Preferences.PREFERENCES_COULEUR_TEXTE, COULEUR_TEXTE_DEFAUT));
		_vFond.setBackgroundColor(preferences.getInt(Preferences.PREFERENCES_COULEUR_FOND, COULEUR_FOND_DEFAUT));
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
			if (_locationManager != null)
				_locationManager.removeUpdates(this);

			Preferences preferences = Preferences.getInstance(this);
			final int minDistance = preferences.getInt(Preferences.PREFERENCES_DISTANCE_MIN, MIN_DISTANCE_M);
			final int minTemps = preferences.getInt(Preferences.PREFERENCES_TEMPS_MIN, MIN_TIME_S);

			_locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

			// Choisir un fournisseur de localisation
			String provider = preferences.getString(Preferences.PREFERENCES_PROVIDER, "");
			if ("".equals(provider))
				provider = GPSUtils.getBestProvider(_locationManager);

			_locationManager.requestLocationUpdates(provider, minTemps*1000L, minDistance, this);
			Toast.makeText(this, getString(R.string.gps_provider, provider), Toast.LENGTH_SHORT).show();
		} catch (Exception e)
		{
			MessageBoxUtils.messageBox(this, R.string.no_gps_titre, R.string.no_gps, MessageBoxUtils.BOUTON_OK | MessageBoxUtils.BOUTON_CANCEL, () -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)));
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
			final float vitesse = GPSUtils.getSpeed(position, _precedente);
			String texte = formatSpeed(vitesse);
			_atvVitesse.setText(texte);

			// Direction
			float bearing = GPSUtils.getBearing(position, _precedente);
			while (bearing<0)
				bearing += 360.0f;

			while (bearing>360.0f)
				bearing -= 360.0f;

			_capView.setCap(bearing);

			// Memoriser la derniere position pour le prochain calcul de vitesse
			_precedente = position;
		}
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
			return Integer.toString((int)speed);
	}

	/***********************************************************************************************
	 * Notre fournisseur de position est desactive, essayer d'en utiliser un autre
	 * @param provider
	 */
	@Override public void onProviderDisabled(@NonNull String provider)
	{
		Toast.makeText(this, R.string.gps_provider_disabled, Toast.LENGTH_SHORT).show();
		startGPS();
		LocationListener.super.onProviderDisabled(provider);
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