package jip.monome.layouts.components;

import java.awt.Rectangle;

import jip.monome.layouts.MidiManager;

import sky.monome.Container;
import sky.monome.Monome;
import sky.monome.exception.MonomeException;
import sky.monome.frame.Frame;
import sky.monome.util.OSCMessageDigester;

public abstract class MidiButtonGroup extends Container{
	int channel, cc1, cc2;
	int value1, value2;
	
	public MidiButtonGroup(String name, int channel, int cc, int x, int y, int width, int height) throws MonomeException{
		this(name, channel, cc, 0, x, y, width, height);
	}
	
	public MidiButtonGroup(String name, int channel, int cc1, int cc2, int x, int y, int width, int height) throws MonomeException {
		super(name, x, y, width, height);
		
		this.channel = channel;
		this.cc1 = cc1;
		this.cc2 = cc2;
		MidiManager.getInstance().plug(channel, this);
		
	}
	
	@Override
	public void notify(OSCMessageDigester messageDigester)
			throws MonomeException {		
		
		if(messageDigester.getInstruction().equals("/press")){
			int buttonx = messageDigester.getArgument(Integer.class,0);
			int buttony = messageDigester.getArgument(Integer.class,1);
			if(	getBounds().contains(buttonx, buttony)){
				boolean pressed = messageDigester.getArgument(Integer.class,2).equals(1);
				notify(pressed, buttonx - getAbsoluteX(), buttony - getAbsoluteY());
			}
		}
	}	
	
	public abstract void notify(boolean pressed, int x, int y);

	public int getChannel(){
		return channel;
	}
	
	public int getCC(){
		return cc1;
	}
	
	public int getCC2(){
		return cc2;
	}
	
	public int getValue(){
		return value1;
	}
	
	public int getValue2(){
		return value2;
	}
	
	public void setValue(int v){
		this.value1 = v;
	}
	
	public void setValue2(int v){
		this.value2 = v;
	}
	
		@Override
	public boolean canHaveParentContainer() {
		return true;
	}

	@Override
	public int getAbsoluteX() {
		
		return x + ((container!=null)? container.getAbsoluteX() : 0);
	}

	@Override
	public int getAbsoluteY() {
		return y + ((container!=null)? container.getAbsoluteY() : 0);

	}

	@Override
	public Rectangle getBounds() {
		return new Rectangle(x, y, width, height);
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
	public String toString() {
		return getName();
	}

	@Override
	public void writeOn(Frame frame) throws MonomeException {
		// TODO Auto-generated method stub
	}
	
	public void refresh(){
		if (isVisible())
			try {
				getMonome().refresh();
			} catch (MonomeException e) {				
			}
	}

}
