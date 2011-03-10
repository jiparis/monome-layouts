package jip.monome.layouts.components;

import javax.sound.midi.ShortMessage;

import jip.monome.layouts.NoteListener;

import sky.monome.behavior.Behavior;
import sky.monome.exception.MonomeException;


public class NoteButton extends MidiButton implements NoteListener {

	
	public NoteButton(String name, int channel, int noteOrCC, int x, int y,
			Behavior b) throws MonomeException {
		super(name, channel, noteOrCC, x, y, b);
	}

	public void noteOffReceived(ShortMessage n) {
		if (n.getData1() == noteOrCC) {
			try {
				setLedState(LedState.OFF, false);
				refresh();
			} catch (MonomeException e) {
				e.printStackTrace();
			}
		}		
	}

	public void noteOnReceived(ShortMessage n) {
		if (n.getData1() == noteOrCC) {
			try {
				setLedState(LedState.ON, false);
				refresh();
			} catch (MonomeException e) {
				e.printStackTrace();
			}
		}				
	}

	
}
