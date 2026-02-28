import React, { useState } from 'react';
import { Outlet, Link, useLocation } from 'react-router-dom';
import { useDemo } from '../App';
import { API } from '../lib/api';
import { cn } from '../lib/utils';
import { Button } from './ui/button';
import {
  LayoutDashboard,
  Building2,
  FolderKanban,
  FileText,
  Receipt,
  Package,
  TrendingUp,
  Bell,
  Menu,
  X,
  HardHat,
  RefreshCw,
} from 'lucide-react';
import { toast } from 'sonner';

const navItems = [
  { path: '/', label: 'Panou de Control', icon: LayoutDashboard },
  { path: '/suppliers', label: 'Furnizori', icon: Building2 },
  { path: '/projects', label: 'Proiecte', icon: FolderKanban },
  { path: '/rfqs', label: 'Cereri de Ofertă', icon: FileText },
  { path: '/quotes', label: 'Oferte Primite', icon: Receipt },
  { path: '/catalog', label: 'Catalog Produse', icon: Package },
  { path: '/price-history', label: 'Istoric Prețuri', icon: TrendingUp },
  { path: '/alerts', label: 'Alerte', icon: Bell },
];

export default function Layout() {
  const location = useLocation();
  const { isDemo } = useDemo();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [resetting, setResetting] = useState(false);

  const handleResetDemo = async () => {
    if (!window.confirm('Resetați toate datele demo? Această acțiune nu poate fi anulată.')) return;
    
    setResetting(true);
    try {
      await API.post('/demo/reset');
      toast.success('Datele demo au fost resetate cu succes');
      window.location.reload();
    } catch (error) {
      toast.error('Eroare la resetarea datelor demo');
    } finally {
      setResetting(false);
    }
  };

  return (
    <div className="min-h-screen bg-background">
      {/* Mobile sidebar toggle */}
      <div className="lg:hidden fixed top-0 left-0 right-0 z-50 bg-white border-b border-border px-4 py-3 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => setSidebarOpen(!sidebarOpen)}
            data-testid="mobile-menu-toggle"
          >
            {sidebarOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </Button>
          <div className="flex items-center gap-2">
            <HardHat className="h-6 w-6 text-accent" />
            <span className="font-heading font-bold text-lg">ConstructIQ</span>
          </div>
        </div>
        {isDemo && (
          <span className="demo-badge">
            <span className="w-2 h-2 rounded-full bg-amber-500"></span>
            Demo
          </span>
        )}
      </div>

      {/* Sidebar */}
      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-40 w-64 bg-primary text-primary-foreground transform transition-transform duration-200 ease-in-out lg:translate-x-0',
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        )}
      >
        <div className="flex flex-col h-full">
          {/* Logo */}
          <div className="px-6 py-5 border-b border-white/10">
            <div className="flex items-center gap-3">
              <HardHat className="h-8 w-8 text-accent" />
              <div>
                <h1 className="font-heading font-bold text-xl">ConstructIQ</h1>
                <p className="text-xs text-primary-foreground/60">Inteligență în Achiziții</p>
              </div>
            </div>
          </div>

          {/* Navigation */}
          <nav className="flex-1 px-3 py-4 overflow-y-auto">
            <ul className="space-y-1">
              {navItems.map((item) => {
                const isActive = location.pathname === item.path || 
                  (item.path !== '/' && location.pathname.startsWith(item.path));
                const Icon = item.icon;
                
                return (
                  <li key={item.path}>
                    <Link
                      to={item.path}
                      onClick={() => setSidebarOpen(false)}
                      data-testid={`nav-${item.label.toLowerCase().replace(' ', '-')}`}
                      className={cn(
                        'flex items-center gap-3 px-3 py-2.5 rounded-md text-sm font-medium transition-colors',
                        isActive
                          ? 'bg-white/15 text-white'
                          : 'text-white/70 hover:bg-white/10 hover:text-white'
                      )}
                    >
                      <Icon className="h-5 w-5 flex-shrink-0" />
                      {item.label}
                    </Link>
                  </li>
                );
              })}
            </ul>
          </nav>

          {/* Demo controls */}
          {isDemo && (
            <div className="px-4 py-4 border-t border-white/10">
              <div className="demo-badge mb-3 w-full justify-center">
                <span className="w-2 h-2 rounded-full bg-amber-500"></span>
                Mod Demo Activ
              </div>
              <Button
                variant="outline"
                size="sm"
                className="w-full bg-transparent border-white/20 text-white/80 hover:bg-white/10 hover:text-white"
                onClick={handleResetDemo}
                disabled={resetting}
                data-testid="reset-demo-btn"
              >
                <RefreshCw className={cn('h-4 w-4 mr-2', resetting && 'animate-spin')} />
                Resetare Date Demo
              </Button>
            </div>
          )}
        </div>
      </aside>

      {/* Mobile overlay */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-30 bg-black/50 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Main content */}
      <main className="lg:pl-64 pt-14 lg:pt-0 min-h-screen">
        <div className="p-6 md:p-8">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
