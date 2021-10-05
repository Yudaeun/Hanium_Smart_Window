import threading
import adafruit_dht
from board import *
import time
import os
import glob
import RPi.GPIO as GPIO
GPIO.setmode(GPIO.BCM)
from bluetooth import *
from sds011 import SDS011
s=SDS011('/dev/ttyUSB0')
dht_pin = D17
dht22 = adafruit_dht.DHT22(dht_pin, use_pulseio=False)
temperature=0
humidity=0
mise_val=0
winFlag=True
auto=0
waiting=0
s.sleep(sleep=False)
StepPins=[12,16,20,21]
# change the ani distraction
# Seq=[[1,0,0,0], [0,1,0,0], [0,0,1,0], [0,0,0,1]]



def get_dht():

    global temperature
    global humidity
    while True:
        temperature=dht22.temperature
        humidity = dht22.humidity
        #print(f"Humidity = {humidity:.2f}")
        #print(f"Temperature = {temperature:.2f}C")
        

def server():
    global mise_val
    global temperature
    global humidity
    global auto
    connection = False
    server_sock=BluetoothSocket( RFCOMM )
    server_sock.bind(("",PORT_ANY))
    server_sock.listen(1)

    port = server_sock.getsockname()[1]

    uuid = "94f39d29-7d6d-437d-973b-fba39e49d4ee"

    advertise_service(server_sock, "VoltMeterPiServer",
                      service_id=uuid,
                      service_classes=[uuid, SERIAL_PORT_CLASS],
                      profiles=[SERIAL_PORT_PROFILE]
                      #                   protocols = [ OBEX_UUID ]
                      )
    while True:
        data_list=[]
        if(connection==False):
            client_sock, client_info = server_sock.accept()
            connection=True
            print("connected")
        try:
            data=client_sock.recv(1024)
            data = str(data, 'utf-8')
            print("string from there: "+data)
            if(data=="disconnect"):
                client_sock.close()
                connection=False
                print("disconnect") 
            elif('connect' in data):#changed
                #time.sleep(10)
                data_list=data.split("*")#connect*auto*openclose(default is close)
                print("send")
                send_string=str(round(mise_val,2))+"*"+str(temperature)+"*"+str(humidity)+"*"+str(waiting)
                client_sock.send(send_string)#mise*temp*humi*waiting
                if(data_list[1]=="0"):#auto
                    auto=0
                elif(data_list[1]=="1" ):#manual
                    auto=1
                    if(data_list[2]=="open" and waiting==0):
                        #call open method
                        if(winFlag==False):
                            win_open()
                    elif(data_list[2]=="close" and waiting==0):
                        #call close method
                        if(winFlag==True):
                            win_close()

            else:
                print("no one")
        except IOError:
            print("connection disconnected")
            client_sock.close()
            connection=False
            pass
        except BluetoothError:
	        print("Something wrong with bluetooth")
        except KeyboardInterrupt:
            print("\nDisconnected")
            client_sock.close()
            server_sock.close()
            break

def win_open():
    StepCounter=0
    StepCount=4
    global winFlag
    global waiting
    waiting=1
    print("open")
    Seq=[[0,0,0,1], [0,0,1,0],[0,1,0,0],[1,0,0,0]]
    #open window
    for _ in range(0,6000):
        for pin in range(0, 4):
            xpin=StepPins[pin]
            if Seq[StepCounter][pin]!=0:
                GPIO.output(xpin, True)
            else:
                GPIO.output(xpin,False)
        StepCounter+=1

        if(StepCounter==StepCount):
            StepCounter=0
        if(StepCounter<0):
            StepCounter=StepCount

        time.sleep(0.01)
    winFlag=True#opened window
    waiting=0

def win_close():
    global waiting
    global winFlag
    waiting=1
    StepCounter=0
    StepCount=4
    print("close")
    Seq2=[[1,0,0,0], [0,1,0,0],[0,0,1,0],[0,0,0,1]]
    #closing window
    for _ in range(0,6000):
        for pin in range(0, 4):
            xpin=StepPins[pin]
            if Seq2[StepCounter][pin]!=0:
                GPIO.output(xpin, True)
            else:
                GPIO.output(xpin,False)
        StepCounter+=1

        if(StepCounter==StepCount):
            StepCounter=0
        if(StepCounter<0):
            StepCounter=StepCount

        time.sleep(0.01)
    winFlag=False#change window state
    waiting=0

def mise():#check mise val, close, 
    global auto
    global mise_val
    global winFlag
    while True:
        val=s.query()
        print('PM2.5:',val[0],'PM10:',val[1])
        mise_val=val[1]
        time.sleep(2)
        if(auto==0):
            if (val[1] <= 50):#mise low
                if (winFlag== False):#when window closed
                    #call open method
                    win_open()
                
            else:#mise high
                if (winFlag == True):#when window opened
                    #call close method
                    win_close()

GPIO.cleanup()
for pin in StepPins:
    GPIO.setup(pin,GPIO.OUT)#set gpio output
    GPIO.output(pin,False)#set gpuo false

dht_thread=threading.Thread(target=get_dht)
server_thread=threading.Thread(target=server)
mise_thread=threading.Thread(target=mise)
dht_thread.daemon=True
server_thread.daemon=True
mise_thread.daemon=True
server_thread.start()
dht_thread.start()
mise_thread.start()
server_thread.join()
dht_thread.join()
mise_thread.join()
print("end of program!")
GPIO.cleanup()

