definition(
    name: "DoorSensor",
    namespace: "com.doorsensor",
    author: "Seattle University IoT Security Research",
    description: "SmartDoor sensor that incorporates a presence sensor device and a button that actuates on door lock",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
  section ("Sensors") {
    input "key", "capability.presenceSensor", title: "Key device(s)", description: "Key owner:", multiple: true, required: true
  }
  section ("Door and Lock") {
    input "door", "capability.doorControl", title: "Door", description: "Main door to operate on", required: true
    input "lock", "capability.lock", title: "Door lock", description: "Actuator for door state", required: true
  }
}

def installed() {
  initialize()
}

def updated() {
  unsubsribe()
  initialize()
}

def initialize() {
  // Sequence: key is present -> lock will unlock -> door will open 
  subscribe(key, "presence", presenceSensorHandler)
  subscribe(lock, "lock", lockHandler)
  subscribe(door, "door", doorControlHandler)
}

def presenceSensorHandler(evt) {
  if (evt.value == "present") {
    def id = getSensorID(evt) 
    openDoor(id)
  }
  else {
    closeDoor()
  }
}

def openDoor(id) {
  if (door.value == "closed") {
    lock.unlock
    door.open
  }
  else {
    // log debug door is already open
  }
}

def closeDoor() {
  if (door.value == "open") {
    lock.lock
    door.close
  }
  else {
    // log debug door is not open
  }
}

def lockHandler(evt) {
  // TODO: send out to endpoint? push notification? text?
}

def doorControlHandler(evt) {
  // TODO: send out to endpoint? push notification? text?
}

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

// TODO: implement event handlers
// returns a list like
// [[name: "kitchen lamp", value: "off"], [name: "bathroom", value: "on"]]
def listSwitches() {
    def resp = []
    switches.each {
        resp << [name: it.displayName, value: it.currentValue("switch")]
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
            switches.on()
            break
        case "off":
            switches.off()
            break
        default:
            httpError(400, "$command is not a valid command for all switches specified")
    }
}
