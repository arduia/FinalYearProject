

#include <EEPROM.h>

#include <Servo.h>
Servo s1a;
Servo s1b;
Servo s2;
Servo s3;
Servo s4;
Servo s5;
Servo s6;


String test = "a";
byte testb[8];
int sv1, sv2, sv3, sv4, sv5, sv6;

void setup() {
  Serial.begin(9600);
  s1a.attach(2);
  s1b.attach(3);
  s2.attach(4);
  s3.attach(5);
  s4.attach(6);
  s5.attach(7);
  s6.attach(8);
 


}
void loop() {
  if (Serial.available()) {
    while (Serial.available()) {

      Serial.readBytes(testb, 8);

      if (testb[0] == 'N') {
        s1a.write(testb[1]);
        s1b.write(180 - testb[1]);
        s2.write(testb[2]);
        s3.write(testb[3]);
        s4.write(testb[4]);
        s5.write(testb[5]);
        s6.write(testb[6]);

      }
    }
  }
}
void Home() {

  if (sv1 < 180 && sv2 < 180 && sv3 < 180 && sv4 < 180 && sv5 < 180 && sv6 < 180) {
    s1a.write(sv1);
    s1b.write(180 - sv1);
    s2.write(sv2);
    s3.write(sv3);
    s4.write(sv4);
    s5.write(sv5);
    s6.write(sv6);

  }

}
void Save() {
  EEPROM.write(1, (byte)sv1);
  EEPROM.write(2, (byte)sv2);
  EEPROM.write(3, (byte)sv3);
  EEPROM.write(4, (byte)sv4);
  EEPROM.write(5, (byte) sv5);
  EEPROM.write(6, (byte)sv6);
}
void Get() {
  sv1 = (int)EEPROM.read(1);
  sv2 = (int)EEPROM.read(2);
  sv3 = (int)EEPROM.read(3);
  sv4 = (int)EEPROM.read(4);
  sv5 = (int)EEPROM.read(5);
  sv6 = (int)EEPROM.read(6);
}
void Print() {

  Serial.print(" sv1 =  ");
  Serial.print(sv1);
  Serial.print(" sv2 =  ");
  Serial.print(sv2);
  Serial.print(" sv3 =  ");
  Serial.print(sv3);
  Serial.print(" sv4 =  ");
  Serial.print(sv4);
  Serial.print(" sv5 =  ");
  Serial.print(sv5);
  Serial.print(" sv6 =  ");
  Serial.print(sv6);
}
