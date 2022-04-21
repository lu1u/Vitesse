package com.lpi.vitesse;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class GPSUtils
{
	private static final int TWO_MINUTES = 1000 * 60 * 2;
	private static final float MS_TO_KMH = 3.6f;
	private static final float MAX_IMPRECISION = 20;

	public static final int DIRECTION_NORD = 0;
	public static final int DIRECTION_SUD = 1;
	public static final int DIRECTION_EST = 2;
	public static final int DIRECTION_OUEST= 3;

	/**
	 * Determines whether one Location reading is better than the current Location fix
	 *
	 * @param location            The new Location that you want to evaluate
	 * @param currentBestLocation The current Location fix, to which you want to compare the new one
	 */
	public static boolean isBetterLocation(@NonNull final Location location, @Nullable final Location currentBestLocation)
	{
		if (currentBestLocation == null)
			// A new location is always better than no location
			return true;

		if ( location.getAccuracy() > MAX_IMPRECISION)
			// Trop imprecis
			return false;


		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use the new location
		// because the user has likely moved
		if (isSignificantlyNewer)
		{
			return true;
			// If the new location is more than two minutes older, it must be worse
		}
		else if (isSignificantlyOlder)
		{
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),	currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and accuracy
		if (isMoreAccurate)
		{
			return true;
		}
		else if (isNewer && !isLessAccurate)
		{
			return true;
		}
		else return isNewer && !isSignificantlyLessAccurate && isFromSameProvider;
	}


	/**
	 * Checks whether two providers are the same
	 */
	private static boolean isSameProvider(@Nullable String provider1, @Nullable String provider2)
	{
		if (provider1 == null)
		{
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	/***
	 * Retrouve la vitesse d'une position GPS
	 * Si la position n'a pas de vitesse, on calcule a partir de la position precedente
	 * @param position Position actuelle
	 * @param precedente Position precedente
	 * @return
	 */
	public static float getSpeed(@Nullable final Location position, @Nullable final Location precedente)
	{
		if ( position == null)
			return 0;

		if (position.hasSpeed())
			return position.getSpeed();

		if (precedente != null)
		{
			float secondes = (float) (position.getTime() - precedente.getTime()) / 1000.0f;
			float distance = position.distanceTo(precedente);
			return distance / secondes * MS_TO_KMH;
		}

		return 0;
	}

	/***
	 * Retrouve le cap d'une position GPS
	 * Si la position n'a pas de vitesse, on calcule a partir de la position precedente
	 * @param position Position actuelle
	 * @param precedente Position precedente
	 * @return
	 */
	public static float getBearing(@Nullable final Location position, @Nullable final Location precedente)
	{
		if (position == null)
			return 0;

		if (position.hasBearing())
			return position.getBearing();

		if (precedente != null)
			return precedente.bearingTo(position);

		return 0;
	}


		/***
		 * Obtient le meilleur fournisseur de position pour avoir une mesure de vitesse precise
		 * @param locationManager
		 * @return
		 */
	public static String getBestProvider(@NonNull final LocationManager locationManager)
	{
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setAltitudeRequired(false);
		criteria.setBearingRequired(false);
		criteria.setCostAllowed(true);
		criteria.setPowerRequirement(Criteria.POWER_HIGH);
		return locationManager.getBestProvider(criteria, true);
	}

	/***
	 * Retourne l'indice du nom de la direction dans un string array qui se trouve dans les ressources
	 * @param heading
	 * @return
	 */
	public static int getDirection(final float heading)
	{
		if (heading < 45)
			return DIRECTION_NORD ;
		else if (heading < 135)
			return DIRECTION_EST;
		else if (heading < 225)
			return DIRECTION_SUD;
		else if (heading < 315)
			return DIRECTION_OUEST;
		else
			return DIRECTION_NORD;
	}
}
