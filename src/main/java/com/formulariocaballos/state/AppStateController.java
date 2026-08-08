package com.formulariocaballos.state;

import com.formulariocaballos.state.dto.AppStateDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/state")
public class AppStateController {

    private final AppStateService appStateService;

    public AppStateController(AppStateService appStateService) {
        this.appStateService = appStateService;
    }

    @GetMapping
    public ResponseEntity<AppStateDto> getState() {
        return ResponseEntity.ok(appStateService.getState());
    }

    @PutMapping
    public ResponseEntity<AppStateDto> replaceState(@Valid @RequestBody AppStateDto state) {
        return ResponseEntity.ok(appStateService.replaceState(state));
    }
}
