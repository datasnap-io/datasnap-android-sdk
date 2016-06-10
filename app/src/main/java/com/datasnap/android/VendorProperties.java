package com.datasnap.android;

import java.util.ArrayList;

/**
 * Created by paolopelagatti on 5/24/16.
 */
public class VendorProperties {

  public enum Vendor{
    GIMBAL,
    ESTIMOTE
  }

  private String gimbalApiKey;
  private ArrayList<Vendor> vendor = new ArrayList<>();

  public ArrayList<Vendor> getVendor() {
    return vendor;
  }

  public void addVendor(Vendor vendor) {
    this.vendor.add(vendor);
  }

  public String getGimbalApiKey() {
    return gimbalApiKey;
  }

  public void setGimbalApiKey(String gimbalApiKey) {
    this.gimbalApiKey = gimbalApiKey;
  }

}
