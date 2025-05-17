---
description: Rule to follow when you have a lot of work to do and it is difficult to do it all at once.
---

# Task Management for Large Changes

1.  **Identify Large Task:** If a user request involves significant changes that cannot be reliably completed in a single response, activate this process.
2.  **Break Down Task:** Decompose the overall task into smaller, logical, sequential steps.
3.  **Create Task File:**
    *   Create a new markdown file inside the `.cursor/tasks/` directory. Name it descriptively based on the task (e.g., `refactor_feature_xyz.md`).
    *   List the decomposed steps as a markdown checklist in the file:
        ```markdown
        # Task: [Brief Task Description]

        - [ ] Step 1: Description of the first step.
        - [ ] Step 2: Description of the second step.
        - [ ] Step 3: ...
        ```
4.  **Announce Plan:** Inform the user that the task has been broken down and state the name of the created task file.
5.  **Execute Step-by-Step:**
    *   Execute **only the first incomplete step** (`- [ ]`) listed in the task file.
    *   After successfully completing the step, update the checklist in the task file by marking the corresponding item as complete (`- [x]`):
        ```markdown
        - [x] Step 1: Description of the first step.
        - [ ] Step 2: Description of the second step.
        ```
6.  **Confirm:**
    *   Report the completion of the specific step to the user.
