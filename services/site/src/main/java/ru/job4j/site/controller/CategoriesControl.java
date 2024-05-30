package ru.job4j.site.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.job4j.site.dto.CategoryDTO;
import ru.job4j.site.dto.InterviewDTO;
import ru.job4j.site.dto.TopicDTO;
import ru.job4j.site.service.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static ru.job4j.site.controller.RequestResponseTools.getToken;

@Controller
@RequestMapping("/categories")
@AllArgsConstructor
@Slf4j
public class CategoriesControl {
    private final CategoriesService categoriesService;
    private final AuthService authService;
    private final NotificationService notifications;
    private final TopicsService topicsService;
    private final InterviewsService interviewsService;

    @GetMapping("/")
    public String categories(Model model, HttpServletRequest req) throws JsonProcessingException {
        try {
            List<CategoryDTO> allWithTopics = categoriesService.getAllWithTopics();
            model.addAttribute("categories", categoriesService.getAllWithTopics());

            List<InterviewDTO> interviewsByType = interviewsService.getByType(1);
            Set<TopicDTO> topicDTOSet = new HashSet<>();
            for (CategoryDTO mostPopularCategory : allWithTopics) {
                topicDTOSet.addAll(topicsService.getByCategory(mostPopularCategory.getId()));
            }
            Map<Integer, List<InterviewDTO>> interviewsByCategoryMap
                    = getInterviewsByCategory(topicDTOSet, interviewsByType);

            var token = getToken(req);
            if (token != null) {
                var userInfo = authService.userInfo(token);
                model.addAttribute("userInfo", userInfo);
                model.addAttribute("userDTO", notifications.findCategoriesByUserId(userInfo.getId()));
                RequestResponseTools.addAttrCanManage(model, userInfo);
            }
            RequestResponseTools.addAttrBreadcrumbs(model,
                    "Главная", "/index",
                    "Категории", "/categories/"
            );
            model.addAttribute("map", interviewsByCategoryMap);
            model.addAttribute("current_page", "categories");
        } catch (Exception e) {
            RequestResponseTools.addAttrBreadcrumbs(model,
                    "Главная", "/index"
            );
            log.error("Remote application not responding. Error: {}. {}, ", e.getCause(), e.getMessage());
        }
        return "categories/categories";
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
