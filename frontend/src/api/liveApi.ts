import axios, { AxiosInstance } from "axios";
import {
  ApiResult,
  BroadcastListResponse,
  BroadcastResponse,
  BroadcastSearch,
  SliceResponse
} from "../types/live";

const client: AxiosInstance = axios.create({
  baseURL: process.env.NODE_ENV === "production" ? "" : "http://localhost:8080/",
  headers: {
    "Content-Type": "application/json",
  },
});

const cleanParams = (params: BroadcastSearch = {}): BroadcastSearch => {
  const cleaned: BroadcastSearch = {};
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      (cleaned as any)[key] = value;
    }
  });
  return cleaned;
};

export const fetchBroadcasts = async (
  search: BroadcastSearch = {}
): Promise<ApiResult<SliceResponse<BroadcastListResponse>>> => {
  const response = await client.get<ApiResult<SliceResponse<BroadcastListResponse>>>(
    "/api/broadcasts",
    { params: cleanParams(search) }
  );
  return response.data;
};

export const fetchBroadcastDetail = async (
  broadcastId: number
): Promise<ApiResult<BroadcastResponse>> => {
  const response = await client.get<ApiResult<BroadcastResponse>>(
    `/api/broadcasts/${broadcastId}`
  );
  return response.data;
};

export default {
  fetchBroadcasts,
  fetchBroadcastDetail,
};
