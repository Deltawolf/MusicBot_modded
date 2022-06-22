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
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.exceptions.detailed.UnauthorizedException;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.PagingCursorbased;
import se.michaelthelin.spotify.model_objects.specification.PlayHistory;
import se.michaelthelin.spotify.requests.data.player.GetCurrentUsersRecentlyPlayedTracksRequest;
import org.apache.hc.core5.http.ParseException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;

import com.fasterxml.jackson.databind.ser.std.JsonValueSerializer;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
	private final static URI redirect = SpotifyHttpManager.makeUri("http://localhost:8888/callback/");
	private final static String deviceName = "Zach-Stream";
	private static AuthorizationCodeCredentials authorizationCodeCredentials;
	private static String code = "";

	private static final SpotifyApi spotifyApi = new SpotifyApi.Builder()
	.setAccessToken("Zjc3YjIxYzgzZjg4NDIyMmFhODZmNTI0YTM3Mzg5M2E6ZTA4YzZlZGNmZjhjNDUyNGEwMTBlNzc1YTk2YzJlNDA=")
	.setClientId("f77b21c83f884222aa86f524a373893a")
	.setClientSecret("e08c6edcff8c4524a010e775a96c2e40")
	.setRedirectUri(redirect)
	.build();

	private static final AuthorizationCodeUriRequest authorizationCodeUriRequest = spotifyApi.authorizationCodeUri()
	//          .state("x4xkmn9pu3j6ukrs8n")
	          .scope("user-modify-playback-state,user-read-playback-state,user-read-currently-playing,user-read-recently-played,user-read-playback-position,user-top-read,streaming,user-library-read,playlist-read-collaborative,playlist-read-private,")
	//          .show_dialog(true)
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

        if(event.getArgs().startsWith("help"))
        {
			URI uri = authorizationCodeUriRequest.execute();
			System.out.println("URI: " + uri.toString() + "\n\n");
            StringBuilder builder = new StringBuilder(event.getClient().getWarning()+" Click the link to receive an authorization code.\nUse the code with the Spotify command to continue.\n" + uri.toString() + "\n");
            for(Command cmd: children)
                builder.append("\n`").append(event.getClient().getPrefix()).append(name).append(" ").append(cmd.getName()).append(" ").append(cmd.getArguments()).append("` - ").append(cmd.getHelp());
            event.reply(builder.toString());
            return;
        }
		else if(event.getArgs().startsWith("code"))
		{

			URI uri = authorizationCodeUriRequest.execute();
			System.out.println("URI: " + uri.toString());

			event.reply("Use this link to retrieve a new authorization code\n" + uri.toString() + "\n");

			System.out.println("Expires in: " + authorizationCodeCredentials.getExpiresIn());
			System.out.println("\nAccess token: "+spotifyApi.getAccessToken());
			System.out.println("\nRefresh code: "+spotifyApi.getRefreshToken() + "\n");
			System.out.println(authorizationCodeCredentials.toString()+ "\n");
		}
		else if(event.getArgs().startsWith("authorize "))
		{
			
			try
			{
				code = event.getArgs().substring(10, event.getArgs().length()-1); 
				AuthorizationCodeRequest authorizationCodeRequest = spotifyApi.authorizationCode(code).build();
				AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRequest.execute();
				spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
				spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());
			}
			catch (IOException | SpotifyWebApiException | ParseException e) 
			{
				System.out.println("\nError: " + e.getMessage() + "\nCause: " + e.getCause() + "\n");
			}
		}

		code = event.getArgs();
        
		try
		{
			spotify_authentication(code);
			
			getRecentlyPlayed();

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
		catch (IOException | SpotifyWebApiException | ParseException e) 
		{
			System.out.println("\nError: " + e.getMessage() + "\nCause: " + e.getCause() + "\n");
		}

		event.reply(loadingEmoji+" Loading... `[ " + deviceName + " ]`", m -> bot.getPlayerManager().loadItemOrdered(event.getGuild(), "http://192.168.1.50/stream.mp3", new ResultHandler(m,event,false)));

	}

	private static void spotify_authentication(String code) throws IOException, SpotifyWebApiException, ParseException
	{
	
		spotifyApi.setRefreshToken(	"AQDoWowuExGPMB68nJ7_GriJCyacb1D5XKHo58a7NH0qQOf34xI3RHCBtAxx2vfiIT0vbyd21HJQrgm24VKUUcGGjpjw7HQVn-3zIzSfJnb4YnBkV7fkPKuxQdn2XiXltiI");
	
		AuthorizationCodeRefreshRequest authorizationCodeRefreshRequest = spotifyApi.authorizationCodeRefresh().build();
		authorizationCodeCredentials = authorizationCodeRefreshRequest.execute();


		spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());

		// System.out.println("Expires in: " + authorizationCodeCredentials.getExpiresIn());
		// System.out.println("\nAccess token: "+spotifyApi.getAccessToken());
		// System.out.println("\nRefresh code: "+spotifyApi.getRefreshToken() + "\n");
		// System.out.println(authorizationCodeCredentials.toString()+ "\n");
			
	}

	private static void refreshTokens() throws IOException, SpotifyWebApiException, ParseException
	{
		// System.out.println("\nOld Access token: "+ spotifyApi.getAccessToken());
		// System.out.println("\nOld Refresh token: "+ spotifyApi.getRefreshToken());

		AuthorizationCodeRefreshRequest authorizationCodeRefreshRequest = spotifyApi.authorizationCodeRefresh().build();
		authorizationCodeCredentials = authorizationCodeRefreshRequest.execute();

		System.out.println("\nNew Access token \n"); //+ authorizationCodeCredentials.getAccessToken() + "\n");

		// System.out.println(authorizationCodeCredentials.toString()+ "\n");

		spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());

	}

	private static void getRecentlyPlayed() throws IOException, SpotifyWebApiException, ParseException
	{

		GetCurrentUsersRecentlyPlayedTracksRequest agb = spotifyApi.getCurrentUsersRecentlyPlayedTracks().build();
		PagingCursorbased<PlayHistory> age = agb.execute();
		
		for (Integer i=0;i<5;i++) 
		{
			System.out.println(String.format("Track %d: %s by %s", i+1,age.getItems()[i].getTrack().getName().toString(), age.getItems()[i].getTrack().getArtists()[0].getName().toString()));
		}   
	}

	private static String[] getNowPlaying() throws IOException, SpotifyWebApiException, ParseException
	{
		refreshTokens();

		GetUsersCurrentlyPlayingTrackRequest getUsersCurrentlyPlayingTrackRequest = spotifyApi
		.getUsersCurrentlyPlayingTrack()
	//          .market(CountryCode.SE)
	//          .additionalTypes("track,episode")
		.build();

		CurrentlyPlaying currentlyPlaying = getUsersCurrentlyPlayingTrackRequest.execute();
		System.out.println("Timestamp: " + currentlyPlaying.getTimestamp());
		String track_id = currentlyPlaying.getItem().getId();
		Track track = spotifyApi.getTrack(track_id).build().execute();

		String artist = track.getArtists()[0].getName().toString();
		String song = track.getName().toString();
		String[] track_info = {"", ""};
		if(artist != null && song != null)
		{
			System.out.println(String.format("Now playing %s by %s", song, artist));
			track_info[0] = artist;
			track_info[1] = song;
		}

		return track_info;

	}

	private static Device[] getDevices() throws IOException, SpotifyWebApiException, ParseException
	{
		refreshTokens();

		GetUsersAvailableDevicesRequest getUsersAvailableDevicesRequest = spotifyApi
		.getUsersAvailableDevices()
		.build();

		Device[] devices = getUsersAvailableDevicesRequest.execute();
		

		return devices;
	}

	private static void transferDevice(Device[] devices) throws IOException, SpotifyWebApiException, ParseException
	{
		refreshTokens();
		Device currentDevice = null;

		for(Device device : devices)
		{
			System.out.println("Device detected: \nName: " + device.getName() + "\tID: " + device.getId() + "\n");
			if(device.getName().equals(deviceName))
				currentDevice = device;
		}

		if(currentDevice == null)
			return;

		
		String[] deviceID = {JsonParser.parseString(currentDevice.getId()).getAsString()};
		Gson gson = new Gson();

		JsonArray deviceIds = JsonParser.parseString(gson.toJson(deviceID)).getAsJsonArray();

		TransferUsersPlaybackRequest transferUsersPlaybackRequest = spotifyApi
		.transferUsersPlayback(deviceIds)
	//          .play(false)
		.build();
		
		final String string = transferUsersPlaybackRequest.execute();
	
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
