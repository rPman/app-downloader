package org.luwrain.app.downloader;

import org.luwrain.controls.ControlEnvironment;
import org.luwrain.controls.DefaultControlEnvironment;
import org.luwrain.controls.NavigateArea;
import org.luwrain.core.Luwrain;
import org.luwrain.core.NullCheck;
import org.luwrain.core.events.EnvironmentEvent;

class DownloaderArea extends NavigateArea
{
	private Luwrain luwrain;
	private ControlEnvironment environment;
	private Actions actions;

	public DownloaderArea(Luwrain luwrain,Actions actions)
	{
		super(new DefaultControlEnvironment(luwrain));
		this.luwrain = luwrain;
		this.environment = new DefaultControlEnvironment(luwrain);
		this.actions = actions;

		NullCheck.notNull(luwrain, "luwrain");
	}

	@Override public String getAreaName()
	{
		return "Downloader";
	}

	@Override public int getLineCount()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override public String getLine(int index)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override public boolean onEnvironmentEvent(EnvironmentEvent event)
	{
		NullCheck.notNull(event, "event");
		switch(event.getCode())
		{
		case EnvironmentEvent.CLOSE:
			actions.closeApp();
			return true;
		//case EnvironmentEvent.THREAD_SYNC:
		//	if (onThreadSyncEvent(event))
		default:
			return super.onEnvironmentEvent(event);
		}
	}
	
}