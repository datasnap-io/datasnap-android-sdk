package com.datasnap.android;

/**
 * Created by paolopelagatti on 5/24/16.
 */
public class VendorProperties {

  public enum Vendor{
    GIMBAL,
    ESTIMOTE
  }

  private String gimbalApiKey;
  private Vendor vendor;

  public Vendor getVendor() {
    return vendor;
  }

  public void setVendor(Vendor vendor) {
    this.vendor = vendor;
  }

  public String getGimbalApiKey() {
    return gimbalApiKey;
  }

  public void setGimbalApiKey(String gimbalApiKey) {
    this.gimbalApiKey = gimbalApiKey;
  }

}
