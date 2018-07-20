definition(
	name: "DoorSensor",
    namespace: "smartthings",
    author: "Seattle University IoT Security Research",
    description: "SmartDoor sensor that incorporates a presence sensor device and a button that actuates on door lock",
    category: "Safety & Security",
    iconURL: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")
    
preferences {
	section ("Lock and Keys") {
    	input "keys", "capability.presenceSensor", title: "Key device(s)", description: "Key owner:", multitple: true, required: false
        input "lock", "capability.lock", title: "Door lock", description: "Actuator for door state", required: false
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    
    initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    
    unsubscribe()
    initialize()
}

def initialize() {
	// Sequence: key is present -> lock will unlock -> door will open
    subscribe(keys, "presence", sensorPresent)
    subscribe(lock, "lock", operateLock)
}

def sensorPresent(evt) {
	// sensor is in proximity
	if (evt.value == "present") {
    	// get the specific sensor id
        def id = getSensorID(evt)
    	initiateUnlock(id)
    }
    // no sensors in proximity
    else {
    	initiateLock()
    }
}

def operateLock(evt) {
	if (evt.value == "lock") {
    	log.debug "Unlock initiated... unlocking"
    }
    else {
    	log.debug "Lock initiated... locking"
    }
    // TODO: send out to endpoint? push notification? text?
}

def operateDoor(evt) {}

def getSensorID(evt) {
	// scan through keys list
    keys?.find{it.id == evt.deviceId}
}

def initiateLock() {
	if (lock.value == "unlock") {
    	log.debug "Lock initiated... locking"
        
        lock?.lock()
    }
    else {
    	log.debug "Lock is already in place"
    }
}

def initiateUnlock(id) {
	if (lock?.value == "lock") {
    	log.debug "Unlock initiated... unlocking"
        log.trace "${id} is in proximity... unlocking"
        
        lock?.unlock()
    }
    else {
    	log.debug "Unlock is already in place"
    }
}

mappings {
	path("/switches") {
    	action: [
        	GET: "listSwitches",
            POST: "listSwitches"
        ]
    }
    path("/switches/:command") {
    	action: [
        	PUT: "updateSwitches"
        ]
    }
}

// TODO: change endpoints to integrate with new app (switches -> presenceSensors, lock, door)
def listSwitches() {
	def resp = []
    keys.each {
    	resp << [name: it.displayName, value: it.currentValue("presenceSensor")]
    }
    return resp
}

void updateSwitches() {
	// use the built-in request object to get the command parameter
    def command = params.command
    
    // all switches have the command
    // execute the command on all switches
    // (note we can do this on the array - the command will be invoked on every element
    switch(command) {
    	case "unlock":
        	lock.unlock()
            break
        case "lock":
        	lock.lock()
            break
        default:
        	httpError(400, "$command is not a valid command!")
    }
}



