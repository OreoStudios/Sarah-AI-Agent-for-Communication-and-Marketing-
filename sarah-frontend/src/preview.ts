import type { AxiosAdapter } from 'axios';
import type { Autonomy, Campaign, Channel, DashboardSummary, Publication } from './api';

export const preview = import.meta.env.DEV && window.location.pathname.startsWith('/preview');
export const previewPages = [
  ['/', 'Dashboard'], ['/chat', 'Chat'], ['/campaigns', 'Campaigns'],
  ['/calendar', 'Calendar'], ['/prospects', 'Prospects'], ['/settings', 'Settings'],
] as const;

const autonomy: Autonomy = { enabled: true, dailyPostLimit: 5, perPlatformCooldownMinutes: 60, maxLlmCallsPerRun: 10 };
const channels: Channel[] = [{ id: 'demo', type: 'linkedin', name: 'Oreo Studios', status: 'CONNECTED', live: false, capabilities: [], setupHint: null }];
const campaigns: Campaign[] = [{
  id: 'demo', name: 'A fresh perspective', objective: 'Introduce our creative studio to new audiences.',
  targetAudience: 'Independent businesses', platforms: ['linkedin'], language: 'en', status: 'RUNNING',
  budgetCents: 0, startAt: null, endAt: null, createdBy: 'Preview',
  createdAt: new Date().toISOString(), updatedAt: new Date().toISOString(),
}];
const publications: Publication[] = [{
  id: 'demo', campaignId: 'demo', platform: 'linkedin', content: 'Good ideas deserve thoughtful design. Discover what we are creating at Oreo Studios.',
  scheduledAt: new Date(Date.now() + 86400000).toISOString(), status: 'SCHEDULED', attempts: 0, maxAttempts: 3,
  externalId: null, externalUrl: null, lastError: null, publishedAt: null,
}];
const summary: DashboardSummary = {
  llmProvider: 'Preview', llmLive: false, autonomyEnabled: true, autonomy,
  campaignCountsByStatus: { RUNNING: 1 }, activeCampaigns: campaigns,
  upcomingPublications: publications, channels, recentActivity: [],
};

// Preview requests never reach the backend, including form submissions.
export const previewAdapter: AxiosAdapter = async (config) => {
  const responses: Record<string, unknown> = {
    '/api/v1/dashboard/summary': summary,
    '/api/v1/campaigns': campaigns,
    '/api/v1/campaigns/demo/drafts': [],
    '/api/v1/connectors': channels,
    '/api/v1/autonomy': autonomy,
    '/api/v1/publications/calendar': publications,
    '/api/v1/publications/history': [],
    '/api/v1/prospects': [{ id: 'demo', businessName: 'Atelier Bloom', website: 'https://example.com', sector: 'Design', location: 'Paris', leadScore: 85, contactStatus: 'NEW', notes: 'Sample prospect', outreachDraft: null, suppressed: false, updatedAt: new Date().toISOString() }],
  };
  const data = config.method === 'get' ? responses[config.url ?? ''] ?? [] : {
    conversationId: 'preview', reply: 'This is a design preview. Sign in to chat with Sarah and save your work.',
    actions: [], campaignId: null, llmProvider: 'Preview',
  };
  return { data, status: 200, statusText: 'OK', headers: {}, config };
};
