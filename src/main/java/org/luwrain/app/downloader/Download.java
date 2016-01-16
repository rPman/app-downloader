package org.luwrain.app.downloader;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

// each object represent single file download
public class Download implements Runnable
{
	final static public int STREAM_COPY_BUF_SIZE=1024*128; // 128kb bufer size for progress event
	final static public int RETRY_DELAY=1000*5; // milliseconds betwin reconnect
	// you can change this before any start only
	public static File defaultDownloadDir=new File(".");
	public static int defaultConnectTimeout=0;
	public static int defaultReadTimeout=0;
	public static int maxRetryCount=3;

	public enum State
	{
		READY,ERROR,DOWNLOADING,PAUSED,FINISHED
	}
	private State state=State.READY;
	private String error="";
	// file output stream, if null replaced by new FileOutputStream by fileName and defaultDownloadDir
	private OutputStream fileStream=null;
	// file name and size, got from stream, but fileName can be declared before start
	private String fileName=null;
	private Long fileSize=null;
	private Long downloadedSize=null;
	private String fileType=null;
	private URL url=null;
	public Event eventConsumer=null;
	private Thread thread=null;
	
	private URLConnection con;
	private InputStream connectionStream; 
	private Integer retryCount=0;
	private boolean acceptRangesSupported;
	
	public State getState()
	{
		synchronized(this)
		{
			return state;
		}
	}
	public void setStateError(State state,String error)
	{
		synchronized(this)
		{
			this.state=state;
		}
		synchronized(this)
		{
			this.error=error;
		}
		if(eventConsumer!=null) eventConsumer.changedState(state,error);
	}
	public int getRetryCount()
	{
		synchronized(this)
		{
			return retryCount;
		}
	}
	public void setRetryCount(int retryCount)
	{
		synchronized(this)
		{
			this.retryCount=retryCount;
		}
	}
	public String getError()
	{
		synchronized(this)
		{
			return error;
		}
	}
	public String getFileName()
	{
		synchronized(this)
		{
			return fileName;
		}
	}
	public void setFileName(String fileName)
	{
		synchronized(this)
		{
			this.fileName=fileName;
		}
	}
	public Long getFileSize()
	{
		synchronized(this)
		{
			return fileSize;
		}
	}
	public void setFileSize(Long fileSize)
	{
		synchronized(this)
		{
			this.fileSize=fileSize;
		}
	}
	public Long getDownloadedSize()
	{
		synchronized(this)
		{
			return downloadedSize;
		}
	}
	public void addDownloadedSize(int addSize)
	{
		synchronized(this)
		{
			this.downloadedSize+=addSize;
		}
		if(eventConsumer!=null) eventConsumer.progress(this.downloadedSize);
	}
	public void setDownloadedSize(Long downloadedSize)
	{
		synchronized(this)
		{
			this.downloadedSize=downloadedSize;
		}
		if(eventConsumer!=null) eventConsumer.progress(downloadedSize);
	}
	public String getFileType()
	{
		synchronized(this)
		{
			return fileType;
		}
	}
	public void setFileType(String fileType)
	{
		synchronized(this)
		{
			this.fileType=fileType;
		}
	}
	// WARNING: fileStream, url and eventConsumer must not be read after start() and not syncronised  
	public void setFileStream(OutputStream fileStream)
	{
		this.fileStream=fileStream;
	}
	public void setUrl(URL url)
	{
		this.url=url;
	}
	public void setEventConsumer(Event eventConsumer)
	{
		this.eventConsumer=eventConsumer;
	}

	public void start(URL url)
	{
		this.url=url;
		start();
	}
	public void start(URL url,Event consumer)
	{
		this.eventConsumer=consumer;
		this.url=url;
		start();
	}
	public void start()
	{
		if(thread!=null) return;
		if(url==null)
		{
			error="Empty url";
			state=State.ERROR;
			return;
		}
		
		thread=new Thread(this);
		// we need automaticaly stop all threads on main program exit
		thread.setDaemon(true);
		thread.start();
			
	}
	public void stop()
	{
		thread.interrupt();
	}

	private static class ProgressListener implements ActionListener
	{
		@Override public void actionPerformed(ActionEvent e)
		{
			System.out.println(e.getClass().getName()+": "+e);
		}
	}

	private void reconnect(long seek) throws IOException
	{
		con=url.openConnection();
		if(seek>0) con.setRequestProperty("Range", "bytes="+seek+"-");
		//con.setDoOutput(true);
		//con.setDoInput(true);
		con.setConnectTimeout(defaultConnectTimeout);
		con.setReadTimeout(defaultReadTimeout);
		// folow redirect
		/*
		if(con instanceof HttpURLConnection)
		{
			((HttpURLConnection)con).setInstanceFollowRedirects(true);
		} else if(con instanceof HttpsURLConnection)
		{
			((HttpsURLConnection)con).setInstanceFollowRedirects(true);
		} else if(con instanceof FtpURLConnection)
		{
			//((FtpURLConnection)con).
		}
		*/
		// check support for resume download
		//String acceptRanges=con.getHeaderField("Accept-Ranges");
		acceptRangesSupported=true;//(acceptRanges!=null&&acceptRanges.equals("bytes"));
		connectionStream = con.getInputStream();
	}
	
