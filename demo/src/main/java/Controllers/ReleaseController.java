package Controllers;

import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/release")
public class ReleaseController {

    @Operation(summary = "Запуск сборки релиза")
    @PostMapping("/build")
    public String buildRelease(
            @RequestParam String project,
            @RequestParam String releaseTask,
            @RequestParam String sourceBranch,
            @RequestParam(required = false) boolean skipPipeline) {
        return "Release build started for project: " + project; // Заглушка
    }
}