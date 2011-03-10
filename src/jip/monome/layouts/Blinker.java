package jip.monome.layouts;

import java.awt.Rectangle;

import sky.monome.Component;
import sky.monome.LedButtonCouple.LedState;
import sky.monome.frame.Frame;

public class Blinker {
	
	Thread thread;
	boolean blinkState = false;
	final Component owner;
	final long delay;
	boolean[][] buttonStates;
	int width, height;
	
	public Blinker(final Component component, long msec){
		this.owner = component;
		delay = msec;
		Rectangle bounds = component.getBounds();
		width = bounds.width;
		height = bounds.height;
		buttonStates = new boolean[width][height];
	
		thread = new Thread(new Runnable(){
	
			public void run() {
				while(true){
					try {
						blinkState = !blinkState;
						if (owner.isVisible()) owner.getMonome().refresh();
						Thread.sleep(delay);
					} catch (Exception e) {
						return;
					}
				}
				
			}
		
		});
		thread.start();
	}
	
	public void blink (int x, int y){
		buttonStates[x][y] = true;
	}
	
	public void unblink (int x, int y){
		buttonStates[x][y] = false;
	}
	
	public void unblinkCol(int x){
		for (int i = width - 1; i >= 0; i--)
			buttonStates[i] = new boolean[height];
	}
	
	public void writeOn(Frame frame){
		for (int i = width - 1; i >= 0; i--){
			for (int j = height - 1; j >= 0 ; j--){
				if (buttonStates[i][j]) frame.set(i, j, blinkState? LedState.ON : LedState.OFF);
			}
		}
	}
}

