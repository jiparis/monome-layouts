package jip.monome.layouts.components;

import java.util.logging.Logger;

import javax.sound.midi.ShortMessage;

import jip.monome.layouts.CCListener;
import jip.monome.layouts.MidiManager;

import sky.monome.LedButtonCouple.LedState;
import sky.monome.exception.MonomeException;
import sky.monome.frame.Frame;


public class Fader extends MidiButtonGroup implements CCListener{
	Logger log = Logger.getLogger(Fader.class.getName());

	int size;
	boolean inverted = false;
	
	public Fader(String name, int channel, int cc, int x, int y, int size) throws MonomeException {
		this(name, channel, cc, x, y, size, false);		
	}
	
	public Fader(String name, int channel, int cc, int x, int y, int size, boolean inverted) throws MonomeException {
		super(name, channel, cc, x, y, 1, size);
		this.size = size;
		this.inverted = inverted;
	}
	

	@Override
	public void press(int x, int y, boolean pressed){		
		if(pressed){	
				setValue((int) Math.round(127.0f / (size - 1) * (inverted? y: size - 1 - y)));
				MidiManager.getInstance().sendCC(channel, getCC(), getValue());
			}
		
		refresh();
	}

	@Override
	public void writeOn(Frame frame) throws MonomeException {
		for (int i = 0; i< size; i++){
			if (getValue() >= (int) Math.round((127.0f / (size - 1)) * (inverted? i : size - 1 - i)))
				frame.set(getAbsoluteX(), getAbsoluteY() + (inverted? size - 1 - i: i ), LedState.ON);	
		}
	}

	// CCListener
	public void controllerChangeReceived(ShortMessage m) {
		if (m.getData1() == getCC()){
			setValue(m.getData2());			
			refresh();			
		}
		
	}
}
