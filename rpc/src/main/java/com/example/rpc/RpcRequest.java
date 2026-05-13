package com.example.rpc;

import java.io.Serializable;
import java.util.Arrays;

public class RpcRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String interfaceName;
    private String methodName;
    private Class<?>[] parameterTypes;
    private Object[] parameters;

    public RpcRequest() {
    }

    public RpcRequest(String interfaceName, String methodName, Class<?>[] parameterTypes, Object[] parameters) {
        this.interfaceName = interfaceName;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.parameters = parameters;
    }

    public String getInterfaceName() { return interfaceName; }
    public void setInterfaceName(String interfaceName) { this.interfaceName = interfaceName; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public Class<?>[] getParameterTypes() { return parameterTypes; }
    public void setParameterTypes(Class<?>[] parameterTypes) { this.parameterTypes = parameterTypes; }

    public Object[] getParameters() { return parameters; }
    public void setParameters(Object[] parameters) { this.parameters = parameters; }

    @Override
    public String toString() {
        return "RpcRequest{interface=" + interfaceName + ", method=" + methodName
                + ", params=" + Arrays.toString(parameters) + "}";
    }
}
