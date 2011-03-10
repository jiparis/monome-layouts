package jip.monome.layouts;

import javax.sound.midi.ShortMessage;

public interface NoteListener {
	public void noteOnReceived(ShortMessage n) ;
	public void noteOffReceived(ShortMessage n) ;
}
