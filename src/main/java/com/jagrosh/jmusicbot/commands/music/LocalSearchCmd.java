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
import com.jagrosh.jmusicbot.utils.FormatUtil;
import net.dv8tion.jda.api.entities.Message;

/**
 * @author Fumitaka Yoshikane (me@bracken.black)
 */
public final class LocalSearchCmd extends SearchCmd {

	protected String searchPrefix = "reclocal:";
    private final OrderedMenu.Builder builder;
    private final String searchingEmoji;
	
	public LocalSearchCmd(Bot bot) {
        super(bot);

        this.name = "localsearch";
        this.searchPrefix = "reclocal:";
		this.searchingEmoji = bot.getConfig().getSearching();
        this.help = "searches local files for a provided query";
        this.aliases = bot.getConfig().getAliases(this.name);
		   builder = new OrderedMenu.Builder()
                .allowTextInput(true)
                .useNumbers()
                .useCancelButton(true)
                .setEventWaiter(bot.getWaiter())
                .setTimeout(1, TimeUnit.MINUTES);
    }

    @Override
    public void doCommand(CommandEvent event) 
    {
        if(event.getArgs().isEmpty())
        {
            event.replyError("Please include a query.");
            return;
        }
        event.reply(searchingEmoji+" Searching... `["+event.getArgs()+"]`", 
                m -> bot.getPlayerManager().loadItemOrdered(event.getGuild(), searchPrefix + event.getArgs(), new ResultHandler(m,event)));
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
        public void trackLoaded(AudioTrack track)
        {
            if(bot.getConfig().isTooLong(track))
            {
                m.editMessage(FormatUtil.filter(event.getClient().getWarning()+" This track (**"+track.getInfo().title+"**) is longer than the allowed maximum: `"
                        + TimeUtil.formatTime(track.getDuration())+"` > `"+ TimeUtil.formatTime(bot.getConfig().getMaxSeconds()*1000)+"`")).queue();
                return;
            }
            AudioHandler handler = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();
            int pos = handler.addTrack(new QueuedTrack(track, RequestMetadata.fromResultHandler(track, event)))+1;
            m.editMessage(FormatUtil.filter(event.getClient().getSuccess()+" Added **"+track.getInfo().title
                    +"** (`"+TimeUtil.formatTime(track.getDuration())+"`) "+(pos==0 ? "to begin playing" 
                        : " to the queue at position "+pos))).queue();
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
                                    +TimeUtil.formatTime(track.getDuration())+"` > `"+bot.getConfig().getMaxTime()+"`");
                            return;
                        }
                        AudioHandler handler = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();
                        int pos = handler.addTrack(new QueuedTrack(track, RequestMetadata.fromResultHandler(track, event)))+1;
                        event.replySuccess("Added **" + FormatUtil.filter(track.getInfo().title)
                                + "** (`" + TimeUtil.formatTime(track.getDuration()) + "`) " + (pos==0 ? "to begin playing" 
                                    : " to the queue at position "+pos));
                    })
                    .setCancel((msg) -> {})
                    .setUsers(event.getAuthor())
                    ;
            for(int i=0; i<10 && i<playlist.getTracks().size(); i++)
            {
                AudioTrack track = playlist.getTracks().get(i);
                builder.addChoices("`["+TimeUtil.formatTime(track.getDuration())+"]` [**"+track.getInfo().title+"**]("+track.getInfo().uri+")");
            }
            builder.build().display(m);
        }

        @Override
        public void noMatches() 
        {
            m.editMessage(FormatUtil.filter(event.getClient().getWarning()+" No results found for `"+event.getArgs()+"`.")).queue();
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