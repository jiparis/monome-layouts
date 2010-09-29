package jip.monome.layouts;

import java.util.Vector;

import jip.monome.layouts.components.AbletonTracks;

public class PatternRecorder {
	public static final int STOPPED = 1;
	public static final int RECORDING = 2;
	public static final int PLAYING = 4;
	public static final int CUED = 8;

	private int mode = STOPPED;
	private int nsteps = 32;
	
	private Vector<ButtonEvent>[] steps;
		
	private AbletonTracks co;
	
	@SuppressWarnings("unchecked")
	public PatternRecorder(AbletonTracks obj, int nsteps){
		this.co = obj;
		this.nsteps = nsteps;
		this.steps = new Vector[nsteps];
		for (int i = 0; i < nsteps; i++)
			steps[i] = new Vector<ButtonEvent>();
	}
	
	public void setMode(int mode){
		this.mode = mode;{
		if (mode == CUED)
			for (int i = 0; i < nsteps; i++)
				steps[i].clear();
		}
	}
	
	public int getMode(){
		return mode;
	}
	
	int counter = 0;
	public void step(){
		switch (mode){
		case PLAYING:		
			if (mode == PLAYING){
				Vector<ButtonEvent> events = steps[counter]; 
				for(ButtonEvent ev: events){
					//System.out.println("sending " + counter);
					co.press(ev.xi, ev.yi);
				}
			}
			counter ++;
			if (counter == nsteps) counter = 0;
			break;
		case RECORDING:
			counter ++;
			if (counter == nsteps) {
				counter = 0;
				mode = PLAYING;
			}
		}
	}
	
	public void start(){
		counter = 0;
	}
	
	public void buttonEvent(int xi, int yi){
	    switch(mode){
	    case CUED:
	    	counter = 0;
	    	
	    	mode = RECORDING;
	    case RECORDING:
			// begin or continue recording
			steps[counter].add(new ButtonEvent(xi, yi));		
	    }		
	}
	
	public class ButtonEvent{
		public int xi, yi;
		public ButtonEvent(int xi, int yi){
			this.xi = xi; this.yi = yi;
		}
	}
}
