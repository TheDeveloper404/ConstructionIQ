import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { API } from '../lib/api';
import { toast } from 'sonner';
import StatusBadge from '../components/StatusBadge';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import {
  FolderKanban,
  Building2,
  FileText,
  Receipt,
  Bell,
  Plus,
  ArrowRight,
  TrendingUp,
  AlertTriangle,
} from 'lucide-react';

export default function Dashboard() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const controller = new AbortController();
    const fetchStats = async () => {
      try {
        const response = await API.get('/dashboard/stats', { signal: controller.signal });
        setStats(response.data);
      } catch (error) {
        if (error.name !== 'CanceledError' && error.name !== 'AbortError') {
          toast.error('Eroare la încărcarea datelor din panou');
        }
      } finally {
        setLoading(false);
      }
    };
    fetchStats();
    return () => controller.abort();
  }, []);

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
          {[...Array(5)].map((_, i) => (
            <Card key={i} className="animate-pulse">
              <CardContent className="p-6">
                <div className="h-4 bg-slate-200 rounded w-1/2 mb-2"></div>
                <div className="h-8 bg-slate-200 rounded w-1/3"></div>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
    );
  }

  const statCards = [
    { label: 'Proiecte', value: stats?.projects_count || 0, icon: FolderKanban, color: 'text-blue-600', href: '/projects' },
    { label: 'Furnizori', value: stats?.suppliers_count || 0, icon: Building2, color: 'text-emerald-600', href: '/suppliers' },
    { label: 'Cereri de Ofertă', value: stats?.rfqs_count || 0, icon: FileText, color: 'text-violet-600', href: '/rfqs' },
    { label: 'Oferte Primite', value: stats?.quotes_count || 0, icon: Receipt, color: 'text-amber-600', href: '/quotes' },
    { label: 'Alerte Active', value: stats?.active_alerts || 0, icon: Bell, color: 'text-red-600', href: '/alerts' },
  ];

  return (
    <div className="space-y-8" data-testid="dashboard">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-heading font-bold tracking-tight">Panou de Control</h1>
          <p className="text-muted-foreground mt-1">Prezentare generală achiziții și acțiuni rapide</p>
        </div>
        <div className="flex gap-3">
          <Button asChild data-testid="new-rfq-btn">
            <Link to="/rfqs/new">
              <Plus className="h-4 w-4 mr-2" />
              Cerere Nouă
            </Link>
          </Button>
          <Button variant="outline" asChild data-testid="new-quote-btn">
            <Link to="/quotes/new">
              <Plus className="h-4 w-4 mr-2" />
              Adaugă Ofertă
            </Link>
          </Button>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
        {statCards.map((stat) => {
          const Icon = stat.icon;
          return (
            <Link key={stat.label} to={stat.href} data-testid={`stat-${stat.label.toLowerCase().replace(' ', '-')}`}>
              <Card className="hover:shadow-md transition-shadow cursor-pointer">
                <CardContent className="p-5">
                  <div className="flex items-center justify-between">
                    <Icon className={`h-5 w-5 ${stat.color}`} />
                    <span className="text-2xl font-heading font-bold">{stat.value}</span>
                  </div>
                  <p className="text-sm text-muted-foreground mt-2">{stat.label}</p>
                </CardContent>
              </Card>
            </Link>
          );
        })}
      </div>

      {/* Recent Activity Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Recent RFQs */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-lg font-heading">Cereri Recente</CardTitle>
            <Button variant="ghost" size="sm" asChild>
              <Link to="/rfqs" data-testid="view-all-rfqs">
                Vezi toate <ArrowRight className="h-4 w-4 ml-1" />
              </Link>
            </Button>
          </CardHeader>
          <CardContent>
            {stats?.recent_rfqs?.length > 0 ? (
              <div className="space-y-3">
                {stats.recent_rfqs.map((rfq) => (
                  <Link
                    key={rfq.id}
                    to={`/rfqs/${rfq.id}`}
                    className="block p-3 rounded-lg border hover:bg-slate-50 transition-colors"
                    data-testid={`rfq-${rfq.id}`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-medium text-sm truncate">{rfq.title}</span>
                      <StatusBadge status={rfq.status} />
                    </div>
                    <p className="text-xs text-muted-foreground mt-1">
                      {rfq.items?.length || 0} articole
                    </p>
                  </Link>
                ))}
              </div>
            ) : (
              <EmptyState message="Nicio cerere de ofertă" action={{ label: 'Creează Cerere', href: '/rfqs/new' }} />
            )}
          </CardContent>
        </Card>

        {/* Recent Quotes */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-lg font-heading">Oferte Recente</CardTitle>
            <Button variant="ghost" size="sm" asChild>
              <Link to="/quotes" data-testid="view-all-quotes">
                Vezi toate <ArrowRight className="h-4 w-4 ml-1" />
              </Link>
            </Button>
          </CardHeader>
          <CardContent>
            {stats?.recent_quotes?.length > 0 ? (
              <div className="space-y-3">
                {stats.recent_quotes.map((quote) => (
                  <Link
                    key={quote.id}
                    to={`/quotes/${quote.id}`}
                    className="block p-3 rounded-lg border hover:bg-slate-50 transition-colors"
                    data-testid={`quote-${quote.id}`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-mono text-xs text-muted-foreground">{quote.id.slice(0, 8)}...</span>
                      <span className="font-mono text-sm font-medium">
                        {quote.total_amount?.toLocaleString('ro-RO')} RON
                      </span>
                    </div>
                    <p className="text-xs text-muted-foreground mt-1">
                      {quote.items?.length || 0} articole
                    </p>
                  </Link>
                ))}
              </div>
            ) : (
              <EmptyState message="Nicio ofertă primită" action={{ label: 'Adaugă Ofertă', href: '/quotes/new' }} />
            )}
          </CardContent>
        </Card>

        {/* Recent Alerts */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-lg font-heading">Alerte Recente</CardTitle>
            <Button variant="ghost" size="sm" asChild>
              <Link to="/alerts" data-testid="view-all-alerts">
                Vezi toate <ArrowRight className="h-4 w-4 ml-1" />
              </Link>
            </Button>
          </CardHeader>
          <CardContent>
            {stats?.recent_alerts?.length > 0 ? (
              <div className="space-y-3">
                {stats.recent_alerts.map((alert) => (
                  <div
                    key={alert.id}
                    className="p-3 rounded-lg border hover:bg-slate-50 transition-colors"
                    data-testid={`alert-${alert.id}`}
                  >
                    <div className="flex items-center gap-2">
                      <AlertTriangle className={`h-4 w-4 ${
                        alert.severity === 'high' ? 'text-red-500' :
                        alert.severity === 'medium' ? 'text-amber-500' : 'text-slate-400'
                      }`} />
                      <span className="text-sm font-medium truncate">
                        {alert.payload?.rule_name || 'Alertă Preț'}
                      </span>
                    </div>
                    <p className="text-xs text-muted-foreground mt-1">
                      {alert.payload?.change_percent > 0 ? '+' : ''}{alert.payload?.change_percent}% modificare
                    </p>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-8">
                <Bell className="h-8 w-8 text-muted-foreground/40 mx-auto mb-2" />
                <p className="text-sm text-muted-foreground">Nicio alertă declanșată</p>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Quick Actions */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg font-heading">Acțiuni Rapide</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <QuickActionCard
              icon={FileText}
              label="Cerere Ofertă"
              description="Solicită oferte de la furnizori"
              href="/rfqs/new"
            />
            <QuickActionCard
              icon={Receipt}
              label="Adaugă Ofertă"
              description="Înregistrează o ofertă primită"
              href="/quotes/new"
            />
            <QuickActionCard
              icon={TrendingUp}
              label="Tendințe Prețuri"
              description="Vizualizează istoricul prețurilor"
              href="/price-history"
            />
            <QuickActionCard
              icon={Building2}
              label="Furnizor Nou"
              description="Înregistrează un furnizor"
              href="/suppliers"
            />
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

function EmptyState({ message, action }) {
  return (
    <div className="text-center py-8">
      <p className="text-sm text-muted-foreground mb-3">{message}</p>
      {action && (
        <Button size="sm" variant="outline" asChild>
          <Link to={action.href}>{action.label}</Link>
        </Button>
      )}
    </div>
  );
}

function QuickActionCard({ icon: Icon, label, description, href }) {
  return (
    <Link
      to={href}
      className="block p-4 rounded-lg border border-border hover:border-accent hover:shadow-md transition-all group"
      data-testid={`quick-action-${label.toLowerCase().replace(' ', '-')}`}
    >
      <Icon className="h-6 w-6 text-muted-foreground group-hover:text-accent transition-colors" />
      <h3 className="font-medium text-sm mt-3">{label}</h3>
      <p className="text-xs text-muted-foreground mt-1">{description}</p>
    </Link>
  );
}
