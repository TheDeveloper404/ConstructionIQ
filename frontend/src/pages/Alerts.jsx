import React, { useState, useEffect } from 'react';
import { API } from '../App';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Badge } from '../components/ui/badge';
import { Switch } from '../components/ui/switch';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../components/ui/table';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '../components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import { Plus, Bell, AlertTriangle, Check, ChevronLeft, ChevronRight, Trash2 } from 'lucide-react';
import { toast } from 'sonner';

export default function Alerts() {
  const [rules, setRules] = useState([]);
  const [events, setEvents] = useState([]);
  const [loadingRules, setLoadingRules] = useState(true);
  const [loadingEvents, setLoadingEvents] = useState(true);
  const [eventPagination, setEventPagination] = useState({ page: 1, total: 0, totalPages: 0 });
  const [eventFilter, setEventFilter] = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    type: 'threshold_vs_last',
    params: { threshold_percent: 10, compare_last_n: 3 },
    is_active: true,
  });

  const fetchRules = async () => {
    setLoadingRules(true);
    try {
      const response = await API.get('/alerts/rules');
      setRules(response.data.rules || []);
    } catch (error) {
      toast.error('Eroare la încărcarea regulilor');
    } finally {
      setLoadingRules(false);
    }
  };

  const fetchEvents = async (page = 1, status = '') => {
    setLoadingEvents(true);
    try {
      const params = new URLSearchParams({ page, page_size: 10 });
      if (status) params.append('status', status);
      
      const response = await API.get(`/alerts/events?${params}`);
      setEvents(response.data.items);
      setEventPagination({
        page: response.data.page,
        total: response.data.total,
        totalPages: response.data.total_pages,
      });
    } catch (error) {
      toast.error('Eroare la încărcarea alertelor');
    } finally {
      setLoadingEvents(false);
    }
  };

  useEffect(() => {
    fetchRules();
    fetchEvents(1, eventFilter);
  }, []);

  useEffect(() => {
    fetchEvents(1, eventFilter);
  }, [eventFilter]);

  const handleSubmitRule = async (e) => {
    e.preventDefault();
    try {
      await API.post('/alerts/rules', formData);
      toast.success('Regulă creată');
      setDialogOpen(false);
      setFormData({
        name: '',
        type: 'threshold_vs_last',
        params: { threshold_percent: 10, compare_last_n: 3 },
        is_active: true,
      });
      fetchRules();
    } catch (error) {
      toast.error('Eroare la crearea regulii');
    }
  };

  const handleToggleRule = async (ruleId, isActive) => {
    try {
      await API.put(`/alerts/rules/${ruleId}`, { is_active: isActive });
      setRules(rules.map((r) => (r.id === ruleId ? { ...r, is_active: isActive } : r)));
      toast.success(isActive ? 'Regulă activată' : 'Regulă dezactivată');
    } catch (error) {
      toast.error('Eroare la actualizarea regulii');
    }
  };

  const handleDeleteRule = async (ruleId) => {
    if (!window.confirm('Ștergeți această regulă?')) return;
    try {
      await API.delete(`/alerts/rules/${ruleId}`);
      toast.success('Regulă ștearsă');
      fetchRules();
    } catch (error) {
      toast.error('Eroare la ștergerea regulii');
    }
  };

  const handleAcknowledgeEvent = async (eventId) => {
    try {
      await API.put(`/alerts/events/${eventId}`, { status: 'ack' });
      setEvents(events.map((e) => (e.id === eventId ? { ...e, status: 'ack' } : e)));
      toast.success('Alertă confirmată');
    } catch (error) {
      toast.error('Eroare la confirmarea alertei');
    }
  };

  const getSeverityBadge = (severity) => {
    const labels = {
      high: 'Ridicată',
      medium: 'Medie',
      low: 'Scăzută',
    };
    const styles = {
      high: 'bg-red-100 text-red-700',
      medium: 'bg-amber-100 text-amber-700',
      low: 'bg-slate-100 text-slate-600',
    };
    return (
      <Badge variant="secondary" className={styles[severity] || styles.low}>
        {labels[severity] || severity}
      </Badge>
    );
  };

  return (
    <div className="space-y-6" data-testid="alerts-page">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-heading font-bold tracking-tight">Alerte</h1>
          <p className="text-muted-foreground mt-1">Notificări pentru modificări de preț</p>
        </div>
      </div>

      <Tabs defaultValue="events" className="space-y-6">
        <TabsList>
          <TabsTrigger value="events" data-testid="events-tab">
            Evenimente Alertă
          </TabsTrigger>
          <TabsTrigger value="rules" data-testid="rules-tab">
            Reguli Alertă
          </TabsTrigger>
        </TabsList>

        {/* Events Tab */}
        <TabsContent value="events" className="space-y-6">
          {/* Filters */}
          <div className="flex gap-3">
            <Select value={eventFilter || "all"} onValueChange={(v) => setEventFilter(v === "all" ? "" : v)}>
              <SelectTrigger className="w-40" data-testid="event-status-filter">
                <SelectValue placeholder="Toate Evenimentele" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">Toate Evenimentele</SelectItem>
                <SelectItem value="new">Noi</SelectItem>
                <SelectItem value="ack">Confirmate</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {/* Events Table */}
          <Card>
            <CardContent className="p-0">
              <Table>
                <TableHeader>
                  <TableRow className="bg-slate-50">
                    <TableHead className="font-semibold">Severitate</TableHead>
                    <TableHead className="font-semibold">Produs</TableHead>
                    <TableHead className="font-semibold">Modificare</TableHead>
                    <TableHead className="font-semibold">Declanșat</TableHead>
                    <TableHead className="font-semibold">Status</TableHead>
                    <TableHead className="font-semibold w-24">Acțiuni</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {loadingEvents ? (
                    [...Array(5)].map((_, i) => (
                      <TableRow key={i}>
                        <TableCell><div className="h-4 bg-slate-200 rounded w-16 animate-pulse"></div></TableCell>
                        <TableCell><div className="h-4 bg-slate-200 rounded w-32 animate-pulse"></div></TableCell>
                        <TableCell><div className="h-4 bg-slate-200 rounded w-20 animate-pulse"></div></TableCell>
                        <TableCell><div className="h-4 bg-slate-200 rounded w-24 animate-pulse"></div></TableCell>
                        <TableCell><div className="h-4 bg-slate-200 rounded w-16 animate-pulse"></div></TableCell>
                        <TableCell><div className="h-4 bg-slate-200 rounded w-16 animate-pulse"></div></TableCell>
                      </TableRow>
                    ))
                  ) : events.length > 0 ? (
                    events.map((event) => (
                      <TableRow key={event.id} data-testid={`event-row-${event.id}`}>
                        <TableCell>
                          <div className="flex items-center gap-2">
                            <AlertTriangle className={`h-4 w-4 ${
                              event.severity === 'high' ? 'text-red-500' :
                              event.severity === 'medium' ? 'text-amber-500' : 'text-slate-400'
                            }`} />
                            {getSeverityBadge(event.severity)}
                          </div>
                        </TableCell>
                        <TableCell>
                          <span className="font-medium">{event.product_name || 'Necunoscut'}</span>
                        </TableCell>
                        <TableCell>
                          <span className={`font-mono ${
                            event.payload?.change_percent > 0 ? 'text-red-600' : 'text-emerald-600'
                          }`}>
                            {event.payload?.change_percent > 0 ? '+' : ''}
                            {event.payload?.change_percent?.toFixed(1)}%
                          </span>
                        </TableCell>
                        <TableCell>
                          <span className="text-sm text-muted-foreground">
                            {new Date(event.triggered_at).toLocaleString('ro-RO')}
                          </span>
                        </TableCell>
                        <TableCell>
                          <Badge variant={event.status === 'new' ? 'default' : 'secondary'}>
                            {event.status === 'new' ? 'Nouă' : 'Confirmată'}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          {event.status === 'new' && (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleAcknowledgeEvent(event.id)}
                              data-testid={`ack-event-${event.id}`}
                            >
                              <Check className="h-4 w-4 mr-1" />
                              OK
                            </Button>
                          )}
                        </TableCell>
                      </TableRow>
                    ))
                  ) : (
                    <TableRow>
                      <TableCell colSpan={6} className="text-center py-12">
                        <Bell className="h-8 w-8 text-muted-foreground/40 mx-auto mb-2" />
                        <p className="text-muted-foreground">Niciun eveniment de alertă</p>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </CardContent>
          </Card>

          {/* Pagination */}
          {eventPagination.totalPages > 1 && (
            <div className="flex items-center justify-between">
              <p className="text-sm text-muted-foreground">
                Afișare {(eventPagination.page - 1) * 10 + 1} - {Math.min(eventPagination.page * 10, eventPagination.total)} din {eventPagination.total}
              </p>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => fetchEvents(eventPagination.page - 1, eventFilter)}
                  disabled={eventPagination.page <= 1}
                >
                  <ChevronLeft className="h-4 w-4" />
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => fetchEvents(eventPagination.page + 1, eventFilter)}
                  disabled={eventPagination.page >= eventPagination.totalPages}
                >
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            </div>
          )}
        </TabsContent>

        {/* Rules Tab */}
        <TabsContent value="rules" className="space-y-6">
          {/* Add Rule Button */}
          <div className="flex justify-end">
            <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
              <DialogTrigger asChild>
                <Button data-testid="add-rule-btn">
                  <Plus className="h-4 w-4 mr-2" />
                  Adaugă Regulă
                </Button>
              </DialogTrigger>
              <DialogContent className="sm:max-w-lg">
                <DialogHeader>
                  <DialogTitle>Creează Regulă Alertă</DialogTitle>
                </DialogHeader>
                <form onSubmit={handleSubmitRule} className="space-y-4">
                  <div className="space-y-2">
                    <Label htmlFor="name">Nume Regulă *</Label>
                    <Input
                      id="name"
                      placeholder="ex: Alertă Preț Oțel"
                      value={formData.name}
                      onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                      required
                      data-testid="rule-name-input"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="type">Tip Alertă</Label>
                    <Select
                      value={formData.type}
                      onValueChange={(value) => setFormData({ ...formData, type: value })}
                    >
                      <SelectTrigger data-testid="rule-type-select">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="threshold_vs_last">Prag vs Ultimul Preț</SelectItem>
                        <SelectItem value="threshold_vs_avg">Prag vs Medie</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-2">
                      <Label htmlFor="threshold">Prag (%)</Label>
                      <Input
                        id="threshold"
                        type="number"
                        min="1"
                        max="100"
                        value={formData.params.threshold_percent}
                        onChange={(e) => setFormData({
                          ...formData,
                          params: { ...formData.params, threshold_percent: parseInt(e.target.value) || 10 }
                        })}
                        data-testid="threshold-input"
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="compare_n">Compară ultimele N</Label>
                      <Input
                        id="compare_n"
                        type="number"
                        min="1"
                        max="10"
                        value={formData.params.compare_last_n}
                        onChange={(e) => setFormData({
                          ...formData,
                          params: { ...formData.params, compare_last_n: parseInt(e.target.value) || 3 }
                        })}
                        data-testid="compare-n-input"
                      />
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <Switch
                      checked={formData.is_active}
                      onCheckedChange={(checked) => setFormData({ ...formData, is_active: checked })}
                    />
                    <Label>Activă</Label>
                  </div>
                  <div className="flex justify-end gap-3 pt-4">
                    <Button type="button" variant="outline" onClick={() => setDialogOpen(false)}>
                      Anulează
                    </Button>
                    <Button type="submit" data-testid="submit-rule-btn">
                      Creează Regulă
                    </Button>
                  </div>
                </form>
              </DialogContent>
            </Dialog>
          </div>

          {/* Rules Table */}
          <Card>
            <CardContent className="p-0">
              <Table>
                <TableHeader>
                  <TableRow className="bg-slate-50">
                    <TableHead className="font-semibold">Nume Regulă</TableHead>
                    <TableHead className="font-semibold">Tip</TableHead>
                    <TableHead className="font-semibold">Prag</TableHead>
                    <TableHead className="font-semibold">Status</TableHead>
                    <TableHead className="font-semibold w-32">Acțiuni</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {loadingRules ? (
                    [...Array(3)].map((_, i) => (
                      <TableRow key={i}>
                        <TableCell><div className="h-4 bg-slate-200 rounded w-32 animate-pulse"></div></TableCell>
                        <TableCell><div className="h-4 bg-slate-200 rounded w-28 animate-pulse"></div></TableCell>
                        <TableCell><div className="h-4 bg-slate-200 rounded w-16 animate-pulse"></div></TableCell>
                        <TableCell><div className="h-4 bg-slate-200 rounded w-16 animate-pulse"></div></TableCell>
                        <TableCell><div className="h-4 bg-slate-200 rounded w-20 animate-pulse"></div></TableCell>
                      </TableRow>
                    ))
                  ) : rules.length > 0 ? (
                    rules.map((rule) => (
                      <TableRow key={rule.id} data-testid={`rule-row-${rule.id}`}>
                        <TableCell className="font-medium">{rule.name}</TableCell>
                        <TableCell>
                          <span className="text-sm">
                            {rule.type === 'threshold_vs_last' ? 'vs Ultimul Preț' : 'vs Medie'}
                          </span>
                        </TableCell>
                        <TableCell>
                          <span className="font-mono">{rule.params?.threshold_percent}%</span>
                        </TableCell>
                        <TableCell>
                          <Switch
                            checked={rule.is_active}
                            onCheckedChange={(checked) => handleToggleRule(rule.id, checked)}
                          />
                        </TableCell>
                        <TableCell>
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => handleDeleteRule(rule.id)}
                            className="text-destructive hover:text-destructive"
                            data-testid={`delete-rule-${rule.id}`}
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))
                  ) : (
                    <TableRow>
                      <TableCell colSpan={5} className="text-center py-12">
                        <Bell className="h-8 w-8 text-muted-foreground/40 mx-auto mb-2" />
                        <p className="text-muted-foreground">Nicio regulă de alertă configurată</p>
                        <Button className="mt-4" onClick={() => setDialogOpen(true)}>
                          Creează prima regulă
                        </Button>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}
