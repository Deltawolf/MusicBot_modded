package com.jagrosh.jmusicbot.commands.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import java.util.concurrent.TimeUnit;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.menu.OrderedMenu;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.commands.MusicCommand;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

/**
 * @author Fumitaka Yoshikane (me@bracken.black)
 */
public final class LocalSearchCmd extends SearchCmd {

    public LocalSearchCmd(Bot bot) {
        super(bot);

        this.name = "localsearch";
        this.searchPrefix = "reclocal:";
        this.help = "searches local files for a provided query";
        this.aliases = bot.getConfig().getAliases(this.name);
    }

private class ResultHandler implements AudioLoadResultHandler 
    {
        private final Message m;
        private final CommandEvent event;
        
        private ResultHandler(Message m, CommandEvent event)
        {
            this.m = m;
            this.event = event;
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist)
        {
            builder.setColor(event.getSelfMember().getColor())
                    .setText(FormatUtil.filter(event.getClient().getSuccess()+" Search results for `"+event.getArgs()+"`:"))
                    .setChoices(new String[0])
                    .setSelection((msg,i) -> 
                    {
                        AudioTrack track = playlist.getTracks().get(i-1);
                        if(bot.getConfig().isTooLong(track))
                        {
                            event.replyWarning("This track (**"+track.getInfo().title+"**) is longer than the allowed maximum: `"
                                    +FormatUtil.formatTime(track.getDuration())+"` > `"+bot.getConfig().getMaxTime()+"`");
                            return;
                        }
                        AudioHandler handler = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();
                        int pos = handler.addTrack(new QueuedTrack(track, event.getAuthor()))+1;
                        event.replySuccess("Added **" + FormatUtil.filter(track.getInfo().title)
                                + "** (`" + FormatUtil.formatTime(track.getDuration()) + "`) " + (pos==0 ? "to begin playing" 
                                    : " to the queue at position "+pos));
                    })
                    .setCancel((msg) -> {})
                    .setUsers(event.getAuthor())
                    ;
            for(int i=0; i<10 && i<playlist.getTracks().size(); i++)
            {
                AudioTrack track = playlist.getTracks().get(i);
                builder.addChoices("`["+FormatUtil.formatTime(track.getDuration())+"]` [**"+track.getInfo().title+"**]("+track.getInfo().uri+")");
            }
            builder.build().display(m);
        }

}