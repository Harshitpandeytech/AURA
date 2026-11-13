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
        messages: request.contents,
        tools: request.tools ?? [],
        tool_choice: "auto"
      })
    });
    if (response.ok) return response.json();
    throw new Error(`Groq request failed (${response.status}): ${await response.text()}`);
  }

  throw new Error("No AI provider is configured");
}
