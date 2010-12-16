package jip.monome.layouts.components;

import java.awt.Rectangle;
import java.util.logging.Logger;

import javax.sound.midi.ShortMessage;

import jip.monome.layouts.CCListener;
import jip.monome.layouts.MidiManager;

import sky.monome.Component;
import sky.monome.Monome;
import sky.monome.LedButtonCouple.LedState;
import sky.monome.exception.MonomeException;
import sky.monome.frame.Frame;
import sky.monome.util.OSCMessageDigester;


public class Fader extends Component implements CCListener{
	Logger log = Logger.getLogger(Fader.class.getName());

	int size, channel, cc, value;
	boolean inverted = false;
	
	public Fader(String name, int channel, int cc, int x, int y, int size) {
		this(name, channel, cc, x, y, size, false);		
	}
	
	public Fader(String name, int channel, int cc, int x, int y, int size, boolean inverted) {
		super(name, x, y);
		this.size = size;
		this.channel = channel;
		this.cc = cc;
		this.inverted = inverted;
		MidiManager.getInstance().plug(channel, this);	
	}
	
	

	@Override
	public boolean canHaveParentContainer() {
		return true;
	}

	@Override
	public int getAbsoluteX() {
		return container.getAbsoluteX() + x;
	}

	@Override
	public int getAbsoluteY() {
		return container.getAbsoluteY() + y;
	}

	@Override
	public Rectangle getBounds() {
		return new Rectangle(x, y, 1, size);
	}

	@Override
	public Monome getMonome() {
		return container.getMonome();
	}

	@Override
	public boolean isVisible() {
		return container.isVisible();
	}

	@Override
	public void notify(OSCMessageDigester messageDigester)
			throws MonomeException {
		
		if(messageDigester.getInstruction().equals("/press")){	
			int buttonx = messageDigester.getArgument(Integer.class,0);
			int buttony = messageDigester.getArgument(Integer.class,1);
			if(	messageDigester.getArgument(Integer.class,2).equals(1) && // pressed
				buttonx == getAbsoluteX() &&
				buttony >= getAbsoluteY() && buttony < getAbsoluteY() + size){
			
				value = (int) Math.round(127.0f / (size - 1) * (inverted? buttony - getAbsoluteY(): size - 1 - (buttony - getAbsoluteY())));
				MidiManager.getInstance().sendCC(channel, cc, value);
			}
		}
		refresh();
	}

	@Override
	public String toString() {
		return "Fader: " + name;
	}

	@Override
	public void writeOn(Frame frame) throws MonomeException {
		for (int i = 0; i< size; i++){
			if (value >= (int) Math.round((127.0f / (size - 1)) * (inverted? i : size - 1 - i)))
				frame.set(getAbsoluteX(), getAbsoluteY() + (inverted? size - 1 - i: i ), LedState.ON);	
		}
	}

	// CCListener
	public void controllerChangeReceived(ShortMessage m) {
		if (m.getData1() == cc){
			value = m.getData2();
			
			refresh();			
		}
		
	}
	
	public void refresh(){
		if (isVisible())
			try {
				getMonome().refresh();
			} catch (MonomeException e) {				
			}
	}
}
