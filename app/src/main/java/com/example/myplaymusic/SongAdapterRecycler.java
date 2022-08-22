package com.example.myplaymusic;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class SongAdapterRecycler extends RecyclerView.Adapter<SongAdapterRecycler.ViewHolder> {

    public interface OnItemClickListener{
        void onItemClick(int pos);
    }

    private OnItemClickListener listener;
    private Context mContext;
    private ArrayList<Song> mSongs;

    public SongAdapterRecycler(Context mContext, ArrayList<Song> mSongs, OnItemClickListener listener){
        this.mContext = mContext;
        this.mSongs = mSongs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View songView = inflater.inflate(R.layout.song, parent, false);
        ViewHolder viewHolder = new ViewHolder(songView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int pos) {
        Song song = mSongs.get(pos);
        holder.songTitle.setText(song.getTitle());
        holder.songArtist.setText(song.getArtist());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClick(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return mSongs.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        private TextView songTitle;
        private TextView songArtist;

        public ViewHolder(View itemView){
            super(itemView);
            songTitle = itemView.findViewById(R.id.song_title);
            songArtist = itemView.findViewById(R.id.song_artist);
        }
    }
}
