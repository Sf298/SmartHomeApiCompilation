# SmartHomeApiCompilation
This project is a Java API for controlling smart home products from various companies. The purpose of this project is to provide common wrapers for devices of similar type, i.e. smart lights from various companies like Philips or Lifx.

## About
Supported devices include:
* Phillips Hue Light Bulbs (Philips Hue API)
* Lifx Light Bulbs (LANProtocol) via [LifxCommander_v1.0](https://github.com/olsenn1/LifxCommander_v1.0)
* TP-Link Smart Plugs via [hs-100](https://github.com/CalicoCatalyst/hs-100)
* Arduino window blinds (will include arduino code)


Planned device support:
* Logitech Harmony Hub
* Meross
* Tuya Smart Life

## Library Overview
This API provides two types of classes. The first type, the API Wrapper must contain the discoverDevices() method. This method behaves similarly to a factory method and should scan the network for available devices then return them as a Collection<Device> object.

The second type of class is Device and its subclasses. A Device object will contain unique identifier information such as its IP address, port number or device ID. Subclasses of Device should provide abstract header methods for controlling devices of that type. For example a WiFi power plug device subclass should contain a method header for setting and getting the power state.

It is recommended that factory classes also impelment to[TYPE]Device() methods. These methods effectively convert the provided Device object into one of its more specific subclasses while filling in the bodies of the abstract methods using the communication protocols specific to the wrapped API. Doing this allows the complex communication protocols to be entirely hidden from the user.
