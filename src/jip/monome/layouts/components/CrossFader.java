package jip.monome.layouts.components;

import java.awt.Rectangle;

import jip.monome.layouts.MidiManager;

import sky.monome.LedButtonCouple.LedState;
import sky.monome.exception.MonomeException;
import sky.monome.frame.Frame;
import sky.monome.util.OSCMessageDigester;


public class CrossFader extends Fader{

	public CrossFader(String name, int channel, int cc, int x, int y, int size) {
		super(name, channel, cc, x, y, size, false);		
	}
	
	public CrossFader(String name, int channel, int cc, int x, int y, int size, boolean inverted) {
		super(name, channel, cc, x, y, size);	
	}
	
	@Override
	public Rectangle getBounds() {
		return new Rectangle(x, y, size, 1);
	}

	@Override
	public void notify(OSCMessageDigester messageDigester)
			throws MonomeException {
		
		if(messageDigester.getInstruction().equals("/press")){
			int buttonx = messageDigester.getArgument(Integer.class,0);
			int buttony = messageDigester.getArgument(Integer.class,1);
			if(	messageDigester.getArgument(Integer.class,2).equals(1) && // pressed
				buttonx >= getAbsoluteX() && buttonx < getAbsoluteX() + size && 
				buttony == getAbsoluteY()){
			
				value = (int) Math.round(127.0f / (size - 1) * (inverted? size - 1 - (buttonx - getAbsoluteX()) : buttonx - getAbsoluteX()));
				MidiManager.getInstance().sendCC(channel, cc, value);
				}
		}
		refresh();
	}

	@Override
	public String toString() {
		return "XFader: " + name;
	}

	@Override
	public void writeOn(Frame frame) throws MonomeException {
		for (int i = 0; i< size; i++){
			if (value >= (int) Math.round((127.0f / (size - 1)) * (inverted? i : size - 1 - i)))
				frame.set(getAbsoluteX() + (inverted? i: size - 1 - i), getAbsoluteY(), LedState.ON);	
		}
	}

}
