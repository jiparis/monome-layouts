package jip.monome.layouts;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;
import javax.sound.midi.MidiDevice.Info;

import sky.monome.Component;

@SuppressWarnings("unchecked")
public class MidiManager implements Receiver{
	public static Logger logger = Logger.getLogger(MidiManager.class.getName());
	
	private static MidiManager instance;
	
	public static Info[] infos;
	MidiDevice inputDevice, outputDevice;
	private static Receiver outputReceiver;
	private static Transmitter inputTransmitter;
	private static HashSet<NoteListener>[] plugs_note = new HashSet[16];
	private static HashSet<CCListener>[] plugs_cc = new HashSet[16];
	private static HashSet<ClockListener> plugs_clock;
	
	private MidiManager() {
		infos = MidiSystem.getMidiDeviceInfo();
		for (int i = 0; i < 16; i++){
			plugs_note[i] = new HashSet<NoteListener>();
			plugs_cc[i] = new HashSet<CCListener>();
		}
		plugs_clock = new HashSet<ClockListener>();
	}
	
	public static MidiManager getInstance(){
		if (instance == null){
			instance = new MidiManager();
		}
		return instance;
	}

	public void setOutputDevice(Info info) {
		closeOutputDevice();
		try {
			outputDevice = MidiSystem.getMidiDevice(info);
			outputDevice.open();
			outputReceiver = outputDevice.getReceiver();
			
			logger.log(Level.INFO, info.getName() + " open");
		} catch (MidiUnavailableException e) {
			logger.log(Level.SEVERE, "Error opening output device", e);
		}
	}
	
	public void closeOutputDevice(){
		if (outputReceiver != null){
			outputReceiver.close();
		}
		if (this.outputDevice != null && this.outputDevice.isOpen()){
			outputDevice.close();
		}
	}

	public void setInputDevice(Info info) {
		closeInputDevice();
		try {
			inputDevice = MidiSystem.getMidiDevice(info);
			inputDevice.open();
			inputTransmitter = inputDevice.getTransmitter();
			inputTransmitter.setReceiver(this);
			logger.log(Level.INFO, info.getName() + " open");
		} catch (MidiUnavailableException e) {
			logger.log(Level.SEVERE, "Error opening input device", e);
		}
	}
	
	public void closeInputDevice(){
		if (inputTransmitter != null){
			inputTransmitter.close();
		}
		if (this.inputDevice != null && this.inputDevice.isOpen()){
			inputDevice.close();
		}
	}

	public List<Info> getAvailableInputs() {
		List<Info> res = new Vector<Info>();
		System.out.println("Available MIDI inputs:");
		for (int i = 0; i < infos.length; i++){
			MidiDevice dev;
			try {
				dev = MidiSystem.getMidiDevice(infos[i]);
				if (dev.getMaxTransmitters() != 0) {
					System.out.println("[" + res.size() + "] " + infos[i].getName());
					res.add(infos[i]);					
				}				
			} catch (MidiUnavailableException e) {
				logger.log(Level.SEVERE, "Error getting midi input infos", e);
			}			
			
		}
		
		return res;
	}

	public List<Info> getAvailableOutputs() {
		List<Info> res = new Vector<Info>();
		System.out.println("Available MIDI outputs:");
		for (int i = 0; i < infos.length; i++){
			MidiDevice dev;
			try {
				dev = MidiSystem.getMidiDevice(infos[i]);
				if (dev.getMaxReceivers() != 0) {
					System.out.println("[" + res.size() + "] " + infos[i].getName());
					res.add(infos[i]);
				}				
			} catch (MidiUnavailableException e) {
				logger.log(Level.SEVERE, "Error getting output device infos", e);
			}			
			
		}
		
		return res;
	}

	public void sendCC(int channel, int ccValue, int value) {
		if (outputReceiver != null) {
			ShortMessage msg = new ShortMessage();
			try {
				msg.setMessage(ShortMessage.CONTROL_CHANGE, channel, ccValue, value);
			} catch (InvalidMidiDataException e) {
				e.printStackTrace();
			}
			outputReceiver.send(msg, -1);
		}
	}

	public void sendNoteOn(int channel, int noteValue, int velValue) {
		if (outputReceiver != null) {
			ShortMessage msg = new ShortMessage();

			try {
				msg.setMessage(ShortMessage.NOTE_ON, channel, noteValue, velValue);
			} catch (InvalidMidiDataException e) {
				e.printStackTrace();
			}
			outputReceiver.send(msg, -1);
		}
	}

	public void sendNoteOff(int channel, int noteValue) {
		if (outputReceiver != null) {
			ShortMessage msg = new ShortMessage();
		
			try {
				msg.setMessage(ShortMessage.NOTE_OFF, channel, noteValue, 0);
			} catch (InvalidMidiDataException e) {
				e.printStackTrace();
			}
			outputReceiver.send(msg, -1);
		}
	}

	// plugging
	public void plug(int channel, Object obj){
		if (obj instanceof NoteListener)
			plugs_note[channel].add((NoteListener) obj);
		if (obj instanceof CCListener)
			plugs_cc[channel].add((CCListener) obj);
		if (obj instanceof ClockListener){
			plugs_clock.add((ClockListener) obj);
		}
	}
		
	public void unplug(int channel, Component obj){
		if (obj instanceof NoteListener)
			plugs_note[channel].remove(obj);
		if (obj instanceof CCListener)
			plugs_cc[channel].remove(obj);
		if (obj instanceof ClockListener){
			plugs_clock.remove(obj);
		}
	}
	
	public void unplugAll(){
		for (int i = 0; i < 16; i++){
			plugs_note[i].clear();
			plugs_cc[i].clear();
		}
		plugs_clock.clear();
	}
	
	// Receiver
	public void close() {
		// do nothing		
	}

	public void send(MidiMessage msg, long time) {
		if (msg instanceof ShortMessage){
			ShortMessage sm = (ShortMessage) msg;
			int cmd = sm.getCommand();
			int channel = sm.getChannel();
			int status = sm.getStatus();			
			
			Set<NoteListener> objects_note = plugs_note[channel];
			Set<CCListener> objects_cc = plugs_cc[channel];
			
			switch(cmd){
			case ShortMessage.NOTE_ON:
				for (NoteListener obj: objects_note){
					obj.noteOnReceived(sm);
				}
				break;
			case ShortMessage.NOTE_OFF:
				for (NoteListener obj: objects_note){
					obj.noteOffReceived(sm);
				}
				break;
			case ShortMessage.CONTROL_CHANGE:
				for (CCListener obj: objects_cc){
					obj.controllerChangeReceived(sm);
				}
				break;			
			case 0xF0: // Sysex for clock (status F8)
				if (status == ShortMessage.TIMING_CLOCK)
					for (ClockListener obj: plugs_clock){
						obj.timingClockReceived();
					}
				else if (status == ShortMessage.START || status == ShortMessage.CONTINUE){
					for (ClockListener obj: plugs_clock){
						obj.start();
					}
				}
				else if (status == ShortMessage.STOP){
					for (ClockListener obj: plugs_clock){
						obj.stop();
					}
				}

				break;
			default:
				System.out.println(sm.getStatus());
			}
		}

	}

}
