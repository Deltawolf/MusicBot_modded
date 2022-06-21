//https://community.spotify.com/t5/Spotify-for-Developers/INVALID-CLIENT-Invalid-redirect-URI/td-p/5228936

package com.jagrosh.jmusicbot.commands.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.menu.ButtonMenu;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.commands.MusicCommand;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.PermissionException;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.Track;
import org.apache.hc.core5.http.ParseException;

import java.io.IOException;
import java.net.URI;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import se.michaelthelin.spotify.requests.data.player.GetUsersAvailableDevicesRequest;
import se.michaelthelin.spotify.requests.data.player.TransferUsersPlaybackRequest;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;
import se.michaelthelin.spotify.requests.data.player.GetUsersCurrentlyPlayingTrackRequest;
import se.michaelthelin.spotify.model_objects.miscellaneous.Device;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.info.AudioTrackInfoBuilder;

/**
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */

public class SpotifyCmd extends MusicCommand
{
    private final static String LOAD = "\uD83D\uDCE5"; // 📥
    private final static String CANCEL = "\uD83D\uDEAB"; // 🚫
	private final static URI redirect = URI.create("http://localhost:8888/callback/");
	private final static String deviceName = "Zach-Stream";
	private static String code = "";

	private static final SpotifyApi spotifyApi = new SpotifyApi.Builder()
	.setAccessToken("Zjc3YjIxYzgzZjg4NDIyMmFhODZmNTI0YTM3Mzg5M2E6ZTA4YzZlZGNmZjhjNDUyNGEwMTBlNzc1YTk2YzJlNDA=")
	.setClientId("f77b21c83f884222aa86f524a373893a")
	.setClientSecret("e08c6edcff8c4524a010e775a96c2e40")
	.setRedirectUri(redirect)
	.build();

	private static final AuthorizationCodeUriRequest authorizationCodeUriRequest = spotifyApi.authorizationCodeUri()
	//          .state("x4xkmn9pu3j6ukrs8n")
	//          .scope("user-read-birthdate,user-read-email")
	//          .show_dialog(true)
		.build();
	//private static final AuthorizationCodeRequest authorizationCodeRequest = spotifyApi.authorizationCode(code)
    //.build();

	private static final AuthorizationCodeRefreshRequest authorizationCodeRefreshRequest = spotifyApi.authorizationCodeRefresh()
    .build();
	
	private static final GetUsersCurrentlyPlayingTrackRequest getUsersCurrentlyPlayingTrackRequest = spotifyApi
    .getUsersCurrentlyPlayingTrack()
//          .market(CountryCode.SE)
//          .additionalTypes("track,episode")
    .build();

	private static final GetUsersAvailableDevicesRequest getUsersAvailableDevicesRequest = spotifyApi
    .getUsersAvailableDevices()
    .build();

    
    private final String loadingEmoji;
    
    public SpotifyCmd(Bot bot)
    {
        super(bot);
        this.loadingEmoji = bot.getConfig().getLoading();
        this.name = "spotify";
        this.arguments = "<title|URL|subcommand>";
        this.help = "streams Zach's Spotify into Discord";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.beListening = true;
        this.bePlaying = false;
    }

