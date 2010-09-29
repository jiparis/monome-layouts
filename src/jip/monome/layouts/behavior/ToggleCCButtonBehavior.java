package jip.monome.layouts.behavior;

import jip.monome.layouts.MidiManager;
import jip.monome.layouts.components.MidiButton;
import sky.monome.behavior.Toggle;
import sky.monome.event.button.ButtonEvent.ButtonAction;
import sky.monome.exception.MonomeException;

public class ToggleCCButtonBehavior extends Toggle {
	public void notify(ButtonAction buttonAction) throws MonomeException {
		super.notify(buttonAction);	
		MidiButton ccb = (MidiButton) getLedButtonCouple();
		if (buttonAction == ButtonAction.BUTTON_PUSHED){
			if(isOn())
				MidiManager.getInstance().sendCC(ccb.getChannel(), ccb.getNoteOrCC(), 127);
			else
				MidiManager.getInstance().sendCC(ccb.getChannel(), ccb.getNoteOrCC(), 0);
		}
	}	
}
