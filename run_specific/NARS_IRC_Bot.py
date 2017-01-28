import thread
from subprocess import Popen, PIPE, STDOUT
import subprocess
import re,os
import socket
import sys
import time
import itertools
import re



knowledge=""""""

# is the nars package compiled as a public release version and moved to the root?
runInReleaseEnvironment = False

def init_proc():
    global proc
    if runInReleaseEnvironment:
        proc = subprocess.Popen(["java","-cp","OpenNARS.jar","nars.io.NARConsole","1"], stdin=subprocess.PIPE, stdout=subprocess.PIPE)
    else:
        proc = subprocess.Popen(["java","-cp", "lib\\*;dist\\*", "nars.io.NARConsole"], stdin=subprocess.PIPE, stdout=subprocess.PIPE)

server = "irc.freenode.net"
channel = "#nars"
botnick = "nars_squared"
Narsese_Filter=["EXE: ","Answer:"]
Max_Outputs_Before_Reset=30

irc = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
irc.connect((server, 6667))
irc.send("USER "+ botnick +" " + botnick +" "+ botnick +" :Hallo!\r\n")
irc.send("NICK "+ botnick +"\r\n")
irc.send("PRIVMSG nickserv :iNOOPE\r\n")
irc.send("JOIN "+ channel +"\r\n") #fails on euirc, works in all others, euirc is not completely RFC 2810 compatible maybe?
print("connected")
init_proc()
print("NARS instance created")
proc.stdin.write("*volume=0\n");
print("NARS is set to shut up: "+proc.stdout.readline())
print("Narsese Filter applied: '"+str(Narsese_Filter)+"'")
print("Max Outputs before Reset: "+str(Max_Outputs_Before_Reset))
cnt=0

proc.stdin.write(knowledge)
print("Knowledge put in")

def receive_thread(a):
    global cnt
    while True:
       msg=proc.stdout.readline()
       if "% {" in msg:
           msg=msg.split("% {")[0]+"%"
       if msg!=None and msg!="" and True in [u in msg for u in Narsese_Filter]: #we received an execution
          cnt+=1
          print("NAR output: "+msg)
          bReset= cnt>=Max_Outputs_Before_Reset
          irc.send("PRIVMSG "+ channel +" : "+msg+"\r\n")
          if bReset:
              irc.send("PRIVMSG "+ channel +" : NAR RESET happened (max. amount of output happened)\r\n")
              cnt=0
              #proc.close()
              #init_proc();
              proc.stdin.write("*****\n")
              proc.stdin.write(knowledge)
              print("Knowledge put in")

def narInput(text):
    print("NAR input: "+TEXT)
    proc.stdin.write(TEXT+"\n")

thread.start_new_thread(receive_thread,(1,))

while True:
    try:
        text=irc.recv(2040)
        if "PING" in text:
            print("ping")
            STR='PONG :' + text.split("PING :")[1].split("\n")[0] + '\r\n';
            irc.send(STR)
        else:
            if "VERSION" in text:
                print("version")
                irc.send("JOIN "+ channel +"\r\n") #join when version private message comes :D
            else:
                if "system" in text.lower() or "from os" in text.lower() or "import os" in text.lower():
                    print("skipped")
                    continue
                SPL=text.split(":")
                TEXT=":".join(SPL[2:len(SPL)])
                if TEXT.replace(" ","").replace("\n","").replace("\r","")=="":
                    continue
                print(TEXT)
                if TEXT[:2] == "**":
                    proc.stdin.write("*****\n")
                    proc.stdin.write(knowledge)
                    print("Knowledge put in")
                elif TEXT[0] == "<" or TEXT[0] == "(":
                    try:
                        narInput(TEXT)
                    except:
                        None
                elif TEXT[:6] == "*start":
                    proc.stdin.write("*start\n") # transmit special token
                elif TEXT[:5] == "*stop":
                    proc.stdin.write("*stop\n") # transmit special token


                # if it is a number it means the user want to wait n cycles so we pass it to NARS
                m = re.search("(\d+)", TEXT)
                if m:
                    narInput(TEXT)


    except:
        print("exception")
        None
