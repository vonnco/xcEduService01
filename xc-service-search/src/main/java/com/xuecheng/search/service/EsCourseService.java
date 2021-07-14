package com.xuecheng.search.service;

import com.xuecheng.framework.domain.course.CoursePub;
import com.xuecheng.framework.domain.course.TeachplanMediaPub;
import com.xuecheng.framework.domain.search.CourseSearchParam;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EsCourseService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EsCourseService.class);
    @Value("${xuecheng.elasticsearch.course.index}")
    private String es_index;
    @Value("${xuecheng.elasticsearch.media.index}")
    private String media_index;
    @Value("${xuecheng.elasticsearch.course.type}")
    private String es_type;
    @Value("${xuecheng.elasticsearch.media.type}")
    private String media_type;
    @Value("${xuecheng.elasticsearch.course.source_field}")
    private String source_field;
    @Value("${xuecheng.elasticsearch.media.source_field}")
    private String media_source_field;
    @Autowired
    RestHighLevelClient restHighLevelClient;

    public QueryResponseResult list(int page, int size, CourseSearchParam courseSearchParam) {
        SearchRequest searchRequest = new SearchRequest(es_index);
        searchRequest.types(es_type);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        if (page <= 0) {
            page = 1;
        }
        if (size <= 0) {
            size = 3;
        }
        int from = (page - 1)*size;
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(size);
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if (StringUtils.isNoneEmpty(courseSearchParam.getKeyword())) {
            MultiMatchQueryBuilder multiMatchQueryBuilder = QueryBuilders.multiMatchQuery(courseSearchParam.getKeyword(), "name", "description", "teachplan")
                    .minimumShouldMatch("70%").field("name",10);
            boolQueryBuilder.must(multiMatchQueryBuilder);
        }
        if (StringUtils.isNoneEmpty(courseSearchParam.getMt())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("mt",courseSearchParam.getMt()));
        }
        if (StringUtils.isNoneEmpty(courseSearchParam.getSt())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("st",courseSearchParam.getSt()));
        }
        if (StringUtils.isNoneEmpty(courseSearchParam.getGrade())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("grade",courseSearchParam.getGrade()));
        }
        searchSourceBuilder.query(boolQueryBuilder);
        String[] split = source_field.split(",");
        searchSourceBuilder.fetchSource(split,new String[]{});
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<font class='eslight'>");
        highlightBuilder.postTags("</font>");
        highlightBuilder.fields().add(new HighlightBuilder.Field("name"));
        searchSourceBuilder.highlighter(highlightBuilder);
        searchRequest.source(searchSourceBuilder);
        QueryResult<CoursePub> queryResult = new QueryResult<>();
        List<CoursePub> list = new ArrayList<>();
        try {
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest);
            SearchHits searchHits = searchResponse.getHits();
            long totalHits = searchHits.getTotalHits();
            queryResult.setTotal(totalHits);
            SearchHit[] hits = searchHits.getHits();
            for (SearchHit hit : hits) {
                CoursePub coursePub = new CoursePub();
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                String name = (String) sourceAsMap.get("name");
                Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                if (highlightFields != null) {
                    HighlightField nameHighlightField = highlightFields.get("name");
                    if (nameHighlightField != null) {
                        StringBuilder stringBuilder = new StringBuilder();
                        Text[] fragments = nameHighlightField.getFragments();
                        for (Text fragment : fragments) {
                            stringBuilder.append(fragment);
                        }
                        name = stringBuilder.toString();
                    }
                }
                coursePub.setName(name);
                String pic = (String) sourceAsMap.get("pic");
                coursePub.setPic(pic);
                Float price = null;
                if(sourceAsMap.get("price")!=null ){
                    price = Float.parseFloat(sourceAsMap.get("price").toString());
                }
                coursePub.setPrice(price);
                Float price_old = null;
                if(sourceAsMap.get("price_old")!=null ){
                    price_old = Float.parseFloat(sourceAsMap.get("price_old").toString());
                }
                coursePub.setPrice_old(price_old);
                list.add(coursePub);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        queryResult.setList(list);
        return new QueryResponseResult(CommonCode.SUCCESS,queryResult);
    }

    //根据id查询课程信息
    public Map<String, CoursePub> getAll(String id) {
        //定义一个搜索请求对象
        SearchRequest searchRequest = new SearchRequest(es_index);
        //指定type
        searchRequest.types(es_type);
        //定义SearchSourceBuilder
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //设置使用termQuery
        searchSourceBuilder.query(QueryBuilders.termQuery("id",id));
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse search = restHighLevelClient.search(searchRequest);
            SearchHits searchHits = search.getHits();
            SearchHit[] hits = searchHits.getHits();
            Map<String, CoursePub> map = new HashMap<>();
            for (SearchHit hit : hits) {
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                String courseId = (String) sourceAsMap.get("id");
                String name = (String) sourceAsMap.get("name");
                String grade = (String) sourceAsMap.get("grade");
                String charge = (String) sourceAsMap.get("charge");
                String pic = (String) sourceAsMap.get("pic");
                String description = (String) sourceAsMap.get("description");
                String teachplan = (String) sourceAsMap.get("teachplan");
                CoursePub coursePub = new CoursePub();
                coursePub.setId(courseId);
                coursePub.setName(name);
                coursePub.setCharge(charge);
                coursePub.setPic(pic);
                coursePub.setGrade(grade);
                coursePub.setTeachplan(teachplan);
                coursePub.setDescription(description);
                map.put(courseId,coursePub);
            }
            return map;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //根据课程计划查询媒资信息
    public QueryResponseResult getMedia(String[] teachplanIds) {
        SearchRequest searchRequest = new SearchRequest(media_index);
        searchRequest.types(media_type);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.termQuery("teachplan_id",teachplanIds));
        String[] split = media_source_field.split(",");
        searchSourceBuilder.fetchSource(split,new String[]{});
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse search = restHighLevelClient.search(searchRequest);
            SearchHits searchHits = search.getHits();
            SearchHit[] hits = searchHits.getHits();
            long totalHits = searchHits.getTotalHits();
            List<TeachplanMediaPub> teachplanMediaPubList = new ArrayList<>();
            for (SearchHit hit : hits) {
                TeachplanMediaPub teachplanMediaPub = new TeachplanMediaPub();
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                String courseid = (String) sourceAsMap.get("courseid");
                String media_id = (String) sourceAsMap.get("media_id");
                String media_url = (String) sourceAsMap.get("media_url");
                String teachplan_id = (String) sourceAsMap.get("teachplan_id");
                String media_fileoriginalname = (String) sourceAsMap.get("media_fileoriginalname");
                teachplanMediaPub.setCourseId(courseid);
                teachplanMediaPub.setMediaUrl(media_url);
                teachplanMediaPub.setMediaFileOriginalName(media_fileoriginalname);
                teachplanMediaPub.setMediaId(media_id);
                teachplanMediaPub.setTeachplanId(teachplan_id);
                teachplanMediaPubList.add(teachplanMediaPub);
            }
            QueryResult<TeachplanMediaPub> queryResult = new QueryResult<>();
            queryResult.setTotal(totalHits);
            queryResult.setList(teachplanMediaPubList);
            return new QueryResponseResult(CommonCode.SUCCESS,queryResult);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new QueryResponseResult(CommonCode.FAIL,null);
    }
}
