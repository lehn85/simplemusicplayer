package com.npl.simplemusicplayer;

import java.util.ArrayList;

import com.npl.simplemusicplayer.R;
import com.npl.simplemusicplayer.MusicCatalogLoader.SongItem;
import com.npl.simplemusicplayer.R.id;
import com.npl.simplemusicplayer.R.layout;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class NowPlayingListAdapter extends BaseAdapter {

	private Activity myActivity;
	private ArrayList<SongItem> data;
	private LayoutInflater inflater = null;
	private SongItem selectedSongItem = null;

	public NowPlayingListAdapter(Activity a, ArrayList<SongItem> d) {
		myActivity = a;
		data = d;
		inflater = (LayoutInflater) myActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public int getCount() {
		return (data == null) ? 0 : data.size();
	}

	public Object getItem(int pos) {
		return (data == null) ? null : data.get(pos);
	}

	public long getItemId(int position) {
		return position;
	}

	public void remove(SongItem si) {
		if (data != null)
			data.remove(si);
	}

	public void insert(SongItem si, int to) {
		if (data != null)
			data.add(to, si);
	}

	public void setSelectedSongItem(SongItem si) {
		selectedSongItem = si;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View vi = convertView;
		if (vi == null) {
			vi = inflater.inflate(R.layout.playlist_row, null);
		}
		
		ImageView playing_indicator = (ImageView) vi.findViewById(R.id.playing_indicator);

		if (selectedSongItem != null) {
			if (selectedSongItem.id == data.get(position).id) {
				playing_indicator.setVisibility(View.VISIBLE);

			} else {
				playing_indicator.setVisibility(View.INVISIBLE);
			}
		} else
			playing_indicator.setVisibility(View.INVISIBLE);

		TextView title = (TextView) vi.findViewById(R.id.Title);
		TextView artist = (TextView) vi.findViewById(R.id.Artist);

		title.setText(data.get(position).title);
		artist.setText(data.get(position).artist);

		return vi;
	}

}
