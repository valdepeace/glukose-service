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

# Licenses
The source code is released under Apache 2.0.
