
package com.lpi.vitesse;

import android.app.AlertDialog;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

/***
 * Classe utilitaire pour afficher une boite de message et declencher un traitement en fonction
 * de la reponse (Listener)
 */
public class MessageBoxUtils
{
	public static final int BOUTON_OK = 1;
	public static final int BOUTON_CANCEL = 2;

	public interface Listener
	{
		void onOk();
		default void onCancel()
		{
		}
	}

	/***
	 * Affiche une message box a partir de chaines dans les ressources
	 * @param context
	 * @param titre
	 * @param texte
	 * @param boutons BOUTON_OK et/ou BOUTON_CANCEL
	 * @param listener
	 */
	public static void messageBox(@NonNull Context context, @StringRes int titre, @StringRes int texte, int boutons, final @Nullable Listener listener)
	{
		messageBox(context, context.getResources().getString(titre), context.getResources().getString(texte), boutons, listener);
	}

	/***
	 * Affiche une message box
	 * @param context
	 * @param titre
	 * @param texte
	 * @param boutons BOUTON_OK et/ou BOUTON_CANCEL
	 * @param listener
	 */
	public static void messageBox(@NonNull Context context, @NonNull String titre, @NonNull String texte, int boutons, final @Nullable Listener listener)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(titre);
		builder.setMessage(texte);

		if ((boutons & BOUTON_OK) != 0)
			builder.setPositiveButton(context.getString(android.R.string.ok), (dialog, id) ->
			{
				if (listener != null)
					listener.onOk();
				dialog.dismiss();
			});

		if ((boutons & BOUTON_CANCEL) != 0)
			builder.setNegativeButton(context.getString(android.R.string.cancel), (dialog, id) ->
			{
				if (listener != null)
					listener.onCancel();
				dialog.dismiss();
			});

		builder.create().show();
	}
}
