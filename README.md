# Dash

Motorcycle dashboard app for Andoid

This app interfaces with DashBle device to fetch interesting information from motorcycle ECU to phone screen. For example RPM, speed,
temperatures, battery voltage can be decoded from the ECU information tables.

The app has simple layout with GPS centered map and grid of realtime ECU information.

Default gear ratios are set for stock Honda CB500F. Gear ratios can be set in a dialog in the app. Put the bike on a stand,
start the bike and set on gear 1. When ratio settles, tap 'Gear 1' and the ratio is set for the gear. Go through all gears and 
tap 'Save' when ready. Now the app should find correct gear from speed/RPM ratio. If no gear is found, ratio is shown instead.

Some bikes might report the excact gear in the table data, maybe that could be an option in the app?

The app handles Honda specific table numbers 0x17 and 0xD1. The table numbers may vary between Honda models, but the data is
pretty much the same. For different tables to be reported, some changes are required in DashBle ECU interfacing code.

See the DashBle repository for the ECU interfacing.
