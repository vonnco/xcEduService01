package com.xuecheng.manage_cms.service;

import com.mongodb.client.gridfs.GridFSBucket;
import com.xuecheng.framework.domain.cms.CmsTemplate;
import com.xuecheng.framework.domain.cms.request.QueryTemplateRequest;
import com.xuecheng.framework.domain.cms.response.CmsCode;
import com.xuecheng.framework.domain.cms.response.CmsTemplateResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_cms.dao.CmsTemplateRepository;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@Service
public class CmsTemplateService {
    @Autowired
    private CmsTemplateRepository cmsTemplateRepository;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Autowired
    private GridFSBucket gridFSBucket;

    /**
     * 查询模板列表
     *
     * @return
     */
    public List<CmsTemplate> findAll() {
        List<CmsTemplate> all = cmsTemplateRepository.findAll();
        return all;
    }

    /**
     * 分页查询模板列表
     *
     * @param page
     * @param size
     * @param queryTemplateRequest
     * @return
     */
    public QueryResponseResult findList(int page, int size, QueryTemplateRequest queryTemplateRequest) {
        if (queryTemplateRequest == null) {
            queryTemplateRequest = new QueryTemplateRequest();
        }
        if (page <= 0) {
            page = 1;
        }
        page = page - 1;
        if (size <= 0) {
            size = 20;
        }
        PageRequest pageable = PageRequest.of(page, size);
        CmsTemplate cmsTemplate = new CmsTemplate();
        if (StringUtils.isNoneEmpty(queryTemplateRequest.getSiteId())) {
            cmsTemplate.setSiteId(queryTemplateRequest.getSiteId());
        }
        if (StringUtils.isNoneEmpty(queryTemplateRequest.getTemplateName())) {
            cmsTemplate.setTemplateName(queryTemplateRequest.getTemplateName());
        }
        ExampleMatcher exampleMatcher = ExampleMatcher.matching()
                .withMatcher("templateName", ExampleMatcher.GenericPropertyMatchers.contains());
        Example example = Example.of(cmsTemplate, exampleMatcher);
        Page all = cmsTemplateRepository.findAll(example, pageable);
        QueryResult<CmsTemplate> queryResult = new QueryResult<>();
        queryResult.setList(all.getContent());
        queryResult.setTotal(all.getTotalElements());
        QueryResponseResult queryResponseResult = new QueryResponseResult(CommonCode.SUCCESS, queryResult);
        return queryResponseResult;
    }

    /**
     * 新增模板
     *
     * @param cmsTemplate
     * @return
     */
    public CmsTemplateResult add(CmsTemplate cmsTemplate, MultipartFile file){
        if (cmsTemplate == null || file.isEmpty()) {
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        CmsTemplate cmsTemplate1 = cmsTemplateRepository.findBySiteIdAndTemplateName(cmsTemplate.getSiteId(), cmsTemplate.getTemplateName());
        if (cmsTemplate1 != null) {
            ExceptionCast.cast(CmsCode.CMS_ADDPAGE_EXISTSNAME);
        }
        InputStream inputStream = null;
        try {
            inputStream = file.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //向GridFS存储文件
        ObjectId objectId = gridFsTemplate.store((FileInputStream)inputStream, file.getName(), "");
        if (objectId !=null){
            String templateFileId = objectId.toString();
            cmsTemplate.setTemplateFileId(templateFileId);
        }
        cmsTemplate.setTemplateId(null);
        CmsTemplate save = cmsTemplateRepository.save(cmsTemplate);
        return new CmsTemplateResult(CommonCode.SUCCESS, save);
    }

    /**
     * 根据Id查询模板
     *
     * @param id
     * @return
     */
    public CmsTemplateResult findById(String id) {
        Optional<CmsTemplate> optional = cmsTemplateRepository.findById(id);
        if (optional.isPresent()) {
            CmsTemplate cmsTemplate = optional.get();
            return new CmsTemplateResult(CommonCode.SUCCESS,cmsTemplate);
        }
        return new CmsTemplateResult(CommonCode.FAIL,null);
    }

    /**
     * 根据id修改模板
     *
     * @param id
     * @param cmsTemplate
     * @return
     */
    public CmsTemplateResult update(String id, CmsTemplate cmsTemplate) {
        CmsTemplate one = this.findById(id).getCmsTemplate();
        if (one != null) {
            one.setTemplateName(cmsTemplate.getTemplateName());
            one.setSiteId(cmsTemplate.getSiteId());
            one.setTemplateParameter(cmsTemplate.getTemplateParameter());
            CmsTemplate save = cmsTemplateRepository.save(one);
            if (save != null) {
                return new CmsTemplateResult(CommonCode.SUCCESS, save);
            }
        }
        return new CmsTemplateResult(CommonCode.FAIL, null);
    }

    /**
     * 删除模板
     * @param id
     * @return
     */
    public ResponseResult delete(String id) {
        CmsTemplate cmsTemplate = this.findById(id).getCmsTemplate();
        if (cmsTemplate != null){
            //根据文件id删除fs.files和fs.chunks中的记录
            gridFsTemplate.delete(Query.query(Criteria.where("_id").is(cmsTemplate.getTemplateFileId())));
            cmsTemplateRepository.delete(cmsTemplate);
            return new ResponseResult(CommonCode.SUCCESS);
        }
        return new ResponseResult(CommonCode.FAIL);
    }
}

