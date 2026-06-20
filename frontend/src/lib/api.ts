import axios from "axios";
import type { ApiResponse, PageResponse } from "@/types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export const apiClient = axios.create({
  baseURL: `${API_BASE_URL}/api`,
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 30000,
});

// Response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const message =
      error.response?.data?.message ||
      error.message ||
      "An unexpected error occurred";
    return Promise.reject(new Error(message));
  }
);

export async function get<T>(path: string, params?: Record<string, unknown>): Promise<T> {
  const response = await apiClient.get<ApiResponse<T>>(path, { params });
  if (!response.data.success) {
    throw new Error(response.data.message || "Request failed");
  }
  return response.data.data as T;
}

export async function getPage<T>(
  path: string,
  page = 0,
  size = 20,
  params?: Record<string, unknown>
): Promise<PageResponse<T>> {
  const response = await apiClient.get<ApiResponse<PageResponse<T>>>(path, {
    params: { page, size, ...params },
  });
  if (!response.data.success) {
    throw new Error(response.data.message || "Request failed");
  }
  return response.data.data as PageResponse<T>;
}

export async function post<T>(path: string, body?: unknown): Promise<T> {
  const response = await apiClient.post<ApiResponse<T>>(path, body);
  if (!response.data.success) {
    throw new Error(response.data.message || "Request failed");
  }
  return response.data.data as T;
}

export async function put<T>(path: string, body?: unknown): Promise<T> {
  const response = await apiClient.put<ApiResponse<T>>(path, body);
  if (!response.data.success) {
    throw new Error(response.data.message || "Request failed");
  }
  return response.data.data as T;
}

export async function del(path: string): Promise<void> {
  const response = await apiClient.delete<ApiResponse<void>>(path);
  if (!response.data.success) {
    throw new Error(response.data.message || "Request failed");
  }
}

// Streaming fetch for SSE
export function streamChat(
  body: unknown,
  onToken: (token: string) => void,
  onDone: () => void,
  onError: (err: Error) => void,
  signal?: AbortSignal
) {
  const url = `${API_BASE_URL}/api/chat`;

  fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "text/event-stream",
    },
    body: JSON.stringify(body),
    signal,
  })
    .then(async (response) => {
      if (!response.ok) {
        const text = await response.text();
        throw new Error(`Chat failed: ${response.status} ${text}`);
      }

      const reader = response.body?.getReader();
      if (!reader) throw new Error("No response body");

      const decoder = new TextDecoder();
      let buffer = "";

      const pump = async (): Promise<void> => {
        const { done, value } = await reader.read();
        if (done) {
          onDone();
          return;
        }

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split("\n");
        buffer = lines.pop() || "";

        for (const line of lines) {
          if (line.startsWith("data:")) {
            const data = line.slice(5).trim();
            if (data && data !== "[DONE]") {
              onToken(data);
            }
          }
        }

        return pump();
      };

      return pump();
    })
    .catch((err) => {
      if (err.name !== "AbortError") {
        onError(err instanceof Error ? err : new Error(String(err)));
      }
    });
}
