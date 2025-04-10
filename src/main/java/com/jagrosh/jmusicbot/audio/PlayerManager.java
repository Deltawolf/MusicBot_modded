/*
 * Copyright 2018 John Grosh <john.a.grosh@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.jmusicbot.audio;

import com.dunctebot.sourcemanagers.DuncteBotSources;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.utils.OtherUtil;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerRegistry;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.getyarn.GetyarnAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.nico.NicoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import net.dv8tion.jda.api.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class PlayerManager extends DefaultAudioPlayerManager
{
    private final static Logger LOGGER = LoggerFactory.getLogger(PlayerManager.class);
    private final Bot bot;

    public PlayerManager(Bot bot)
    {
        this.bot = bot;
    }

    public void init()
    {
        //AudioSourceManagers.registerRemoteSources(this);
        TransformativeAudioSourceManager.createTransforms(bot.getConfig().getTransforms()).forEach(t -> registerSourceManager(t));

        YoutubeAudioSourceManager yt = setupYoutubeAudioSourceManager();
        registerSourceManager(yt);

        registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        registerSourceManager(new BandcampAudioSourceManager());
        registerSourceManager(new VimeoAudioSourceManager());
        registerSourceManager(new TwitchStreamAudioSourceManager());
        registerSourceManager(new BeamAudioSourceManager());
        registerSourceManager(new GetyarnAudioSourceManager());
        registerSourceManager(new NicoAudioSourceManager());
        registerSourceManager(new HttpAudioSourceManager(MediaContainerRegistry.DEFAULT_REGISTRY));

        AudioSourceManagers.registerLocalSource(this);

        DuncteBotSources.registerAll(this, "en-US");
        AudioSourceManagers.registerLocalSource(this);
        registerSourceManager(new RecursiveLocalAudioSourceManager());
        source(YoutubeAudioSourceManager.class).setPlaylistPageCount(10);
    }

    public Bot getBot()
    {
        return bot;
    }

    public boolean hasHandler(Guild guild)
    {
        return guild.getAudioManager().getSendingHandler() != null;
    }

    public AudioHandler setUpHandler(Guild guild)
    {
        AudioHandler handler;
        if (guild.getAudioManager().getSendingHandler() == null) {
            AudioPlayer player = createPlayer();
            player.setVolume(bot.getSettingsManager().getSettings(guild).getVolume());
            handler = new AudioHandler(this, guild, player);
            player.addListener(handler);
            guild.getAudioManager().setSendingHandler(handler);
        } else
            handler = (AudioHandler) guild.getAudioManager().getSendingHandler();
        return handler;
    }

    private YoutubeAudioSourceManager setupYoutubeAudioSourceManager()
    {
        YoutubeAudioSourceManager yt = new YoutubeAudioSourceManager(true);
        yt.setPlaylistPageCount(bot.getConfig().getMaxYTPlaylistPages());

        // OAuth2 setup
        if (bot.getConfig().useYoutubeOauth2())
        {
            String token = null;
            try
            {
                token = Files.readString(OtherUtil.getPath("youtubetoken.txt"));
            }
            catch (NoSuchFileException e)
            {
                /* ignored */
            }
            catch (IOException e)
            {
                LOGGER.warn("Failed to read YouTube OAuth2 token file: {}", e.getMessage());
                return yt;
            }
            LOGGER.debug("Read YouTube OAuth2 refresh token from youtubetoken.txt");
            try
            {
                yt.useOauth2(token, false);
            }
            catch (Exception e)
            {
                LOGGER.warn("Failed to authorise with YouTube. If this issue persists, delete the youtubetoken.txt file to reauthorise.", e);
            }
        }
        return yt;
    }

    private static class RecursiveLocalAudioSourceManager extends LocalAudioSourceManager
    {
        private static final String SEARCH_PREFIX = "reclocal:";

        @Override
        public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) 
        {
            try {
                List<AudioTrack> tracks = searchPathsCorrespondingQuery(reference.getIdentifier())
                        .stream()
                        .map(path -> super.loadItem(manager, new AudioReference(path.toFile().toString(), path.getFileName().toString())))
                        .filter(audioItem -> audioItem instanceof AudioTrack)
                        .map(audioItem -> (AudioTrack) audioItem)
                        .collect(Collectors.toList());

                if (tracks.isEmpty()) return null;
                if (tracks.size() == 1) return tracks.get(0);

                return new BasicAudioPlaylist(reference.identifier, tracks, tracks.get(0), true);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            return null;
        }

        private List<Path> searchPathsCorrespondingQuery(String query) throws IOException
        {
            if (!query.startsWith(SEARCH_PREFIX)) return Collections.emptyList();

            List<String> words = Arrays.stream(query.substring(SEARCH_PREFIX.length()).trim().split(" "))
                    .filter(word -> !word.isEmpty())
                    .map(String::toUpperCase)
                    .collect(Collectors.toList());

            return Files.walk(Paths.get("."))
                    .filter(path -> {
                        String fileName = path.getFileName().toString().toUpperCase();
                        return words.stream().allMatch(fileName::contains);
                    })
                    .collect(Collectors.toList());
        }
    }


}
