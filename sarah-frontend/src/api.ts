import axios from 'axios';
import { preview, previewAdapter } from './preview';

// Same-origin. The HttpOnly session cookie and the XSRF-TOKEN cookie travel with every request;
// axios echoes the latter back as the X-XSRF-TOKEN header automatically. No tokens in localStorage.
export const api = axios.create({
  baseURL: '/',
  withCredentials: true,
  ...(preview ? { adapter: previewAdapter } : {}),
});

// ---- Conversational auth ------------------------------------------------

export type AuthStateValue =
  | 'GUEST' | 'EMAIL_PENDING' | 'CODE_SENT' | 'VERIFYING' | 'AUTHENTICATED' | 'EXPIRED' | 'LOCKED';

/** Sarah's reply to an onboarding/auth turn, and the resulting state. */
export interface AuthReply {
  reply: string;
  state: AuthStateValue;
  email: string | null;
  authenticated: boolean;
}

// ---- API types (mirror backend DTOs) ------------------------------------

export interface Me { email: string; roles: string[]; }

export interface AgentAction { agent: string; action: string; detail: string; }
export interface ChatOutcome {
  conversationId: string;
  reply: string;
  campaignId: string | null;
  actions: AgentAction[];
  llmProvider: string;
}

export interface Campaign {
  id: string; name: string; objective: string; targetAudience: string;
  platforms: string[]; language: string; status: string; budgetCents: number;
  startAt: string | null; endAt: string | null; createdBy: string;
  createdAt: string; updatedAt: string;
}

export interface Draft {
  id: string; campaignId: string; platform: string; language: string;
  title: string | null; body: string; imageBrief: string | null;
  status: string; complianceScore: number | null; complianceNotes: string | null; createdAt: string;
}

export interface Publication {
  id: string; campaignId: string | null; platform: string; content: string;
  scheduledAt: string; status: string; attempts: number; maxAttempts: number;
  externalId: string | null; externalUrl: string | null; lastError: string | null; publishedAt: string | null;
}

export interface Channel {
  id: string; type: string; name: string; status: string;
  live: boolean; capabilities: string[]; setupHint: string | null;
}

export interface Autonomy {
  enabled: boolean; dailyPostLimit: number; perPlatformCooldownMinutes: number; maxLlmCallsPerRun: number;
}

export interface DashboardSummary {
  llmProvider: string; llmLive: boolean; autonomyEnabled: boolean; autonomy: Autonomy;
  campaignCountsByStatus: Record<string, number>;
  activeCampaigns: Campaign[];
  upcomingPublications: Publication[];
  channels: Channel[];
  recentActivity: { actor: string; action: string; entityType: string | null; entityId: string | null; at: string }[];
}

export interface Prospect {
  id: string; businessName: string; website: string | null; sector: string | null;
  location: string | null; leadScore: number; contactStatus: string; notes: string | null;
  outreachDraft: string | null; suppressed: boolean; updatedAt: string;
}
