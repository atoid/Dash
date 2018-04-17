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

## Honda ECU tables 17 and D1

Dash EcuData class currently parses tables 0x17 and 0xD1 which contain useful information and those tables are also somewhat
decoded 'in internet'. Table 17 data at offset 20 could be AFR, this was not previously decoded?

### Table 17

Full message from idling CB500F bike. Offset in decimal, data in hex.

| Offset | Data | Description |
| -- | -- | --- |
|  0 | 02 | Sender (ECU) |
|  1 | 19 | Length |
|  2 | 71 | Cmd |
|  3 | 11 | Table nbr |
|  4 | 06 | RPM hi byte |
|  5 | A2 | RPM lo byte |
|  6 | 1C | TPS voltage |
|  7 | 00 | TPS (0-121) |
|  8 | B8 | ECT voltage |
|  9 | 32 | ECT degrees in Celcius, offset -40 |
| 10 | B7 | IAT voltage |
| 11 | 33 | IAT degrees in Celcius, offset -40 |
| 12 | 72 | MAP voltage |
| 13 | 4C | MAP kPa |
| 14 | FF | ? |
| 15 | FF | ? |
| 16 | 8B | Battery voltage, divide by 10 |
| 17 | 00 | Speed in km/h |
| 18 | 04 | Injectors hi byte? |
| 19 | 35 | Injectors lo byte? |
| 20 | 98 | AFR, divide by 10, not sure about this but could be? |
| 21 | 5B | ? |
| 22 | 17 | ? |
| 23 | 23 | ? |
| 24 | 1E | Checksum |

* TPS = Throttle Position Sensor
* ECT = Engine Coolant Temp
* IAT = Intake Air Temp
* MAP = Manifold Absolute Pressure
* AFR = Air Fuel Ratio

### Table D1

Full message from idling CB500F bike. Offset in decimal, data in hex.

| Offset | Data | Description |
| -- | -- | --- |
|  0 | 02 | Sender (ECU) |
|  1 | 0B | Length |
|  2 | 71 | Cmd |
|  3 | D1 | Table nbr |
|  4 | 01 | Engine off = bit 7 is set, 1 = neutral, 3 = kickstand |
|  5 | 00 | ? |
|  6 | 00 | ? |
|  7 | 00 | ? |
|  8 | 01 | Engine on = 1, off = 0|
|  9 | 00 | ? |
| 10 | AF | Checksum |	
