package ru.rbpo.lab1;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class LabController {
    @GetMapping("/message")
    public Map<String, String> message() {
        return Map.of("message", "Проект работает");
    }

    @GetMapping("/numbers")
    public Map<String, List<Integer>> numbers() {
        return Map.of("numbers", List.of(1, 2, 3));
    }
}
