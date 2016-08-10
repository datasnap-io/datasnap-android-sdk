package com.datasnap.android.eventproperties;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;

public class Device extends Property {

  private String userAgent;
  private String ipAddress;
  private String platform;
  private String osVersion;
  private String model;
  private String manufacturer;
  private String name;
  private String vendorId;
  private String carrierName;
  private String countryCode;
  private String networkCode;
  private static Device instance;

  public static void initialize(Context context) {
    instance = new Device(context);
  }

  public static Device getInstance() {
    return instance;
  }

  private Device(Context context) {
    WifiManager wifiMan = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    WifiInfo wifiInf = wifiMan.getConnectionInfo();
    int ip = wifiInf.getIpAddress();
    ipAddress = String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
    setPlatform(android.os.Build.VERSION.SDK);
    setOsVersion(System.getProperty("os.version"));
    setModel(android.os.Build.MODEL);
    setManufacturer(android.os.Build.MANUFACTURER);
    setName(android.os.Build.DEVICE);
    setVendorId(android.os.Build.BRAND);
    TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    setCarrierName(manager.getNetworkOperatorName());
  }


  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public String getPlatform() {
    return platform;
  }

  public void setPlatform(String platform) {
    this.platform = platform;
  }

  public String getOsVersion() {
    return osVersion;
  }

  public void setOsVersion(String osVersion) {
    this.osVersion = osVersion;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public String getManufacturer() {
    return manufacturer;
  }

  public void setManufacturer(String manufacturer) {
    this.manufacturer = manufacturer;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVendorId() {
    return vendorId;
  }

  public void setVendorId(String vendorId) {
    this.vendorId = vendorId;
  }

  public String getCarrierName() {
    return carrierName;
  }

  public void setCarrierName(String carrierName) {
    this.carrierName = carrierName;
  }

  public String getCountryCode() {
    return countryCode;
  }

  public void setCountryCode(String countryCode) {
    this.countryCode = countryCode;
  }

  public String getNetworkCode() {
    return networkCode;
  }

  public void setNetworkCode(String networkCode) {
    this.networkCode = networkCode;
  }


}
