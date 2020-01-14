package com.xuecheng.manage_cms.service;

import com.xuecheng.framework.domain.cms.CmsSite;
import com.xuecheng.manage_cms.dao.CmsSiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CmsSiteService {
    @Autowired
    private CmsSiteRepository cmsSiteRepository;

    /**
     * 查询站点列表
     * @return
     */
    public List<CmsSite> findAll(){
        List<CmsSite> list = cmsSiteRepository.findAll();
        return list;
    }
}
