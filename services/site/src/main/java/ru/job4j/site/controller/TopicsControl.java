package ru.job4j.site.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.job4j.site.dto.InterviewDTO;
import ru.job4j.site.dto.TopicDTO;
import ru.job4j.site.service.*;

import javax.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ru.job4j.site.controller.RequestResponseTools.getToken;

@Controller
@RequestMapping("/topics")
@AllArgsConstructor
@Slf4j
public class TopicsControl {
    private final TopicsService topicsService;
    private final AuthService authService;
    private final CategoriesService categoriesService;
    private final InterviewsService interviewsService;
    private final NotificationService notifications;

    @GetMapping("/{categoryId}")
    public String getByCategory(@PathVariable int categoryId,
                                Model model,
                                HttpServletRequest req) throws JsonProcessingException {
        var topics = topicsService.getByCategory(categoryId);
        List<InterviewDTO> interviewsByType = interviewsService.getByType(1);

        Map<Integer, List<InterviewDTO>> interviewsByTopicMap
                = interviewsByTopicMap(topics, interviewsByType);

        model.addAttribute("categoryId", categoryId);
        model.addAttribute("topics", topics);
        model.addAttribute("new_interviews", interviewsByType);
        model.addAttribute("map", interviewsByTopicMap);
        String categoryName = topics.isEmpty() ? "" : topics.get(0).getCategory().getName();
        RequestResponseTools.addAttrBreadcrumbs(model,
                "Главная", "/index",
                "Категории", "/categories/",
                categoryName, String.format("/topics/%d", categoryId));
        try {
            var token = getToken(req);
            if (token != null) {
                var userInfo = authService.userInfo(token);
                model.addAttribute("userInfo", userInfo);
                RequestResponseTools.addAttrCanManage(model, userInfo);
                categoriesService.updateStatistic(token, categoryId);
                model.addAttribute("userTopicDTO", notifications.findTopicByUserId(userInfo.getId()));
            }
        } catch (Exception e) {
            RequestResponseTools.addAttrBreadcrumbs(model,
                    "Главная", "/index",
                    "Категории", "/categories/",
                    categoryName, String.format("/topics/%d", categoryId));
            log.error("Remote application not responding. Error: {}. {}, ", e.getCause(), e.getMessage());
        }
        return "topic/topics";
    }
    private Map<Integer, List<InterviewDTO>> interviewsByTopicMap(
            List<TopicDTO> topics, List<InterviewDTO> interviews) {
        Map<Integer, List<InterviewDTO>> map = new HashMap<>();
        for (TopicDTO topicDTO : topics) {
            map.put(
                    topicDTO.getId(),
                    interviews.stream().filter(i -> i.getId() == topicDTO.getId()).toList()
            );
        }
        return map;
    }
}