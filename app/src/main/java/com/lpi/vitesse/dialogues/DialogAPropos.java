package com.lpi.vitesse.dialogues;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.lpi.vitesse.BuildConfig;
import com.lpi.vitesse.R;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Calendar;

public class DialogAPropos
{
	public static void start(@NonNull final Activity activity)
	{
		final AlertDialog dialogBuilder = new AlertDialog.Builder(activity).create();
		LayoutInflater inflater = activity.getLayoutInflater();
		View dialogView = inflater.inflate(R.layout.activity_apropos, null);
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(BuildConfig.BUILD_TIME.getTime());
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
		LocalDate buildDate = LocalDate.ofYearDay(c.get(Calendar.YEAR), c.get(Calendar.DAY_OF_YEAR));

		String message = "Application Id:" + BuildConfig.APPLICATION_ID
				+ "\nBuild type:" + BuildConfig.BUILD_TYPE
				+ "\nDate: " + buildDate.format(dateFormatter)
				+ "\nVersion name:" + BuildConfig.VERSION_NAME
				+ "\nVersion code:" + BuildConfig.VERSION_CODE;

		TextView tv = dialogView.findViewById(R.id.textViewDescription);
		tv.setText(message);
		dialogBuilder.setView(dialogView);
		dialogBuilder.show();
	}
}
