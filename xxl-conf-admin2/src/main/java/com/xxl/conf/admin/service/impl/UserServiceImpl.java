package com.xxl.conf.admin.service.impl;

import com.xxl.conf.admin.constant.enums.RoleEnum;
import com.xxl.conf.admin.mapper.UserMapper;
import com.xxl.conf.admin.model.adaptor.UserAdaptor;
import com.xxl.conf.admin.model.dto.LoginUserDTO;
import com.xxl.conf.admin.model.dto.UserDTO;
import com.xxl.conf.admin.model.entity.User;
import com.xxl.conf.admin.service.UserService;
import com.xxl.conf.admin.util.I18nUtil;
import com.xxl.tool.core.CollectionTool;
import com.xxl.tool.core.StringTool;
import com.xxl.tool.response.PageModel;
import com.xxl.tool.response.Response;
import com.xxl.tool.response.ResponseBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * user service
 *
 * @author xuxueli
 */
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    /**
     * 新增
     */
    @Override
    public Response<String> insert(UserDTO xxlJobUser) {

        // adapt
        User user = UserAdaptor.adapt(xxlJobUser);

        // valid empty
        if (user == null) {
            return new ResponseBuilder<String>().fail(I18nUtil.getString("system_param_empty")).build();
        }
        // valid username
        if (StringTool.isBlank(user.getUsername())) {
            return new ResponseBuilder<String>().fail(I18nUtil.getString("system_please_input") + I18nUtil.getString("user_username")).build();
        }
        user.setUsername(user.getUsername().trim());
        if (!(user.getUsername().length()>=4 && user.getUsername().length()<=20)) {
            return new ResponseBuilder<String>().fail(I18nUtil.getString("system_lengh_limit")+"[4-20]").build();
        }
        // valid password
        if (StringTool.isBlank(user.getPassword())) {
            return new ResponseBuilder<String>().fail( I18nUtil.getString("system_please_input")+I18nUtil.getString("user_password") ).build();
        }
        user.setPassword(user.getPassword().trim());
        if (!(user.getPassword().length()>=4 && user.getPassword().length()<=20)) {
            return new ResponseBuilder<String>().fail( I18nUtil.getString("system_lengh_limit")+"[4-20]" ).build();
        }
        // md5 password
        user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes()));    // todo，move to token

        // valid user role
        if (RoleEnum.matchByValue(user.getRole()) == null) {
            return new ResponseBuilder<String>().fail("操作失败，角色ID非法").build();
        }

        // check repeat
        User existUser = userMapper.loadByUserName(user.getUsername());
        if (existUser != null) {
            return new ResponseBuilder<String>().fail( I18nUtil.getString("user_username_repeat") ).build();
        }

        // save user
        userMapper.insert(user);

        return new ResponseBuilder<String>().success().build();
    }

    /**
     * 删除
     */
    @Override
    public Response<String> delete(int id) {
        int ret = userMapper.delete(id);
        return ret>0? new ResponseBuilder<String>().success().build()
                : new ResponseBuilder<String>().fail().build() ;
    }

    /**
     * 删除
     */
    @Override
    public Response<String> deleteByIds(List<Integer> userIds, LoginUserDTO loginUser) {

        // valid
        if (CollectionTool.isEmpty(userIds)) {
            return new ResponseBuilder<String>().fail(I18nUtil.getString("system_please_choose") + I18nUtil.getString("user_tips")).build();
        }

        // avoid opt login seft
        if (userIds.contains(loginUser.getId())) {
            return new ResponseBuilder<String>().fail( I18nUtil.getString("user_update_loginuser_limit") ).build();
        }

        int ret = userMapper.deleteByIds(userIds);
        return ret>0? new ResponseBuilder<String>().success().build()
                : new ResponseBuilder<String>().fail().build() ;
    }

    /**
     * 更新
     */
    @Override
    public Response<String> update(UserDTO xxlJobUser, LoginUserDTO loginUser) {

        // adapt
        User user = UserAdaptor.adapt(xxlJobUser);

        // avoid opt login seft
        if (loginUser.getUsername().equals(user.getUsername())) {
            return new ResponseBuilder<String>().fail( I18nUtil.getString("user_update_loginuser_limit") ).build();
        }

        // valid password
        if (StringTool.isNotBlank(user.getPassword())) {
            user.setPassword(user.getPassword().trim());
            if (!(user.getPassword().length()>=4 && user.getPassword().length()<=20)) {
                return new ResponseBuilder<String>().fail(  I18nUtil.getString("system_lengh_limit")+"[4-20]" ).build();
            }
            // md5 password
            user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes()));
        } else {
            user.setPassword(null);
        }

        // valid user role
        if (RoleEnum.matchByValue(user.getRole()) == null) {
            return new ResponseBuilder<String>().fail("操作失败，角色ID非法").build();
        }

        // update user
        int ret = userMapper.update(user);

        return ret>0? new ResponseBuilder<String>().success().build()
                : new ResponseBuilder<String>().fail().build() ;
    }

    /**
     * 修改密码
     */
    public Response<String> updatePwd(LoginUserDTO loginUser, String password){
        // valid password
        if (StringTool.isBlank(password)){
            new ResponseBuilder<String>().fail( "密码不可为空" ).build();
        }
        password = password.trim();
        if (!(password.length()>=4 && password.length()<=20)) {
            new ResponseBuilder<String>().fail( I18nUtil.getString("system_lengh_limit")+"[4-20]" ).build();
        }

        // md5 password
        String md5Password = DigestUtils.md5DigestAsHex(password.getBytes());

        // update pwd
        User existUser = userMapper.loadByUserName(loginUser.getUsername());
        existUser.setPassword(md5Password);
        userMapper.update(existUser);

        return new ResponseBuilder<String>().success().build();
    }

    /**
     * Load查询
     */
    @Override
    public Response<User> loadByUserName(String username){
        User record = userMapper.loadByUserName(username);
        return new ResponseBuilder<User>().success(record).build();
    }

    @Override
    public Response<String> grantPermission(String username, String permission) {
        // update
        User existUser = userMapper.loadByUserName(username);
        existUser.setPermission(permission);
        userMapper.update(existUser);
        return new ResponseBuilder<String>().success().build();
    }

    /**
     * 分页查询
     */
    @Override
    public PageModel<UserDTO> pageList(int offset, int pagesize, String username, int status) {

        // data
        List<User> pageList = userMapper.pageList(offset, pagesize, username, status);
        int totalCount = userMapper.pageListCount(offset, pagesize, username, status);

        // adaptor
        List<UserDTO> pageListDto = new ArrayList<>();
        if (CollectionTool.isNotEmpty(pageList)) {
            // dto list
            pageListDto = pageList
                    .stream()
                    .map(item-> UserAdaptor.adapt(item))
                    .collect(Collectors.toList());
        }

        // result
        PageModel<UserDTO> pageModel = new PageModel<UserDTO>();
        pageModel.setPageData(pageListDto);
        pageModel.setTotalCount(totalCount);

        return pageModel;
    }

}
