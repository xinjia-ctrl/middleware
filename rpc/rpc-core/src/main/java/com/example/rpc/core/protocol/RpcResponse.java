package com.example.rpc.core.protocol;

/**
 * RPC 响应体
 */
public class RpcResponse {

    private Object result;
    private Throwable exception;
    private boolean success;

    public RpcResponse() {
    }

    public RpcResponse(Object result) {
        this.result = result;
        this.success = true;
    }

    public RpcResponse(Throwable exception) {
        this.exception = exception;
        this.success = false;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
