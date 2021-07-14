package com.xuecheng.manage_course.service;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.domain.cms.response.CmsPostPageResult;
import com.xuecheng.framework.domain.course.*;
import com.xuecheng.framework.domain.course.ext.CourseInfo;
import com.xuecheng.framework.domain.course.ext.CourseView;
import com.xuecheng.framework.domain.course.ext.TeachplanNode;
import com.xuecheng.framework.domain.course.request.CourseListRequest;
import com.xuecheng.framework.domain.course.response.CourseCode;
import com.xuecheng.framework.domain.course.response.CoursePublishResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_course.client.CmsPageClient;
import com.xuecheng.manage_course.dao.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class CourseService {
    @Autowired
    private TeachplanMapper teachplanMapper;

    @Autowired
    private CourseBaseRepository courseBaseRepository;
    
    @Autowired
    private TeachplanRepository teachplanRepository;

    @Autowired
    CourseMapper courseMapper;

    @Autowired
    private CourseMarketRepository courseMarketRepository;

    @Autowired
    private CoursePicRepository coursePicRepository;

    @Autowired
    private CoursePubRepository coursePubRepository;

    @Autowired
    private CmsPageClient cmsPageClient;

    @Autowired
    private TeachplanMediaRepository teachplanMediaRepository;

    @Autowired
    private TeachplanMediaPubRepository teachplanMediaPubRepository;

    @Value("${course-publish.dataUrlPre}")
    private String publish_dataUrlPre;
    @Value("${course-publish.pagePhysicalPath}")
    private String publish_page_physicalpath;
    @Value("${course-publish.pageWebPath}")
    private String publish_page_webpath;
    @Value("${course-publish.siteId}")
    private String publish_siteId;
    @Value("${course-publish.templateId}")
    private String publish_templateId;
    @Value("${course-publish.previewUrl}")
    private String previewUrl;


    //查询课程计划
    public TeachplanNode findTeachplanList(String courseId){
        return teachplanMapper.selectList(courseId);
    }

    //添加课程计划
    @Transactional
    public ResponseResult addteachplan(Teachplan teachplan) {
        if (teachplan == null || StringUtils.isEmpty(teachplan.getCourseid()) || StringUtils.isEmpty(teachplan.getPname())){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        //课程id
        String courseid = teachplan.getCourseid();
        //课程父节点
        String parentid = teachplan.getParentid();
        //如果parentid为空
        if (StringUtils.isEmpty(parentid)) {
            //获取课程根节点
            parentid = this.getTeachplanRoot(courseid);
        }
        Teachplan teachplanNew = new Teachplan();
        //将teachplan的信息拷贝到teachplanNew
        BeanUtils.copyProperties(teachplan,teachplanNew);
        teachplanNew.setParentid(parentid);
        //根据父节点的级别判断该节点级别
        Optional<Teachplan> optional = teachplanRepository.findById(parentid);
        if(!optional.isPresent()){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        String grade = optional.get().getGrade();
        if (grade.equals("1")) {
            teachplanNew.setGrade("2");
        } else {
            teachplanNew.setGrade("3");
        }
        teachplanRepository.save(teachplanNew);
        return new ResponseResult(CommonCode.SUCCESS);
    }
    
    //根据课程id查询课程根节点
    private String getTeachplanRoot(String courseId) {
        //校验课程id
        Optional<CourseBase> optional = courseBaseRepository.findById(courseId);
        if (!optional.isPresent()){
            return null;
        }
        CourseBase courseBase = optional.get();
        List<Teachplan> teachplanList = teachplanRepository.findByCourseidAndParentid(courseId, "0");
        //如果没有父节点添加父节点
        if (teachplanList == null || teachplanList.size() <= 0) {
            Teachplan teachplanRoot = new Teachplan();
            teachplanRoot.setCourseid(courseId);
            teachplanRoot.setGrade("1");//1级
            teachplanRoot.setParentid("0");
            teachplanRoot.setStatus("0");//未发布
            teachplanRoot.setPname(courseBase.getName());
            teachplanRepository.save(teachplanRoot);
            return teachplanRoot.getId();
        }
        return teachplanList.get(0).getId();
    }

    //查询课程列表
    public QueryResponseResult findCourseList(int page, int size, CourseListRequest courseListRequest) {
        PageHelper.startPage(page, size);
        Page<CourseInfo> courseInfos = courseMapper.selectCourseList(courseListRequest);
        List<CourseInfo> courseInfoList = courseInfos.getResult();
        QueryResult<CourseInfo> queryResult = new QueryResult<>();
        queryResult.setList(courseInfoList);
        queryResult.setTotal(courseInfos.getTotal());
        return new QueryResponseResult(CommonCode.SUCCESS,queryResult);
    }

    //添加课程基础信息
    public ResponseResult addCourseBase(CourseBase courseBase) {
        if (courseBase == null || StringUtils.isEmpty(courseBase.getName())
                || StringUtils.isEmpty(courseBase.getGrade()) || StringUtils.isEmpty(courseBase.getStudymodel())){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        courseBaseRepository.save(courseBase);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //根据id查询课程基础信息
    public CourseBase getCourseBaseById(String courseId) {
        Optional<CourseBase> optional = courseBaseRepository.findById(courseId);
        if (optional.isPresent()){
            CourseBase courseBase = optional.get();
            return courseBase;
        }
        return null;
    }

    //修改课程基础信息
    public ResponseResult updateCourseBase(String id, CourseBase courseBase) {
        CourseBase OldCourseBase = this.getCourseBaseById(id);
        if (OldCourseBase != null) {
            if (!StringUtils.isEmpty(courseBase.getName())){
                OldCourseBase.setName(courseBase.getName());
            }
            if (!StringUtils.isEmpty(courseBase.getUsers())){
                OldCourseBase.setUsers(courseBase.getUsers());
            }
            if (!StringUtils.isEmpty(courseBase.getMt())){
                OldCourseBase.setMt(courseBase.getMt());
            }
            if (!StringUtils.isEmpty(courseBase.getSt())){
                OldCourseBase.setSt(courseBase.getSt());
            }
            if (!StringUtils.isEmpty(courseBase.getGrade())){
                OldCourseBase.setGrade(courseBase.getGrade());
            }
            if (!StringUtils.isEmpty(courseBase.getStudymodel())){
                OldCourseBase.setStudymodel(courseBase.getStudymodel());
            }
            if (!StringUtils.isEmpty(courseBase.getDescription())){
                OldCourseBase.setDescription(courseBase.getDescription());
            }
            courseBaseRepository.save(OldCourseBase);
            return new ResponseResult(CommonCode.SUCCESS);
        }
        return new ResponseResult(CommonCode.FAIL);
    }

    //根据课程id查询课程营销信息
    public CourseMarket getCourseMarketById(String courseId) {
        Optional<CourseMarket> optional = courseMarketRepository.findById(courseId);
        if (optional.isPresent()){
            return optional.get();
        }
        return null;
    }

    //修改课程营销信息
    public ResponseResult updateCourseMarket(String id, CourseMarket courseMarket) {
        CourseMarket OldCourseMarket = this.getCourseMarketById(id);
        if (OldCourseMarket !=null ){
            if (!StringUtils.isEmpty(courseMarket.getCharge())){
                OldCourseMarket.setCharge(courseMarket.getCharge());
            }
            if (!StringUtils.isEmpty(courseMarket.getValid())){
                OldCourseMarket.setValid(courseMarket.getValid());
            }
            if (courseMarket.getPrice() != null) {
                OldCourseMarket.setPrice(courseMarket.getPrice());
            }
            if (courseMarket.getStartTime() != null) {
                OldCourseMarket.setStartTime(courseMarket.getStartTime());
            }
            if (courseMarket.getEndTime() != null) {
                OldCourseMarket.setEndTime(courseMarket.getEndTime());
            }
            if (!StringUtils.isEmpty(courseMarket.getQq())) {
                OldCourseMarket.setQq(courseMarket.getQq());
            }
            if (courseMarket.getExpires() != null) {
                OldCourseMarket.setExpires(courseMarket.getExpires());
            }
            if (courseMarket.getPrice_old() != null) {
                OldCourseMarket.setPrice_old(courseMarket.getPrice_old());
            }
            courseMarketRepository.save(OldCourseMarket);
            return new ResponseResult(CommonCode.SUCCESS);
        } else {
            courseMarket.setId(id);
            courseMarketRepository.save(courseMarket);
            return new ResponseResult(CommonCode.SUCCESS);
        }
    }

    //保存课程图片信息
    @Transactional
    public ResponseResult addCoursePic(String courseId, String pic) {
        Optional<CoursePic> optional = coursePicRepository.findById(courseId);
        CoursePic coursePic = null;
        if (optional.isPresent()) {
            coursePic = optional.get();
        }
        if (coursePic == null) {
            coursePic = new CoursePic();
        }
        coursePic.setCourseid(courseId);
        coursePic.setPic(pic);
        coursePicRepository.save(coursePic);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //查询课程图片列表
    public CoursePic findCoursePicList(String courseId) {
        Optional<CoursePic> optional = coursePicRepository.findById(courseId);
        if (optional.isPresent()) {
            CoursePic coursePic = optional.get();
            return coursePic;
        }
        return null;
    }

    //删除课程图片信息
    @Transactional
    public ResponseResult deleteCoursePic(String courseId) {
        long result = coursePicRepository.deleteByCourseid(courseId);
        if (result > 0) {
            return new ResponseResult(CommonCode.SUCCESS);
        } else {
            return new ResponseResult(CommonCode.FAIL);
        }
    }

    //查询课程视图信息，包括基础信息、图片信息、营销信息、课程计划
    public CourseView getCourseView(String id) {
        CourseView courseView = new CourseView();
        Optional<CourseBase> courseBaseOptional = courseBaseRepository.findById(id);
        if (courseBaseOptional.isPresent()) {
            courseView.setCourseBase(courseBaseOptional.get());
        }
        Optional<CoursePic> coursePicOptional = coursePicRepository.findById(id);
        if (coursePicOptional.isPresent()) {
            courseView.setCoursePic(coursePicOptional.get());
        }
        Optional<CourseMarket> courseMarketOptional = courseMarketRepository.findById(id);
        if (courseMarketOptional.isPresent()) {
            courseView.setCourseMarket(courseMarketOptional.get());
        }
        TeachplanNode teachplanNode = teachplanMapper.selectList(id);
        courseView.setTeachplanNode(teachplanNode);
        return courseView;
    }

    //课程预览
    public CoursePublishResult preview(String id) {
        CourseBase courseBase = this.findCourseBaseById(id);
        //发布课程预览页面
        CmsPage cmsPage = new CmsPage();
        //站点
        cmsPage.setSiteId(publish_siteId);//课程预览站点
        //模板
        cmsPage.setTemplateId(publish_templateId);
        //页面名称
        cmsPage.setPageName(id+".html");
        //页面别名
        cmsPage.setPageAliase(courseBase.getName());
        //页面访问路径
        cmsPage.setPageWebPath(publish_page_webpath);
        //页面存储路径
        cmsPage.setPagePhysicalPath(publish_page_physicalpath);
        //数据url
        cmsPage.setDataUrl(publish_dataUrlPre+id);
        //远程请求cms保存页面信息
        CmsPageResult cmsPageResult = cmsPageClient.saveCmsPage(cmsPage);
        if (!cmsPageResult.isSuccess()) {
            return new CoursePublishResult(CommonCode.FAIL,null);
        }
        String pageId = cmsPageResult.getCmsPage().getPageId();
        String url = previewUrl + pageId;
        return new CoursePublishResult(CommonCode.SUCCESS,url);
    }

    //查询课程基础信息
    public CourseBase findCourseBaseById(String id) {
        Optional<CourseBase> optional = courseBaseRepository.findById(id);
        if (optional.isPresent()) {
            return optional.get();
        }
        ExceptionCast.cast(CourseCode.COURSE_GET_NOTEXISTS);
        return null;
    }

    //发布课程
    @Transactional
    public CoursePublishResult publish(String id) {
        //发布课程详情页面
        CmsPostPageResult cmsPostPageResult = this.publish_page(id);
        if (!cmsPostPageResult.isSuccess()) {
            ExceptionCast.cast(CommonCode.FAIL);
        }
        CourseBase courseBase = this.saveCoursePubState(id);
        if (courseBase == null) {
            ExceptionCast.cast(CommonCode.FAIL);
        }
        //创建课程索引信息
        CoursePub coursePub = this.createCoursePub(id);
        //向数据库保存课程索引信息
        CoursePub coursePubNew = this.saveCoursePub(id, coursePub);
        if (coursePubNew == null) {
            //创建课程索引信息失败
            ExceptionCast.cast(CourseCode.COURSE_PUBLISH_CREATE_INDEX_ERROR);
        }
        //页面url
        String pageUrl = cmsPostPageResult.getPageUrl();
        //向teachplan_media_pub中保存课程媒资信息
        this.saveTeachplanMediaPub(id);
        return new CoursePublishResult(CommonCode.SUCCESS,pageUrl);
    }

    //保存coursePub
    private CoursePub saveCoursePub(String id,CoursePub coursePub) {
        if(StringUtils.isEmpty(id)){
            ExceptionCast.cast(CourseCode.COURSE_PUBLISH_COURSEIDISNULL);
        }
        CoursePub coursePubNew = null;
        Optional<CoursePub> coursePubOptional = coursePubRepository.findById(id);
        if (coursePubOptional.isPresent()) {
            coursePubNew = coursePubOptional.get();
        }
        if (coursePubNew == null) {
            coursePubNew = new CoursePub();
        }
        BeanUtils.copyProperties(coursePub,coursePubNew);
        coursePubNew.setId(id);
        coursePubNew.setTimestamp(new Date());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
        String date = simpleDateFormat.format(new Date());
        coursePubNew.setPubTime(date);
        coursePubRepository.save(coursePubNew);
        return coursePubNew;
    }

    //创建coursePub对象
    private CoursePub createCoursePub(String id) {
        CoursePub coursePub = new CoursePub();
        Optional<CourseBase> courseBaseOptional = courseBaseRepository.findById(id);
        if (courseBaseOptional.isPresent()) {
            CourseBase courseBase = courseBaseOptional.get();
            BeanUtils.copyProperties(courseBase,coursePub);
        }
        Optional<CoursePic> coursePicOptional = coursePicRepository.findById(id);
        if (coursePicOptional.isPresent()) {
            CoursePic coursePic = coursePicOptional.get();
            BeanUtils.copyProperties(coursePic,coursePub);
        }
        Optional<CourseMarket> courseMarketOptional = courseMarketRepository.findById(id);
        if (courseMarketOptional.isPresent()) {
            CourseMarket courseMarket = courseMarketOptional.get();
            BeanUtils.copyProperties(courseMarket,coursePub);
        }
        TeachplanNode teachplanNode = teachplanMapper.selectList(id);
        String s = JSON.toJSONString(teachplanNode);
        coursePub.setTeachplan(s);
        return coursePub;
    }

    //更新课程发布状态
    private CourseBase saveCoursePubState(String courseId){
        CourseBase courseBase = this.findCourseBaseById(courseId);
        //更新发布状态
        courseBase.setStatus("202002");
        CourseBase save = courseBaseRepository.save(courseBase);
        return save;
    }

    //发布课程正式页面
    public CmsPostPageResult publish_page(String courseId){
        CourseBase one = this.findCourseBaseById(courseId);
        //发布课程预览页面
        CmsPage cmsPage = new CmsPage();
        //站点
        cmsPage.setSiteId(publish_siteId);//课程预览站点
        //模板
        cmsPage.setTemplateId(publish_templateId);
        //页面名称
        cmsPage.setPageName(courseId+".html");
        //页面别名
        cmsPage.setPageAliase(one.getName());
        //页面访问路径
        cmsPage.setPageWebPath(publish_page_webpath);
        //页面存储路径
        cmsPage.setPagePhysicalPath(publish_page_physicalpath);
        //数据url
        cmsPage.setDataUrl(publish_dataUrlPre+courseId);
        //发布页面
        CmsPostPageResult cmsPostPageResult = cmsPageClient.postPageQuick(cmsPage);
        return cmsPostPageResult;
    }

    //保存媒资信息
    @Transactional
    public ResponseResult saveMedia(TeachplanMedia teachplanMedia) {
        if (teachplanMedia == null || StringUtils.isEmpty(teachplanMedia.getTeachplanId())) {
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        String teachplanId = teachplanMedia.getTeachplanId();
        Optional<Teachplan> optionalTeachplan = teachplanRepository.findById(teachplanId);
        if (!optionalTeachplan.isPresent()) {
            ExceptionCast.cast(CourseCode.COURSE_MEDIA_TEACHPLAN_ISNULL);
        }
        Teachplan teachplan = optionalTeachplan.get();
        if (StringUtils.isEmpty(teachplan.getGrade()) || !teachplan.getGrade().equals("3")) {
            ExceptionCast.cast(CourseCode.COURSE_MEDIA_TEACHPLAN_GRADEERROR);
        }
        TeachplanMedia one = null;
        Optional<TeachplanMedia> optionalTeachplanMedia = teachplanMediaRepository.findById(teachplanId);
        if (optionalTeachplanMedia.isPresent()) {
            one = optionalTeachplanMedia.get();
        } else {
            one = new TeachplanMedia();
        }
        one.setCourseId(teachplanMedia.getCourseId());
        one.setMediaId(teachplanMedia.getMediaId());
        one.setMediaUrl(teachplanMedia.getMediaUrl());
        one.setMediaFileOriginalName(teachplanMedia.getMediaFileOriginalName());
        teachplanMediaRepository.save(one);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //保存课程计划媒资信息
    private void saveTeachplanMediaPub(String courseId) {
        teachplanMediaPubRepository.deleteByCourseId(courseId);
        List<TeachplanMedia> teachplanMediaList = teachplanMediaRepository.findByCourseId(courseId);
        List<TeachplanMediaPub> teachplanMediaPubList = new ArrayList<>();
        for (TeachplanMedia teachplanMedia : teachplanMediaList) {
            TeachplanMediaPub teachplanMediaPub = new TeachplanMediaPub();
            BeanUtils.copyProperties(teachplanMedia,teachplanMediaPub);
            teachplanMediaPubList.add(teachplanMediaPub);
        }
        teachplanMediaPubRepository.saveAll(teachplanMediaPubList);
    }
}

