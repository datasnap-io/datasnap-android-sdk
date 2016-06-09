package com.datasnap.android.events;

public enum EventType {
  BEACON_ARRIVED, // A users first time receiving a broadcast from a particular beacon.
  BEACON_SIGHTING, // A user is within a particular beacon range and has received a broadcast from the beacon.
  BEACON_DEPART_VENDOR, // This event can be thrown when a vendors's SDK detect an event. For some vendors this setting can be erroneous so its recommended ot trackEvent separately for audit reporting.
  BEACON_DEPART, // A true departed event meaning the user has departed and we feel confident that this event is correct. Some folks use their own calculation to throw this event.
  BEACON_VISIT, // A true user visit into a proximity or geotrigger. This is many times calulated by looking at sighting data or inferred through the use of the departed event.
  BEACON_VISIT_VENDOR, // A user visit into a proximity or geotrigger as sent by the beacon vendor. This is used ot compare with the true calculation of the beacon_visit event.
  COMMUNICATION_DELIVERED, // - A communication has been delivered to the user. It can be associated with a campaign or not. For mobile these are typically push notifications or in App Modals.
  CAMPAIGN_REQUEST, // - The app has requested a campaign to be show to the user.
  CAMPAIGN_REDEMPTION, // - The user has redeemed a coupon or similar as part of the campaign.
  GEOFENCE_ARRIVE, // - A user has entered a geofence.
  GEOFENCE_SIGHTING, // - A user is still in the geofence after a interval based check of their location.
  GEOFENCE_DEPART, // - A user has left the geofence.
  GLOBAL_POSITION_SIGHTING, // - Records the Device/Users current Global Position.
  OPT_IN_LOCATION, // - The user has opted in to trackEvent their location and accepted all appropriate terms of use.
  OPT_IN_PUSH_NOTIFICATIONS, // - The user has opted in to receive push notifications.
  OPT_IN_VENDOR, // - A user has opted in to the vendors specific terms.
  APP_INSTALLED, // - The user installed the app
  INTERACTION_TAP, // - User tapped a communication (in app or push notification)
  INTERACTION_VIEW, //  - User viewed something
  INTERACTION_SWIPE, // - User swiped
  INTERACTION_SHAKE, // - user shook the device
  INTERACTION_TILT, // - User tilted the devices
  PLACE_UPDATE, //- Indicates that a place update has occurred. Usually would only contain the place.
  CAMPAIGN_UPDATE, // - Indicates that a campaign update has occurred. Usually would only contain the campaign.
  BEACON_UPDATE, // - Indicates that a beacon update has occurred. Usually would only contain the beacon.
  USER_UPDATE, // - Indicates that a beacon update has occurred. Usually would only contain the beacon.
  COMMUNICATION_UPDATE // - Indicates that a beacon update has occurred. Usually would only contain the beacon.
}
