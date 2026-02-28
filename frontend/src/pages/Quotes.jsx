import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { API, fetchAllPages } from '../lib/api';
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
import { Plus, Receipt, ChevronLeft, ChevronRight, Building2 } from 'lucide-react';
import { toast } from 'sonner';

export default function Quotes() {
  const [quotes, setQuotes] = useState([]);
  const [suppliers, setSuppliers] = useState({});
  const [loading, setLoading] = useState(true);
  const [pagination, setPagination] = useState({ page: 1, total: 0, totalPages: 0 });
  const [statusFilter, setStatusFilter] = useState('');

  const fetchQuotes = useCallback(async (page = 1, status = '') => {
    setLoading(true);
    try {
      const params = new URLSearchParams({ page, page_size: 10 });
      if (status) params.append('status', status);
      
      const [quotesRes, suppliersList] = await Promise.all([
        API.get(`/quotes?${params}`),
        fetchAllPages('/suppliers'),
      ]);

      setQuotes(quotesRes.data.items);
      setPagination({
        page: quotesRes.data.page,
        total: quotesRes.data.total,
        totalPages: quotesRes.data.total_pages,
      });

      const supplierMap = {};
      suppliersList.forEach((s) => { supplierMap[s.id] = s.name; });
      setSuppliers(supplierMap);
    } catch (error) {
      toast.error('Eroare la încărcarea ofertelor');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchQuotes(1, statusFilter);
  }, [fetchQuotes, statusFilter]);

  return (
    <div className="space-y-6" data-testid="quotes-page">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-heading font-bold tracking-tight">Oferte Primite</h1>
          <p className="text-muted-foreground mt-1">Gestionează ofertele primite de la furnizori</p>
        </div>
        <Button asChild data-testid="add-quote-btn">
          <Link to="/quotes/new">
            <Plus className="h-4 w-4 mr-2" />
            Adaugă Ofertă
          </Link>
        </Button>
      </div>

      {/* Filters */}
      <div className="flex gap-3">
        <Select value={statusFilter || "all"} onValueChange={(v) => setStatusFilter(v === "all" ? "" : v)}>
          <SelectTrigger className="w-40" data-testid="quote-status-filter">
            <SelectValue placeholder="Toate Statusurile" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Toate Statusurile</SelectItem>
            <SelectItem value="received">Primită</SelectItem>
            <SelectItem value="validated">Validată</SelectItem>
            <SelectItem value="archived">Arhivată</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* Table */}
      <Card>
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow className="bg-slate-50">
                <TableHead className="font-semibold">ID Ofertă</TableHead>
                <TableHead className="font-semibold">Furnizor</TableHead>
                <TableHead className="font-semibold">Articole</TableHead>
                <TableHead className="font-semibold text-right">Total</TableHead>
                <TableHead className="font-semibold">Status</TableHead>
                <TableHead className="font-semibold">Data Primirii</TableHead>
                <TableHead className="font-semibold w-24">Acțiuni</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading ? (
                [...Array(5)].map((_, i) => (
                  <TableRow key={i}>
                    <TableCell><div className="h-4 bg-slate-200 rounded w-24 animate-pulse"></div></TableCell>
                    <TableCell><div className="h-4 bg-slate-200 rounded w-32 animate-pulse"></div></TableCell>
                    <TableCell><div className="h-4 bg-slate-200 rounded w-12 animate-pulse"></div></TableCell>
                    <TableCell><div className="h-4 bg-slate-200 rounded w-20 animate-pulse"></div></TableCell>
                    <TableCell><div className="h-4 bg-slate-200 rounded w-16 animate-pulse"></div></TableCell>
                    <TableCell><div className="h-4 bg-slate-200 rounded w-20 animate-pulse"></div></TableCell>
                    <TableCell><div className="h-4 bg-slate-200 rounded w-16 animate-pulse"></div></TableCell>
                  </TableRow>
                ))
              ) : quotes.length > 0 ? (
                quotes.map((quote) => (
                  <TableRow key={quote.id} data-testid={`quote-row-${quote.id}`}>
                    <TableCell>
                      <span className="font-mono text-sm">{quote.id.slice(0, 8)}...</span>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <Building2 className="h-4 w-4 text-muted-foreground" />
                        <span>{suppliers[quote.supplier_id] || 'Necunoscut'}</span>
                      </div>
                    </TableCell>
                    <TableCell>
                      <span className="font-mono text-sm">{quote.items?.length || 0}</span>
                    </TableCell>
                    <TableCell className="text-right">
                      <span className="font-mono font-medium">
                        {quote.total_amount?.toLocaleString('ro-RO')} {quote.currency || 'RON'}
                      </span>
                    </TableCell>
                    <TableCell><StatusBadge status={quote.status} /></TableCell>
                    <TableCell>
                      <span className="text-sm text-muted-foreground">
                        {new Date(quote.received_at).toLocaleDateString('ro-RO')}
                      </span>
                    </TableCell>
                    <TableCell>
                      <Button variant="ghost" size="sm" asChild>
                        <Link to={`/quotes/${quote.id}`} data-testid={`view-quote-${quote.id}`}>
                          Vezi
                        </Link>
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              ) : (
                <TableRow>
                  <TableCell colSpan={7} className="text-center py-12">
                    <Receipt className="h-8 w-8 text-muted-foreground/40 mx-auto mb-2" />
                    <p className="text-muted-foreground">Nicio ofertă găsită</p>
                    <Button className="mt-4" asChild>
                      <Link to="/quotes/new">Adaugă prima ofertă</Link>
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
              onClick={() => fetchQuotes(pagination.page - 1, statusFilter)}
              disabled={pagination.page <= 1}
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => fetchQuotes(pagination.page + 1, statusFilter)}
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
