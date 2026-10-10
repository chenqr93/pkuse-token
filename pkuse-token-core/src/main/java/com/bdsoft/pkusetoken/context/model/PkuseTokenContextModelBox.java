package com.bdsoft.pkusetoken.context.model;

/**
 * Box 盒子类，用于存储 [ PkuseRequest、PkuseResponse、PkuseStorage ] 三个包装对象
 *
 * @author click33
 * @since 1.16.0
 */
public class PkuseTokenContextModelBox {

    public PkuseRequest request;

    public PkuseResponse response;

    public PkuseStorage storage;

    public PkuseTokenContextModelBox(PkuseRequest request, PkuseResponse response, PkuseStorage storage) {
        this.request = request;
        this.response = response;
        this.storage = storage;
    }

    public PkuseRequest getRequest() {
        return request;
    }

    public void setRequest(PkuseRequest request) {
        this.request = request;
    }

    public PkuseResponse getResponse() {
        return response;
    }

    public void setResponse(PkuseResponse response) {
        this.response = response;
    }

    public PkuseStorage getStorage() {
        return storage;
    }

    public void setStorage(PkuseStorage storage) {
        this.storage = storage;
    }

    @Override
    public String toString() {
        return "Box [request=" + request + ", response=" + response + ", storage=" + storage + "]";
    }

}