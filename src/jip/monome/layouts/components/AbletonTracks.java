package jip.monome.layouts.components;

import java.util.logging.Logger;

import javax.sound.midi.ShortMessage;

import jip.monome.layouts.Blinker;
import jip.monome.layouts.ClockListener;
import jip.monome.layouts.MidiManager;
import jip.monome.layouts.NoteListener;
import jip.monome.layouts.PatternRecorder;
import jip.monome.layouts.Recordable;

import sky.monome.LedButtonCouple.LedState;
import sky.monome.exception.MonomeException;
import sky.monome.frame.Frame;

public class AbletonTracks extends MidiButtonGroup implements NoteListener, ClockListener, Recordable {
	Logger log = Logger.getLogger(AbletonTracks.class.getName());
	int controlCol;
	int[] lastNoteOn;
	PatternRecorder[] prs;
	
	int STEPS = 32;

	Blinker blinker;
	MidiManager midi;
	
	public AbletonTracks(String name, int channel, int note, int x, int y,
			int width, int height) throws MonomeException {
		super(name, channel, note, x, y, width, height);	
	    
		controlCol = width - 1;
		lastNoteOn = new int[width - 1];

		for (int i=0;i<lastNoteOn.length;i++) lastNoteOn[i] = -1;
		if (height >= width)
			prs = new PatternRecorder[height - width + 1];
		
		for (int i = 0; i < prs.length; i++) prs[i] = new PatternRecorder(this, STEPS);
		
		for (int i=0;i<lastNoteOn.length;i++) lastNoteOn[i] = -1;
		
		blinker = new Blinker(this, 200);
		midi = MidiManager.getInstance();
	}

	@Override
	public void notify(boolean pressed, int x, int y) {
		
		if (x < controlCol) {
			if (pressed) {			
				press(x, y);
				for (PatternRecorder pr: prs)
					pr.buttonEvent(x, y);
			}
			else 
				release(x, y);
		}		
		else 
			if (pressed) pressControl(x, y);
	}

	// REcordable
	public void press (int xi, int yi){
		int onVal = getCC() + (height * xi + yi);
		midi.sendNoteOn(channel, onVal, 127);
		lastNoteOn[xi] = yi;
	}
	
	public void release (int xi, int yi){
		//send note offs
		int onVal = getCC() + (height * xi + yi);
		midi.sendNoteOff(channel, onVal);
	}
	
	public void pressControl(int xi, int yi){		
		if (yi < width - 1){
			int onVal = getCC() + (height * (width - 1)) + yi;
			midi.sendNoteOn(channel, onVal, 127);
			lastNoteOn[yi] = -1;
		}		
		else{
			PatternRecorder pr = prs[yi - (width - 1)];
			if (pr.getMode() == PatternRecorder.STOPPED){
				pr.setMode(PatternRecorder.CUED);
				blinker.blink(xi, yi);
			}
			else{
				pr.setMode(PatternRecorder.STOPPED);
				blinker.unblink(xi, yi);
			}
		}
			
	}

	@Override
	public void writeOn(Frame frame) throws MonomeException {
		for (int i=0; i<width - 1; i++){
			if (lastNoteOn[i]!= -1) {
				frame.set(i,lastNoteOn[i], LedState.ON);
				frame.set(controlCol, i, LedState.ON);
			}				
		}
		blinker.writeOn(frame);

		// pattern recorders
		for (int i = 0; i<prs.length; i++){
			PatternRecorder pr = prs[i];
			int mode = pr.getMode();
			if (mode == PatternRecorder.CUED || mode == PatternRecorder.RECORDING)
				blinker.blink(width - 1, width - 1 + i);
//			else 
				if (mode == PatternRecorder.PLAYING){
					blinker.unblink(controlCol, width - 1 + i);
					frame.set(controlCol, width - 1 + i, LedState.ON);	
				}
		}
	}

	@Override
	public String getName() {
		return "abletonTracks";
	}

	public void noteOnReceived(ShortMessage n) {
		int pitch = n.getData1();
		int vel = n.getData2();
		if (pitch >= cc1 && pitch < cc1 + ((width - 1) * height)){
			int xi = (pitch - cc1) / height;
			int yi = pitch - (cc1 + (height * xi));
			if (vel==127 || vel==1) {//Play or continue. Other values (0, 64:off; 126:cue)
				//log.info("unblink: xi " + xi + " yi: "+ lastNoteOn[xi]);
				//if (lastNoteOn[xi] != -1) blinker.unblink(xi, lastNoteOn[xi]);
				//if (yi != -1) blinker.unblink(xi, yi);
				lastNoteOn[xi] = yi;					
				refresh();
			}
			else if (vel == 126){
				//blinker.blink(xi, yi);			
			}
			//log.info("xi: " + xi + " yi: "+ yi);
		}
	}
	
	public void noteOffReceived(ShortMessage n) {
//		int pitch = n.getData1();
//		int vel = n.getData2();
//		int xi = (pitch - cc1) / height;
//		int yi = pitch - (cc1 + (height * xi));
//
//		if (vel == 0){
//			blinker.unblinkCol(xi);
//			lastNoteOn[xi] = -1;			
//		}
//		log.info("xi: " + xi + "yi: " + yi);

	}
	
	int RESOLUTION = 6; // 1/16
	int i = 0;
	public void timingClockReceived() {	
		i++;
		if (i == RESOLUTION){
			i = 0;
			step();			
		}
	}
	
	public void start(){
		i = 0;
		for (PatternRecorder pr: prs){
			pr.start();
			pr.step();
		}
	}
	
	public void stop(){
	}
	
	int stepc;
	// pattern recorder
	protected void step(){
		for (PatternRecorder pr: prs)
			pr.step();
	}
	
//	@Override
//	public void loadJDOMXMLElement(Element el) {
//		RESOLUTION = Integer.parseInt(el.getAttribute("resolution").getValue());
//		STEPS = Integer.parseInt(el.getAttribute("steps").getValue());
//	}
//
//	@Override
//	public Element toJDOMXMLElement(Element el) {
//		el.setAttribute("resolution", String.valueOf(RESOLUTION));
//		el.setAttribute("steps", String.valueOf(STEPS));
//		return el;
//	}

	
}
