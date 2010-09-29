package jip.monome.layouts;

import javax.sound.midi.ShortMessage;

public interface CCListener {
	public void controllerChangeReceived(ShortMessage m);
}