	@Override public void run()
	{
		System.setProperty("http.keepAlive", "false");
		boolean streamNeedToClose=false;
		connectionStream = null;
		try
		{
			setStateError(State.DOWNLOADING,"");
			
			setRetryCount(0);
			while(retryCount<maxRetryCount)
			{
				try
				{
					reconnect(0);
					break;
				}
				catch(IOException e)
				{
					System.out.println("Retry: "+e.getMessage());
					retryCount++;
					Thread.sleep(RETRY_DELAY);
				}
			}
			//String lastModified = con.getHeaderField("Last-Modified");
			//System.out.println("lastModified="+lastModified);
			if(retryCount>=maxRetryCount)
			{
				setStateError(State.ERROR,"Maximum retry count reached");
				return;
			}
			// try to get file type, size from http header
			// mime type
			String ft=con.getHeaderField("Content-Type");
			if(ft!=null&&!ft.isEmpty()) setFileType(ft);
			// file size, but may not equal real size
			String size=con.getHeaderField("Content-Length");
			if(size!=null) setFileSize(Long.valueOf(size));
			// file name
			if(getFileName()==null)
			{ // try to get file name from content-disposition
				// "attachment; filename=myfile.zip"
				String cd=con.getHeaderField("Content-Disposition");
				if(cd==null) cd="";
				String fn=cd.replaceFirst("(?i)^.*filename=\"([^\"]+)\".*$", "$1"); // FIXME: support for urlencoded filenames, for example cyrylic
				// security issue, replace .. \ and / for _
				fn=fn.replaceAll("\\.{2,}|[/\\\\]","_");
				//
				if(!fn.isEmpty()&&isFilenameValid(fn))
					setFileName(fn);
			}
			if(getFileName()==null)
			{
				String fn=getFileNameFromUrl(url);
				if(!isFilenameValid(fn))
				{ // not valid filename, try to generate
					fn=fn.replaceAll("[^a-zA-Z0-9\\.\\-\\\\\\/]","");
				}
				// check for empty name (replaced invalid characters or have no file name anywhere
				if(!fn.isEmpty())
					setFileName(fn);
			}
			// we have file info
			if(eventConsumer!=null) eventConsumer.fileInfo();
			// if saveFile is null, we use stream
			File saveFile=null;
			if(fileStream==null)
			{
				if(getFileName()==null)
				{ // can'not find file name anywhere, make it by index
					String ext="unknown";
					// FIXMEL you can use MimeTypes from https://tika.apache.org/0.10/detection.html
					String fn="lwrn"+url.toString().hashCode()+"."+ext;
					setFileName(fn);
				}
				// create file name (if accept range, use resume)
				saveFile=new File(defaultDownloadDir,getFileName());
				try
				{
					if(saveFile.exists())
					{
						if(saveFile.length()>=getFileSize())
						{
							setStateError(State.FINISHED,"");
							return;
						}
					}
					if(saveFile.exists()&&acceptRangesSupported)
					{ // file exist, we need to resume
						fileStream= new FileOutputStream(saveFile,true);
						//con.setRequestProperty("If-Range", lastModified);
						reconnect(saveFile.length());
						//con.setRequestProperty("Range", "bytes="+saveFile.length()+"-");
					} else
					{ // file not exist, create new
						fileStream= new FileOutputStream(saveFile);
					}
				} catch(FileNotFoundException e)
				{
					setStateError(State.ERROR,"Can't create file: "+e.getMessage());
					return;
				}
				// we need close file finally
				streamNeedToClose=true;
			}
			
			setDownloadedSize(new Long(0));
			
			byte[] buffer = new byte[STREAM_COPY_BUF_SIZE];
			boolean needReconnect=false;
			while (true)
			{
				int len=0;
				try
				{
					if(needReconnect)
					{
						if(acceptRangesSupported)
						{
							reconnect(getDownloadedSize());
						} else
						{ // need to resume, but server not support it
							// we need to recreate file
							fileStream.close();
							fileStream= new FileOutputStream(saveFile,true);
							// 
							reconnect(0);
						}
					}
					len=connectionStream.read(buffer);
				}
				catch(IOException e)
				{
					System.out.println("Retry: "+e.getMessage());
					retryCount++;
					if(retryCount>=maxRetryCount)
					{
						setStateError(State.ERROR,"Maximum retry count reached");
						return;
					}
					needReconnect=true;
					continue;
				}
				if(len==-1) break;
				fileStream.write(buffer, 0, len);
				addDownloadedSize(len);
				if (Thread.interrupted())
					throw new InterruptedException();
			}
			setStateError(State.FINISHED,"");
			return;
		}
		catch(InterruptedException e)
		{
			setStateError(State.ERROR,"Interrupted by user");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if(connectionStream!=null)
					connectionStream.close();
				if(streamNeedToClose&&fileStream!=null)
					fileStream.close();
			} catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	public static boolean isFilenameValid(String file)
	{
		File f = new File(file);
		try
		{
			f.getCanonicalFile().getName().equals(file);
			return true;
		} catch (IOException e)
		{
			return false;
		}
	}
	/**
	 * This function will take an URL as input and return the file name.
	 * <p>Examples :</p>
	 * <ul>
	 * <li>http://example.com/a/b/c/test.txt -> test.txt</li>
	 * <li>http://example.com/ -> an empty string </li>
	 * <li>http://example.com/test.txt?param=value -> test.txt</li>
	 * <li>http://example.com/test.txt#anchor -> test.txt</li>
	 * </ul>
	 * 
	 * @param url The input URL
	 * @return The URL file name
	 */
	public static String getFileNameFromUrl(URL url)
	{

	    String urlString = url.getFile();
	    return urlString.substring(urlString.lastIndexOf('/') + 1).split("\\?")[0].split("#")[0];
	}
}
