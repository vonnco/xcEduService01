package com.xuecheng.ucenter.service;

import com.xuecheng.framework.domain.ucenter.XcCompanyUser;
import com.xuecheng.framework.domain.ucenter.XcMenu;
import com.xuecheng.framework.domain.ucenter.XcUser;
import com.xuecheng.framework.domain.ucenter.ext.XcUserExt;
import com.xuecheng.ucenter.dao.XcCompanyUserRepository;
import com.xuecheng.ucenter.dao.XcMenuMapper;
import com.xuecheng.ucenter.dao.XcUserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UcenterService {
    @Autowired
    private XcUserRepository xcUserRepository;
    @Autowired
    private XcCompanyUserRepository xcCompanyUserRepository;
    @Autowired
    private XcMenuMapper xcMenuMapper;

    //根据用户名查询用户信息
    public XcUserExt getUserExt(String username) {
        XcUserExt xcUserExt = new XcUserExt();
        XcUser xcUser = this.findXcUsersByUsername(username);
        if (xcUser == null) {
            return null;
        }
        BeanUtils.copyProperties(xcUser,xcUserExt);
        String userId = xcUser.getId();
        XcCompanyUser xcCompanyUser = this.findXcCompanyUserByUserId(userId);
        if (xcCompanyUser != null) {
            xcUserExt.setCompanyId(xcCompanyUser.getCompanyId());
        }
        List<XcMenu> xcMenuList = this.findMenuListByUserId(userId);
        if (xcMenuList != null) {
            xcUserExt.setPermissions(xcMenuList);
        }
        return xcUserExt;
    }
    //根据用户名查询用户信息
    public XcUser findXcUsersByUsername(String username) {
        return xcUserRepository.findXcUsersByUsername(username);
    }
    //根据用户id查询用户公司信息
    public XcCompanyUser findXcCompanyUserByUserId(String userId) {
        return xcCompanyUserRepository.findXcCompanyUserByUserId(userId);
    }
    //根据用户id查询用户菜单权限
    public List<XcMenu> findMenuListByUserId(String userId) {
        return xcMenuMapper.findMenuListByUserId(userId);
    }
}
