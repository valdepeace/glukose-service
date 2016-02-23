package com.thingtrack.parser;

import java.util.Hashtable;

import org.usb4java.Context;
import org.usb4java.Device;

public abstract class AbstractUsbParser {
    private short vendorId;
    private short productId;
    
    public AbstractUsbParser(short vendorId, short productId) {
    	this.vendorId = vendorId;
    	this.productId = productId;
    }
        
    public short getVendorId() {
    	return vendorId;
    }
    
    public short getProductId() {
    	return productId;
    }
    
	public abstract Hashtable<String, Object> execute(Context context, Device device, int event, Object userData); 
	
	public abstract Hashtable<String, Object> getValues();
	
	public abstract void export(String path);
}
