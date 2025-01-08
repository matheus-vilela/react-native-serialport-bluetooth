# react-native-serialport-bluetooth

`react-native-serialport-bluetooth` is an open-source library for managing serial port connections over Bluetooth and USB in React Native applications. It provides a simple API for listing devices, requesting permissions, opening connections, sending data, and handling received data.

## Features

- **List Devices**: Retrieve a list of available Bluetooth and USB devices.
- **Request Permissions**: Ensure permissions are granted for accessing devices.
- **Open Connections**: Establish connections to devices with customizable settings.
- **Send Data**: Transmit data to connected devices in hexadecimal format.
- **Close Connections**: Safely terminate connections with devices.
- **Handle Received Data**: Respond to data received from connected devices.

## Compatible Devices

This library supports USB to serial converter chips:

- **FTDI** FT232R, FT232H, FT2232H, FT4232H, FT230X, FT231X, FT234XD
- **Prolific** PL2303
- **Silabs** CP2102, CP210\*
- **Qinheng** CH340, CH341A, CH9102

## Installation

Install the library using npm or yarn:

```bash
npm install react-native-serialport-bluetooth
# or
yarn add react-native-serialport-bluetooth
```

Link the native modules:

```zsh
npx react-native link react-native-serialport-bluetooth
```

## API Reference

Device Interface

```typescript
export interface Device {
  readonly name: string;
  readonly type: 'bluetooth' | 'usb';
  readonly deviceId: number;
  readonly vendorId: number;
  readonly productId: number;
}
```

Methods

### list()

Returns a list of available devices.

```typescript
list(): Promise<Device[]>;
```

Example:

```javascript
const devices = await SerialportBluetooth.list();
console.log(devices);
```

### tryRequestPermission(deviceId: number)

Requests permission to access a specific device.

```typescript
tryRequestPermission(deviceId: number): Promise<number>;
```

Example:

```javascript
const permissionCode = await SerialportBluetooth.tryRequestPermission(123);
if (permissionCode === 1) {
  console.log('Permission granted');
}
```

### hasPermission(deviceId: number)

Checks if the app has permission to access a device.

```typescript
hasPermission(deviceId: number): Promise<boolean>;
```

Example:

```javascript
const hasPermission = await SerialportBluetooth.hasPermission(123);
console.log(`Has permission: ${hasPermission}`);
```

### open()

Opens a connection to a device with the specified parameters.

```typescript
open(
  deviceId: number,
  baudRate: number,
  dataBits: number,
  stopBits: number,
  parity: number,
  readWaitMillis: number,
  writeWaitMillis: number
): Promise<number>;
```

Example:

```javascript
const connectionId = await SerialportBluetooth.open(
  123,
  9600,
  8,
  1,
  0,
  100,
  100
);
console.log(`Connection opened with ID: ${connectionId}`);
```

### send()

Sends data to the connected device in hexadecimal format.

```typescript
send(deviceId: number, hexStr: string): Promise<null>;
```

Example:

```javascript
await SerialportBluetooth.send(123, '4a6f686e');
console.log('Data sent successfully');
```

### close()

Closes the connection to a device.

```typescript
close(deviceId: number): Promise<null>;
```

Example:

```javascript
await SerialportBluetooth.close(123);
console.log('Connection closed');
```

### Event: onReceived

Handles data received from the connected device.

```typescript
SerialportBluetooth.onReceived((data) => {
  console.log('Data received:', data);
});
```

Usage Example

```javascript
import SerialportBluetooth from 'react-native-serialport-bluetooth';

async function connectToDevice() {
  const devices = await SerialportBluetooth.list();
  console.log('Available devices:', devices);

  const device = devices[0]; // Select the first device
  const hasPermission = await SerialportBluetooth.hasPermission(
    device.deviceId
  );

  if (!hasPermission) {
    await SerialportBluetooth.tryRequestPermission(device.deviceId);
  }

  const connectionId = await SerialportBluetooth.open(
    device.deviceId,
    9600, // baudRate
    8, // dataBits
    1, // stopBits
    0, // parity
    100, // readWaitMillis
    100 // writeWaitMillis
  );

  console.log('Connected with ID:', connectionId);

  await SerialportBluetooth.send(device.deviceId, '4a6f686e'); // Send "John" in hex
  SerialportBluetooth.onReceived((data) => {
    console.log('Received data:', data);
  });

  await SerialportBluetooth.close(device.deviceId);
  console.log('Connection closed');
}
```

## Contributing

Contributions are welcome! Please follow these steps to contribute:

1. Fork the repository.
2. Create a new branch for your feature or bugfix.
3. Commit your changes and push the branch.
4. Open a pull request.

## License

`react-native-serialport-bluetooth` is licensed under the MIT License. See [LICENSE](LICENSE) for details.
