package jip.monome.layouts.components;

import javax.sound.midi.ShortMessage;

import jip.monome.layouts.CCListener;

import sky.monome.behavior.Behavior;
import sky.monome.exception.MonomeException;


public class CCButton extends MidiButton implements CCListener {
	
	public CCButton(String name, int channel, int noteOrCC, int x, int y,
			Behavior b) throws MonomeException {
		super(name, channel, noteOrCC, x, y, b);
	}

	public void controllerChangeReceived(ShortMessage m) {
		if (m.getData1() == noteOrCC) {
			try {
				if (m.getData2()>=64)						
					setLedState(LedState.ON, false);				
				else
					setLedState(LedState.OFF, false);
				
				refresh();
			} catch (MonomeException e) {
				e.printStackTrace();
			}
		}
	}

	
}
