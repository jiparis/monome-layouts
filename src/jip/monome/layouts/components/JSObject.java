package jip.monome.layouts.components;

import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import javax.sound.midi.ShortMessage;

import jip.monome.layouts.CCListener;
import jip.monome.layouts.ClockListener;
import jip.monome.layouts.MidiManager;
import jip.monome.layouts.NoteListener;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;

import sky.monome.exception.MonomeException;
import sky.monome.frame.Frame;

public class JSObject extends MidiButtonGroup implements CCListener,
		NoteListener, ClockListener {
	Logger log = Logger.getLogger(JSObject.class.getName());

	String filename;
	Map args;
	Context cx;
	ScriptableObject scope;
	Function init, notify, writeOn, 
			ccReceived,	noteOnReceived, noteOffReceived, 
			start, stop, clkReceived;
	
	int resolution = 12; // default resolution 1/8
	
	public JSObject(String filename, Map args, int channel, int cc, int x, int y, int width,
			int height) throws MonomeException {
		super(filename, channel, cc, x, y, width, height);
		
		FileReader reader = null;
		try {
			this.filename = filename;
			this.args = args;
			
			reader = new FileReader(filename);		
		
			cx = Context.enter();
			scope = cx.initStandardObjects();
					
			initRootObjects();			
			
			cx.evaluateReader(scope, reader, filename, 1, null);			
			
			init = initFunction("init");
			notify = initFunction("notify");
			writeOn = initFunction("writeOn");
			ccReceived = initFunction("ccReceived");
			noteOnReceived = initFunction("noteOnReceived");
			noteOffReceived = initFunction("noteOffReceived");
			start = initFunction("start");
			stop = initFunction("stop");
			clkReceived = initFunction("clkReceived");
			
			// tick resolution
			String res = (String) args.get("res");
			if (res!=null){
				if ("1".equals(res))
					resolution = 96;
				else if ("1/2".equals(res))
					resolution = 48;
				else if ("1/4".equals(res))
					resolution = 24;
				else if ("1/8".equals(res))
					resolution = 12;
				else if ("1/16".equals(res))
					resolution = 6;				
			}
			
			if (init!=null){
				init.call(cx, scope, scope, new Object[]{});
			}			
		}
		catch(Exception e){					
			throw new MonomeException("Error setting up " + filename, e);
		}
		finally{
			if (reader!=null)
				try {
					reader.close();
				} catch (IOException e) {					
				}			
		}
		
	}	
	
	private void initRootObjects(){
		// This
		Object wrappedObject = Context.javaToJS(this, scope);
		ScriptableObject.putProperty(scope, "jsobject", wrappedObject);		
		
		// Config args
		wrappedObject = Context.javaToJS(args, scope);
		ScriptableObject.putProperty(scope, "jsargs", wrappedObject);	
		
		// Midi
		wrappedObject = Context.javaToJS(MidiManager.getInstance(), scope);
		ScriptableObject.putProperty(scope, "midi", wrappedObject);
		
		wrappedObject = Context.javaToJS(sky.monome.LedButtonCouple.LedState.OFF, scope);
		ScriptableObject.putProperty(scope, "LED_OFF", wrappedObject);		
		
		wrappedObject = Context.javaToJS(sky.monome.LedButtonCouple.LedState.ON, scope);
		ScriptableObject.putProperty(scope, "LED_ON", wrappedObject);
		
		// System.out for debugging, use with caution
		wrappedObject = Context.javaToJS(System.out, scope);
		ScriptableObject.putProperty(scope, "out", wrappedObject);			
		
		
	}
	
	
	private Function initFunction(String funName){
		Object fun = scope.get(funName, scope);
		if (!(fun instanceof Function)) {
		    log.fine(funName + " undefined or not a function") ;
		    return null;
		} 	
		
		return (Function)fun;		
	}
	
	@Override
	public void notify(boolean pressed, int x, int y) {
		try{
			if (notify!=null){		
				notify.call(cx, scope, scope, new Object[]{pressed, x, y});
			}		
		}catch(RuntimeException e){
			//eat exceptions
		}
	}
	
	@Override
	public void writeOn(Frame frame) throws MonomeException {
		try{
			Context.enter();	//runs on another thread		
			if (writeOn!=null){
				writeOn.call(cx, scope, scope, new Object[]{frame});
			}				
			Context.exit();
		}catch(RuntimeException e){
			//eat exceptions
		}
	}
	
	public void controllerChangeReceived(ShortMessage m) {
		try{
			if (ccReceived!=null){
				ccReceived.call(cx, scope, scope, new Object[]{m});
			}			
		}catch(RuntimeException e){
			//eat exceptions
		}	
	}
	
	public void noteOffReceived(ShortMessage n) {
		try{
			if (noteOffReceived!=null){
				noteOffReceived.call(cx, scope, scope, new Object[]{n});
			}		
		}catch(RuntimeException e){
			//eat exceptions
		}		
	}
	
	public void noteOnReceived(ShortMessage n) {
		try{
			if (noteOnReceived!=null){
				noteOnReceived.call(cx, scope, scope, new Object[]{n});
			}				
		}catch(RuntimeException e){
			//eat exceptions
		}
	}
	
	public void start() {
		try{
			if (start!=null){
				start.call(cx, scope, scope, new Object[]{});
			}				
		}catch(RuntimeException e){
			//eat exceptions
		}
	}
	
	public void stop() {
		try{
			if (stop!=null){
				stop.call(cx, scope, scope, new Object[]{});
			}			
		}catch(RuntimeException e){
			//eat exceptions
		}
		counter = 0;
	}	
	
	int counter = 0;
	public void timingClockReceived() {
		counter++;
		if (counter == this.resolution){
			counter = 0;
			try{
				if (clkReceived!=null){			
					clkReceived.call(cx, scope, scope, new Object[]{});
				}			
			}catch(RuntimeException e){
				//eat exceptions
			}	
		}
	}
	
	

}
