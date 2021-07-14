package com.xuecheng.learning.service;

import com.xuecheng.framework.domain.course.TeachplanMediaPub;
import com.xuecheng.framework.domain.learning.response.GetMediaResult;
import com.xuecheng.framework.domain.learning.response.LearningCode;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.learning.client.CourseSearchClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CourseLearningService {
    @Autowired
    private CourseSearchClient courseSearchClient;

    //获取课程学习地址
    public GetMediaResult getMedia(String courseId, String teachplanId) {
        TeachplanMediaPub teachplanMediaPub = courseSearchClient.getMedia(teachplanId);
        if (teachplanMediaPub == null || StringUtils.isEmpty(teachplanMediaPub.getMediaUrl())) {
            ExceptionCast.cast(LearningCode.LEARNING_GETMEDIA_ERROR);
        }
        return new GetMediaResult(CommonCode.SUCCESS,teachplanMediaPub.getMediaUrl());
    }
}
