package org.geektimes.web.mvc;

import org.geektimes.web.mvc.controller.Controller;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * 处理方法信息类
 *
 * @since 1.0
 */
public class HandlerMethodInfo {

    private final String requestPath;

    private final Controller controller;

    private final Method handlerMethod;

    private final Set<String> supportedHttpMethods;

    public HandlerMethodInfo(String requestPath,
                             Controller controller,
                             Method handlerMethod,
                             Set<String> supportedHttpMethods) {
        this.requestPath = requestPath;
        this.controller = controller;
        this.handlerMethod = handlerMethod;
        this.supportedHttpMethods = supportedHttpMethods;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public Controller getController() {
        return controller;
    }

    public Method getHandlerMethod() {
        return handlerMethod;
    }

    public Set<String> getSupportedHttpMethods() {
        return supportedHttpMethods;
    }

    @Override
    public String toString() {
        return "HandlerMethodInfo{" +
                "requestPath='" + requestPath + '\'' +
                ", controller=" + controller +
                ", handlerMethod=" + handlerMethod +
                ", supportedHttpMethods=" + supportedHttpMethods +
                '}';
    }
}
