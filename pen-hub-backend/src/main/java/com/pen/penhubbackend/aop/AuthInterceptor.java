package com.pen.penhubbackend.aop;

import com.pen.penhubbackend.annotation.AuthCheck;
import com.pen.penhubbackend.exception.BusinessException;
import com.pen.penhubbackend.exception.ErrorCode;
import com.pen.penhubbackend.model.entity.User;
import com.pen.penhubbackend.model.enums.UserRoleEnum;
import com.pen.penhubbackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static com.pen.penhubbackend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 权限校验切面
 */
@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 执行权限校验
     *
     * @param joinPoint  切入点
     * @param authCheck  权限注解
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();

        // 获取当前登录用户
        // 获取当前线程的请求上下文
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        User loginUser = userService.getLoginUser(request);
        UserRoleEnum mustRoleEnum = UserRoleEnum.getByValue(mustRole);

        // 不需要权限，直接放行
        if (mustRoleEnum == null) {
            return joinPoint.proceed();
        }

        // 权限校验
        UserRoleEnum userRoleEnum = UserRoleEnum.getByValue(loginUser.getUserRole());
        // 没有权限，直接拒绝
        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 要求必须有管理员权限，但当前登录用户没有
        if (UserRoleEnum.ADMIN.getValue().equals(mustRole) && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 通过权限校验，执行方法
        return joinPoint.proceed();
    }


}
