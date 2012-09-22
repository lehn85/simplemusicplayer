package com.npl.simplemusicplayer;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;

public class MusicCatalogLoader {
	private String TAG = "MusicCatalog";

	private Activity activity;
	private ContentResolver mCR;

	// cache
	private ArrayList<AlbumItem> list_albums = null;
	private ArrayList<ArtistItem> list_artists = null;
	private ArrayList<FolderItem> list_folders = null;
	private ArrayList<SongItem> list_songs = null;
	private ArrayList<PlaylistItem> list_playlists = null;

	private boolean bAlwaysGetNew = false;

	/**
	 * 
	 * @param a
	 *            Activity that call this
	 */
	public MusicCatalogLoader(Activity a) {
		activity = a;
		mCR = activity.getContentResolver();
	}

	public void setAlwaysGetNew(boolean b) {
		bAlwaysGetNew = b;
	}

	public void loadMusicCatalog() {
		list_albums = getAlbumsList();
		list_artists = getArtistsList();
		list_songs = getSongsList();
		list_folders = getFoldersList(null);
		list_playlists = getPlaylistsList();
	}

	public ArrayList<AlbumItem> ListAlbums() {
		return list_albums;
	}

	public ArrayList<ArtistItem> ListArtists() {
		return list_artists;
	}

	public ArrayList<SongItem> ListSongs() {
		return list_songs;
	}

	public ArrayList<FolderItem> ListFolders() {
		return list_folders;
	}

	public ArrayList<PlaylistItem> ListPlaylists() {
		return list_playlists;
	}

	// ///// Artist ///////////
	// Artist Item
	public class ArtistItem {
		public long id;
		public String artist;
		public int numberOfAlbums;
		public int numberOfSongs;

		public ArtistItem(long i, String a, int ac, int tc) {
			id = i;
			artist = a;
			numberOfAlbums = ac;
			numberOfSongs = tc;
		}
	}

