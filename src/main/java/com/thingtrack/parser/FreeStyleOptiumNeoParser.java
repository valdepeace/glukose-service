package com.thingtrack.parser;

import java.io.FileWriter;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.usb4java.BufferUtils;
import org.usb4java.Context;
import org.usb4java.Device;
import org.usb4java.DeviceHandle;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

import com.thingtrack.protocol.FreestyleOptiumNeoProtocols;

public class FreeStyleOptiumNeoParser extends AbstractUsbParser {
	/** The USB communication timeout. */
    private static final int TIMEOUT = 1000;
        
    /** The USB default interface of the FreeStyle Optium Neo */
    private static final int DEF_INTERFACE = 0;
    
    /** The input endpoint of the FreeStyle Optium Neo */
    private static final byte IN_ENDPOINT = (byte)0x81;
    
    /** Default read size of the FreeStyle Optium Neo */
    private static final int SIZE = 64;
    
    /** Wait milliseconds from send command to receive response */
    private static final int WAIT = 1000;

    /** Measure class */
    public class Measure {
    	private Date date;
    	private float value;
    	
    	public Measure(Date date, float value) {
    		this.date = date;
    		this.value = value;
    	}
    	
    	public Date getDate() {
    		return this.date;
    	}
    	
    	public float getValue() {
    		return value;
    	}
    }
    
    private Hashtable<String, Object> values = new Hashtable<String, Object>();
    
	public FreeStyleOptiumNeoParser(short vendorId, short productId) {
		super(vendorId, productId);
	}
	
    /**
     * Send transfer control commands from the device.
     * 
     * @param handle
     *            The device handle.
     * @param command
     *            The command to execute
     */	
    private void sendCommand(DeviceHandle handle, byte[] transfer) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(transfer.length);
        buffer.put(transfer);
        buffer.rewind();
        
        int transfered = LibUsb.controlTransfer(handle,
            (byte) (LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE),
            (byte) 0x09, (short) 0x200, (short) 0x00, buffer, TIMEOUT);
        
        if (transfered < 0)
            throw new LibUsbException("Control transfer failed", transfered);
        
