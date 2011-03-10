package jip.monome.layouts.components;

import jip.monome.layouts.MidiManager;
import sky.monome.LedButtonCouple;
import sky.monome.behavior.Behavior;
import sky.monome.exception.MonomeException;

public abstract class MidiButton extends LedButtonCouple{
	
	MidiManager midi;
	int channel, noteOrCC;
	public MidiButton(String name, int channel, int noteOrCC, int x,
			int y, Behavior b) throws MonomeException{
		
		super(name, x, y, b);
		
		midi = MidiManager.getInstance();
		midi.plug(channel, this);
		
		this.channel = channel;
		this.noteOrCC = noteOrCC;		
	}
	
	public int getChannel(){
		return channel;
	}

	public int getNoteOrCC(){
		return noteOrCC;
	}	
	
	public void refresh(){
		if (isVisible())
			try {
				getMonome().refresh();
			} catch (MonomeException e) {				
			}
	}
}
