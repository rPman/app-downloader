package org.luwrain.app.downloader;

import org.luwrain.app.downloader.Download.State;

public interface Event
{
	// called for each received bytes block size STREAM_COPY_BUF_SIZE)
	void progress(Long len);
	// called for each state changes
	void changedState(State state,String error);
	// called when file info was received (mime type, file name and file size,..)
	void fileInfo();
}
