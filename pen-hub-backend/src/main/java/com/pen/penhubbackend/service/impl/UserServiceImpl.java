package com.pen.penhubbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.pen.penhubbackend.exception.BusinessException;
import com.pen.penhubbackend.exception.ErrorCode;
import com.pen.penhubbackend.mapper.UserMapper;
import com.pen.penhubbackend.model.dto.user.UserQueryRequest;
import com.pen.penhubbackend.model.entity.User;
import com.pen.penhubbackend.model.enums.UserRoleEnum;
import com.pen.penhubbackend.model.vo.LoginUserVO;
import com.pen.penhubbackend.model.vo.UserVO;
import com.pen.penhubbackend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.pen.penhubbackend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户表 Service 实现类
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    /**
     * 盐值，用于密码加密
     */
    private static final String SALT = "pen_hub";

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 用户ID
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {

        // 校验参数
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        // 检查账号是否已存在
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("user_account", userAccount);
        long count = this.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }

        // 加密密码
        String encryptPassword = getEncryptPassword(userPassword);

        // 创建用户
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        // 这里给一个随机中文词组当用户昵称
        user.setUserNickname("默认用户");
        user.setUserRole(UserRoleEnum.USER.getValue());


        boolean result = this.save(user);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "注册失败");
        }

        return user.getId();
    }

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request          HTTP请求
     * @return 脱敏后的用户信息
     */
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {

        // 校验参数
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度过短");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度过短");
        }

        // 加密密码
        String encryptPassword = getEncryptPassword(userPassword);

        // 查询用户
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("user_account", userAccount)
                .eq("user_password", encryptPassword);
        User user = this.getOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不存在或密码错误");
        }

        // 记录用户登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);

        // 返回脱敏后的用户信息
        return getLoginUserVO(user);
    }

    /**
     * 获取当前登录用户
     *
     * @param request HTTP请求
     * @return 脱敏后的用户信息
     */
    public User getLoginUser(HttpServletRequest request) {
        // 获取当前登录用户对象
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        User currentUser = (User) userObj;
        if (currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        //查询数据库，获取当前用户信息
        User user = this.getById(currentUser.getId());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 用户注销
     *
     * @param request HTTP请求
     * @return 是否成功
     */
    public boolean userLogout(HttpServletRequest request) {
        // 判断用户是否登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    /**
     * 加密
     *
     * @param userPassword 用户密码
     * @return 加密后的用户密码
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        return encryptPassword;
    }

    /**
     * 将用户实体转换为脱敏的VO对象
     *
     * @param user 用户实体
     * @return 用户VO
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 将用户实体转换为脱敏的登录用户VO对象
     *
     * @param user 用户实体
     * @return 登录用户VO
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 将用户实体列表转换为脱敏的VO对象列表
     *
     * @param userList 用户实体列表
     * @return 用户VO列表
     */
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }


    /**
     * 根据查询条件构造数据查询参数
     *
     * @param userQueryRequest 用户查询条件
     * @return 查询条件
     */
    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        // 创建查询条件
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();


        QueryWrapper queryWrapper = QueryWrapper.create();

        if (id != null) {
            queryWrapper.eq("id", id);
        }
        if (userAccount != null && !userAccount.isEmpty()) {
            queryWrapper.like("user_account", userAccount);
        }
        if (userName != null && !userName.isEmpty()) {
            queryWrapper.like("user_nickname", userName);
        }
        if (userProfile != null && !userProfile.isEmpty()) {
            queryWrapper.like("user_profile", userProfile);
        }
        if (userRole != null && !userRole.isEmpty()) {
            queryWrapper.eq("user_role", userRole);
        }
        if (sortField != null && !sortField.isEmpty()) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        }

        return queryWrapper;
    }
}
