# Database

Room database `aura_database` contains:

- `memories`: key/value/category records with created and updated timestamps.
- `scheduled_tasks`: original instruction, normalized schedule, workflow configuration, enabled state, and next execution time.
- `execution_logs`: task command, success/failure status, result, duration, and timestamp.
- `pending_actions`: confirmation-gated actions.

Memory records are intentionally separate from chat messages. The application does not persist conversation history as personal memory.

Repositories expose domain models and mappers isolate Room entities from UI and agent code. Keyword retrieval searches memory key, value, and category using SQL `LIKE`.
