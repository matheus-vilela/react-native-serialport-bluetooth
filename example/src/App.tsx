import * as React from 'react';

import { StyleSheet, View, Text, Button } from 'react-native';
import { SerialBluetoothManager } from 'react-native-serialport-bluetooth';

export default function App() {
  const [rfid, setRfid] = React.useState<string>('');

  const readRfidCard = async () => {
    try {
      setRfid('Waiting card...');
      const rfid = await SerialBluetoothManager.readRfidCard();
      setRfid(rfid);
    } catch (e) {
      setRfid('Erro!');
      console.error(e);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.text}>RFID found: '{rfid}'</Text>
      <Button title="Search RFID" onPress={readRfidCard} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
  text: {
    fontSize: 16,
    marginBottom: 20,
  },
});
