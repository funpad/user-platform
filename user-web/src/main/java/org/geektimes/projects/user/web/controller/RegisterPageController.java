package org.geektimes.projects.user.web.controller;

import org.geektimes.projects.user.domain.User;
import org.geektimes.projects.user.service.UserService;
import org.geektimes.projects.user.service.impl.UserServiceImpl;
import org.geektimes.web.mvc.controller.PageController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

/**
 * 用户注册
 *
 * @author Frank
 */
@Path("/user")
public class RegisterPageController implements PageController {

    private UserService userService = new UserServiceImpl();

    @GET
    @Path("/register-page")
    public String getPage(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        return "user/register-form.jsp";
    }

    @POST
    @Path("register")
    public String register(HttpServletRequest request, HttpServletResponse response) {
        User user = new User();
        user.setName(request.getParameter("name"));
        user.setPhoneNumber(request.getParameter("phone"));
        user.setEmail(request.getParameter("email"));
        user.setPassword(request.getParameter("password"));

        boolean isSuccess = userService.register(user);

        if (isSuccess) {
            return "user/success.jsp";
        } else {
            return "user/failed.jsp";
        }
    }

}
