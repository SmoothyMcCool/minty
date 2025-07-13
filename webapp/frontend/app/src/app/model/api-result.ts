export interface ApiResult {
    ok: boolean;
    code: number;
    messages: string[];
    data: unknown;
}