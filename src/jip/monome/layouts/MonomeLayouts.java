package jip.monome.layouts;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

import jip.monome.layouts.behavior.CCButtonBehavior;
import jip.monome.layouts.behavior.NoteButtonBehavior;
import jip.monome.layouts.behavior.ToggleCCButtonBehavior;
import jip.monome.layouts.components.AbletonTracks;
import jip.monome.layouts.components.CCButton;
import jip.monome.layouts.components.CrossFader;
import jip.monome.layouts.components.Fader;
import jip.monome.layouts.components.JSObject;
import jip.monome.layouts.components.Looper;
import jip.monome.layouts.components.NoteButton;
import jip.monome.layouts.components.XYFader;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import sky.monome.LedButtonCouple;
import sky.monome.Monome;
import sky.monome.Page;
import sky.monome.StrictPageSwitcher;
import sky.monome.Monome.MonomeSize;
import sky.monome.behavior.Radio;
import sky.monome.event.button.ButtonEvent;
import sky.monome.event.button.ButtonListener;
import sky.monome.exception.MonomeException;

public class MonomeLayouts{
	Logger log = Logger.getLogger(MonomeLayouts.class.getName());
	
	Monome monome;
	
	StrictPageSwitcher pageSwitcher;
	HashMap<String, Page> pages = new HashMap<String, Page>();
	Radio.RadioGroup pageSelectionGroup;

	MidiManager midi;
	
	@SuppressWarnings("deprecation")
	public MonomeLayouts(JSONObject config) throws MonomeException {	
		// setup monome
		
		try {
			monome = setupMonome(config);
		} catch (MonomeException e) {
			log.severe("Error setting up monome");
			throw e;
		}
		int width = monome.getWidth();
		int height = monome.getHeight();
		
		// setup pages
		pageSwitcher = new StrictPageSwitcher("PageSwitcher", 0, 0, width - 1, height);
		pageSelectionGroup = new Radio.RadioGroup(); 		
		try {
			monome.addComponent(pageSwitcher);
		
			// setup pages
			for (int i = 0; i < monome.getHeight() ; i++){
				// Page model setup
				Page p = new Page("page" + i);
				pages.put("page" + i, p);
			
				pageSwitcher.addPage(p);
							
				// Page selection radios
				LedButtonCouple button = new LedButtonCouple("page"+i, monome, width - 1, i, new Radio(pageSelectionGroup));
				monome.addComponent(button);
			
			}
		} catch (MonomeException e) {
			log.severe("Error setting up page switcher");
			throw e;
		}				
		
		// setup midi
		setupMidi(config);	
		
		try {
			setupLayout (config);
		} catch (MonomeException e) {
			log.severe("Error loading layouts: " + e.getMessage());
		}			
		
//		setupClock();

		// Page listener
		pageSelectionGroup.addButtonListener(changePageListener);		
		
		Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));
		
		monome.refresh();
	}
	
