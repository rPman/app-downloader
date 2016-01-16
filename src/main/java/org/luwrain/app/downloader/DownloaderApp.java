package org.luwrain.app.downloader;

import org.luwrain.core.Application;
import org.luwrain.core.AreaLayout;
import org.luwrain.core.Luwrain;

public class DownloaderApp implements Application, Actions
{
    private Luwrain luwrain;
    private DownloaderArea area;
    
    // open downloader UI
    public DownloaderApp()
    {
    	
    }
    
    // open downloader UI and add link to download
    public DownloaderApp(String link)
    {
    	
    }

    @Override public boolean onLaunch(Luwrain luwrain)
    {
    	this.luwrain = luwrain;
    	area = new DownloaderArea(luwrain,this);
    	return true;
    }

	@Override public String getAppName()
	{
		return "Downloader app";
	}

	@Override public AreaLayout getAreasToShow()
	{
		return new AreaLayout(area);
	}

	@Override public void closeApp()
	{
		luwrain.closeApp();
	}

}
