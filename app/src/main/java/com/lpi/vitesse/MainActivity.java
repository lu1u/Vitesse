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
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import java.text.NumberFormat;

public class MainActivity extends AppCompatActivity implements LocationListener
{
	public static final int COULEUR_TEXTE_DEFAUT = Color.WHITE;
	public static final int COULEUR_FOND_DEFAUT = 0x88000000;

	public static final @NonNull String[] PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
	private static final int PERMISSIONS_REQUEST_CODE = 12458 ;
	public static final int MIN_TIME_MS = 1000;
	public static final int MIN_DISTANCE_M = 0;

	ViewGroup _fullscreenLayout, _pipLayout;
	private @Nullable LocationManager _locationManager;
	private @Nullable Location _precedente;
	private AutosizeTextView _atvVitesse, _atvVitessePip, _atvDirection, _atvDirectionPip ;
	private String[] _tableauDirections;

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

		if ( isInPictureInPictureMode())
			switchToPip();
		else
			switchToFullScreen();

		_tableauDirections = getResources().getStringArray(R.array.directions);
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
		if ( requestCode == PERMISSIONS_REQUEST_CODE)
		{
			if ( checkPermissions())
				initActivity();
		}

		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	/***
	 * Initialisation des controles de l'activity
	 */
	private void initActivity()
	{
		_fullscreenLayout = findViewById(R.id.layoutFullScreen);
		_pipLayout = findViewById(R.id.layoutPip);

		Button b = findViewById(R.id.bSwitch);
		b.setOnClickListener(view ->
		{
			PictureInPictureParams params = new PictureInPictureParams.Builder().build();
			MainActivity.this.enterPictureInPictureMode(params);
		});

		final Preferences preferences = Preferences.getInstance(this);

		////////////////////////////////////////////////////////////////////////////////////////////
		// Affichage de la vitesse
		_atvVitesse = findViewById(R.id.atvVitesse);
		_atvVitesse.setTextColor(preferences.getInt(Preferences.PREFERENCES_COULEUR_TEXTE, COULEUR_TEXTE_DEFAUT));
		_atvVitesse.setBackgroundColor(preferences.getInt(Preferences.PREFERENCES_COULEUR_FOND, COULEUR_FOND_DEFAUT));

		_atvVitessePip = findViewById(R.id.atvVitessePip);
		_atvVitessePip.setTextColor(preferences.getInt(Preferences.PREFERENCES_COULEUR_TEXTE, COULEUR_TEXTE_DEFAUT));
		_atvVitessePip.setBackgroundColor(preferences.getInt(Preferences.PREFERENCES_COULEUR_FOND, COULEUR_FOND_DEFAUT));

		////////////////////////////////////////////////////////////////////////////////////////////
		// Direction
		_atvDirection = findViewById(R.id.atvDirection);
		_atvDirection.setTextColor(preferences.getInt(Preferences.PREFERENCES_COULEUR_TEXTE, COULEUR_TEXTE_DEFAUT));
		_atvDirectionPip = findViewById(R.id.atvDirectionPip);
		_atvDirectionPip.setTextColor(preferences.getInt(Preferences.PREFERENCES_COULEUR_TEXTE, COULEUR_TEXTE_DEFAUT));

		////////////////////////////////////////////////////////////////////////////////////////////
		// Fragment Couleur de texte
		ColorFragment fragmentCouleurTexte = (ColorFragment) getSupportFragmentManager().findFragmentById(R.id.frCouleurTexte);
		fragmentCouleurTexte.setTitre(getString(R.string.texte));
		fragmentCouleurTexte.setCouleur(preferences.getInt(Preferences.PREFERENCES_COULEUR_TEXTE, COULEUR_TEXTE_DEFAUT));
		fragmentCouleurTexte.setListener(couleur ->
		{
			_atvVitesse.setTextColor(couleur);
			_atvVitessePip.setTextColor(couleur);
			_atvDirection.setTextColor(couleur);
			_atvDirectionPip.setTextColor(couleur);
			preferences.setInt(Preferences.PREFERENCES_COULEUR_TEXTE, couleur);
		});

		////////////////////////////////////////////////////////////////////////////////////////////
		// Fragment Couleur de fond
		ColorFragment fragmentCouleurFond = (ColorFragment) getSupportFragmentManager().findFragmentById(R.id.frCouleurFond);
		fragmentCouleurFond.setTitre(getString(R.string.fond));
		fragmentCouleurFond.setCouleur(preferences.getInt(Preferences.PREFERENCES_COULEUR_FOND, COULEUR_FOND_DEFAUT));
		fragmentCouleurFond.setListener(couleur ->
		{
			_atvVitesse.setBackgroundColor(couleur);
			_atvVitessePip.setBackgroundColor(couleur);
			preferences.setInt(Preferences.PREFERENCES_COULEUR_FOND, couleur);
		});

		startGPS();
	}


	/***
	 * Le systeme nous averti du passage au mode Picture in Picture
	 * @param isInPictureInPictureMode
	 * @param newConfig
	 */
	@Override public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig)
	{
		if (isInPictureInPictureMode)
			switchToPip();
		else
			switchToFullScreen();
	}

	/***
	 * Passer en mode Activite en plein ecran
	 */
	private void switchToFullScreen()
	{
		_fullscreenLayout.setVisibility(View.VISIBLE);
		_pipLayout.setVisibility(View.GONE);
	}

	/***
	 * Passer en mode Activite en Picture in Picture
	 */
	private void switchToPip()
	{
		_fullscreenLayout.setVisibility(View.GONE);
		_pipLayout.setVisibility(View.VISIBLE);
	}

	/***
	 * Demarre le GPS
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

			_locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			_locationManager.requestLocationUpdates(GPSUtils.getBestProvider(_locationManager), MIN_TIME_MS, MIN_DISTANCE_M, this);
		} catch (Exception e)
		{
			MessageBoxUtils.messageBox(this, R.string.no_gps_titre, R.string.no_gps, MessageBoxUtils.BOUTON_OK | MessageBoxUtils.BOUTON_CANCEL, new MessageBoxUtils.Listener()
			{
				@Override public void onOk()
				{
					startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
				}
			});
			e.printStackTrace();
		}
	}

	/***
	 * Arreter le GPS
	 */
	private void stopGPS()
	{
		if (_locationManager!=null)
		{
			_locationManager.removeUpdates(this);
			_locationManager = null;
		}
	}

	/***
	 * Reception d'une nouvelle position du GPS
	 * @param position
	 */
	public void onLocationChanged(@NonNull Location position)
	{
		if (GPSUtils.isBetterLocation(position, _precedente))
		{
			// Vitesse
			String texte = getTextVitesse(GPSUtils.getSpeed(position, _precedente));
			_atvVitesse.setText(texte);
			_atvVitessePip.setText(texte);

			// Direction
			float bearing = GPSUtils.getBearing(position, _precedente);
			int indice = GPSUtils.getDirection(bearing);
			if ( indice > 0 && indice < _tableauDirections.length)
			{
				_atvDirection.setText(_tableauDirections[indice]);
			}	_atvDirectionPip.setText(_tableauDirections[indice]);

			_precedente = position;
		}
	}

	/***
	 * Retourne un texte representant la vitesse courante
	 * @param speed
	 * @return
	 */
	private static @NonNull String getTextVitesse(float speed)
	{
		return NumberFormat.getInstance().format(Math.round(speed));
	}

	/***
	 * Arret de l'application
	 */
	@Override	protected void onDestroy()
	{
		stopGPS();
		super.onDestroy();
	}

	@Override protected void onPause()
	{
		super.onPause();
		if (! isInPictureInPictureMode())
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