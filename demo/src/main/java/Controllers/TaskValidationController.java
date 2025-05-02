package Controllers;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskValidationController {

    @GetMapping("/validate")
    public String validateTasks(@RequestParam String releaseTaskId) {
        return "Validation started for task: " + releaseTaskId; // Заглушка
    }
}