    @Override
    public void doCommand(CommandEvent event) 
    {
		
        if(event.getArgs().contains("help"))
        {

            StringBuilder builder = new StringBuilder(event.getClient().getWarning()+" Play Commands:\n");
            builder.append("\n`").append(event.getClient().getPrefix()).append(name).append(" <song title>` - plays the first result from Youtube");
            builder.append("\n`").append(event.getClient().getPrefix()).append(name).append(" <URL>` - plays the provided song, playlist, or stream");
            for(Command cmd: children)
                builder.append("\n`").append(event.getClient().getPrefix()).append(name).append(" ").append(cmd.getName()).append(" ").append(cmd.getArguments()).append("` - ").append(cmd.getHelp());
            event.reply(builder.toString());
            return;
        }
  
        event.reply(loadingEmoji+" Loading... `[ " + deviceName + " ]`", m -> bot.getPlayerManager().loadItemOrdered(event.getGuild(), "http://127.0.0.1/stream.mp3", new ResultHandler(m,event,false)));

		try 
		{
			final URI uri = authorizationCodeUriRequest.execute();
			System.out.println("URI: " + uri.toString());
			code = uri.toString();
			AuthorizationCodeRequest authorizationCodeRequest = spotifyApi.authorizationCode(code).build();
			final AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRequest.execute();
			System.out.println("Expires in: " + authorizationCodeCredentials.getExpiresIn());
			spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
			spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());
			System.out.println("Expires in: " + authorizationCodeCredentials.getExpiresIn());
		} 
		catch (IOException | SpotifyWebApiException | ParseException e) 
		{
		  System.out.println("Error: " + e.getMessage());
		}
			spotifyApi.authorizationCodeRefresh();


		String[] track = getNowPlaying();
		if(event.getAuthor().getId().equals(event.getClient().getOwnerId()))
		{
			Device[] devices = getDevices();
			transferDevice(devices);
		}