        if (transfered != transfer.length)
            throw new RuntimeException("Not all data was sent to device");
    }
    
    /**
     * Reads some data from the device endpoint.
     * 
     * @param handle
     *            The device handle.
     * @param size
     *            The number of bytes to read from the device.
     * @return The read data.
     */
    private ByteBuffer read(DeviceHandle handle, int size) {
        ByteBuffer buffer = BufferUtils.allocateByteBuffer(size).order(ByteOrder.LITTLE_ENDIAN);        
        IntBuffer transferred = BufferUtils.allocateIntBuffer();
        
        int result = LibUsb.bulkTransfer(handle, IN_ENDPOINT, buffer, transferred, TIMEOUT);
        if (result != LibUsb.SUCCESS) {
            throw new LibUsbException("Unable to read data", result);
        }
        
        System.out.println(transferred.get() + " bytes read from device");
        
        return buffer;
    }   
	
    /**
     * execute command from the device.
     * 
     * @param handle
     *            The device handle.
     * @param commandCode
     *            The command code to execute. 
     * @param commandPayload
     *            The command payload to execute.                             
     * @param size
     *            The number of bytes to read from the device.
     * @return The read data.
     */    
    private Object executeCommand(DeviceHandle handle, String commandCode, byte[] commandPayload, int size, boolean multi) throws InterruptedException {
    	List<String> tokens = new ArrayList<String>();
    	
    	ByteBuffer buffer=null;
        String data = null;
        
    	System.out.println("send " + commandCode + " command");
        sendCommand(handle, commandPayload);
        Thread.sleep(WAIT);
        try {		                
            while(true) {
                buffer = read(handle, size);	  
                data = StandardCharsets.US_ASCII.decode(buffer).toString();
                System.out.println("Data " + commandCode + ":" + data);
                
                // if the control transfer result is multi
                if (multi)
                	tokens.add(data);
                
            	if (data.contains("CMD OK")) {
            		if (multi)
            			return tokens;
            		else            			
            			return data;		      
            	}
            }
        } catch (LibUsbException ex) {
        	return tokens;
        }
    }
	
    /* Parse Free Style Optium Neo functions*/
    private void parseDate(Object data) throws ParseException {
    	String[] lines = ((String) data).split("\n");
    	
    	//SimpleDateFormat formatter = new SimpleDateFormat("MM-dd-yy");
    	
    	String str = (lines[0]).substring(2, lines[0].length()-1);
    	String[] tokens = str.split(",");
    	
    	//values.put("date", formatter.parse(tokens[0] + "-" + tokens[1] + "-" + tokens[2]));    	
    	values.put("date", tokens[0] + "-" + tokens[1] + "-" + tokens[2]);
    }

    private void parseTime(Object data) throws ParseException {
    	String[] lines = ((String) data).split("\n");
    	
    	//SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
    	
    	String str = (lines[0]).substring(2, lines[0].length()-1);    	
    	String[] tokens = str.split(",");
    	
    	//values.put("time", formatter.parse(tokens[0] + ":" + tokens[1]));
    	values.put("time", tokens[0] + ":" + tokens[1]);
    }
    
    private void parseSerlnum(Object data) {
    	String[] lines = ((String) data).split("\n");
    	
    	values.put("serlnum", lines[0].substring(2, lines[0].length()-1));
    }

    private void parseSwver(Object data) {
    	String[] lines = ((String) data).split("\n");
    	
    	values.put("swver", lines[0].substring(2, lines[0].length()-1));
    }

    private void parsePtname(Object data) {
    	String[] lines = ((String) data).split("\n");
    	
    	values.put("ptname", lines[0].substring(2, lines[0].length()-1));
    } 
    
    private void parsePtid(Object data) {
    	String[] lines = ((String) data).split("\n");
    	
    	values.put("ptid", lines[0].substring(2, lines[0].length()-1));
    }

    @SuppressWarnings({ "unchecked" })
	private void parseResult(Object data) throws NumberFormatException, ParseException {
    	List<Measure> measures = new ArrayList<Measure>();
    	
    	SimpleDateFormat formatter = new SimpleDateFormat("MM-dd-yy HH:mm");
    	
    	for (Object line : (List<Object>)data) {
    		String[] tokens = ((String)line).split(",");
    		    
    		if (tokens.length > 8) {
	    		Date date = formatter.parse(tokens[2] + "-" + tokens[3] + "-" + tokens[4] + " " + tokens[5] + ":" + tokens[6]);
	    		float value = Float.parseFloat(tokens[8]);    		
	    		if (value == 1)
	    			value = 0;
	    		
	    		measures.add(new Measure(date, value));
    		}
    	}
    	
		values.put("result", measures);
    }
    
    @Override
	public Hashtable<String, Object> execute(Context context, Device device, int event, Object userData) {
    	int result;
    	
    	// Open Abbott FreeStyle Optium Neo device
        final DeviceHandle handle = new DeviceHandle();
        result = LibUsb.open(device, handle);
        
        if (result != LibUsb.SUCCESS) {
            throw new LibUsbException("Unable to open USB device", result);
        }
        	   
        int attached = 0;
        try {
            // Check if kernel driver is attached to the interface
            attached = LibUsb.kernelDriverActive(handle, DEF_INTERFACE);
            if (attached < 0) {
                throw new LibUsbException(
                    "Unable to check kernel driver active", result);
            }

            // Detach kernel driver from interface 0. This can fail if
            // kernel is not attached to the device or operating system
            // doesn't support this operation. These cases are ignored here.
            result = LibUsb.detachKernelDriver(handle, DEF_INTERFACE);
            if (result != LibUsb.SUCCESS &&
                result != LibUsb.ERROR_NOT_SUPPORTED &&
                result != LibUsb.ERROR_NOT_FOUND) {
                throw new LibUsbException("Unable to detach kernel driver",
                    result);
            }
            
            // Claim interface
            result = LibUsb.claimInterface(handle, DEF_INTERFACE);
            if (result != LibUsb.SUCCESS) {
                throw new LibUsbException("Unable to claim interface", result);
            }	                

            // Clear the halt/stall condition for an endpoint
            LibUsb.clearHalt(handle, IN_ENDPOINT);
            
            // sleep 5 seconds
            System.out.println("Wait 5 second ...");
            Thread.sleep(5000);
            
            System.out.println("Init commands");
        		                
            // send com01_init command
            executeCommand(handle, "com01_init", FreestyleOptiumNeoProtocols.com01_init, SIZE, false);	              
            
            // send com02_time command
            executeCommand(handle, "com02_time", FreestyleOptiumNeoProtocols.com02_time, SIZE, false);
            
            // send com03_time command
            executeCommand(handle, "com03_time", FreestyleOptiumNeoProtocols.com03_time, SIZE, false);		          
            
            // send com04_time command
            executeCommand(handle, "com04_time", FreestyleOptiumNeoProtocols.com04_time, SIZE, false);
            	                
            // send com05_time command
            executeCommand(handle, "com05_time", FreestyleOptiumNeoProtocols.com05_time, SIZE, false);	              
            
            // send com06_serlnum command
            parseSerlnum(executeCommand(handle, "com06_serlnum", FreestyleOptiumNeoProtocols.com06_serlnum, SIZE, false));
            
            // send com07_swver command
            parseSwver(executeCommand(handle, "com07_swver", FreestyleOptiumNeoProtocols.com07_swver, SIZE, false));
            
            // send com08_date command
            parseDate(executeCommand(handle, "com08_date", FreestyleOptiumNeoProtocols.com08_date, SIZE, false));	              
            
            // send com09_time command
            parseTime(executeCommand(handle, "com09_time", FreestyleOptiumNeoProtocols.com09_time, SIZE, false));
            
            // send com10_ptname command
            parsePtname(executeCommand(handle, "com10_ptname", FreestyleOptiumNeoProtocols.com10_ptname, SIZE, false));

            // send com11_ptid command
            parsePtid(executeCommand(handle, "com11_ptid", FreestyleOptiumNeoProtocols.com11_ptid, SIZE, false));
                          		              	               
            // send com26_result command
            parseResult(executeCommand(handle, "com26_result", FreestyleOptiumNeoProtocols.com26_result, SIZE, true));	                	                
            
            System.out.println("End commands");
        } catch (InterruptedException e) {
        	System.out.println("InterruptedException: " + e.getMessage());		
		} catch (ParseException e) {
			System.out.println("ParseException: " + e.getMessage());
		} catch (Exception e) {
			System.out.println("Exception: " + e.getMessage());
		}
        finally {
        	// Release the interface
        	System.out.println("Release interface");
            result = LibUsb.releaseInterface(handle, DEF_INTERFACE);
            if (result != LibUsb.SUCCESS) {
                throw new LibUsbException("Unable to release interface", 
                    result);
            }
            
            // Re-attach kernel driver if needed
            if (attached == 1) {
                LibUsb.attachKernelDriver(handle, DEF_INTERFACE);
                if (result != LibUsb.SUCCESS) {
                    throw new LibUsbException(
                        "Unable to re-attach kernel driver", result);
                }
            }
        }
                    
		return values;
	}

	@Override
	public Hashtable<String, Object> getValues() {
		return values;
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public void export(String path) {
		try {
			// create the JSON file from hashtable
			SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yy HH:mm");
			
			TimeZone tz = TimeZone.getTimeZone("UTC");
			DateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
			isoDateFormat.setTimeZone(tz);
					
			JSONObject json = new JSONObject();		
			
			json.put("swver", values.get("swver").toString());
			json.put("serlnum", values.get("serlnum").toString());
			json.put("ptname", values.get("ptname").toString());
			json.put("ptid", values.get("ptid").toString());
			json.put("date", isoDateFormat.format(dateFormat.parse(values.get("date") + " " + values.get("time"))));
			
			JSONArray result = new JSONArray();
			for(Measure measure : (List<Measure>)values.get("result")) {
	    		JSONObject val = new JSONObject();
	    		
	    		val.put("date", isoDateFormat.format(measure.getDate()));
	    		val.put("measure", measure.getValue());
	    		
	    		result.add(val);
	    	}
			json.put("result", result);		
		
			// export jSON file
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");	
			
			String fileName = formatter.format(new Date()) + "#" + values.get("serlnum").toString() + ".json";
			
			FileWriter jsonFile = new FileWriter(fileName);
			jsonFile.write(json.toJSONString());
			jsonFile.flush();
			jsonFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {			
			e.printStackTrace();
		}
	}      
}
