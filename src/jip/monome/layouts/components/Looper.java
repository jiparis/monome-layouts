package jip.monome.layouts.components;

import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

import jip.monome.layouts.Blinker;
import jip.monome.layouts.ClockListener;
import jip.monome.layouts.MidiManager;
import jip.monome.layouts.PatternRecorder;
import jip.monome.layouts.Recordable;

import sky.monome.LedButtonCouple.LedState;
import sky.monome.exception.MonomeException;
import sky.monome.frame.Frame;

public class Looper extends MidiButtonGroup implements ClockListener, Recordable{
	Logger log = Logger.getLogger(Looper.class.getName());
	public static final int NOT_SET = -1;
	
	private Loop[] loops;
	
	private final static int OFFSET_START_CTRL = 96;

	boolean gateLoopChokes = true;
	private boolean muteNotes = false;

	public boolean[] stopLoopsOnNextStep;
	
	int controlCol;
	int resolution = 12; // 1/8
	
	public static final int C3 = 60;
	public static final int F7 = 113;

	PatternRecorder[] prs;
	Blinker blinker;

	public Looper(String name, Map args, int midiChannel, int ccValue, int positionX,
			int positionY, int sizeX, int sizeY) throws MonomeException {
		super(name, midiChannel, ccValue, positionX, positionY, sizeX, sizeY);

		stopLoopsOnNextStep = new boolean[sizeX];
		
		loops = new Loop[sizeX - 1];
		for (int i = 0; i < loops.length; i++) {
			loops[i] = new Loop();
		}
		controlCol = sizeX - 1;
		blinker = new Blinker(this, 200);
		
		// Parameters
		String param = (String) args.get("res");
		if (param!=null){
			if ("1".equals(param))
				resolution = 96;
			else if ("1/2".equals(param))
				resolution = 48;
			else if ("1/4".equals(param))
				resolution = 24;
			else if ("1/8".equals(param))
				resolution = 12;
			else if ("1/16".equals(param))
				resolution = 6;				
		}
		
		param = (String) args.get("gate");
		if (param!=null){
			if ("yes".equals(param))
				gateLoopChokes = true;
			else if ("no".equals(param))
				gateLoopChokes = false;
		}
		
		ArrayList loopConfigs = (ArrayList) args.get("loops");
		for(int i = 0; i < loops.length && i < loopConfigs.size() ; i++){
			Map config = (Map) loopConfigs.get(i);
			int length = ((Number) config.get("length")).intValue();
			int group = ((Number) config.get("group")).intValue();
			String type = (String) config.get("type");
			loops[i].setLength(Float.valueOf(length));
			loops[i].setChokeGroup(group);
			if ("loop".equals(type))
				loops[i].setType(Loop.LOOP);
			else if ("step".equals(type))
				loops[i].setType(Loop.STEP);
			else if ("shot".equals(type))
				loops[i].setType(Loop.SHOT);
			else if ("momentary".equals(type))
				loops[i].setType(Loop.MOMENTARY);
			else if ("hit".equals(type))
				loops[i].setType(Loop.HIT);
			else if ("slice".equals(type))
				loops[i].setType(Loop.SLICE);		
		}
		
		// Pattern recorders
		if (height >= width)
			prs = new PatternRecorder[height - width + 1];
		for (int i = 0; i < prs.length; i++) prs[i] = new PatternRecorder(this, 32);
	}

