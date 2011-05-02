package sky.monome;

import java.awt.Dimension;
import jip.monome.serialosc.GridListener;
import jip.monome.serialosc.MonomeDevice;
import jip.monome.serialosc.MonomeZeroconf;
import sky.monome.LedButtonCouple.LedState;
import sky.monome.exception.MonomeException;
import sky.monome.frame.DefaultFrame;

/**
 * Logical Monome device. Instances of this class must be always on top of Monome assembling trees.
 * Due to its logical nature, only one instance of this class can manage up to 4 physical 40h Monome devices at the same time, with a correct mapping done in MonomeSerial.
 * @author PJ Skyman
 */
public final class Monome extends Group
{
    /**
     * Connection object that communicates with physical Monome device.
     */
    private final MonomeDevice device;
    /**
     * Size of this logical Monome device.
     */
    private final MonomeSize monomeSize;
    /**
     * Constant used to denote that a coordinate was not found.
     * @deprecated Since SkyMonome v1.3, this constant field is not used
     * anymore.
     */
    @Deprecated
    public static final int COORDINATE_NOT_FOUND=-1;

    /**
     * Constructs a logical Monome device with specified name, size, network addressing and prefix.
     * @param name Name of this Monome device.
     * @param prefix Prefix of this Monome device. Prefix is used by MonomeSerial. Typically "/40h".
     * @param portInNumber Port number for entering communication, typically 8000.
     * @throws sky.monome.exception.MonomeException When it's impossible to create network connections with MonomeSerial.
     */
    public Monome(String name, String prefix,int portInNumber) throws MonomeException
    {
        super(name,0,0,8,8); //pass default values for dimension
        try{
            MonomeZeroconf s = new MonomeZeroconf();
            String[] monomes;

            // wait for devices available
            do {
                Thread.sleep(20);
                monomes = s.getDevices();
            } while (monomes.length == 0);
            
            device = s.connect(monomes[0], prefix, portInNumber);
            
            device.addListener(new GridListener() {                
                public void press(int x, int y, int s) {
                     notifyPress(x, y, s);
                }
            });
            
            int sizex = device.getSizeX();
            int sizey = device.getSizeY();
            
            if (sizex == 8 && sizey == 8)
                monomeSize = MonomeSize.MONOME_64;
            else if ((sizex == 8 && sizey == 16) || (sizey == 8 && sizex == 16))
                monomeSize = MonomeSize.MONOME_128;
            else
                monomeSize = MonomeSize.MONOME_256;
        }
        catch (Exception e) {
            throw new MonomeException("Error connecting to serialosc", e);
        }
    }

    /**
     * Returns {@code this}.
     * @return {@code this}.
     */
    @Override
    public Monome getMonome()
    {
        return this;
    }

    /**
     * Returns {@code 0}.
     * @return {@code 0}.
     */
    @Override
    public int getAbsoluteX()
    {
        return 0;
    }

    /**
     * Returns {@code 0}.
     * @return {@code 0}.
     */
    @Override
    public int getAbsoluteY()
    {
        return 0;
    }

    /**
     * Returns a string representation of this logical Monome device.
     * @return A string representation of this logical Monome device.
     */
    @Override
    public String toString()
    {
        return "Monome "+name;
    }

    /**
     * Returns {@code true}.
     * @return {@code true}.
     */
    @Override
    public boolean isVisible()
    {
        return true;
    }

    /**
     * Returns {@code false}.
     * @return {@code false}.
     */
    @Override
    public boolean canHaveParentContainer()
    {
        return false;
    }

    /**
     * Refreshes the physical Monome device according to this logical Monome device.
     * It internally uses a {@link sky.monome.frame.DefaultFrame DefaultFrame}.
     * This logicial Monome device asks to its contained components to write down to the DefaultFrame,
     * and then the virtual "image" represented by the DefaultFrame is sent in one shot to the physical Monome device.
     * @throws sky.monome.exception.MonomeException When an error appears when communicating with MonomeSerial.
     */
    public void refresh() throws MonomeException
    {
//        synchronized(lockObject)
//        {
            DefaultFrame defaultFrame=new DefaultFrame(device.getSizeX(),device.getSizeY());
            writeOn(defaultFrame);
            if(monomeSize==MonomeSize.MONOME_64)
            {
                int[] values=new int[8];
                for(int i=0;i<8;i++)
                    for(int j=0;j<8;j++)
                        values[i]=(values[i]<<1)|(defaultFrame.get(7-j,i)==LedState.ON?1:0);
                
                device.grid.map(0, 0, values); //send(device.getPrefix()+"/frame",values[0],values[1],values[2],values[3],values[4],values[5],values[6],values[7]);
              
            }
            else{
                for(int m=0;m<16;m+=8){
                    for(int n=0;n<height;n+=8)
                    {
                        int[] values=new int[8];
                        for(int i=0;i<8;i++)
                            for(int j=0;j<8;j++)
                                values[i]=(values[i]<<1)|(defaultFrame.get(7-j+m,i+n)==LedState.ON?1:0);
                        
                        device.grid.map(m, n, values); //(device.getPrefix()+"/frame",m,n,values[0],values[1],values[2],values[3],values[4],values[5],values[6],values[7]);
                        
                    }
                }
            }

    }
    
    /**
     * Clears the led state of all leds of the Monome device.
     * @throws sky.monome.exception.MonomeException When an error appears when communicating with MonomeSerial.
     * @since SkyMonome v1.1
     */
    public void clear() throws MonomeException
    {
        clear(false);
    }

    /**
     * Clears the led state of all leds of the Monome device.
     * @param state {@code true} if all leds should be turned on, or {@code false} if all leds should turned off.
     * @throws sky.monome.exception.MonomeException When an error appears when communicating with MonomeSerial.
     * @since SkyMonome v1.1
     */
    public void clear(boolean state) throws MonomeException
    {
        device.grid.all(state ? 1 : 0); //send(device.getPrefix()+"/clear",state?"1":"0");
    }
    
    @Override
    public int getWidth() {
        return device.getSizeX();
    }

    @Override
    public int getHeight() {
        return device.getSizeY();
    }



    /**
     * Monome size.
     * @author PJ Skyman
     */
    public static enum MonomeSize
    {
        /**
         * Denotes a Monome 64 or a Monome 40h (8x8).
         */
        MONOME_64
        {
            public Dimension getDimension()
            {
                return new Dimension(8,8);
            }
        },
        /**
         * Denotes a Monome 128 or a Monome 80h (16x8). Note that this Monome should be used horizontally only.
         */
        MONOME_128
        {
            public Dimension getDimension()
            {
                return new Dimension(16,8);
            }
        },
        /**
         * Denotes a Monome 256 or a Monome 100h (16x16).
         */
        MONOME_256
        {
            public Dimension getDimension()
            {
                return new Dimension(16,16);
            }
        };

        /**
         * Returns the physical dimension equivalent to this Monome size.
         * @return The physical dimension equivalent to this Monome size.
         * @since SkyMonome v1.2
         */
        public abstract Dimension getDimension();
    }

    /**
     * Throws an exception and prints the stack trace.
     * This method is designed for debugging purpose only.
     * @since SkyMonome v1.1
     */
    public static void throwException()
    {
        try
        {
            throw new Exception();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
    /**
     * Prints out an about message on the console.
     */
    private static void printAbout()
    {
        System.out.println("SkyMonome v1.3 by PJ Skyman");
    }

    /**
     * Main method. This prints out an about message on the console.
     * @param args Arguments passed to the program.
     * They won't be used.
     */
    public static void main(String[] args)
    {
        printAbout();
    }
}