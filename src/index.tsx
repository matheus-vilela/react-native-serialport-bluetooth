import { NativeEventEmitter, NativeModules, Platform } from 'react-native';
import Connect from './connect';
import SerialportBluetooth, { type Device } from './native_module';

export type { EventData, Listener } from './connect';
export type { Connect, Device };

export interface OpenOptions {
  baudRate: number;
  parity: Parity;
  dataBits: number;
  stopBits: number;
  readWaitMillis?: number;
  writeWaitMillis?: number;
}

export enum Parity {
  None = 0,
  Odd,
  Even,
  Mark,
  Space,
}

export interface Manager {
  list(): Promise<Device[]>;
  tryRequestPermission(deviceId: number): Promise<boolean>;
  hasPermission(deviceId: number): Promise<boolean>;
  open(deviceId: number, options: OpenOptions): Promise<Connect>;
}

const defaultManager: Manager = {
  list(): Promise<Device[]> {
    return SerialportBluetooth.list();
  },

  async tryRequestPermission(deviceId: number): Promise<boolean> {
    const result = await SerialportBluetooth.tryRequestPermission(deviceId);
    return result === 1;
  },

  hasPermission(deviceId: number): Promise<boolean> {
    return SerialportBluetooth.hasPermission(deviceId);
  },

  async open(deviceId: number, options: OpenOptions): Promise<Connect> {
    if (Platform.OS !== 'android')
      throw new Error(`Not support ${Platform.OS}`);
    return SerialportBluetooth.open(
      deviceId,
      options.baudRate,
      options.dataBits,
      options.stopBits,
      options.parity,
      options.readWaitMillis || 200,
      options.writeWaitMillis || 200
    ).then(() => {
      return Promise.resolve(
        new Connect(
          deviceId,
          new NativeEventEmitter(NativeModules.SerialportBluetooth)
        )
      );
    });
  },
};

export const SerialBluetoothManager: Manager =
  Platform.OS === 'android'
    ? defaultManager
    : (new Proxy(
        {},
        {
          get() {
            return () => {
              throw new Error(`Not support ${Platform.OS}`);
            };
          },
        }
      ) as Manager);
