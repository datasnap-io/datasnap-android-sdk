package com.datasnap.android.eventproperties;

public class DeviceInfo extends Property {

    public String created;
    public Device device;
    private static DeviceInfo deviceInfo;

    public static DeviceInfo getInstance(){
        return deviceInfo;
    }

    public static void initialize(DeviceInfo info){
        deviceInfo = info;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

}
