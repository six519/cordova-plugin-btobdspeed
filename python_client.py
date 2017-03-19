import bluetooth
import time
from threading import Thread
import serial

SERVICE_UUID = "fa87c0d0-afac-11de-8a39-0800200c9a66"

class MessagingThread(Thread):

    def __init__(self, btSocket):
        super(MessagingThread, self).__init__()
        self.socket = btSocket

    def run(self):
        while True:
            data = self.socket.recv(1024)
            print "Data received: %s" % data

            if data == "END":
                self.socket.close()
                break

class SendingMessageThread(Thread):

    def __init__(self, btSocket, thisSerial):
        super(SendingMessageThread, self).__init__()
        self.socket = btSocket
        self.thisSerial = thisSerial

    def run(self):
        while True:
            self.thisSerial.write(b"010D\r\n")
            dt = self.thisSerial.readline()

            speed_hex = dt.split(' ')
            speed = float(int('0x' + speed_hex[2] ,0))
            #print "The speed is: %s km/h" % speed
            self.socket.send(str(int(speed)))
            time.sleep(1)

class ServiceThread(Thread):

    def __init__(self):
        super(ServiceThread, self).__init__()

    def run(self):
        while True:
            service_matches = bluetooth.find_service(uuid=SERVICE_UUID)

            if len(service_matches) > 0:
                print "Service found..."

                first_match = service_matches[0]
                port = first_match["port"]
                name = first_match["name"]
                host = first_match["host"]

                sock=bluetooth.BluetoothSocket(bluetooth.RFCOMM)
                sock.connect((host, port))
                print "Connected to %s..." % host
                messagingThread = MessagingThread(sock)
                messagingThread.start()
                ser = serial.Serial('/dev/ttyUSB0', 38400, timeout=1)
                sendThread = SendingMessageThread(sock, ser)
                sendThread.start()
                break
            
            time.sleep(2)

if __name__ == "__main__":
    serviceThread = ServiceThread()
    serviceThread.start()