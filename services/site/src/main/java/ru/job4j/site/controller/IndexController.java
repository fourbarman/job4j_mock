package ru.job4j.site.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ru.job4j.site.dto.CategoryDTO;
import ru.job4j.site.dto.InterviewDTO;
import ru.job4j.site.dto.ProfileDTO;
import ru.job4j.site.dto.TopicDTO;
import ru.job4j.site.service.*;

import javax.servlet.http.HttpServletRequest;

import java.util.*;
import java.util.stream.Collectors;

import static ru.job4j.site.controller.RequestResponseTools.getToken;

@Controller
@AllArgsConstructor
@Slf4j
public class IndexController {
    private final CategoriesService categoriesService;
    private final InterviewsService interviewsService;
    private final AuthService authService;
    private final NotificationService notifications;
    private final ProfilesService profilesService;
    private final TopicsService topicsService;

    @GetMapping({"/", "index"})
    public String getIndexPage(Model model, HttpServletRequest req) throws JsonProcessingException {
        RequestResponseTools.addAttrBreadcrumbs(model,
                "Главная", "/"
        );
        try {
            List<CategoryDTO> mostPopular = categoriesService.getMostPopular();
            List<InterviewDTO> interviewsByType = interviewsService.getByType(1);
            Set<ProfileDTO> authors = interviewsByType.stream()
                    .map(x -> profilesService.getProfileById(x.getSubmitterId()))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
            Set<TopicDTO> topicDTOSet = new HashSet<>();
            for (CategoryDTO mostPopularCategory : mostPopular) {
                topicDTOSet.addAll(topicsService.getByCategory(mostPopularCategory.getId()));
            }
            Map<Integer, List<InterviewDTO>> interviewsByCategoryMap
                    = getInterviewsByCategory(topicDTOSet, interviewsByType);
            model.addAttribute("categories", mostPopular);
            model.addAttribute("authors", authors);
            model.addAttribute("new_interviews", interviewsByType);
            model.addAttribute("map", interviewsByCategoryMap);
            var token = getToken(req);
            if (token != null) {
                var userInfo = authService.userInfo(token);
                model.addAttribute("userInfo", userInfo);
                model.addAttribute("userDTO", notifications.findCategoriesByUserId(userInfo.getId()));
                RequestResponseTools.addAttrCanManage(model, userInfo);
            }
        } catch (Exception e) {
            log.error("Remote application not responding. Error: {}. {}, ", e.getCause(), e.getMessage());
        }

        return "index";
    }

    private Map<Integer, List<InterviewDTO>> getInterviewsByCategory(
            Set<TopicDTO> topics, List<InterviewDTO> interviews) {
        Map<Integer, List<InterviewDTO>> map = new HashMap<>();
        for (TopicDTO topicDTO : topics) {
            map.put(
                    topicDTO.getCategory().getId(),
                    interviews.stream().filter(i -> i.getId() == topicDTO.getId()).toList()
            );
        }
        return map;
    }
}