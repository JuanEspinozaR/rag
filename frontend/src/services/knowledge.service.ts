import { get, getPage, post, put, del } from "@/lib/api";
import type {
  KnowledgeSource,
  KnowledgeSourceRequest,
  Document,
  PageResponse,
} from "@/types";

const BASE = "/knowledge-sources";

export const knowledgeService = {
  list: (page = 0, size = 20): Promise<PageResponse<KnowledgeSource>> =>
    getPage<KnowledgeSource>(BASE, page, size),

  getById: (id: string): Promise<KnowledgeSource> =>
    get<KnowledgeSource>(`${BASE}/${id}`),

  create: (data: KnowledgeSourceRequest): Promise<KnowledgeSource> =>
    post<KnowledgeSource>(BASE, data),

  update: (id: string, data: KnowledgeSourceRequest): Promise<KnowledgeSource> =>
    put<KnowledgeSource>(`${BASE}/${id}`, data),

  delete: (id: string): Promise<void> =>
    del(`${BASE}/${id}`),

  sync: (id: string): Promise<void> =>
    post<void>(`${BASE}/${id}/sync`),

  listDocuments: (
    sourceId: string,
    page = 0,
    size = 20
  ): Promise<PageResponse<Document>> =>
    getPage<Document>(`${BASE}/${sourceId}/documents`, page, size),
};
