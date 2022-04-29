package com.lpi.vitesse;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/***
 * Classe pour gerer les preferences
 */
@SuppressWarnings("CommentedOutCode")
public class Preferences
{
	public static final @NonNull String PREFERENCES_COULEUR_TEXTE = "CouleurTexte";
	public static final @NonNull String PREFERENCES_COULEUR_FOND = "CouleurFond";
	public static final String PREFERENCES_DISTANCE_MIN = "DistanceMin";
	public static final String PREFERENCES_TEMPS_MIN = "TempsMin";
	public static final String PREFERENCES_PROVIDER = "Provider";

	@NonNull private static final String PREFERENCES = Preferences.class.getName();

	private @Nullable static Preferences _instance;
	private @NonNull final SharedPreferences settings;
	private @NonNull final SharedPreferences.Editor editor;

	/***
	 * Obtenir l'instance (unique) de Preferences
	 * On peut donner un Context null si l'objet a deja ete initialisé
	 */
	public static synchronized Preferences getInstance(@Nullable final Context context)
	{
		if (_instance == null)
			if (context != null)
				_instance = new Preferences(context);

		return _instance;
	}

	/***
	 * Constructeur privé, utilisable uniquement dans getInstance
	 */
	private Preferences(final @NonNull Context context)
	{
		settings = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
		editor = settings.edit();

	}

	public int getInt(@NonNull final String nom, int defaut)
	{
		return settings.getInt(nom, defaut);
	}


	public void setBoolean(@NonNull final String nom, boolean val)
	{
		editor.putBoolean(nom, val);
		editor.apply();
	}


	public boolean getBoolean(@NonNull final String nom, boolean defaut)
	{
		return settings.getBoolean(nom, defaut);
	}


	public void setInt(@NonNull final String nom, int val)
	{
		editor.putInt(nom, val);
		editor.apply();
	}

/*
	public float getFloat(@NonNull final String nom, float defaut)
	{
		return settings.getFloat(nom, defaut);
	}


	public void setFloat(@NonNull final String nom, float val)
	{
		editor.putFloat(nom, val);
		editor.apply();
	}


	public void setChar(@NonNull final String nom, char val)
	{
		editor.putString(nom, "" + val);
		editor.apply();
	}

	public char getChar(@NonNull final String nom, char defaut)
	{
		return settings.getString(nom, defaut + " ").charAt(0);
	}
*/
	public String getString(@NonNull final String nom, final String defaut)
	{
		return settings.getString(nom, defaut);
	}

	public void setString(@NonNull final String nom, String val)
	{
		editor.putString(nom, val);
		editor.apply();
	}

}
