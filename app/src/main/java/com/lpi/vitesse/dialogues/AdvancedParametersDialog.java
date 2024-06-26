package com.lpi.vitesse.dialogues;

import android.app.Activity;
import android.app.AlertDialog;
import android.location.LocationManager;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lpi.vitesse.Preferences;
import com.lpi.vitesse.R;

public class AdvancedParametersDialog
{
	// R.arrays.providers
	public static final String[] PROVIDERS = {"", LocationManager.FUSED_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER};
	public interface Listener
	{
		void onOK();
		void onCancel();
	}

	private static InputFilter inRange(int min, int max)
	{
		return (source, start1, end, dest, dstart, dend) ->
		{
			try
			{
				int input = Integer.parseInt(dest.toString() + source.toString());
				if (input < min || input > max)
					return "";
				else
					return null;
			} catch (Exception e)
			{
				e.printStackTrace();
				return null;
			}
		};
	}

	private static InputFilter inRange(float min, float max)
	{
		return (source, start1, end, dest, dstart, dend) ->
		{
			try
			{
				float input = Float.parseFloat(dest.toString() + source.toString());
				if (input < min || input > max)
					return "";
				else
					return null;
			} catch (Exception e)
			{
				e.printStackTrace();
				return null;
			}
		};
	}
	public static void start(@NonNull final Activity activity, @Nullable final Listener listener)
	{
		final AlertDialog dialogBuilder = new AlertDialog.Builder(activity).create();
		LayoutInflater inflater = activity.getLayoutInflater();
		View dialogView = inflater.inflate(R.layout.advanced_parameters, null);

		final Preferences preferences = Preferences.getInstance(activity);

		// Provider
		final String provider = preferences.getString(Preferences.PROVIDER, LocationManager.GPS_PROVIDER);
		Spinner spProvider = dialogView.findViewById(R.id.spProvider);
		spProvider.setSelection(getPosition(provider));

		// Distance min
		EditText etMinDistance = dialogView.findViewById(R.id.etMinDistance);
		etMinDistance.setText(Integer.toString(preferences.getInt(Preferences.DISTANCE_MIN, 0)));
		etMinDistance.setFilters(new InputFilter[]{inRange(0, 200)});

		// Temps max
		EditText etMinTemps = dialogView.findViewById(R.id.etMinTime);
		etMinTemps.setText(Integer.toString(preferences.getInt(Preferences.TEMPS_MIN, 0)));
		etMinTemps.setFilters(new InputFilter[]{inRange(0, 10)});

		// Vitesse GPS
		Switch swVitesse = dialogView.findViewById(R.id.swVitesseGPS);
		swVitesse.setChecked(preferences.getBoolean(Preferences.USE_SPEED, false));

		// Cap GPS
		Switch swCap = dialogView.findViewById(R.id.swCapGPS);
		swCap.setChecked(preferences.getBoolean(Preferences.USE_BEARING, false));

		// Lissage cap
		EditText etLissageCap = dialogView.findViewById(R.id.etNbValeursLissage);
		etLissageCap.setText(Integer.toString(preferences.getInt(Preferences.DELAI_LISSAGE_CAP_SECONDES, 10)));
		etLissageCap.setFilters(new InputFilter[]{inRange(10, 1000)});

		// Boutons OK et Annuler
		Button bOk = dialogView.findViewById(R.id.btOk);
		bOk.setOnClickListener(view ->
		{
			final int minDistance = contraint(0, 200, etMinDistance.getText().toString());
			final int minTemps = contraint(0, 200, etMinTemps.getText().toString());
			final int nbValeurs = contraint(10, 1000, etLissageCap.getText().toString());

			final int prov = spProvider.getSelectedItemPosition();
			preferences.setInt(Preferences.DISTANCE_MIN, minDistance);
			preferences.setInt(Preferences.TEMPS_MIN, minTemps);
			preferences.setInt(Preferences.DELAI_LISSAGE_CAP_SECONDES, nbValeurs);

			preferences.setString(Preferences.PROVIDER, PROVIDERS[prov]);
			preferences.setBoolean(Preferences.USE_SPEED, swVitesse.isChecked());
			preferences.setBoolean(Preferences.USE_BEARING, swCap.isChecked());
			if (listener != null)
				listener.onOK();
			dialogBuilder.dismiss();
		});

		Button bCancel = dialogView.findViewById(R.id.btCancel);
		bCancel.setOnClickListener(view ->
		{
			if (listener != null)
				listener.onCancel();
			dialogBuilder.dismiss();
		});

		dialogBuilder.setView(dialogView);
		dialogBuilder.show();
	}

	/***
	 * Retourne l'indice du provider dans le tableau PROVIDERS
	 * @param provider
	 * @return
	 */
	private static int getPosition(String provider)
	{
		for ( int i =0; i< PROVIDERS.length;i++)
			if ( PROVIDERS[i].equals(provider))
				return i;

		return 0;
	}

	/***
	 * Retourne la valeur representee par la chaine de caracteres, contrainte entre min et max
	 * @param min
	 * @param max
	 * @param text
	 * @return
	 */
	private static int contraint(int min, int max, String text)
	{
		try
		{
			int val = Integer.parseInt(text);
			if (val < min)
				return min;

			if (val > max)
				return max;

			return val;
		} catch (Exception e)
		{
			return min;
		}
	}

	/***
	 * Retourne la valeur representee par la chaine de caracteres, contrainte entre min et max
	 * @param min
	 * @param max
	 * @param text
	 * @return
	 */
	private static float contraint(float min, float max, String text)
	{
		try
		{
			float val = Float.parseFloat(text);
			if (val < min)
				return min;

			if (val > max)
				return max;

			return val;
		} catch (Exception e)
		{
			return min;
		}
	}
}