	// Artist List Retriever
	public ArrayList<ArtistItem> getArtistsList() {
		if (list_artists != null && !bAlwaysGetNew)
			return list_artists;

		Uri uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;

		Cursor cur = mCR.query(uri, null, null, null, null);

		if (cur == null) {
			return null;
		}

		if (!cur.moveToFirst()) {
			return null;
		}

		ArrayList<ArtistItem> list = new ArrayList<ArtistItem>();

		int idColumn = cur.getColumnIndex(MediaStore.Audio.Artists._ID);
		int artistColumn = cur.getColumnIndex(MediaStore.Audio.Artists.ARTIST);
		int albumsCountColumn = cur.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS);
		int tracksCountColumn = cur.getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_TRACKS);

		do {
			list.add(new ArtistItem(cur.getLong(idColumn), cur.getString(artistColumn), cur.getInt(albumsCountColumn),
					cur.getInt(tracksCountColumn)));
		} while (cur.moveToNext());

		list_artists = list;
		return list;
	}

	// ///// Artist.Album ///////////
	// Artist Album Item
	public class ArtistAlbumItem {
		public long albumID;
		public String album;
		public int numberOfSongs;
		public int numberOfSongsByArtist;
		public ArtistItem artist;

		public ArtistAlbumItem(long id, String al, int tc, int tba, ArtistItem ai) {
			albumID = id;
			album = al;
			numberOfSongs = tc;
			numberOfSongsByArtist = tba;
			artist = ai;
		}
	}

	// Artist Album Item Retriever
	public ArrayList<ArtistAlbumItem> getArtistAlbumsList(ArtistItem ai) {
		Uri uri = MediaStore.Audio.Artists.Albums.getContentUri("external", ai.id);

		Cursor cur = mCR.query(uri, null, null, null, null);

		if (cur == null) {
			return null;
		}

		if (!cur.moveToFirst()) {
			return null;
		}

		ArrayList<ArtistAlbumItem> list = new ArrayList<ArtistAlbumItem>();

		int IDColumn = cur.getColumnIndex(MediaStore.Audio.Albums._ID);
		int albumColumn = cur.getColumnIndex(MediaStore.Audio.Albums.ALBUM);
		int tracksCountColumn = cur.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS);
		int tracksByArtistCount = cur.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS_FOR_ARTIST);

		do {
			list.add(new ArtistAlbumItem(cur.getLong(IDColumn), cur.getString(albumColumn), cur
					.getInt(tracksCountColumn), cur.getInt(tracksByArtistCount), ai));

		} while (cur.moveToNext());

		return list;
	}

	// //// Playlist ///////////
	// Playlist Item
	public class PlaylistItem {
		public long id;
		public String dataStream;
		public String name;

		public PlaylistItem(long _id, String d, String n) {
			id = _id;
			dataStream = d;
			name = n;
		}
	}

	// Playlist Item Retriever
	public ArrayList<PlaylistItem> getPlaylistsList() {
		if (list_playlists != null && !bAlwaysGetNew)
			return list_playlists;
		Uri uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;

		Cursor cur = mCR.query(uri, null, null, null, null);

		if (cur == null) {
			return null;
		}

		if (!cur.moveToFirst()) {
			return null;
		}

		ArrayList<PlaylistItem> list = new ArrayList<PlaylistItem>();

		int idColumn = cur.getColumnIndex(MediaStore.Audio.Playlists._ID);
		int dataColumn = cur.getColumnIndex(MediaStore.Audio.Playlists.DATA);
		int nameColumn = cur.getColumnIndex(MediaStore.Audio.Playlists.NAME);

		do {
			list.add(new PlaylistItem(cur.getLong(idColumn), cur.getString(dataColumn), cur.getString(nameColumn)));
		} while (cur.moveToNext());

		list_playlists = list;
		return list;
	}

	// ///// Album //////////////
	// Album Item
	public class AlbumItem {
		public long id;
		public String album;
		public String album_art;
		public int numberOfSongs;

		public AlbumItem(long _id, String a, String aa, int n) {
			id = _id;
			album = a;
			album_art = aa;
			numberOfSongs = n;
		}
	}

	// Albums List retriever
	public ArrayList<AlbumItem> getAlbumsList() {
		if (list_albums != null && !bAlwaysGetNew)
			return list_albums;
		Uri uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;

		Cursor cur = mCR.query(uri, null, null, null, null);

		if (cur == null) {
			return null;
		}

		if (!cur.moveToFirst()) {
			return null;
		}

		ArrayList<AlbumItem> list = new ArrayList<AlbumItem>();

		int idColumn = cur.getColumnIndex(MediaStore.Audio.Albums._ID);
		int albumColumn = cur.getColumnIndex(MediaStore.Audio.Albums.ALBUM);
		int albumArtColumn = cur.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);
		int numberOfSongsColumn = cur.getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS);

		do {
			list.add(new AlbumItem(cur.getLong(idColumn), cur.getString(albumColumn), cur.getString(albumArtColumn),
					cur.getInt(numberOfSongsColumn)));
		} while (cur.moveToNext());

		list_albums = list;
		return list;
	}

	// ///////// Folder Item//////////////////
	public static class FolderItem {
		public long id;
		public String name;
		public String fullPath;
		public int numberOfSongs;
		public ArrayList<FolderItem> childs = null;

		/**
		 * Construct from path
		 * 
		 * @param path
		 *            can point to a file or a folder. Whichever type can be
		 *            convert to folder path only.
		 */
		public FolderItem(String path) {
			File f = new File(path);

			// Try to make it a directory
			if (f.isFile()) {
				String fdPath = f.getParent();
				f = new File(fdPath);
				numberOfSongs = 1;
			} else
				numberOfSongs = 0;
			fullPath = f.getPath();
			name = f.getName();
			id = f.hashCode();
		}
	}

	// Retriever of list of FolderItems
	/**
	 * Because android doesn't have a provider for us to get folders, where
	 * music are stored. So that I need to write this function
	 * 
	 * @param listSI
	 *            list of songitems, from which we get the list of folders
	 * @return list of FolderItem
	 */
	public ArrayList<FolderItem> getFoldersList(ArrayList<SongItem> listSI) {
		if (list_folders != null && !bAlwaysGetNew)
			return list_folders;
		if (listSI == null)
			listSI = getSongsList();

		ArrayList<FolderItem> list = new ArrayList<MusicCatalogLoader.FolderItem>();

		int i, j;
		for (i = 0; i < listSI.size(); i++) {
			String data = listSI.get(i).dataStream;
			FolderItem fi = new FolderItem(data);
			if (list.size() == 0) {
				list.add(fi);
			} else {
				boolean found = false;
				for (j = 0; j < list.size(); j++) {
					if (list.get(j).id == fi.id) {
						found = true;
						list.get(j).numberOfSongs += 1;
						break;
					}
				}
				if (!found) {
					list.add(fi);
				}
			}
		}

		list_folders = list;
		return list;
	}

	// ///// SongItem /////////////
	// SongItem
	public static class SongItem {
		public long id = 0;
		public String title = "";
		public String artist = "";
		public long artistID = 0;
		public String album = "";
		public long albumID = 0;
		public long duration = 0;
		public String dataStream = "";

		public SongItem() {

		}

		public SongItem(long _id, String t, String a, String p) {
			id = _id;
			title = t;
			artist = a;
			dataStream = p;
		}

//		public int describeContents() {
//			return 0;
//		}
//
//		public void writeToParcel(Parcel dest, int flags) {
//			dest.writeLong(id);
//			dest.writeString(title);
//			dest.writeString(artist);
//			dest.writeLong(artistID);
//			dest.writeString(album);
//			dest.writeLong(albumID);
//			dest.writeLong(duration);
//			dest.writeString(dataStream);
//		}
//
//		// this is used to regenerate your object. All Parcelables must have a
//		// CREATOR that implements these two methods
//		public static final Parcelable.Creator<SongItem> CREATOR = new Parcelable.Creator<SongItem>() {
//			public SongItem createFromParcel(Parcel in) {
//				return new SongItem(in);
//			}
//
//			public SongItem[] newArray(int size) {
//				return new SongItem[size];
//			}
//		};
//
//		private SongItem(Parcel in) {
//			id = in.readLong();
//			title = in.readString();
//			artist = in.readString();
//			artistID = in.readLong();
//			album = in.readString();
//			albumID = in.readLong();
//			duration = in.readLong();
//			dataStream = in.readString();
//		}
	}

	// Query for Songs
	private ArrayList<SongItem> queryForSongs(Uri uri, String[] projection, String selection, String[] args,
			String sortOrder) {
		Cursor cur = mCR.query(uri, projection, selection, args, sortOrder);

		if (cur == null) {
			return null;
		}

		if (!cur.moveToFirst()) {
			return null;
		}

		ArrayList<SongItem> list = new ArrayList<SongItem>();

		int idColumn = cur.getColumnIndex(MediaStore.Audio.Media._ID);
		int titleColumn = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
		int artistColumn = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
		int artistIDColumn = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID);
		int albumColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
		int albumIDColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
		int durationColumn = cur.getColumnIndex(MediaStore.Audio.Media.DURATION);
		int dataColumn = cur.getColumnIndex(MediaStore.Audio.Media.DATA);

		do {
			SongItem si = new SongItem();
			si.id = cur.getLong(idColumn);
			si.title = cur.getString(titleColumn);
			si.artist = cur.getString(artistColumn);
			si.artistID = cur.getLong(artistIDColumn);
			si.album = cur.getString(albumColumn);
			si.albumID = cur.getLong(albumIDColumn);
			si.duration = cur.getLong(durationColumn);
			si.dataStream = cur.getString(dataColumn);
			list.add(si);
		} while (cur.moveToNext());

		return list;
	}

	// Playlist member retriever
	public ArrayList<SongItem> getSongsInPlaylist(PlaylistItem pl) {
		Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", pl.id);

		return queryForSongs(uri, null, MediaStore.Audio.Media.IS_MUSIC + "=1", null, null);
	}

	// Album member retriever
	public ArrayList<SongItem> getSongsInAlbum(AlbumItem ai) {
		Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

		// WHERE statement
		String selection = MediaStore.Audio.Media.IS_MUSIC + "=1";
		selection = selection + " AND " + MediaStore.Audio.Media.ALBUM_ID + "=?";

		// argument for the WHERE statement at the place "=?"
		String[] selectionArgs = new String[] { Long.toString(ai.id) };

		return queryForSongs(uri, null, selection, selectionArgs, null);
	}

	// Get Songs list in a folder
	public ArrayList<SongItem> getSongsInFolder(FolderItem fi) {
		Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

		// WHERE statement
		String selection = MediaStore.Audio.Media.IS_MUSIC + "=1";
		selection = selection + " AND " + MediaStore.Audio.Media.DATA + " LIKE ?";

		// argument for the WHERE statement at the place "=?"
		String[] selectionArgs = new String[] { "%" + fi.fullPath + "%" };

		return queryForSongs(uri, null, selection, selectionArgs, null);
	}

	// Get all the songs
	public ArrayList<SongItem> getSongsList() {
		if (list_songs != null && !bAlwaysGetNew)
			return list_songs;

		Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

		// WHERE statement
		String selection = MediaStore.Audio.Media.IS_MUSIC + "=1";

		list_songs = queryForSongs(uri, null, selection, null, null);
		return list_songs;
	}

}
