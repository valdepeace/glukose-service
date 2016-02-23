# glukose-drivers-java
Java glucomenter protocol service implementation using Reverse Engineering Hacking techniques

# Hacked Devices
- [FreeStyle Optium Neo Glucometer from Abbott](http://www.abbottdiabetescare.es/freestyle-optium-neo)

# Sniffing Tools
The sniffing tool used for reverse engineering the glucometer device protocols is the Windows tool called [USBTrace](http://www.sysnucleus.com/)

# The application uses
- [Usb4Java library] (http://usb4java.org/) version 1.2.0: Library to access USB devices.

# Installation
- Add the USB rules (60-freestyle-optium-neo.rules) on your Linux Box directory(/etc/udev/rules.d) using the script contributed in the project, you only must change the group user used when you start your node app. 
- Refresh udev to reflect changes with the command: sudo udevadm trigger

# Licenses
The source code is released under Apache 2.0.