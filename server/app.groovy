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

  sections ("Notifications") {
    input ("receipients", "contacts", title: "Send notifications to:", required: false) {
      input "pushNotifications", "enum", title: "Send push notifications?", options: ["Yes", "No"], required: false
      input "textNotifications", "phone", title "Send text messages?", required: false
    }
  }
}

include 'asynchttp_v1'

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
}

def sensorPresent(evt) {
  // sensor is in proximity
  if (evt.value == "present") {
    // get the specific sensor id
      def id = getSensorID(evt)
    unlockDoor(id)
  }
  // no sensors in proximity
  else {
    lockDoor()
  }
}

private getSensorID(evt) {
  // scan through keys list
  keys.find{it.id == evt.deviceId}
}

private lockDoor() {
  if (lock.value == "unlock") {
    log.debug "Lock initiated... locking"
      
    lock.lock()
  }
  else {
    log.debug "Lock is already in place"
  }
}

private unlockDoor(id) {
  if (lock.value == "lock") {
    log.debug "Unlock initiated... unlocking"
    log.trace "${id} is in proximity... unlocking"

    lock.unlock()
    msg = "${id} is present, unlocking"
    sendNotifications(msg)
  }
  else {
    log.debug "Unlock is already in place"
  }
}

private sendNotifications(msg) {
  if (location.contactBookEnabled) {
    log.debug "Sending notifications to ${recipients?.size()}

    sendNotificationsToContacts(msg, recipients)
  } 
  else {
    // if push notifications is enabled
    if (pushNotifications != "No") {
      log.debug "Sending push message"

      sendPush(msg)
    }

    // if text messages is enabled
    if (textNotifications) {
      log.debug "Sending text message"

      sendSms(textNotifications, msg)
    }
  }

  // optional send to endpoint
  listActivity(msg)
}

mappings {
  path("/activity") {
    action: [
      GET: "listActivity",
      POST: "listActivity"
    ]
  }
  
  path("/keys") {
    action: [
      GET: "listKeyOwners",
      POST: "listKeyOwners"
    ]
  }

  path("/switches/:command") {
    action: [
      PUT: "updateSwitches"
    ]
  }
}

def listActivity(msg) {
  def resp = []
  
  resp << msg
  resp << [name: it.displayName, value: it.currentValue("presenceSensor")]
  
  return resp
}

def listKeyOwners() {
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
