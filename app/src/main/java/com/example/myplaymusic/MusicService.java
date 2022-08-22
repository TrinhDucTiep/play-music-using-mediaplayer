package com.example.myplaymusic;

import static android.content.ContentValues.TAG;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.MediaController;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.ArrayList;
import java.util.Random;

public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private static final int NOTIFY_ID = 1;
    private String songTitle = "";

    public void setMediaPlayerControl(MediaController.MediaPlayerControl mediaPlayerControl) {
        this.mediaPlayerControl = mediaPlayerControl;
    }

    private  MediaController.MediaPlayerControl mediaPlayerControl;
    private boolean loadPrepared;

    private MediaPlayer mediaPlayer;
    private ArrayList<Song> songs;
    private int songPosn;

    private boolean shuffle = false;
    private Random rand;

    private final IBinder musicBind = new MusicBinder();


    @Override
    public void onCreate(){
        super.onCreate();
        songPosn = 0;
        mediaPlayer = new MediaPlayer();

        rand = new Random();

        initMusicPlayer();
    }

    public void initMusicPlayer(){
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
    }

    public void setList(ArrayList<Song> theSongs){
        songs = theSongs;
    }

    public void playSong(){
        mediaPlayer.reset();
        loadPrepared = false;
        Song playSong = songs.get(songPosn);
        songTitle = playSong.getTitle();
        long currentSong = playSong.getId();
        Uri trackUri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currentSong
        );

        try{
            mediaPlayer.setDataSource(getApplicationContext(), trackUri);
        }catch (Exception e){
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }
        mediaPlayer.prepareAsync();

    }

    public class MusicBinder extends Binder{
        MusicService getService(){
            return MusicService.this;
        }
    }


    public void setSongs(int songIndex){
        songPosn = songIndex;
    }

    public void setShuffle(){
        if(shuffle) shuffle=false;
        else shuffle=true;
    }
    public boolean getShuffle(){
        return shuffle;
    }

    //methods for controlling MediaPlayerControl
    public MediaPlayer getMediaPlayer(){
        return mediaPlayer;
    }
    public int getSongPosn(){
        return mediaPlayer.getCurrentPosition();
    }
    public int getDur(){
        return mediaPlayer.getDuration();
    }
    public boolean isPng(){
        return mediaPlayer.isPlaying();
    }
    public void pausePlayer(){
        mediaPlayer.pause();
    }
    public void seek(int position){
        mediaPlayer.seekTo(position);
    }

    public void playPrev(){
        songPosn--;
        if(songPosn < 0) songPosn = songs.size()-1;
            playSong();
    }
    public void playNext(){
        if(shuffle){
            int newSong = songPosn;
            while(newSong==songPosn){
                newSong = rand.nextInt(songs.size());
            }
            songPosn = newSong;
        } else {
            songPosn++;
            if(songPosn >= songs.size()) songPosn=0;
        }
        playSong();
    }


    public void resume(){
        mediaPlayer.start();
    }

    public void clearMedia(){
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = null;
    }

    public void setLoadPrepared(boolean state){
        this.loadPrepared = state;
    }
    public boolean getLoadPrepared(){
        return loadPrepared;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent){
        mediaPlayer.stop();
        mediaPlayer.release();
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if(mediaPlayer.getCurrentPosition() > 0){
            mp.reset();
            playNext();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }



    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        mediaPlayerControl.start();
        Intent notiIntent = new Intent(this, MainActivity.class);
        notiIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notiIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification noti = new NotificationCompat.Builder(this, ChannelNotification.CHANNEL_ID)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_play)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle("Playing")
                .setContentText(songTitle)
                .build();

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(1, noti);
    }

    @Override
    public void onDestroy(){
        stopForeground(true);
    }
}
