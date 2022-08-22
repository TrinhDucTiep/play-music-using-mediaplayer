package com.example.myplaymusic;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.example.myplaymusic.MusicService.MusicBinder;

public class MainActivity extends AppCompatActivity implements MediaController.MediaPlayerControl{

    private ArrayList<Song> songList;
    private RecyclerView recyclerSong;

    //variables represents service class
    private MusicService musicService;
    private Intent playIntent;
    private boolean musicBound = false; // flag to keep track whether activity is bound to the service class

    private MusicController controller;

    //variables for controller buttons
    private ImageView backBtn, playBtn, nextBtn;
    private SeekBar seekBar;
    private TextView seekBarHint;
    Handler handler = new Handler();

    private boolean paused=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerSong = findViewById(R.id.song_list);
        backBtn = findViewById(R.id.back_btn);
        playBtn = findViewById(R.id.play_btn);
        nextBtn = findViewById(R.id.next_btn);
        seekBar = findViewById(R.id.seekbar);
        seekBarHint = findViewById(R.id.seekbar_hint);

        songList = new ArrayList<Song>();

        getSongList();
        Collections.sort(songList, new Comparator<Song>() {
            @Override
            public int compare(Song o1, Song o2) {
                return o1.getTitle().compareTo(o2.getTitle());
            }
        });

        SongAdapterRecycler songAdapter = new SongAdapterRecycler(this, songList, new SongAdapterRecycler.OnItemClickListener() {
            @Override
            public void onItemClick(int pos) {
                songPicked(pos);
                playBtn.setImageResource(R.drawable.ic_pause);
            }
        });

        recyclerSong.setAdapter(songAdapter);
        recyclerSong.setLayoutManager(new LinearLayoutManager(this));

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
                playBtn.setImageResource(R.drawable.ic_pause);
            }
        });
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
                playBtn.setImageResource(R.drawable.ic_pause);
            }
        });
        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(paused){
                    playBtn.setImageResource(R.drawable.ic_pause);
                    paused = false;
                    musicService.resume();
                    UpdateSeekBar updateSeekBar = new UpdateSeekBar();
                    handler.post(updateSeekBar);
                }else{
                    playBtn.setImageResource(R.drawable.ic_play);
                    paused = true;
                    musicService.pausePlayer();
                }
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekBarHint.setVisibility(View.VISIBLE);

                if(fromUser && !musicService.isPng()){
                    musicService.seek(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seekBarHint.setVisibility(View.VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(musicService.isPng()){
                    seekTo(seekBar.getProgress());
                    start();
                }

            }
        });

    }


    //inner class for update seekbar
    public class UpdateSeekBar implements Runnable{

        @Override
        public void run() {
            if(!paused){
                seekBar.setProgress(musicService.getSongPosn());
                handler.postDelayed(this, 100);
            }

        }
    }


    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder binder = (MusicBinder) service;
            musicService = binder.getService(); // get service
            musicService.setList(songList);
            musicBound = true;
            musicService.setMediaPlayerControl(MainActivity.this);

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }

        @Override
        public void onBindingDied(ComponentName name) {
            ServiceConnection.super.onBindingDied(name);
        }

        @Override
        public void onNullBinding(ComponentName name) {
            ServiceConnection.super.onNullBinding(name);
        }
    };

    public void getSongList(){
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
        if(musicCursor!=null && musicCursor.moveToFirst()){
//            getcolumns
            int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            do{
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                songList.add(new Song(thisId, thisTitle, thisArtist));
            }while (musicCursor.moveToNext());
        }
    }

    private void playNext(){
        musicService.playNext();
        if(paused){
            paused=false;
        }
    }
    private void playPrev(){
        musicService.playPrev();
        if(paused){
            paused=false;
        }
    }


    public void songPicked(int pos){
        musicService.setSongs(pos);
        // check until preparing completed
        musicService.playSong();

        if(paused){
            paused=false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(playIntent == null){
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    @Override
    protected void onDestroy(){
        stopService(playIntent);
        musicService = null;
        super.onDestroy();
    }

    @Override
    protected void onPause(){
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(paused){
            paused = false;
        }
    }

    @Override
    protected void onStop(){
        super.onStop();
        if(musicBound){
            unbindService(musicConnection);
            musicBound = false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.action_end:
                stopService(playIntent);
                musicService = null;
                System.exit(0);
                break;
            case R.id.action_shuffle:
                musicService.setShuffle();
                if(musicService.getShuffle()){
                    Toast.makeText(musicService, "Random turn on", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(musicService, "Random turn off", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void start() {
        if(paused){
            paused=false;
        }
        seekBar.setMax(musicService.getDur());
        UpdateSeekBar updateSeekBar = new UpdateSeekBar();
        handler.post(updateSeekBar);
    }

    @Override
    public void pause() {
        paused=true;
        musicService.pausePlayer();
    }

    @Override
    public int getDuration() {
        if(musicService!=null && musicBound && musicService.isPng())
            return musicService.getDur();
        else return 0;
    }

    @Override
    public int getCurrentPosition() {
        if(musicService!=null && musicBound)
            return musicService.getSongPosn();
        else return 0;
    }

    @Override
    public void seekTo(int pos) {
        musicService.seek(pos);
    }

    @Override
    public boolean isPlaying() {
        if(musicService!=null && musicBound)
            return musicService.isPng();
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }
}