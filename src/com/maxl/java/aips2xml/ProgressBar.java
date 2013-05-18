package com.maxl.java.aips2xml;

public class ProgressBar extends Thread {
	
	boolean show_progress = true;

	private String msg = "";

	public void init(String msg) {
		this.msg = msg;
	}
	
	public void run() {
		String anim = "|/-\\";
		int x = 0;
		while (show_progress) {
			System.out.print("\r" + msg + anim.charAt(x++ % anim.length()));
			try {
				Thread.sleep(100);
			} catch (Exception e) {
				//
			}				
		}
	}
	
	public void stopp() {
		show_progress = false;
	}
}
