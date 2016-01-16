package org.luwrain.app.downloader;

import java.util.*;

import org.luwrain.core.*;

public class Extension extends org.luwrain.core.extensions.EmptyExtension
{
    @Override public Command[] getCommands(Luwrain luwrain)
    {
	return new Command[]{
	    new Command(){
		@Override public String getName()
		{
		    return "d";
		}
		@Override public void onCommand(Luwrain luwrain)
		{
		    luwrain.launchApp("d");
		}
	    }};
    }

    @Override public Shortcut[] getShortcuts(Luwrain luwrain)
    {
	return new Shortcut[]{
	    new Shortcut() {
		@Override public String getName()
		{
		    return "d";
		}
		@Override public Application[] prepareApp(String[] args)
		{
		    if (args == null || args.length < 1)
			return new Application[]{new DownloaderApp()};
		    LinkedList<Application> v = new LinkedList<Application>();
		    for(String s: args)
			if (s != null)
			    v.add(new DownloaderApp(s));
		    if (v.isEmpty())
			return new Application[]{new DownloaderApp()};
		    return v.toArray(new Application[v.size()]);
		}
	    }};
    }

    @Override public void i18nExtension(Luwrain luwrain, I18nExtension i18nExt)
    {
	i18nExt.addCommandTitle("en", "downloader", "Downloader");
	i18nExt.addCommandTitle("ru", "downloader", "Загрузчик файлов");
    }
}
