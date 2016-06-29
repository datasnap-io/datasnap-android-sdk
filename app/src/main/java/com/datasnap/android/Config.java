package com.datasnap.android;

/**
 * Created by paolopelagatti on 6/27/16.
 */
public class Config {
  private Builder builder;

  private Config(Builder builder) {
    this.builder = builder;
  }

  public String getApiKeyId() {
    return builder.apiKeyId;
  }

  public String getApiKeySecret() {
    return builder.apiKeySecret;
  }

  public String getOrganizationId() {
    return builder.organizationId;
  }

  public String getProjectId() {
    return builder.projectId;
  }

  public VendorProperties getVendorProperties() {
    return builder.vendorProperties;
  }

  public static class Builder {
    private String apiKeyId;
    private String apiKeySecret;
    private String organizationId;
    private String projectId;
    private VendorProperties vendorProperties;

    public Builder() {

    }
    public Config build(){
      //TODO check that data is initialized
      return new Config(this);
    }

    public Builder setOrganizationId(String organizationId) {
      this.organizationId = organizationId;
      return this;
    }

    public Builder setApiKeyId(String apiKeyId) {
      this.apiKeyId = apiKeyId;
      return this;
    }

    public Builder setApiKeySecret(String apiKeySecret) {
      this.apiKeySecret = apiKeySecret;
      return this;
    }

    public Builder setProjectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public Builder setVendorProperties(VendorProperties vendorProperties) {
      this.vendorProperties = vendorProperties;
      return this;
    }
  }
}
