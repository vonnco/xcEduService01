package com.xuecheng.manage_course.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xuecheng.framework.domain.course.CourseBase;
import com.xuecheng.framework.domain.course.CourseMarket;
import com.xuecheng.framework.domain.course.Teachplan;
import com.xuecheng.framework.domain.course.ext.CourseInfo;
import com.xuecheng.framework.domain.course.ext.TeachplanNode;
import com.xuecheng.framework.domain.course.request.CourseListRequest;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_course.dao.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}

