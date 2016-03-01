# glukose-drivers-java
Java glucomenter protocol service implementation using Reverse Engineering Hacking techniques

# Hacked Devices
- [FreeStyle Optium Neo Glucometer from Abbott](http://www.abbottdiabetescare.es/freestyle-optium-neo)

# Sniffing Tools
The sniffing tool used for reverse engineering the glucometer device protocols is the Windows tool called [USBTrace](http://www.sysnucleus.com/)

# The application uses
- [Usb4Java library] (http://usb4java.org/) version 1.2.0: Library to access USB devices.

# Installation
- Create a USB rules (60-freestyle-optium-neo.rules) on your Linux Box directory(/etc/udev/rules.d) using this script, you only must change the group name used when you start your node app. 
```
ATTRS{idVendor}=="1a61", ATTRS{idProduct}=="3850", SUBSYSTEMS=="usb", ACTION=="add", MODE="0666", GROUP+="miguel"
```
- Refresh udev to reflect changes with the command: sudo udevadm trigger

# Execution
The service have two parameters:
- -p: indicate the path where we must export the json result of the service. Default path the same directory where we start the service.
- -w: the wait time (in milliseconds) between the send a command and receice the result from the device. Default time is 1000 ms

An example could be:
```
java -jar glukose.jar -p /opt/glukose/export -w 1500
```
# Result
The result of the service have a JSON format to be consumed by the app glukose. The file created have the format of:
[Device Serial Number]#[Created time].json

An example could be LAGV041S04629#20160225234331.json
```
{"ptname":"THINGTRACK",
 "ptid":"masalinas",
 "measures":[{"value":74.0,"date":"2016-02-23T18:50+0000"},
             {"value":107.0,"date":"2016-02-04T12:02+0000"},
             {"value":0.0,"date":"2016-01-31T00:43+0000"},
             {"value":0.0,"date":"2016-01-31T00:43+0000"},
             {"value":0.0,"date":"2016-01-31T00:26+0000"},
             {"value":0.0,"date":"2016-01-31T00:25+0000"},
             {"value":119.0,"date":"2016-01-30T13:14+0000"},
             {"value":0.0,"date":"2016-01-30T13:08+0000"}],
 "date":"2016-02-29T19:38+0000",
 "serlnum":"LAGV041S04629"}
```

# Licenses
The source code is released under Apache 2.0.
