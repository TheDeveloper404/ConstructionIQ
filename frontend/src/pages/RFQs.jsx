import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { API } from '../lib/api';
import { Card, CardContent } from '../components/ui/card';
import { Button } from '../components/ui/button';
import StatusBadge from '../components/StatusBadge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../components/ui/table';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import { Plus, FileText, ChevronLeft, ChevronRight, Calendar } from 'lucide-react';
import { toast } from 'sonner';

export default function RFQs() {
  const [rfqs, setRfqs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [pagination, setPagination] = useState({ page: 1, total: 0, totalPages: 0 });
  const [statusFilter, setStatusFilter] = useState('');

  const fetchRFQs = useCallback(async (page = 1, status = '') => {
    setLoading(true);
    try {
      const params = new URLSearchParams({ page, page_size: 10 });
      if (status) params.append('status', status);
      
      const response = await API.get(`/rfqs?${params}`);
      setRfqs(response.data.items);
      setPagination({
        page: response.data.page,
        total: response.data.total,
        totalPages: response.data.total_pages,
      });
    } catch (error) {
      toast.error('Eroare la încărcarea cererilor de ofertă');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchRFQs(1, statusFilter);
  }, [fetchRFQs, statusFilter]);

  return (
    <div className="space-y-6" data-testid="rfqs-page">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-heading font-bold tracking-tight">Cereri de Ofertă</h1>
          <p className="text-muted-foreground mt-1">Gestionează cererile trimise furnizorilor</p>
        </div>
        <Button asChild data-testid="create-rfq-btn">
          <Link to="/rfqs/new">
            <Plus className="h-4 w-4 mr-2" />
            Cerere Nouă
          </Link>
        </Button>
      </div>

      {/* Filters */}
      <div className="flex gap-3">
        <Select value={statusFilter || "all"} onValueChange={(v) => setStatusFilter(v === "all" ? "" : v)}>
          <SelectTrigger className="w-40" data-testid="rfq-status-filter">
            <SelectValue placeholder="Toate Statusurile" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Toate Statusurile</SelectItem>
            <SelectItem value="draft">Ciornă</SelectItem>
            <SelectItem value="sent">Trimisă</SelectItem>
            <SelectItem value="closed">Închisă</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* Table */}
      <Card>
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow className="bg-slate-50">
                <TableHead className="font-semibold">Titlu Cerere</TableHead>
                <TableHead className="font-semibold">Articole</TableHead>
                <TableHead className="font-semibold">Furnizori</TableHead>
                <TableHead className="font-semibold">Termen Limită</TableHead>
                <TableHead className="font-semibold">Status</TableHead>
                <TableHead className="font-semibold w-24">Acțiuni</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading ? (
                [...Array(5)].map((_, i) => (
                  <TableRow key={i}>
                    <TableCell><div className="h-4 bg-slate-200 rounded w-40 animate-pulse"></div></TableCell>
                    <TableCell><div className="h-4 bg-slate-200 rounded w-12 animate-pulse"></div></TableCell>
                    <TableCell><div className="h-4 bg-slate-200 rounded w-12 animate-pulse"></div></TableCell>
                    <TableCell><div className="h-4 bg-slate-200 rounded w-24 animate-pulse"></div></TableCell>
                    <TableCell><div className="h-4 bg-slate-200 rounded w-16 animate-pulse"></div></TableCell>
                    <TableCell><div className="h-4 bg-slate-200 rounded w-16 animate-pulse"></div></TableCell>
                  </TableRow>
                ))
              ) : rfqs.length > 0 ? (
                rfqs.map((rfq) => (
                  <TableRow key={rfq.id} data-testid={`rfq-row-${rfq.id}`}>
                    <TableCell>
                      <div className="flex items-center gap-3">
                        <div className="h-9 w-9 rounded-md bg-violet-100 flex items-center justify-center">
                          <FileText className="h-4 w-4 text-violet-600" />
                        </div>
                        <div>
                          <p className="font-medium">{rfq.title}</p>
                          <p className="text-xs text-muted-foreground font-mono">{rfq.id.slice(0, 8)}...</p>
                        </div>
                      </div>
                    </TableCell>
                    <TableCell>
                      <span className="font-mono text-sm">{rfq.items?.length || 0}</span>
                    </TableCell>
                    <TableCell>
                      <span className="font-mono text-sm">{rfq.supplier_ids?.length || 0}</span>
                    </TableCell>
                    <TableCell>
                      {rfq.due_date ? (
                        <div className="flex items-center gap-1.5 text-sm">
                          <Calendar className="h-3.5 w-3.5 text-muted-foreground" />
                          <span>{new Date(rfq.due_date).toLocaleDateString('ro-RO')}</span>
                        </div>
                      ) : (
                        <span className="text-muted-foreground text-sm">—</span>
                      )}
                    </TableCell>
                    <TableCell><StatusBadge status={rfq.status} /></TableCell>
                    <TableCell>
                      <Button variant="ghost" size="sm" asChild>
                        <Link to={`/rfqs/${rfq.id}`} data-testid={`view-rfq-${rfq.id}`}>
                          Vezi
                        </Link>
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              ) : (
                <TableRow>
                  <TableCell colSpan={6} className="text-center py-12">
                    <FileText className="h-8 w-8 text-muted-foreground/40 mx-auto mb-2" />
                    <p className="text-muted-foreground">Nicio cerere de ofertă găsită</p>
                    <Button className="mt-4" asChild>
                      <Link to="/rfqs/new">Creează prima cerere</Link>
                    </Button>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {/* Pagination */}
      {pagination.totalPages > 1 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            Afișare {(pagination.page - 1) * 10 + 1} - {Math.min(pagination.page * 10, pagination.total)} din {pagination.total}
          </p>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => fetchRFQs(pagination.page - 1, statusFilter)}
              disabled={pagination.page <= 1}
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => fetchRFQs(pagination.page + 1, statusFilter)}
              disabled={pagination.page >= pagination.totalPages}
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
