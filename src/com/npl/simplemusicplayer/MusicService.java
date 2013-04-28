package com.npl.simplemusicplayer;

import java.util.ArrayList;

import android.support.v4.app.NotificationCompat;
import com.npl.simplemusicplayer.MusicCatalogLoader.SongItem;
import com.npl.simplemusicplayer.PlaylistManager.RepeatMode;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

public class MusicService extends Service implements OnPreparedListener, OnCompletionListener, OnErrorListener {

	private static final String TAG = "npl.com.simplemusicplayer_MusicService";

	// Intent action
	public static final String ACTION_TOGGLE_PLAYBACK = "com.npl.simplemusicplayer.TOGGLE_PLAYBACK";
	public static final String ACTION_PLAY = "com.npl.simplemusicplayer.PLAY";
	public static final String ACTION_PREVIOUS = "com.npl.simplemusicplayer.PREVIOUS";
	public static final String ACTION_NEXT = "com.npl.simplemusicplayer.NEXT";
	public static final String ACTION_PAUSE = "com.npl.simplemusicplayer.PAUSE";
	public static final String ACTION_SEEK = "com.npl.simplemusicplayer.SEEK";
	public static final String SUPPLY_PLAYLIST = "com.npl.simplemusicplayer.SUPPLY_PLAYLIST";
	public static final String ACTION_PLAY_SPECIFIC_SONG = "com.npl.simplemusicplayer.PLAY_SPECIFIC_SONG";
	public static final String ACTION_TOGGLE_REPEATMODE = "com.npl.simplemusicplayer.TOGGLE_REPEATMODE";
	public static final String ACTION_TOGGLE_SHUFFLE = "com.npl.simplemusicplayer.TOGGLE_SHUFFLE";
	public static final String REQUEST_STATUS = "com.npl.simplemusicplayer.REQUEST_STATUS";
	public static final String ACTION_REORDER_PLAYLIST = "com.npl.simplemusicplayer.REORDER_PLAYLIST";
	public static final String ACTION_SETUP_AS_FOREGROUND = "com.npl.simplemusicplayer.SETUP_AS_FOREGROUND";

	// Bundle key string
	public static final String BKEY_PLAYLIST = "PLAYLIST";
	public static final String BKEY_PLAYSONG = "PLAYSONG";
	public static final String BKEY_STATE = "MEDIAPLAYER_STATE";
	public static final String BKEY_CURSONG_DURATION = "SONG_DURATION";
	public static final String BKEY_CURSONG_POSITION = "SONG_POSITION";
	public static final String BKEY_PERCENTAGE = "PERCENTAGE";
	public static final String BKEY_REPEATMODE = "REPEAT_MODE";
	public static final String BKEY_SHUFFLE = "SHUFFLE";
	public static final String BKEY_REORDER_FROM = "REORDER_FROM";
	public static final String BKEY_REORDER_TO = "REORDER_TO";

	// Broadcast status action
	public static final String STATUS_BC_NOW_PLAYING = "com.npl.simplemusicplayer.NOW_PLAYING";
	public static final String STATUS_BC_PLAYTIME = "com.npl.simplemusicplayer.PLAYTIME";
	public static final String STATUS_BC_ALL = "com.npl.simplemusicplayer.ALLSTATUS";
	public static final String STATUS_BC_PLAYMODE = "com.npl.simplemusicplayer.PLAYMODE";
	public static final String STATUS_BC_NOWPLAYING_PLAYLIST = "com.npl.simplemusicplayer.NOWPLAYING_PLAYLIST";

	// Media player
	private MediaPlayer mMediaPlayer = null;

	// Media state
	// Enum trạng thái hoạt động của service
	public enum MediaPlayerState {
		Stopped, Preparing, Playing, Paused
	}

	private static MediaPlayerState mState = MediaPlayerState.Stopped;

	// Playlist playing for this Music service
	private static PlaylistManager mPlaylistMgr = null;

	// SongItem current playing
	private SongItem playItem = null;

