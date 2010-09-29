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
	public void notify(boolean pressed, int x, int y){
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
			int valuex = 127 * coordx / (getWidth() - 1);
			MidiManager.getInstance().sendCC(getChannel(), getCC(), valuex);
		}
		
		if (coordy != oldy){// Y axis is inverted
			int valuey = 127 * (getHeight() - coordy - 1) / (getHeight() - 1);
			MidiManager.getInstance().sendCC(getChannel(), getCC2(), valuey);
		}
	}		

	public void controllerChangeReceived(ShortMessage m) {
		int ccin = m.getData1();
		if (ccin == cc1)
			value1 = m.getData2();
		if (ccin == cc2)
			value2 = m.getData2();
		coordx = getValue() * (getWidth() - 1)/ 127;
		coordy = getHeight() - (getValue2() * (getHeight() - 1) / 127) - 1;
		
		refresh();		
	}	

	@Override
	public void writeOn(Frame frame) throws MonomeException {
		for (int i = 0; i < getWidth(); i++)
			for (int j = 0; j< getHeight(); j++)
				frame.set(i, j, (i == coordx || j == coordy)? LedState.ON: LedState.OFF);
	}
	
	@Override
	public String toString() {
		return "XY[" + getName() + "]";
	}

}
