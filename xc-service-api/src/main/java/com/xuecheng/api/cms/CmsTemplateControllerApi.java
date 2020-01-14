package com.xuecheng.api.cms;

import com.xuecheng.framework.domain.cms.CmsTemplate;
import com.xuecheng.framework.domain.cms.request.QueryTemplateRequest;
import com.xuecheng.framework.domain.cms.response.CmsTemplateResult;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Api(value = "cms模板管理接口",description = "cms模板管理接口，提供模板的增、删、改、查")
public interface CmsTemplateControllerApi {
    @ApiOperation("查询模板列表")
    public List<CmsTemplate> findList();
    @ApiOperation("分页查询模板列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page",value = "页码",required = true,paramType = "path",dataType = "int"),
            @ApiImplicitParam(name = "size",value = "每页记录数",required = true,paramType = "path",dataType = "int")
    })
    public QueryResponseResult findList(int page, int size, QueryTemplateRequest queryTemplateRequest);
    @ApiOperation("新增模板")
    public CmsTemplateResult add(String cmsTemplateStr, MultipartFile file);
    @ApiOperation("修改模板")
    public CmsTemplateResult edit(String id,CmsTemplate cmsTemplate);
    @ApiOperation("删除模板")
    public ResponseResult delete(String id);
    @ApiOperation("根据Id查询模板")
    public CmsTemplate findById(String id);
}
