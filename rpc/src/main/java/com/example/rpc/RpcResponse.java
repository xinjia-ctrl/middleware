package com.example.rpc;

import java.io.Serializable;

public class RpcResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;
    private Object data;
    private String message;

    public RpcResponse() {
    }

    public RpcResponse(int code, Object data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public boolean isSuccess() {
        return code == 200;
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    @Override
    public String toString() {
        return "RpcResponse{code=" + code + ", data=" + data + ", message='" + message + "'}";
    }
}
