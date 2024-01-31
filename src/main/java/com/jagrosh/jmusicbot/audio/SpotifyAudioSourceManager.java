package com.jagrosh.jmusicbot.audio;


import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Audio source manager that implements finding Spotify videos or playlists based on an URL or ID.
 */
public class SpotifyAudioSourceManager implements AudioSourceManager
{
  private static final Logger log = LoggerFactory.getLogger(SpotifyAudioSourceManager.class);

  public String getSourceName()
  {
    return "Spotify";
  }

  public boolean isTrackEncodable(AudioTrack track)
  {
    return false;
  }

  public void encodeTrack(AudioTrack track, DataOutput datastream) throws IOException
  {

  }

  public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput datastream) throws IOException
  {
    AudioTrack track = null;
    return track;
  }

  public void shutdown()
  {

  }

  public AudioItem loadItem(AudioPlayerManager manager, AudioReference myref)
  {

    AudioItem item = null;
    return item;
  }


}


/*
 *   public abstract java.lang.String getSourceName();
  
  public abstract com.sedmelluq.discord.lavaplayer.track.AudioItem loadItem(com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager arg0, com.sedmelluq.discord.lavaplayer.track.AudioReference arg1);
  
  public abstract boolean isTrackEncodable(com.sedmelluq.discord.lavaplayer.track.AudioTrack arg0);
  
  public abstract void encodeTrack(com.sedmelluq.discord.lavaplayer.track.AudioTrack arg0, java.io.DataOutput arg1) throws java.io.IOException;
  
  public abstract com.sedmelluq.discord.lavaplayer.track.AudioTrack decodeTrack(com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo arg0, java.io.DataInput arg1) throws java.io.IOException;
  
  public abstract void shutdown();

    public abstract  void configureRequests(java.util.function.Function<org.apache.http.client.config.RequestConfig,org.apache.http.client.config.RequestConfig> arg0);
  
  public abstract  void configureBuilder(java.util.function.Consumer<org.apache.http.impl.client.HttpClientBuilder> arg0);
*/