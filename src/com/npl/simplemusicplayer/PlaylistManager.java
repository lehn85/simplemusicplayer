package com.npl.simplemusicplayer;

import java.util.ArrayList;
import java.util.Random;

import com.npl.simplemusicplayer.MusicCatalogLoader.SongItem;


public class PlaylistManager {

	public enum RepeatMode {
		NoRepeat, RepeatAll, RepeatOne
	}

	private RepeatMode mRepeatMode = RepeatMode.NoRepeat;
	private boolean mShuffle = false;

	private ArrayList<SongItem> mItems = null;
	private SongItem currentSong = null;
	private int currentIndex = -1;

	public PlaylistManager() {
		mItems = new ArrayList<SongItem>();
	}

	public PlaylistManager(ArrayList<SongItem> list) {
		mItems = list;
	}

	public void setArrayList(ArrayList<SongItem> listSongs) {
		mItems = listSongs;
	}

	public ArrayList<SongItem> getArrayList() {
		return mItems;
	}

	public void add(SongItem si) {
		mItems.add(si);
	}

	public SongItem gotoNextSong() {
		if (mItems.size() == 0) {
			return null;
		}

		if (mRepeatMode == RepeatMode.RepeatOne && currentSong != null)
			return currentSong;

		if (mShuffle) {
			Random rd = new Random();
			currentIndex = rd.nextInt(mItems.size());
		} else {
			if (currentIndex == -1) {
				currentIndex = 0;
			} else {
				currentIndex++;
				if (currentIndex == mItems.size() && mRepeatMode == RepeatMode.RepeatAll) {
					currentIndex = 0;
				}
			}
		}

		if (currentIndex >= 0 && currentIndex < mItems.size())
			currentSong = mItems.get(currentIndex);
		else {
			currentIndex = -1;
			currentSong = null;
		}

		return currentSong;
	}

	public SongItem gotoPreviousSong() {
		if (mItems.size() == 0) {
			return null;
		}
		if (mRepeatMode == RepeatMode.RepeatOne && currentSong != null)
			return currentSong;

		if (mShuffle) {
			Random rd = new Random();
			currentIndex = rd.nextInt(mItems.size());
		} else {
			if (currentIndex == -1) {
				currentIndex = 0;
			} else {
				currentIndex--;
				if (currentIndex == -1 && mRepeatMode == RepeatMode.RepeatAll)
					currentIndex = mItems.size() - 1;
			}
		}

		if (currentIndex >= 0 && currentIndex < mItems.size())
			currentSong = mItems.get(currentIndex);
		else {
			currentIndex = -1;
			currentSong = null;
		}

		return currentSong;
	}

	public boolean setCurrentSong(SongItem si) {
		if (si == null) { // means stop
			currentIndex = -1;
			currentSong = null;
			return true;
		}

		int i;
		int index = -1;
		for (i = 0; i < mItems.size(); i++) {
			if (si.id == mItems.get(i).id) {
				index = i;
				break;
			}
		}
		if (index >= 0) {
			currentIndex = index;
			currentSong = mItems.get(currentIndex);
		}

		return (index >= 0);
	}

	public boolean setCurrentSongIndex(int index) {
		if (index < 0 || index >= mItems.size())
			return false;
		currentIndex = index;
		currentSong = mItems.get(currentIndex);
		return true;
	}

	public SongItem getCurrentSong() {
		return currentSong;
	}

	public int getCurrentSongIndex() {
		return currentIndex;
	}

	public void setRepeatMode(RepeatMode rp) {
		mRepeatMode = rp;
	}

	public RepeatMode getRepeatMode() {
		return mRepeatMode;
	}

	public void setShuffle(boolean s) {
		mShuffle = s;
	}

	public boolean isShuffle() {
		return mShuffle;
	}
}