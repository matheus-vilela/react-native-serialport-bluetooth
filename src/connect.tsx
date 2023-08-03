import EventEmitter from 'react-native/Libraries/vendor/emitter/EventEmitter';
import SerialportBluetooth from './native_module';

const DataReceivedEvent = 'serialportDataReceived';

export interface EventData {
  deviceId: number;
  data: string;
}

export type Listener = (data: EventData) => void;

export default class Connect {
  deviceId: number;
  private eventEmitter: EventEmitter;
  private listeners: Listener[];
  buffer = '';

  constructor(deviceId: number, eventEmitter: EventEmitter) {
    this.deviceId = deviceId;
    this.eventEmitter = eventEmitter;
    this.listeners = [];
  }

  send(hexStr: string): Promise<null> {
    return SerialportBluetooth.send(this.deviceId, hexStr);
  }

  private processHexString(hexString: string) {
    // Divide a hex string em pares
    const pairs = hexString.match(/.{1,2}/g);
    // Filtra os pares que não são quebras de linha
    const filteredPairs = pairs?.filter(
      (pair: string) => pair !== '0D' && pair !== '0A'
    );
    // Converte os pares de volta para caracteres e concatena em uma string
    const newString = filteredPairs
      ?.map((pair: string) => String.fromCharCode(parseInt(pair, 16)))
      .join('');
    return newString;
  }

  onReceived(listener: Listener) {
    const listenerProxy = (event: EventData) => {
      this.buffer += event.data.toUpperCase();
      if (event.data.includes('0D')) {
        const data = this.processHexString(this.buffer)?.trim() || '';
        if (data) {
          listener({
            deviceId: event.deviceId,
            data: data,
          });
        }
        this.buffer = '';
      }
    };

    this.listeners.push(listenerProxy);
    return this.eventEmitter.addListener(DataReceivedEvent, listenerProxy);
  }

  close(): Promise<any> {
    this.eventEmitter.removeAllListeners(DataReceivedEvent);
    return SerialportBluetooth.close(this.deviceId);
  }
}
