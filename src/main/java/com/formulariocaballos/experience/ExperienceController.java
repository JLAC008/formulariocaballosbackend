package com.formulariocaballos.experience;

import com.formulariocaballos.state.dto.ExperienceDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/experiences")
public class ExperienceController {
    private final ExperienceRepository experiences;
    private final ObjectMapper objectMapper;

    public ExperienceController(ExperienceRepository experiences, ObjectMapper objectMapper) {
        this.experiences = experiences;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public List<ExperienceDto> listActive() {
        return experiences.findAll().stream()
            .filter(Experience::getActive)
            .sorted(Comparator.comparing(Experience::getId))
            .map(this::toDto)
            .toList();
    }

    private ExperienceDto toDto(Experience experience) {
        return new ExperienceDto(experience.getId(), experience.getType(), experience.getTitle(),
            experience.getDescription(), experience.getLevel(), experience.getDuration(), experience.getPrice(),
            experience.getImage(), experience.getActive(), readList(experience.getHours()), readMap(experience.getHourMessages()));
    }

    private List<String> readList(String value) {
        try { return objectMapper.readValue(value, new TypeReference<>() {}); }
        catch (Exception ignored) { return List.of(); }
    }

    private java.util.Map<String, String> readMap(String value) {
        try { return objectMapper.readValue(value, new TypeReference<>() {}); }
        catch (Exception ignored) { return java.util.Map.of(); }
    }
}
