package com.lpi.vitesse.dialogues;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lpi.vitesse.FontManager;
import com.lpi.vitesse.Preferences;
import com.lpi.vitesse.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FontPickerDialog
{
	public interface Listener
	{
		void onOK(@NonNull final String fontPath);

		void onCancel();
	}


	public static void start(@NonNull final Activity activity, @Nullable final Listener listener)
	{
		final AlertDialog dialogBuilder = new AlertDialog.Builder(activity).create();
		LayoutInflater inflater = activity.getLayoutInflater();
		View dialogView = inflater.inflate(R.layout.font_picker, null);

		// Liste des fontes
		ListView lv = dialogView.findViewById(R.id.lvFontes);
		// Get the fonts on the device
		HashMap<String, String> fonts = FontManager.enumerateFonts();
		ArrayList<String> fontPaths = new ArrayList<String>();
		ArrayList<String> fontNames = new ArrayList<String>();

		// Get the current value to find the checked item
		Preferences prefs = Preferences.getInstance(activity);
		String selectedFontPath = prefs.getString(Preferences.FONTE, "");

		int idx = 0;
		int selectedItem = 0;

		if (fonts != null)
			for (String path : fonts.keySet())
			{
				if (path.equals(selectedFontPath))
					selectedItem = idx;

				fontPaths.add(path);
				fontNames.add(fonts.get(path));
				idx++;
			}

		FontAdapter adapter = new FontAdapter(activity, fontNames, fontPaths);
		lv.setAdapter(adapter);
		lv.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
			{
				if (listener != null)
					listener.onOK(fontPaths.get(i));
				dialogBuilder.dismiss();
			}
		});
		lv.setSelection(selectedItem);
		lv.setItemChecked(selectedItem, true);

		dialogBuilder.setView(dialogView);
		dialogBuilder.show();
	}

	// Font adaptor responsible for redrawing the item TextView with the appropriate font.
	// We use BaseAdapter since we need both arrays, and the effort is quite small.
	public static class FontAdapter extends BaseAdapter
	{
		private final Context _context;
		private final List<String> m_fontPaths;
		private final List<String> m_fontNames;

		public FontAdapter(@NonNull final Context context, @NonNull final ArrayList fontNames, @NonNull final ArrayList fontPathes)
		{
			_context = context;
			m_fontNames = fontNames;
			m_fontPaths = fontPathes;
		}

		@Override
		public int getCount()
		{
			return m_fontNames.size();
		}

		@Override
		public Object getItem(int position)
		{
			return m_fontNames.get(position);
		}

		@Override
		public long getItemId(int position)
		{
			// We use the position as ID
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View view = convertView;

			// This function may be called in two cases: a new view needs to be created,
			// or an existing view needs to be reused
			if (view == null)
			{
				// Since we're using the system list for the layout, use the system inflater
				final LayoutInflater inflater = (LayoutInflater) _context.getSystemService(
						Context.LAYOUT_INFLATER_SERVICE);

				// And inflate the view android.R.layout.select_dialog_singlechoice
				// Why? See com.android.internal.app.AlertController method createListView()
				view = inflater.inflate(android.R.layout.select_dialog_item, parent, false);
			}

			if (view != null)
			{
				// Find the text view from our interface
				TextView tv = view.findViewById(android.R.id.text1);
				Typeface tface = Typeface.createFromFile(m_fontPaths.get(position));
				if (tface != null)
					tv.setTypeface(tface);
				tv.setText(m_fontNames.get(position));
			}

			return view;
		}
	}
}
