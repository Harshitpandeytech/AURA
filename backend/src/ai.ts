import { config } from "./config.js";

export type AiRequest = {
  contents: unknown[];
  tools?: unknown[];
};

export async function generateWithProvider(request: AiRequest): Promise<unknown> {
  if (config.GEMINI_API_KEY) {
    const response = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/${config.GEMINI_MODEL}:generateContent?key=${encodeURIComponent(config.GEMINI_API_KEY)}`,
      {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ contents: request.contents, tools: request.tools ?? [] })
      }
    );
    if (response.ok) return response.json();
    if (response.status !== 429 && response.status < 500) {
      throw new Error(`Gemini request failed (${response.status}): ${await response.text()}`);
    }
  }

  if (config.GROQ_API_KEY) {
    const response = await fetch("https://api.groq.com/openai/v1/chat/completions", {
      method: "POST",
      headers: {
        authorization: `Bearer ${config.GROQ_API_KEY}`,
        "content-type": "application/json"
      },
      body: JSON.stringify({
        model: config.GROQ_MODEL,
        messages: request.contents.map(toOpenAiMessage),
        tools: toOpenAiTools(request.tools ?? []),
        tool_choice: "auto"
      })
    });
    if (response.ok) return response.json();
    throw new Error(`Groq request failed (${response.status}): ${await response.text()}`);
  }

  throw new Error("No AI provider is configured");
}

function toOpenAiMessage(content: unknown): unknown {
  if (!isRecord(content)) return content;
  const parts = Array.isArray(content.parts) ? content.parts : [];
  const text = parts
    .filter(isRecord)
    .map((part) => typeof part.text === "string" ? part.text : "")
    .filter(Boolean)
    .join("\n");
  const functionCalls = parts
    .filter(isRecord)
    .map((part) => part.functionCall)
    .filter(isRecord);
  if (functionCalls.length > 0) {
    return {
      role: "assistant",
      content: text || null,
      tool_calls: functionCalls.map((call, index) => ({
        id: `aura_call_${index}`,
        type: "function",
        function: {
          name: String(call.name ?? ""),
          arguments: JSON.stringify(call.args ?? {})
        }
      }))
    };
  }
  return { role: content.role === "model" ? "assistant" : "user", content: text };
}

function toOpenAiTools(tools: unknown[]): unknown[] {
  return tools.flatMap((tool) => {
    if (!isRecord(tool) || !Array.isArray(tool.functionDeclarations)) return [];
    return tool.functionDeclarations.filter(isRecord).map((declaration) => ({
      type: "function",
      function: {
        name: String(declaration.name ?? ""),
        description: String(declaration.description ?? ""),
        parameters: declaration.parameters ?? { type: "object", properties: {} }
      }
    }));
  });
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
