package jip.monome.layouts.components;

import java.util.logging.Logger;

import javax.sound.midi.ShortMessage;

import jip.monome.layouts.CCListener;
import jip.monome.layouts.MidiManager;

import sky.monome.LedButtonCouple.LedState;
import sky.monome.exception.MonomeException;
import sky.monome.frame.Frame;

public class XYFader extends MidiButtonGroup implements CCListener{
	Logger log = Logger.getLogger(XYFader.class.getName());

	int coordx, coordy;
	int oldx, oldy;
	
	public XYFader(String name, int channel, int cc1, int cc2, int x, int y,
			int width, int height) throws MonomeException {
		super(name, channel, cc1, cc2, x, y, width, height);
		coordx = 0;
		coordy = height - 1;
	}	

	@Override
	public void press(int x, int y, boolean pressed){
		if (pressed){
			coordx = x;
			coordy = y;
			sendMidi();		
			refresh();	
			oldx = coordx;
			oldy = coordy;
		}		
		
	}
	
	public void sendMidi(){

		if (coordx != oldx){
			//int valuex = 127 * coordx / (getWidth() - 1);
			value1 = (int) Math.round(127.0f / (getWidth() - 1) * coordx);
			MidiManager.getInstance().sendCC(getChannel(), getCC(), value1);
		}
		
		if (coordy != oldy){// Y axis is inverted
			//int valuey = 127 * (getHeight() - coordy - 1) / (getHeight() - 1);
			value2 = (int) Math.round(127.0f / (getHeight() - 1) * (getHeight() - 1 - coordy));
			MidiManager.getInstance().sendCC(getChannel(), getCC2(), value2);
		}
	}		

	public void controllerChangeReceived(ShortMessage m) {
		int ccin = m.getData1();
		if (ccin == cc1)
			value1 = m.getData2();
		if (ccin == cc2)
			value2 = m.getData2();
		coordx = Math.round(getValue() * (getWidth() - 1)/ 127.0f);
		//coordx = Math.round((127.0f / (getWidth() - 1)) / getValue());
		coordy = Math.round(getHeight() - (getValue2() * (getHeight() - 1) / 127.0f) - 1);
		
		refresh();		
	}	

	@Override
	public void writeOn(Frame frame) throws MonomeException {
		for (int i = 0; i < getWidth(); i++)
			for (int j = 0; j< getHeight(); j++)
				frame.set(i + getAbsoluteX(), j + getAbsoluteY(), (i == coordx || j == coordy)? LedState.ON: LedState.OFF);
	}
	
	@Override
	public String toString() {
		return "XY[" + getName() + "]";
	}

}
