# Dash
Motorcycle dashboard app for Andoid

This app interfaces with DashBle device to fetch interesting information from motorcycle ECU to phone screen. For example RPM, speed,
temperatures, battery voltage can be decoded from the ECU information tables.

The app has simple layout with GPS centered map and grid of realtime ECU information.

This app was designed for CB500F and gear rate calculatio is for that bike. Changing the ratios for different models should be no
problem. Ratios could be predefined per bike model or user could input the ratio for each gear. Also some bikes might report the
excact gear in the table data?

The app handles Honda specific table numbers 0x17 and 0xD1. The table numbers may vary between Honda models, but the data is
pretty much the same. For different tables to be reported, some changes are required in DashBle ECU interfacing code.

See the DashBle repository for the ECU interfacing.
