package com.lmgy.redirect.bean;

import java.io.Serializable;

/*
 * Created by lmgy on 14/8/2019
 */
public class HostData implements Serializable {

    private static final long serialVersionUID = -5406585850636078136L;
    private boolean type;
    private String ipAddress;
    private String hostName;
    private String remark;

    public HostData(boolean type, String ipAddress, String hostName, String remark){
        this.type = type;
        this.ipAddress = ipAddress;
        this.hostName = hostName;
        this.remark = remark;
    }

    public boolean getType() {
        return type;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getHostName() {
        return hostName;
    }

    public String getRemark() {
        return remark;
    }

    public void setType(boolean type) {
        this.type = type;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
