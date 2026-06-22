import type { VirtualTimeResponse } from '../types/api';
import { get } from './apiClient';

export function fetchVirtualTime(): Promise<VirtualTimeResponse> {
  return get<VirtualTimeResponse>('/api/virtual-time');
}
