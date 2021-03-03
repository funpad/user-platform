package org.geektimes.web.mvc;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.geektimes.web.mvc.controller.Controller;
import org.geektimes.web.mvc.controller.PageController;
import org.geektimes.web.mvc.controller.RestController;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.substringAfter;

public class FrontControllerServlet extends HttpServlet {

    /**
     * 请求路径和 Controller 的映射关系缓存
     */
    // private Map<String, Controller> controllersMapping = new HashMap<>();

    /**
     * 请求路径和 {@link HandlerMethodInfo} 映射关系缓存
     */
    private Map<String, HandlerMethodInfo> handlerMapping = new HashMap<>();

    /**
     * 初始化 Servlet
     *
     * @param servletConfig
     */
    @Override
    public void init(ServletConfig servletConfig) {
        initHandleMethods();

        System.out.println("Mapping: ");
        for (String k : handlerMapping.keySet()) {
            System.out.println(k + " -> " + handlerMapping.get(k));
        }
    }

    /**
     * 读取所有的 RestController 的注解元信息 @Path
     * 利用 ServiceLoader 技术（Java SPI）
     */
    private void initHandleMethods() {
        for (Controller controller : ServiceLoader.load(Controller.class)) {
            Class<?> controllerClass = controller.getClass();
            Path pathFromClass = controllerClass.getAnnotation(Path.class);
            String pathOfClass = pathFromClass.value();
            Method[] publicMethods = controllerClass.getDeclaredMethods();

            // 处理方法支持的 HTTP 方法集合
            for (Method method : publicMethods) {
                Set<String> supportedHttpMethods = findSupportedHttpMethods(method);
                Path pathFromMethod = method.getAnnotation(Path.class);

                String requestPath = pathOfClass;
                if (pathFromMethod != null) {
                    requestPath += addSlash(pathFromMethod.value());
                }

                // FIXME Path 相同会被覆盖
                handlerMapping.put(requestPath,
                        new HandlerMethodInfo(requestPath, controller, method, supportedHttpMethods));
            }
            // controllersMapping.put(requestPath, controller);
        }
    }

    /**
     * 获取处理方法中标注的 HTTP方法集合
     *
     * @param method 处理方法
     * @return
     */
    private Set<String> findSupportedHttpMethods(Method method) {
        Set<String> supportedHttpMethods = new LinkedHashSet<>();
        for (Annotation annotationFromMethod : method.getAnnotations()) {
            HttpMethod httpMethod = annotationFromMethod.annotationType().getAnnotation(HttpMethod.class);
            if (httpMethod != null) {
                supportedHttpMethods.add(httpMethod.value());
            }
        }

        if (supportedHttpMethods.isEmpty()) {
            supportedHttpMethods.addAll(asList(HttpMethod.GET, HttpMethod.POST,
                    HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.HEAD, HttpMethod.OPTIONS));
        }

        return supportedHttpMethods;
    }

    /**
     * SCWCD
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    public void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // 建立映射关系
        // requestURI = /a/hello/world
        String requestURI = request.getRequestURI();
        // contextPath  = /a or "/" or ""
        String servletContextPath = request.getContextPath();
        String prefixPath = servletContextPath;
        // 映射路径（子路径）
        String requestMappingPath = substringAfter(requestURI,
                StringUtils.replace(prefixPath, "//", "/"));
        // 映射到 Controller
        // Controller controller = controllersMapping.get(requestMappingPath);

        HandlerMethodInfo handlerMethodInfo = handlerMapping.get(requestMappingPath);
        if (handlerMethodInfo == null) {
            return;
        }

        Controller controller = handlerMethodInfo.getController();
        if (controller == null) {
            return;
        }

        // HTTP 方法不支持
        if (!handlerMethodInfo.getSupportedHttpMethods().contains(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        try {
            if (controller instanceof PageController) {
                // PageController pageController = PageController.class.cast(controller);
                // FIXME 调用具体的方法（签名）属于硬编码
                Object obj = handlerMethodInfo.getHandlerMethod().invoke(controller, request, response);
                String viewPath = String.valueOf(obj);
                //String viewPath = pageController.execute(request, response);

                // 页面请求 forward
                // request -> RequestDispatcher forward
                // RequestDispatcher requestDispatcher = request.getRequestDispatcher(viewPath);
                // ServletContext -> RequestDispatcher forward
                // ServletContext -> RequestDispatcher 必须以 "/" 开头
                ServletContext servletContext = request.getServletContext();
                viewPath = addSlash(viewPath);

                RequestDispatcher requestDispatcher = servletContext.getRequestDispatcher(viewPath);
                requestDispatcher.forward(request, response);
                return;
            } else if (controller instanceof RestController) {
                // RestController restController = RestController.class.cast(controller);
                // FIXME 调用具体的方法（签名）属于硬编码
                Object obj = handlerMethodInfo.getHandlerMethod().invoke(controller, request, response);

                response.setStatus(200);
                PrintWriter out = response.getWriter();
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                String jsonString = JSON.toJSONString(obj, true);
                System.out.println("DEBUG: " + jsonString);
                out.print(jsonString);
                out.flush();
            }
        } catch (Throwable throwable) {
            if (throwable.getCause() instanceof IOException) {
                throw (IOException) throwable.getCause();
            } else {
                throw new ServletException(throwable.getCause());
            }
        }
    }

    private String addSlash(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path;
    }

//    private void beforeInvoke(Method handleMethod, HttpServletRequest request, HttpServletResponse response) {
//
//        CacheControl cacheControl = handleMethod.getAnnotation(CacheControl.class);
//
//        Map<String, List<String>> headers = new LinkedHashMap<>();
//
//        if (cacheControl != null) {
//            CacheControlHeaderWriter writer = new CacheControlHeaderWriter();
//            writer.write(headers, cacheControl.value());
//        }
//    }
}