//	private void setupClock(){
//		midi.plug(0, new ClockListener(){
//			final int ticksperbar = 24; // 1/4
//			public void start() {
//				counter = ticksperbar - 1;
//			}
//
//			public void stop() {
//				counter = 0;
//			}
//			
//			int counter = 0;
//			public void timingClockReceived() {
//				counter ++;
//				if (counter == ticksperbar){
//					counter = 0;
//					try {
//						Radio r = pageSelectionGroup.getActiveRadio();
//						if (r != null) {
//							r.getLedButtonCouple().setLedState(LedState.OFF, true);
//							Thread.sleep(25);
//							r.getLedButtonCouple().setLedState(LedState.ON, true);
//						}
//					} catch (MonomeException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
//			}			
//		});
//	}
	
	public void setupLayout(JSONObject config) throws MonomeException{
		midi.unplugAll();
		JSONObject layouts = (JSONObject) config.get("layout");
		for (int i = 0; i < monome.getHeight(); i++){			
			Page p = pages.get("page" +i);
			// empty page
			p.removeComponents();
			JSONArray pageLayout = (JSONArray) layouts.get(Integer.toString(i));
			if (pageLayout != null){
				for (int j = 0; j < pageLayout.size(); j++){
					JSONObject obj = (JSONObject) pageLayout.get(j);
					String type = (String) obj.get("type");				
					setupObject(p, type, obj);
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public void setupObject(Page p, String type, JSONObject obj) throws MonomeException{
		
		int channel = ((Number) obj.get("channel")).intValue();
		int note = ((Number) obj.get("note")).intValue();
		int x = ((Number) obj.get("x")).intValue();
		int y = ((Number) obj.get("y")).intValue();
		
		if ("note".equals(type)){
			p.addComponent(new NoteButton("nb", channel, note, x, y, new NoteButtonBehavior()));
		}
		else if ("cc".equals(type)){
			p.addComponent(new CCButton("ccb", channel, note, x, y, new CCButtonBehavior()));
		}
		else if ("toggle".equals(type)){
			p.addComponent(new CCButton("tb", channel, note, x, y, new ToggleCCButtonBehavior()));
		}
		else if ("fader".equals(type)){
			int size = ((Number) obj.get("size")).intValue();
			p.addComponent(new Fader("fader",channel, note, x, y, size));
		}
		else if ("xfader".equals(type)){
			int size = ((Number) obj.get("size")).intValue();
			p.addComponent(new CrossFader("xfader", channel, note, x, y, size));
		}
		else if ("xyfader".equals(type)){
			int note2 = ((Number) obj.get("note2")).intValue();
			int width = ((Number) obj.get("width")).intValue();
			int height = ((Number) obj.get("height")).intValue();
			p.addComponent(new XYFader("xy", channel, note, note2, x, y, width, height ));	
		}
		else if ("tracks".equals(type)){
			int width = ((Number) obj.get("width")).intValue();
			int height = ((Number) obj.get("height")).intValue();
			p.addComponent(new AbletonTracks("tracks", channel, note, x, y, width, height));
		}
		else if ("group".equals(type)){
			String of = (String) obj.get("of");
			int width = ((Number) obj.get("width")).intValue();
			int height = ((Number) obj.get("height")).intValue();
			String mode = (String) obj.get("mode");
			if (mode == null) mode = "chromatic";
			for (int i = 0; i < width; i++){
				for(int j = 0; j < height; j++){
					obj.put("note", Intervals.get(mode).getValue(note, i, j, width, height));
					obj.put("x", Integer.valueOf(x + i));
					obj.put("y", Integer.valueOf(y + j));
					setupObject(p, of, obj);
				}
				
			}
		}
		else if ("js".equals(type)){
			String file = (String) obj.get("file");
			int width = ((Number) obj.get("width")).intValue();
			int height = ((Number) obj.get("height")).intValue();
			p.addComponent(new JSObject(file, obj, channel, note, x, y, width, height));
		}
		else if ("7uplooper".equals(type)){
			int width = ((Number) obj.get("width")).intValue();
			int height = ((Number) obj.get("height")).intValue();
			p.addComponent(new Looper("looper", obj, channel, note, x, y, width, height ));	
		}
		else{
			log.warning("Type not found:" + type);
		}
	
	}
	
	public Monome setupMonome(JSONObject config) throws MonomeException{
		Number size = (Number) config.get("size");
		MonomeSize msize = null;
		switch(size.intValue()){		
		case 128:
			msize = MonomeSize.MONOME_128;
			break;
		case 256:
			msize = MonomeSize.MONOME_256;
			break;
		case 64:
		default:
			msize = MonomeSize.MONOME_64;		
		}
		
		String host = (String) config.get("host");
		String prefix = (String) config.get("prefix");
		Number portin = (Number) config.get("portin");
		Number portout = (Number) config.get("portout");
		
		log.info("Found monome config: " + size + "," + host + "," + prefix + "," + portin + "," + portout);
		return new Monome("Monome", msize, host, prefix, portin.intValue(), portout.intValue());
	}
		
	public void setupMidi(JSONObject config){
		midi = MidiManager.getInstance();
		Number input = (Number) config.get("midiin");
		Number output = (Number) config.get("midiout");
		midi.setInputDevice(MidiManager.getAvailableInputs().get(input.intValue()));
		midi.setOutputDevice(MidiManager.getAvailableOutputs().get(output.intValue()));	
	}
	
	public void shutdownMidi(){
		System.out.println("Shuting down midi ...");
		midi.closeOutputDevice();
		midi.closeInputDevice();
		System.out.println("Bye.");
	}
	
	ButtonListener<Radio.RadioGroup> changePageListener = new ButtonListener<Radio.RadioGroup>(){
		public void buttonActionned(ButtonEvent<Radio.RadioGroup> buttonEvent) {
			String butName = buttonEvent.getSource2().getActiveRadio().getLedButtonCouple().getName();
			try {
				pageSwitcher.showPage(pages.get(butName));
			} catch (MonomeException e) {
				log.severe("page not found:" + butName);
			}
		}		
	};	
	
	public static JSONObject parseConfig(String file){
		JSONParser parser = new JSONParser();
		Object parseresult = null;
		FileReader reader = null;
		try {
			reader = new FileReader(file);
			parseresult = parser.parse(reader);
		} catch (FileNotFoundException e) {			
			e.printStackTrace();
		} catch (IOException e) {			
			e.printStackTrace();
		} catch (ParseException pe) {
			System.out.println("position: " + pe.getPosition());
		    System.out.println(pe);
		}finally{
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
				}
		}
		return (JSONObject) parseresult;
	}

	public static void main(String[] args) throws MonomeException, IOException{
		if (args.length != 1){
			System.out.println("Usage: layouts config-file.json");
			System.exit(0);
		}		
		
		JSONObject parseresult = parseConfig(args[0]);		
		if (parseresult == null) System.exit(0);
		
		MonomeLayouts layouts = new MonomeLayouts((JSONObject) parseresult);
		
		char userin;
		while ((userin=(char)System.in.read()) != -1){
			switch (userin){
			case 'r':
				System.out.println("Reloading config file: " + args[0]);
				parseresult = parseConfig(args[0]);
				layouts.setupLayout((JSONObject)parseresult);
				break;
			}
			
			
		}
	}
	
}