	// Handler xu ly cap nhat timer
	private Handler mHandler;
	private int mUpdateInterval = 500; // 1000ms update interval
//	private int mPausedTime = 0;
//	private int mPausedTimeThreshold = (int) ((1000.0 / ((double) mUpdateInterval)) * 60);

	// Audio focus helper
	private AudioFocusHelper mAudioFocusHelper;
	private float mDuckVolume = 0.5f;

	// Notification
	private NotificationHelper mNotificationHelper;

	// Error continue
	private int mAttempCount = 0;

	@Override
	public void onCreate() {
		mState = MediaPlayerState.Stopped;
		mHandler = new Handler();

		mAudioFocusHelper = new AudioFocusHelper(this);
		mAudioFocusHelper.requestFocus();

		mNotificationHelper = new NotificationHelper(this);

	}

	public void onDestroy() {
		Log.i(TAG, "onDestroy");
		// Service is being killed, so make sure we release our resources
		mState = MediaPlayerState.Stopped;
		relaxResources(true);
		mAudioFocusHelper.abandonFocus();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public static PlaylistManager getPlaylistManager() {
		return mPlaylistMgr;
	}

	public static MediaPlayerState getState() {
		return mState;
	}

	public int onStartCommand(Intent i, int flags, int startID) {
		String action = i.getAction();
		Log.i(TAG, "intent:" + action);

		if (action.equals(SUPPLY_PLAYLIST)) {
			retrievePlaylist();
			return START_NOT_STICKY;
		}

		if (action.equals(REQUEST_STATUS)) {
			requestStatus();
		} else {
			// check if playlist is null?
			if (mPlaylistMgr == null)
				return START_NOT_STICKY;

			if (action.equals(ACTION_PLAY))
				processPlayRequest();
			else if (action.equals(ACTION_NEXT))
				processNextRequest();
			else if (action.equals(ACTION_PREVIOUS))
				processPreviousRequest();
			else if (action.equals(ACTION_TOGGLE_PLAYBACK))
				processTogglePlaybackRequest();
			else if (action.equals(ACTION_PLAY_SPECIFIC_SONG))
				processPlaySpecificSong();
			else if (action.equals(ACTION_PAUSE))
				processPauseRequest();
			else if (action.equals(ACTION_SEEK))
				processSeek(i);
			// else if (action.equals(ACTION_TOGGLE_SHUFFLE) ||
			// action.equals(ACTION_TOGGLE_REPEATMODE))
			// processTogglePlaymode();
			else if (action.equals(ACTION_REORDER_PLAYLIST))
				processReorderPlaylist();
			else if (action.equals(ACTION_SETUP_AS_FOREGROUND))
				processSetupAsForeground();
		}

		return START_NOT_STICKY;

	}

	private void processSetupAsForeground() {
		mNotificationHelper.bringServiceToForeground();
	}

	private void processReorderPlaylist() {
		// try to refresh current song playing index
		mPlaylistMgr.setCurrentSong(playItem);
	}

	// private void processTogglePlaymode() {
	//
	// }

	private void requestStatus() {
		// stop foreground of service if application came into foreground
		sendStatus(STATUS_BC_ALL);
	}

	private void retrievePlaylist() {
		Singleton s = Singleton.getInstance();
		mPlaylistMgr = s.getPlaylistManager();
	}

	private void processPlaySpecificSong() {
		if (mState == MediaPlayerState.Preparing)
			return;
		Singleton singleton = Singleton.getInstance();
		SongItem si = singleton.getCurrentSongItem();

		if (mPlaylistMgr.setCurrentSong(si))
			playSong(si);
	}

	private void processTogglePlaybackRequest() {
		if (mState == MediaPlayerState.Paused || mState == MediaPlayerState.Stopped)
			processPlayRequest();
		else
			processPauseRequest();
	}

	private void processPlayRequest() {
		if (mState == MediaPlayerState.Stopped) {
			playSong(mPlaylistMgr.gotoNextSong());
		} else if (mState == MediaPlayerState.Paused) {
			mState = MediaPlayerState.Playing;
			mNotificationHelper.bringServiceToForeground();
			configAndStartMediaPlayer();
		}
	}

	private void processPauseRequest() {
		if (mState == MediaPlayerState.Playing) {
			mState = MediaPlayerState.Paused;
			mMediaPlayer.pause();

			sendStatus(STATUS_BC_NOW_PLAYING);
			relaxResources(false);
		}
	}

	private void processPreviousRequest() {
		if (mState == MediaPlayerState.Playing || mState == MediaPlayerState.Paused) {
			playSong(mPlaylistMgr.gotoPreviousSong());
		}
	}

	private void processNextRequest() {
		if (mState == MediaPlayerState.Playing || mState == MediaPlayerState.Paused) {
			playSong(mPlaylistMgr.gotoNextSong());
		}
	}

	private void processSeek(Intent i) {
		if ((mState != MediaPlayerState.Playing) && (mState != MediaPlayerState.Paused))
			return;
		Bundle b = i.getExtras();
		int pos = Util.percentageToPosition(b.getInt(BKEY_PERCENTAGE), mMediaPlayer.getDuration());
		mMediaPlayer.seekTo(pos);
	}

	private void processStopRequest(boolean force) {
		if (mState == MediaPlayerState.Playing || mState == MediaPlayerState.Paused || force) {
			mState = MediaPlayerState.Stopped;

			// let go of all resources...
			relaxResources(true);
			mAudioFocusHelper.abandonFocus();

			// service is no longer necessary. Will be started again if needed.
			stopSelf();
		}

	}

	public void onPrepared(MediaPlayer mp) {
		Log.i(TAG, "onPrepared");
		// set state to playing, even if lost audio focus
		mState = MediaPlayerState.Playing;
		// if lost audio focus, then not playing, but pending by setting the
		// state to playing
		// when regain focus, it will be played
		if (mAudioFocusHelper.getLastFocusChange() != AudioManager.AUDIOFOCUS_GAIN
				&& mAudioFocusHelper.getLastFocusChange() != AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
			return;
		if (mAudioFocusHelper.getLastFocusChange() != AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
			mMediaPlayer.setVolume(mDuckVolume, mDuckVolume);
		configAndStartMediaPlayer();
	}

	public boolean onError(MediaPlayer mp, int what, int extra) {
		Log.e(TAG, "Error: what=" + String.valueOf(what) + ", extra=" + String.valueOf(extra));

		mState = MediaPlayerState.Stopped;
		relaxResources(true);

		if (mAttempCount < 2) {
			mAttempCount += 1;
			Toast.makeText(getApplicationContext(), "Error playing! Trying next song. ", Toast.LENGTH_SHORT).show();
			processPlayRequest(); // attempt to play next song
		} else { // reset and do nothing
			Toast.makeText(getApplicationContext(), "Error! Stop. ", Toast.LENGTH_SHORT).show();
			mAttempCount = 0;
		}

		return true;
	}

	public void onCompletion(MediaPlayer mp) {
		playSong(mPlaylistMgr.gotoNextSong());
	}

	private void createMediaPlayerIfNeeded() {
		Log.i(TAG, "Create mediaplayer now is:" + String.valueOf(mMediaPlayer));
		if (mMediaPlayer == null) {
			mMediaPlayer = new MediaPlayer();

			// make mediaplayer wakelock
			mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

			mMediaPlayer.setOnPreparedListener(this);
			mMediaPlayer.setOnCompletionListener(this);
			mMediaPlayer.setOnErrorListener(this);
		} else
			mMediaPlayer.reset();
	}

	/**
	 * Releases resources used by the service for playback. This includes the
	 * "foreground service" status and notification, the wake locks and possibly
	 * the MediaPlayer.
	 * 
	 * @param releaseMediaPlayer
	 *            Indicates whether the Media Player should also be released or
	 *            not
	 */
	void relaxResources(boolean releaseMediaPlayer) {

		// stop being a foreground service
		stopForeground(true);

		// stop update playing time to activity
		stopUpdatePlaytime();

		// stop and release the Media Player, if it's available
		if (releaseMediaPlayer && mMediaPlayer != null) {
			mMediaPlayer.reset();
			mMediaPlayer.release();
			mMediaPlayer = null;
		}

	}

	private void playSong(SongItem si) {
		mState = MediaPlayerState.Stopped;
		relaxResources(false);

		mAudioFocusHelper.requestFocus();

		try {
			playItem = si;
			if (playItem == null) {
				Toast.makeText(
						this,
						"No available music to play. Place some music on your external storage "
								+ "device (e.g. your SD card) and try again.", Toast.LENGTH_LONG).show();
				processStopRequest(true); // stop everything!
				return;
			}

			createMediaPlayerIfNeeded();
			mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mMediaPlayer.setDataSource(getApplicationContext(), Uri.parse(playItem.dataStream));

			mState = MediaPlayerState.Preparing;
			mMediaPlayer.prepareAsync();
		} catch (Exception ex) {
			Log.e("MusicService", "IOException playing next song: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	private void configAndStartMediaPlayer() {
		if (mMediaPlayer.isPlaying() == false) {
			mState = MediaPlayerState.Playing;
			mMediaPlayer.start();
			mNotificationHelper.bringServiceToForeground();
			startUpdatePlaytime();
			sendStatus(STATUS_BC_NOW_PLAYING);
		}
	}

	/**
	 * Send broadcast media playing status Any activity can receive this
	 * information and display
	 * 
	 * @param action
	 *            start with STATUS_
	 */
	private void sendStatus(String action) {
		Intent i = new Intent();
		i.setAction(action);

		Bundle b = new Bundle();

		Singleton singleton = Singleton.getInstance();

		// data send tuy theo action
		if (action.equals(STATUS_BC_NOW_PLAYING) || action.equals(STATUS_BC_ALL)) {
			// b.putParcelable(BKEY_PLAYSONG, playItem);
			singleton.setCurrentSongItem(playItem);
		}

		if (action.equals(STATUS_BC_PLAYTIME) || action.equals(STATUS_BC_ALL)) {
			int pos = 0, dura = 0;
			// Check mediaplayer and state before try to call any method
			if (mMediaPlayer != null && mState != MediaPlayerState.Preparing) {
				pos = mMediaPlayer.getCurrentPosition();
				dura = mMediaPlayer.getDuration();
			}
			b.putInt(BKEY_CURSONG_POSITION, pos);
			b.putInt(BKEY_CURSONG_DURATION, dura);
		}

		if (action.equals(STATUS_BC_PLAYMODE) || action.equals(STATUS_BC_ALL)) {
			if (mPlaylistMgr != null) {
				b.putSerializable(BKEY_REPEATMODE, mPlaylistMgr.getRepeatMode());
				b.putBoolean(BKEY_SHUFFLE, mPlaylistMgr.isShuffle());
			}
		}

		if (action.equals(STATUS_BC_NOWPLAYING_PLAYLIST) || action.equals(STATUS_BC_ALL)) {
			singleton.setPlaylistManager(mPlaylistMgr);
		}

		// state
		b.putSerializable(BKEY_STATE, mState);

		// Put into intent and send broadcast
		i.putExtras(b);
		sendBroadcast(i);
	}

	private void startUpdatePlaytime() {
		mHandler.postDelayed(mUpdateTimeTask, mUpdateInterval);
	}

	private void stopUpdatePlaytime() {
		mHandler.removeCallbacks(mUpdateTimeTask);
	}

	private Runnable mUpdateTimeTask = new Runnable() {

		public void run() {
			sendStatus(STATUS_BC_PLAYTIME);
			mHandler.postDelayed(mUpdateTimeTask, mUpdateInterval);
		}
	};

	/**
	 * Class AudioFocusHelper to help capsulating OnAudioFocusChange
	 * 
	 * @author Phuongle
	 * 
	 */
	private class AudioFocusHelper implements OnAudioFocusChangeListener {
		// context
		Context mContext;

		// Audio focus management
		AudioManager mAudioManager;

		//
		int lastFocusChange = AudioManager.AUDIOFOCUS_GAIN;

		public AudioFocusHelper(Context context) {
			mContext = context;
			// Audio manager
			mAudioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
		}

		public boolean requestFocus() {
			return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager.requestAudioFocus(this,
					AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		}

		public boolean abandonFocus() {
			return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager.abandonAudioFocus(this);
		}

		public int getLastFocusChange() {
			return lastFocusChange;
		}

		public void onAudioFocusChange(int focusChange) {
			// save this
			lastFocusChange = focusChange;
			// Nothing to do if mediaplayer is nothing
			if (mMediaPlayer == null)
				return;
			// If preparing then return;
			if (mState == MediaPlayerState.Preparing)
				return;

			switch (focusChange) {
				case AudioManager.AUDIOFOCUS_GAIN:
					// resume playback
					Log.i(TAG, "Audio focus gain");
					if (mState == MediaPlayerState.Playing)
						configAndStartMediaPlayer();
					mMediaPlayer.setVolume(1.0f, 1.0f);
					break;

				case AudioManager.AUDIOFOCUS_LOSS:
					Log.i(TAG, "Audio focus loss");
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
					// Lost focus for a short time, but we have to stop
					// playback. We don't release the media player because
					// playback
					// is likely to resume
					// mState will be keep, if we gain focus again, it will be
					// run again
					Log.i(TAG, "Audio focus loss transient");
					if (mMediaPlayer.isPlaying()) {
						processPauseRequest();
						// maintain state playing
						mState = MediaPlayerState.Playing;
					}
					break;

				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
					// Lost focus for a short time, but it's ok to keep playing
					// at an attenuated level
					Log.i(TAG, "Audio focus loss transient can duck");
					if (mMediaPlayer.isPlaying())
						mMediaPlayer.setVolume(mDuckVolume, mDuckVolume);
					break;
			}

		}
	}

	/**
	 * This class to help capsulating the notification
	 * 
	 * @author Phuongle
	 * 
	 */
	private class NotificationHelper {
		private Context mContext;
		private NotificationManager mManager;
		private final int NOTIFICATION_ID = 1;
		Notification mNotification = null;

		public NotificationHelper(Context c) {
			mContext = c;
			mManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		}

		/** Updates the notification. */
		@SuppressWarnings("unused")
		private void updateNotification(String text) {
			buildNotification(text);
			mManager.notify(NOTIFICATION_ID, mNotification);
		}

		// setup as foreground
		private void setUpAsForeground(String text) {
			buildNotification(text);
			mNotification.flags |= Notification.FLAG_ONGOING_EVENT;

			startForeground(NOTIFICATION_ID, mNotification);
		}

		private void buildNotification(String text) {
			NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);

			builder.setContentTitle("LeMusicPlayer");
			builder.setContentText(text);
			builder.setTicker(text);
			builder.setSmallIcon(R.drawable.ic_launcher_2);

			// Set intent and pendingintent - the one is fired when notification
			// is clicked
			Intent intent = new Intent(mContext, MainPlayer.class);

			PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			builder.setContentIntent(pendingIntent);

			mNotification = builder.build();
		}

		/**
		 * Bring service to foreground with notification If application is still
		 * in foreground then do nothing If application isn't in foreground, and
		 * music stop, then kill service Else: application isn't in foreground,
		 * music playing, then bring service to foreground
		 */
		public void bringServiceToForeground() {
			// if no playlist, no playing song
			if (mPlaylistMgr == null)
				return;
			SongItem si = mPlaylistMgr.getCurrentSong();
			if (si != null)
				setUpAsForeground(si.title + " (Playing)");
		}
	}
}