		AudioTrackInfo trackInfo = AudioTrackInfoBuilder.empty()
        .setAuthor(track[0])
		.setTitle(track[1])
        .build();

	}

	private static String[] getNowPlaying() 
	{

		try 
		{
			final CurrentlyPlaying currentlyPlaying = getUsersCurrentlyPlayingTrackRequest.execute();

			System.out.println("Timestamp: " + currentlyPlaying.getTimestamp());
			String track_id = currentlyPlaying.getItem().getId();
			Track track = spotifyApi.getTrack(track_id).build().execute();

			String artist = track.getArtists().toString();
			String song = track.getName().toString();

			String[] track_info = {artist, song};
			return track_info;

		} 
		catch (IOException | SpotifyWebApiException | ParseException e) 
		{
			System.out.println("Error: " + e.getCause().getMessage());
			String[] var = {"",""};
			return var;
		} 
	
	}

	private static Device[] getDevices() 
	{
		try 
		{
			final Device[] devices = getUsersAvailableDevicesRequest.execute();
			System.out.println("Length: " + devices.length);
			return devices;
		} 
		catch (IOException | SpotifyWebApiException | ParseException e) 
		{
			System.out.println("Error: " + e.getMessage());
			final Device[] devices = null;
			return devices;
		}

	}

	private static void transferDevice(Device[] devices) 
	{
		Device currentDevice = null;
		for(Device device : devices)
		{
			if(device.getName().equals(deviceName))
				currentDevice = device;
		}

		if(currentDevice == null)
			return;

		JsonArray deviceIds = JsonParser.parseString(currentDevice.getId()).getAsJsonArray();
		final TransferUsersPlaybackRequest transferUsersPlaybackRequest = spotifyApi
		.transferUsersPlayback(deviceIds)
	//          .play(false)
		.build();
		
		try 
		{
		  final String string = transferUsersPlaybackRequest.execute();
	
		  System.out.println("Null: " + string);
		} catch (IOException | SpotifyWebApiException | ParseException e) {
		  System.out.println("Error: " + e.getMessage());
		}
	  }

    
    private class ResultHandler implements AudioLoadResultHandler
    {
        private final Message m;
        private final CommandEvent event;
        private final boolean ytsearch;
        
        private ResultHandler(Message m, CommandEvent event, boolean ytsearch)
        {
            this.m = m;
            this.event = event;
            this.ytsearch = ytsearch;
        }
        
        private void loadSingle(AudioTrack track, AudioPlaylist playlist)
        {

            String addMsg = FormatUtil.filter(event.getClient().getSuccess()+ " Loaded stream!");
            if(playlist==null || !event.getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_ADD_REACTION))
                m.editMessage(addMsg).queue();
            else
            {
                new ButtonMenu.Builder()
                        .setText(addMsg+"\n"+event.getClient().getWarning()+" This track has a playlist of **"+playlist.getTracks().size()+"** tracks attached. Select "+LOAD+" to load playlist.")
                        .setChoices(LOAD, CANCEL)
                        .setEventWaiter(bot.getWaiter())
                        .setTimeout(30, TimeUnit.SECONDS)
                        .setAction(re ->
                        {
                            if(re.getName().equals(LOAD))
                                m.editMessage(addMsg+"\n"+event.getClient().getSuccess()+" Loaded **"+loadPlaylist(playlist, track)+"** additional tracks!").queue();
                            else
                                m.editMessage(addMsg).queue();
                        }).setFinalAction(m ->
                        {
                            try{ m.clearReactions().queue(); }catch(PermissionException ignore) {}
                        }).build().display(m);
            }
        }
        
        private int loadPlaylist(AudioPlaylist playlist, AudioTrack exclude)
        {
            int[] count = {0};
            playlist.getTracks().stream().forEach((track) -> {
                if(!bot.getConfig().isTooLong(track) && !track.equals(exclude))
                {
                    AudioHandler handler = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();
                    handler.addTrack(new QueuedTrack(track, event.getAuthor()));
                    count[0]++;
                }
            });
            return count[0];
        }
        
        @Override
        public void trackLoaded(AudioTrack track)
        {
			            loadSingle(track, null);
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist)
        {
            if(playlist.getTracks().size()==1 || playlist.isSearchResult())
            {
                AudioTrack single = playlist.getSelectedTrack()==null ? playlist.getTracks().get(0) : playlist.getSelectedTrack();
                loadSingle(single, null);
            }
            else if (playlist.getSelectedTrack()!=null)
            {
                AudioTrack single = playlist.getSelectedTrack();
                loadSingle(single, playlist);
            }
            else
            {
                int count = loadPlaylist(playlist, null);
                if(playlist.getTracks().size() == 0)
                {
                    m.editMessage(FormatUtil.filter(event.getClient().getWarning()+" The playlist "+(playlist.getName()==null ? "" : "(**"+playlist.getName()
                            +"**) ")+" could not be loaded or contained 0 entries")).queue();
                }
                else if(count==0)
                {
                    m.editMessage(FormatUtil.filter(event.getClient().getWarning()+" All entries in this playlist "+(playlist.getName()==null ? "" : "(**"+playlist.getName()
                            +"**) ")+"were longer than the allowed maximum (`"+bot.getConfig().getMaxTime()+"`)")).queue();
                }
                else
                {
                    m.editMessage(FormatUtil.filter(event.getClient().getSuccess()+" Found "
                            +(playlist.getName()==null?"a playlist":"playlist **"+playlist.getName()+"**")+" with `"
                            + playlist.getTracks().size()+"` entries; added to the queue!"
                            + (count<playlist.getTracks().size() ? "\n"+event.getClient().getWarning()+" Tracks longer than the allowed maximum (`"
                            + bot.getConfig().getMaxTime()+"`) have been omitted." : ""))).queue();
                }
            }
        }

        @Override
        public void noMatches()
        {
            if(ytsearch)
                m.editMessage(FormatUtil.filter(event.getClient().getWarning()+" No results found for `"+event.getArgs()+"`.")).queue();
            else
                bot.getPlayerManager().loadItemOrdered(event.getGuild(), "ytsearch:"+event.getArgs(), new ResultHandler(m,event,true));
        }

        @Override
        public void loadFailed(FriendlyException throwable)
        {
            if(throwable.severity==Severity.COMMON)
                m.editMessage(event.getClient().getError()+" Error loading: "+throwable.getMessage()).queue();
            else
                m.editMessage(event.getClient().getError()+" Error loading track.").queue();
        }
    }
    
}
