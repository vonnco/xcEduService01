package com.xuecheng.manage_cms_client.service;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.CmsSite;
import com.xuecheng.framework.domain.cms.response.CmsCode;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.manage_cms_client.dao.CmsPageRepository;
import com.xuecheng.manage_cms_client.dao.CmsSiteRepository;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Service
public class PageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PageService.class);
    @Autowired
    private CmsPageRepository cmsPageRepository;
    @Autowired
    private CmsSiteRepository cmsSiteRepository;
    @Autowired
    private GridFsTemplate gridFsTemplate;
    @Autowired
    private GridFSBucket gridFSBucket;

    public void savePageToServerPath(String pageId){
        Optional<CmsPage> optional = cmsPageRepository.findById(pageId);
        if (optional.isPresent()){
            CmsPage cmsPage = optional.get();
            String siteId = cmsPage.getSiteId();
            CmsSite cmsSite = this.getCmsSiteById(siteId);
            String path = cmsPage.getPagePhysicalPath()+cmsPage.getPageName();
            String htmlFileId = cmsPage.getHtmlFileId();
            InputStream inputStream = this.getFileById(htmlFileId);
            if(inputStream == null){
                LOGGER.error("getFileById inputStream is null,htmlFileId:{}",htmlFileId);
                ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_HTMLISNULL);
            }
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(new File(path));
                IOUtils.copy(inputStream,fileOutputStream);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    //根据文件id获取文件内容
    public InputStream getFileById(String fileId){
        try {
            GridFSFile gridFSFile = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(fileId)));
            GridFSDownloadStream gridFSDownloadStream = gridFSBucket.openDownloadStream(gridFSFile.getObjectId());
            GridFsResource gridFsResource = new GridFsResource(gridFSFile, gridFSDownloadStream);
            return gridFsResource.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    //根据站点id得到站点
    public CmsSite getCmsSiteById(String siteId){
        Optional<CmsSite> optional = cmsSiteRepository.findById(siteId);
        if (optional.isPresent()){
            return optional.get();
        }
        return null;
    }
}
