package com.datasnap.android.eventproperties;

public class Communication extends Property {

    private String description;
    private Tags tags;
    private String identifier;
    private String status;
    private String communicationVendorId;
    private String name;
    private Type types;
    private Content content;


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Tags getTags() {
        return tags;
    }

    public void setTags(Tags tags) {
        this.tags = tags;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCommunicationVendorId() {
        return communicationVendorId;
    }

    public void setCommunicationVendorId(String communicationVendorId) {
        this.communicationVendorId = communicationVendorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getTypes() {
        return types;
    }

    public void setTypes(Type types) {
        this.types = types;
    }

    public Content getContent() {
        return content;
    }

    public void setContent(Content content) {
        this.content = content;
    }


}



