package com.thingtrack;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.usb4java.Context;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.HotplugCallback;
import org.usb4java.HotplugCallbackHandle;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

import com.thingtrack.parser.FreeStyleOptiumNeoParser;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class App {
	// Default Log Path
	private static final String DEF_PATH = "./";
	
    // Default wait milliseconds from send command to receive response
	private static final int DEF_WAIT = 1000;
	
	// assumes the current class is called logger 
	private final static Logger logger = Logger.getLogger(App.class);
	
    private static FreeStyleOptiumNeoParser freeStyleOptiumNeoParser = 
    		new FreeStyleOptiumNeoParser((short)0x1a61, (short)0x3850);
    
    public static String path;
    public static Integer wait;
    
	/**
     * This is the event handling thread. libusb doesn't start threads by its
     * own so it is our own responsibility to give libusb time to handle the
     * events in our own thread.
     */
    private static class EventHandlingThread extends Thread {
        /** If thread should abort. */
        private volatile boolean abort;

        /**
         * Aborts the event handling thread.
         */
        public void abort() {
            this.abort = true;
        }

        @Override
        public void run() {
            while (!this.abort) {
                // Let libusb handle pending events. This blocks until events
                // have been handled, a hotplug callback has been deregistered
                // or the specified time of 1 second (Specified in Microseconds) has passed.
                int result = LibUsb.handleEventsTimeout(null, 500000);
                if (result != LibUsb.SUCCESS)
                    throw new LibUsbException("Unable to handle events", result);
            }
        }
    }
         
    /**
     * The hotplug callback handler
     */
    private static class Callback implements HotplugCallback {
        @Override
        public int processEvent(Context context, Device device, int event, Object userData) {
            DeviceDescriptor descriptor = new DeviceDescriptor();
            int result = LibUsb.getDeviceDescriptor(device, descriptor);
            if (result != LibUsb.SUCCESS) {
            	logger.error("LibUsbException", new LibUsbException("Unable to read device descriptor", result));
                throw new LibUsbException("Unable to read device descriptor", result);
            }                
                            
            System.out.format("%s: %04x:%04x%n",
                event == LibUsb.HOTPLUG_EVENT_DEVICE_ARRIVED ? "Connected" : "Disconnected",
                descriptor.idVendor(), descriptor.idProduct());
            
            logger.info(String.format("%s: %04x:%04x%n", event == LibUsb.HOTPLUG_EVENT_DEVICE_ARRIVED ? "Connected" : "Disconnected",
                        descriptor.idVendor(), descriptor.idProduct()));                        
            
            // execute Free Style Optium Neo Glucometer device
            if (descriptor.idVendor() == freeStyleOptiumNeoParser.getVendorId() &&
            	descriptor.idProduct() == freeStyleOptiumNeoParser.getProductId() &&
            	event == 1) {
            		// export data from device
            		freeStyleOptiumNeoParser.execute(context, device, event, userData);
            		
            		// export json data to file
            		freeStyleOptiumNeoParser.export("");
            }
             
            return 0;
        }
    }
    
    public static void main( String[] args ) throws IOException, InterruptedException {
    	logger.info("Starting LibUsb ...");
    	
    	// parse path export argument: -p: export path
    	OptionParser parser = new OptionParser( "p::w::" );    	
    	OptionSet options = parser.parse(args);
    	
    	if (options.has("p"))  		    	
    		path = options.valueOf("p").toString();    	
    	else
    		path = DEF_PATH;
    	
    	if (options.has("w"))  		    	
    		wait = Integer.parseInt(options.valueOf("w").toString());    	
    	else
    		wait = DEF_WAIT;
    	
    	// Initialize the libusb context
        int result = LibUsb.init(null);
        
        if (result != LibUsb.SUCCESS) {
        	logger.error("LibUsbException", new LibUsbException("Unable to initialize libusb", result));
        	
            throw new LibUsbException("Unable to initialize libusb", result);
        }

        // Check if hotplug is available
        if (!LibUsb.hasCapability(LibUsb.CAP_HAS_HOTPLUG)) {
        	logger.error("libusb doesn't support hotplug on this system");
        	
            System.err.println("libusb doesn't support hotplug on this system");
            System.exit(1);
        }
        
        // Start the event handling thread
        EventHandlingThread thread = new EventHandlingThread();
        thread.start();        
        
        // Register the hotplug callback
        HotplugCallbackHandle callbackHandle = new HotplugCallbackHandle();
        result = LibUsb.hotplugRegisterCallback(null,
            LibUsb.HOTPLUG_EVENT_DEVICE_ARRIVED | LibUsb.HOTPLUG_EVENT_DEVICE_LEFT,
            LibUsb.HOTPLUG_ENUMERATE,
            LibUsb.HOTPLUG_MATCH_ANY,
            LibUsb.HOTPLUG_MATCH_ANY,
            LibUsb.HOTPLUG_MATCH_ANY,
            new Callback(), null, callbackHandle);
        
        if (result != LibUsb.SUCCESS) {
        	logger.error("LibUsbException", new LibUsbException("Unable to register hotplug callback", result));        	
            throw new LibUsbException("Unable to register hotplug callback", result);
        }
        
        // Our faked application. Hit enter key to exit the application.
        System.out.println("Hit enter to exit ...");
        System.in.read();

        // Unregister the hotplug callback and stop the event handling thread
        thread.abort();
        LibUsb.hotplugDeregisterCallback(null, callbackHandle);
        thread.join();
        
        // Deinitialize the libusb context
        LibUsb.exit(null);
        
        logger.info("Exit LibUsb ...");
    }
}
