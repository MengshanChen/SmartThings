definition(
    name: "DoorSensor",
    namespace: "com.doorsensor",
    author: "Seattle University IoT Security Research",
    description: "SmartDoor sensor that incorporates a presence sensor device and a button that actuates on door lock",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences(oauthPage: "deviceAuthorization") {
  page(name: "deviceAuthorization", title: "", nextPage: "instructionPage", install: false, uninstall: true) {
    section ("Sensors") {
      input "keys", "capability.presenceSensor", title: "Key device(s)", description: "Key owner:", multiple: true, required: true
    }
    section ("Door and Lock") {
      input "door", "capability.doorControl", title: "Door", description: "Main door to operate on", required: true
      input "lock", "capability.lock", title: "Door lock", description: "Actuator for door state", required: true
    }
  }

  page(name: "insturctionPage", title: "Device Discovery", install: true) {
    section() {
      paragraph "Test"
    }
  }
}

def installed() {
  log.debug "Installed with settings: ${settings}"
  
  initialize()
}

def updated() {
  log.debug "Updated with settings: ${settings}"

  unsubsribe()
  initialize()
}

def initialize() {
  // Sequence: key is present -> lock will unlock -> door will open 
  subscribe(keys, "presence", presenceSensorHandler)
  subscribe(lock, "lock", lockHandler)
  subscribe(door, "door", doorControlHandler)
}

def presenceSensorHandler(evt) {
  // sensor is in proximity
  if (evt.value == "present") {
    // get the specific sensor id
    def id = getSensorID(evt) 
    openDoor(id)
  }
  // no sensors in proximity
  else {
    closeDoor()
  }
}

def openDoor(id) {
  if (door.value == "closed") {
    lock.unlock
    door.open

    log.debug "Door unlocked by: ${id}"
  }
  else {
    log.debug "Door already open"
  }
}

def closeDoor() {
  if (door.value == "open") {
    lock.lock
    door.close

    log.debug "Door locked"
  }
  else {
    log.debug "Door already closed"
  }
}

def lockHandler(evt) {
  if (evt.value == "lock") {
    log.debug "Unlock initiated... unlocking"
  }
  else {
    log.debug "Lock initiated... locking"
  }
  // TODO: send out to endpoint? push notification? text?
}

def doorControlHandler(evt) {
  // TODO: send out to endpoint? push notification? text?
}

def getSensorID(evt) {
  // scan through keys list 
  keys.find{it.id == evt.deviceId}
}


// this section is outdated**
mappings {
  path("/switches") {
    action: [
      GET: "listSwitches"
    ]
  }
  path("/switches/:command") {
    action: [
      PUT: "updateSwitches"
    ]
  }
}

// TODO: change endpoints to integrate with new app (switches -> presenceSensors, lock, door)
// returns a list like
// [[name: "kitchen lamp", value: "off"], [name: "bathroom", value: "on"]]
def listSwitches() {
    def resp = []
    keys.each {
      resp << [name: it.displayName, value: it.currentValue("keys")]
    }
    return resp
}

void updateSwitches() {
    // use the built-in request object to get the command parameter
    def command = params.command

    // all switches have the comand
    // execute the command on all switches
    // (note we can do this on the array - the command will be invoked on every element
    switch(command) {
        case "on":
            lock.unlock()
            break
        case "off":
            lock.lock()
            break
        default:
            httpError(400, "$command is not a valid command for all switches specified")
    }
}
