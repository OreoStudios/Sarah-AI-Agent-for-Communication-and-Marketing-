import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import './index.css';
import { AuthProvider, useAuth } from './auth';
import { Layout, Loading } from './components/Layout';
import { GuestGate } from './pages/GuestGate';
import { Dashboard } from './pages/Dashboard';
import { Chat } from './pages/Chat';
import { Campaigns } from './pages/Campaigns';
import { Calendar } from './pages/Calendar';
import { Prospects } from './pages/Prospects';
import { Settings } from './pages/Settings';
import { preview } from './preview';

function Workspace() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/chat" element={<Chat />} />
        <Route path="/campaigns" element={<Campaigns />} />
        <Route path="/calendar" element={<Calendar />} />
        <Route path="/prospects" element={<Prospects />} />
        <Route path="/settings" element={<Settings />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Layout>
  );
}

/** Guests meet Sarah (and sign in by conversation); authenticated users get the workspace. */
function Gated() {
  const { me, loading } = useAuth();
  if (preview) return <Workspace />;
  if (loading) return <div className="gate"><Loading /></div>;
  return me ? <Workspace /> : <GuestGate />;
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter basename={preview ? '/preview' : '/'}>
      <AuthProvider>
        <Gated />
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
);