	@Override
	public void writeOn(Frame frame) throws MonomeException {		
		for (int i = 0; i < loops.length; i++) {
			if (loops[i].isPlaying())
				frame.set(i,  loops[i].getStep(), LedState.ON);
			frame.set(controlCol, i, loops[i].isPlaying() ? LedState.ON: LedState.OFF);
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
	public void press(int x, int y, boolean pressed) {			
		if (pressed) {
			pressLooper(x, y);
		}
		else 
			if (x < controlCol) release(x, y);
	}
	
	//ClockListener
	int counter = 0;
	public void timingClockReceived() {	
		if (counter == 0) step();
		counter++;
		if (counter == resolution){
			counter = 0;						
		}
	}
	
	public void start() {
		for (PatternRecorder pr: prs){
			pr.start();
			pr.step();
		}	
	}

	public void stop() {
		counter = 0;		
	}
	
	// Recorder
	
	public void press(int x, int y){
		pressDisplay(x,y);		
	}
	// SevenUp
	
	public void release(int x, int y)
	{
		if (loops[x].isPlaying() && loops[x].getType() == Loop.MOMENTARY && loops[x].getLastTriggedStep() == y) {
			stopLoop(x);
		}
	}
	
	public void pressLooper(int x, int y)
	{
	  
		if(x == controlCol)
		{
			pressNavCol(x, y);			
		}
		else{
			pressDisplay(x,y);
			for (PatternRecorder pr: prs)
				pr.buttonEvent(x, y);
		}
		
		//updateNavGrid(); // @TODO clloyd not needed, done in play and stop functions
	}
	
	private void pressNavCol(int x, int y)
	{
		//Inverse the mode of the corresponding loop
		if (y<loops.length){
			if(loops[y].isPlaying())
			{
				loops[y].stop();
				MidiManager.getInstance().sendNoteOff(getChannel(), Looper.C3+y);
			}
			else
			{
				playLoop(y, 0);
			}
		}
		else{
			PatternRecorder pr = prs[y - (width - 1)];
			if (pr.getMode() == PatternRecorder.STOPPED){
				pr.setMode(PatternRecorder.CUED);
				blinker.blink(x, y);
			}
			else{
				pr.setMode(PatternRecorder.STOPPED);
				blinker.unblink(x, y);
			}
		}
	}
	
	private void pressDisplay(int x, int y)
	{
			//Choke loops in the same choke group
			int curChokeGroup = loops[x].getChokeGroup();
			if(curChokeGroup > -1)
			{
				for(int i=0; i<loops.length;i++)
					if(loops[i].getChokeGroup() == curChokeGroup && i != x)
					{
						if(gateLoopChokes)
							stopLoop(i);
						else
							stopLoopsOnNextStep[i] = true;
					}
			}
			
			stopLoopsOnNextStep[x] = false;
			int loopCtrlValue = (y * 16);
			MidiManager.getInstance().sendCC(getChannel(), OFFSET_START_CTRL+x, loopCtrlValue);
			playLoop(x, y);
			
	}
	
	

	public int getNumLoops()
	{
		return loops.length;
	}
	
	public Loop getLoop(int index)
	{
		return loops[index];
	}
	
	public boolean isLoopPlaying(int loopNum)
	{
		return loops[loopNum].isPlaying();
	}
	
	public void stopLoop(int loopNum)
	{
		loops[loopNum].stop();
		refresh();
		if (loops[loopNum].getType() != Loop.HIT)
			MidiManager.getInstance().sendNoteOff(getChannel(), Looper.C3+loopNum);
	}
	
	public void setLoopStopOnNextStep(int loopNum)
	{
		stopLoopsOnNextStep[loopNum] = true;
	}
	
	public void playLoop(int loopNum, int step)
	{
		loops[loopNum].setTrigger(step, true);
		loops[loopNum].setStep(step);
		loops[loopNum].setPressedRow(step);
		refresh();		
	}
		

	public void step()
	{
	
		for(int i=0; i<loops.length; i++)
		{
			if(stopLoopsOnNextStep[i])
			{
				stopLoop(i);
				stopLoopsOnNextStep[i] = false;
			}
			
			stepOneLoop(i);
		}
		for (PatternRecorder pr: prs)
			pr.step();
		refresh();
	}
	 
	public void stepOneLoop(int loopNum)
	{
		int pressedRow;
		int resCounter;
		int step;
		int i = loopNum;
			
		if(loops[i].isPlaying()) 
    	{
    		pressedRow = loops[i].getPressedRow();
    		resCounter = loops[i].getResCounter();
    		step = loops[i].getStep();
    		
    		//In buzz you have to send the controller AFTER the note is played
    		int loopCtrlValue = (loops[i].getStep() * 16);
    		
    		
    		// Only send the controller if we are changing position. This allows the sample to play smoothly and linearly.
    		if (pressedRow > -1) {
    			switch (loops[i].getType()) {
    				case Loop.HIT: // Hits we let it run to the end of the sample and don't send a noteOff on release
    					if (loops[i].getTrigger(step) == true) {
    						MidiManager.getInstance().sendCC(getChannel(), OFFSET_START_CTRL+i, loopCtrlValue);
    						if(!muteNotes)
    							MidiManager.getInstance().sendNoteOn(getChannel(), Looper.C3+i,pressedRow * 16  +1);
    						loops[i].setTrigger(step, false);
    					} else {
    						stopLoop(i);
    						pressedRow = -1;
    	  			}
    					break;
    				case Loop.MOMENTARY:
    				case Loop.SLICE:
    					if (resCounter == 0 || loops[i].getTrigger(step)) {
    						MidiManager.getInstance().sendCC(getChannel(), OFFSET_START_CTRL+i, loopCtrlValue);
    						if(!muteNotes)
    							MidiManager.getInstance().sendNoteOn(getChannel(), Looper.C3+i,pressedRow * 16  +1);
    						loops[i].setTrigger(step, false);
    					}
    					// If it's a one shot loop, then we stop after the first iteration
    	        		if (loops[i].isLastResInStep()) {
    	    				stopLoop(i);
    	    				loops[i].setPressedRow(-1);
    	    				pressedRow = -1;
    	    			}
    				// Don't break here, flow into SHOT	
    				case Loop.SHOT:
    					// If it's a one shot loop, then we stop after the first iteration
    	        		if (loops[i].getType() == Loop.SHOT && loops[i].isLastResStep()) {
    	    				stopLoop(i);
    	    				loops[i].setPressedRow(-1);
    	    				pressedRow = -1;
    	    			}
    	        	// Don't break flow into LOOP	
    				case Loop.LOOP:
    				case Loop.STEP:
    				default:
    					if (resCounter == 0) 
    						MidiManager.getInstance().sendCC(getChannel(), OFFSET_START_CTRL+i, loopCtrlValue);
    				
    					//Send note every time looprow is 0 or at it's offset
    	        		if((resCounter == 0) && (step == 0 || pressedRow > -1))
    	        		{
    	        			if (!muteNotes) {
    	        				boolean sendNote = false;
    	        				if(loops[i].getTrigger(step) == true) { 
        	        				loops[i].setTrigger(step, false);
        	        				sendNote = true;
        	        			}	
        	        			
    	        				// We only want to retrigger when necessary to avoid additional microfades or minor timing issues.
        	        			if (resCounter == 0 && loops[i].getIteration() > 0) {
        	        				if (loops[i].getType() == Loop.STEP) { // Else we are stepping in Loop.STEP mode and we retrigger every step
        	        					sendNote = true;
        	        				} else if (step == 0) { // We only retrigger at step 0 in other modes
        	        					sendNote = true;
	        	        				
        	        				}
        	        			}	
        	        			if (sendNote)
        	        				MidiManager.getInstance().sendNoteOn(getChannel(), Looper.C3+i,pressedRow * 16  +1);
    	        			}
    	        			pressedRow = -1;
    	        				
    	        		}
    					break;
    					
    			};
    		}	
    	
    		if(loops[i].isPlaying())
    			loops[i].nextResCount();
    	}  
	}
	
	public void setGateLoopChokes(boolean _gateLoopChokes)
	{
		gateLoopChokes = _gateLoopChokes;
	}
	
	public boolean getGateLoopChokes()
	{
		return gateLoopChokes;
	}

	public void reset() {
		for(int i=0;i<7;i++)
			stopLoop(i);
	}

	public void setLooperMute(boolean mute) {
		muteNotes = mute;
		
	}

}
