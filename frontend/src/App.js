import React, { createContext, useContext, useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from './components/ui/sonner';

// API - imported here and re-exported for backward compatibility with pages that import from '../App'
import { API } from './lib/api';
export { API };

// Layout
import Layout from './components/Layout';

// Pages
import Dashboard from './pages/Dashboard';
import Suppliers from './pages/Suppliers';
import SupplierDetail from './pages/SupplierDetail';
import Projects from './pages/Projects';
import ProjectDetail from './pages/ProjectDetail';
import RFQs from './pages/RFQs';
import RFQDetail from './pages/RFQDetail';
import RFQCreate from './pages/RFQCreate';
import Quotes from './pages/Quotes';
import QuoteDetail from './pages/QuoteDetail';
import QuoteCreate from './pages/QuoteCreate';
import Catalog from './pages/Catalog';
import PriceHistory from './pages/PriceHistory';
import Alerts from './pages/Alerts';

// Demo Context
const DemoContext = createContext({
  isDemo: true,
  demoOrgId: null,
  demoUserId: null,
});

export const useDemo = () => useContext(DemoContext);

function App() {
  const [demoStatus, setDemoStatus] = useState({
    isDemo: true,
    demoOrgId: null,
    demoUserId: null,
    loading: true,
  });

  useEffect(() => {
    const fetchDemoStatus = async () => {
      try {
        const response = await API.get('/demo/status');
        setDemoStatus({
          isDemo: response.data.demo_mode,
          demoOrgId: response.data.demo_org_id,
          demoUserId: response.data.demo_user_id,
          loading: false,
        });
      } catch (error) {
        console.error('Failed to fetch demo status:', error);
        setDemoStatus(prev => ({ ...prev, loading: false }));
      }
    };
    fetchDemoStatus();
  }, []);

  if (demoStatus.loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
          <p className="text-muted-foreground">Loading ConstructIQ...</p>
        </div>
      </div>
    );
  }

  return (
    <DemoContext.Provider value={demoStatus}>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Layout />}>
            <Route index element={<Dashboard />} />
            <Route path="suppliers" element={<Suppliers />} />
            <Route path="suppliers/:id" element={<SupplierDetail />} />
            <Route path="projects" element={<Projects />} />
            <Route path="projects/:id" element={<ProjectDetail />} />
            <Route path="rfqs" element={<RFQs />} />
            <Route path="rfqs/new" element={<RFQCreate />} />
            <Route path="rfqs/:id" element={<RFQDetail />} />
            <Route path="quotes" element={<Quotes />} />
            <Route path="quotes/new" element={<QuoteCreate />} />
            <Route path="quotes/:id" element={<QuoteDetail />} />
            <Route path="catalog" element={<Catalog />} />
            <Route path="price-history" element={<PriceHistory />} />
            <Route path="alerts" element={<Alerts />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
      <Toaster position="top-right" richColors />
    </DemoContext.Provider>
  );
}

export default App;
