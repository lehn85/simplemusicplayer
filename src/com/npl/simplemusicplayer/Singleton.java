package com.npl.simplemusicplayer;

import java.util.ArrayList;

import com.npl.simplemusicplayer.MusicCatalogLoader.SongItem;


public class Singleton {
	private static Singleton instance;

	private ArrayList<SongItem> nowPlayingList = null;
	private SongItem currentSong = null;
	private PlaylistManager mPlaylistMgr = null;
	
	private int index;

	private Singleton() {
	}

	public static Singleton getInstance() {
		if (instance == null)
			instance = new Singleton();
		return instance;
	}

	// Playlist transfer
	public void setNowPlayingList(ArrayList<SongItem> pl) {
		nowPlayingList = pl;
	}

	public ArrayList<SongItem> getNowPlayingList() {
		return nowPlayingList;
	}

	// Songitem transfer
	public void setCurrentSongItem(SongItem si) {
		currentSong = si;
	}

	public SongItem getCurrentSongItem() {
		return currentSong;
	}

	// playlistmanager
	public void setPlaylistManager(PlaylistManager pm) {
		mPlaylistMgr = pm;
	}

	public PlaylistManager getPlaylistManager() {
		return mPlaylistMgr;
	}

	public void setCurrentIndex(int i) {
		index = i;
	}

	public int getCurrentIndex() {
		return index;
	}
}
