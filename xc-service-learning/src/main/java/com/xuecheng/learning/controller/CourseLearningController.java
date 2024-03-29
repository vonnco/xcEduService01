package com.xuecheng.learning.controller;

import com.xuecheng.api.learning.CourseLearningControllerApi;
import com.xuecheng.framework.domain.learning.response.GetMediaResult;
import com.xuecheng.learning.service.CourseLearningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/learning/course")
public class CourseLearningController implements CourseLearningControllerApi {
    @Autowired
    private CourseLearningService courseLearningService;

    @Override
    @GetMapping("/getmedia/{courseId}/{teachplanId}")
    public GetMediaResult getMedia(@PathVariable("courseId") String courseId,@PathVariable("teachplanId") String teachplanId) {
        return courseLearningService.getMedia(courseId,teachplanId);
    }
}
