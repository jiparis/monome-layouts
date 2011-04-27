package jip.monome.layouts;

public class ShutdownHook extends Thread {
	MonomeLayouts ml;
	
    public ShutdownHook(MonomeLayouts ml) {
        super();
        this.ml = ml;
        System.out.println("Installing shutdown. Press Ctrl-C to exit");
    }
    
    public void run() {
        System.out.println("ShutDown thread started ...");
        ml.shutdownMidi();
    }
	
}
