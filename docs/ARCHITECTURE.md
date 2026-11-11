# Architecture

```text
Compose UI
  -> ViewModels / StateFlow
  -> Domain repositories and AgentOrchestrator
  -> Room repositories, Gemini HTTP client, Gmail API, controlled WebView
  -> WorkManager AgentWorker for recurring tasks
```

## Agent flow

The orchestrator retrieves keyword-matching memories, sends the user command and tool declarations to Gemini, validates returned function calls, executes registered tools, and sends tool results back in a bounded loop. Five tool calls is the maximum per command.

Email tools are confirmable. Gemini can prepare a draft, but the send operation only occurs after the user presses the confirmation button. Pending drafts are persisted in Room so process death does not silently lose an approval request.

## Scheduling

`ScheduleParser` accepts daily and weekly phrases such as `Every day at 9 AM` and `Every Monday at 10 AM`. Tasks are persisted in Room and scheduled as unique periodic WorkManager work. The validated `schedule_task` tool uses the same repository and scheduler as the Tasks screen. `BootReceiver` re-registers enabled tasks after a device restart. Android may defer WorkManager execution; the application does not promise exact wall-clock execution.

## Security and errors

API keys are loaded from ignored `local.properties`. Gmail tokens are stored with encrypted preferences. Browser URLs must use HTTPS and JavaScript is disabled. API, tool, worker, and repository failures are surfaced as errors and written to execution history where applicable.
