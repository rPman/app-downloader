Usage example:

		Download d=new Download();
		d.start(new URL("http://example.com"),new Event()
		{
			
			@Override public void progress(Long len)
			{
				System.out.println("progress: "+len);
			}
			
			@Override public void fileInfo()
			{
				System.out.println("fileinfo: ");
			}
			
			@Override public void changedState(State state,String error)
			{
				System.out.println("state: "+state.name()+", error:"+error);
				if(state==State.FINISHED) System.exit(0);
			}
		});
		Thread.sleep(10000);
