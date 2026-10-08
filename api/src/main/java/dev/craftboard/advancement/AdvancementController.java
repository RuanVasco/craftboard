package dev.craftboard.advancement;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AdvancementController {

    private final AdvancementService advancementService;

    @GetMapping("/api/advancements")
    public AdvancementsOverview overview(@RequestParam(defaultValue = "20") int limit) {
        return advancementService.overview(Math.clamp(limit, 1, 100));
    }

    @GetMapping("/api/players/{uuid}/advancements")
    public List<Advancement> of(@PathVariable UUID uuid) {
        return advancementService.of(uuid);
    }
}
