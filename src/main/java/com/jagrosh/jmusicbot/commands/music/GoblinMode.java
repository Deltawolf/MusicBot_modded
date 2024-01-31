package com.jagrosh.jmusicbot.commands.music;

import java.util.List;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.impl.CommandClientImpl;
import com.jagrosh.jmusicbot.commands.MusicCommand;
import com.jagrosh.jmusicbot.Bot;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;

public class GoblinMode extends MusicCommand {

	Bot localBot;

	public GoblinMode(Bot bot)
    {
		super(bot);

		localBot = bot;
        this.name = "goblinmode";
        this.arguments = "<title|URL|subcommand>";
        this.help = "plays the provided song";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.beListening = true;
        this.bePlaying = false;
        

		//Can I find the PlayCMD type within this list?
	

	}

	/*Test if this works*/
	@Override
	public void doCommand(CommandEvent event) {
		List<Object> listeners = localBot.getJDA().getEventManager().getRegisteredListeners();
		for (Object listener : listeners) {
			System.out.println(listener.getClass().getName());
			if (listener instanceof CommandClientImpl) {
				List<Command> commands = ((CommandClientImpl)listener).getCommands();
				for (Object command : commands) {
					System.out.println(command.getClass().getName());
					if(command instanceof PlayCmd) {
						((PlayCmd)command).setGoblinMode();
					}
					
				}

			}
			
		}

		event.reply("Goblin mode set!");
		//bot.getPlayerManager()
		//localBot.getJDA().upsertCommand(play);
		
	}

	@Override
	protected void execute(CommandEvent event) {
		// TODO Auto-generated method stub
		super.execute(event);
	}

	@Override
	public String[] getAliases() {
		// TODO Auto-generated method stub
		return super.getAliases();
	}

	@Override
	public String getArguments() {
		// TODO Auto-generated method stub
		return super.getArguments();
	}

	@Override
	public Permission[] getBotPermissions() {
		// TODO Auto-generated method stub
		return super.getBotPermissions();
	}

	@Override
	public Category getCategory() {
		// TODO Auto-generated method stub
		return super.getCategory();
	}

	@Override
	public Command[] getChildren() {
		// TODO Auto-generated method stub
		return super.getChildren();
	}

	@Override
	public int getCooldown() {
		// TODO Auto-generated method stub
		return super.getCooldown();
	}

	@Override
	public String getCooldownError(CommandEvent event, int remaining) {
		// TODO Auto-generated method stub
		return super.getCooldownError(event, remaining);
	}

	@Override
	public String getCooldownKey(CommandEvent event) {
		// TODO Auto-generated method stub
		return super.getCooldownKey(event);
	}

	@Override
	public String getHelp() {
		// TODO Auto-generated method stub
		return super.getHelp();
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return super.getName();
	}

	@Override
	public String getRequiredRole() {
		// TODO Auto-generated method stub
		return super.getRequiredRole();
	}

	@Override
	public Permission[] getUserPermissions() {
		// TODO Auto-generated method stub
		return super.getUserPermissions();
	}

	@Override
	public boolean isAllowed(TextChannel channel) {
		// TODO Auto-generated method stub
		return super.isAllowed(channel);
	}

	@Override
	public boolean isCommandFor(String arg0) {
		// TODO Auto-generated method stub
		return super.isCommandFor(arg0);
	}

	@Override
	public boolean isGuildOnly() {
		// TODO Auto-generated method stub
		return super.isGuildOnly();
	}

	@Override
	public boolean isHidden() {
		// TODO Auto-generated method stub
		return super.isHidden();
	}

	@Override
	public boolean isOwnerCommand() {
		// TODO Auto-generated method stub
		return super.isOwnerCommand();
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		return super.clone();
	}

	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		return super.equals(obj);
	}

	@Override
	protected void finalize() throws Throwable {
		// TODO Auto-generated method stub
		super.finalize();
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return super.hashCode();
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return super.toString();
	}
	
	
}
