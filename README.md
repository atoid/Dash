# Dash

Motorcycle dashboard app for Andoid

This app interfaces with DashBle device to fetch interesting information from motorcycle ECU to phone screen. For example RPM, speed,
temperatures, battery voltage can be decoded from the ECU information tables.

The app has simple layout with GPS centered map and grid of realtime ECU information.

Default gear ratios are set for stock Honda CB500F. Gear ratios can be set by putting the bike on a stand and there is a dialog in the
app to set the ratio for each gear. Tap the gear 1 in the dialog and set bike on gear 1. After the ratio settles, tap next gear and set the bike on next gear. Go through all gears to find out correct ratios and hit Save when ready. Some bikes might report the excact gear
in the table data, maybe that could be an option in the app?

The app handles Honda specific table numbers 0x17 and 0xD1. The table numbers may vary between Honda models, but the data is
pretty much the same. For different tables to be reported, some changes are required in DashBle ECU interfacing code.

See the DashBle repository for the ECU interfacing.
