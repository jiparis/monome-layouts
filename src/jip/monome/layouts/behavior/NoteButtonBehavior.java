/**
 * 
 */
package jip.monome.layouts.behavior;

import jip.monome.layouts.MidiManager;
import jip.monome.layouts.components.MidiButton;
import sky.monome.behavior.LightOnPush;
import sky.monome.event.button.ButtonEvent.ButtonAction;
import sky.monome.exception.MonomeException;

public class NoteButtonBehavior extends LightOnPush{
	public void notify(ButtonAction buttonAction) throws MonomeException {
		super.notify(buttonAction);	
		MidiButton nb = (MidiButton) getLedButtonCouple();
		if (buttonAction == ButtonAction.BUTTON_PUSHED)
			MidiManager.getInstance().sendNoteOn(nb.getChannel(), nb.getNoteOrCC(), 127);
		else
			MidiManager.getInstance().sendNoteOff(nb.getChannel(), nb.getNoteOrCC());
	}		
}