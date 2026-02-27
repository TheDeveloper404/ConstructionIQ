import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { API } from '../App';
import { Card, CardContent } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Badge } from '../components/ui/badge';
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
import { Label } from '../components/ui/label';
import { Textarea } from '../components/ui/textarea';
import { Plus, Search, Building2, Mail, Phone, ChevronLeft, ChevronRight } from 'lucide-react';
import { toast } from 'sonner';

export default function Suppliers() {
  const [suppliers, setSuppliers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [pagination, setPagination] = useState({ page: 1, total: 0, totalPages: 0 });
  const [dialogOpen, setDialogOpen] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    contact_email: '',
    phone: '',
    address: '',
    tags: '',
    notes: '',
  });

  const fetchSuppliers = async (page = 1, searchQuery = '') => {
    setLoading(true);
    try {
      const params = new URLSearchParams({ page, page_size: 10 });
      if (searchQuery) params.append('search', searchQuery);
      
      const response = await API.get(`/suppliers?${params}`);
      setSuppliers(response.data.items);
      setPagination({
        page: response.data.page,
        total: response.data.total,
        totalPages: response.data.total_pages,
      });
    } catch (error) {
      toast.error('Eroare la încărcarea furnizorilor');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSuppliers(1, search);
  }, []);

  const handleSearch = (e) => {
    e.preventDefault();
    fetchSuppliers(1, search);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const data = {
        ...formData,
        tags: formData.tags.split(',').map(t => t.trim()).filter(Boolean),
      };
      await API.post('/suppliers', data);
      toast.success('Furnizor adăugat cu succes');
      setDialogOpen(false);
      setFormData({ name: '', contact_email: '', phone: '', address: '', tags: '', notes: '' });
      fetchSuppliers(pagination.page, search);
    } catch (error) {
      toast.error('Eroare la adăugarea furnizorului');
    }
  };

  return (
    <div className="space-y-6" data-testid="suppliers-page">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-heading font-bold tracking-tight">Furnizori</h1>
          <p className="text-muted-foreground mt-1">Gestionează directorul de furnizori</p>
        </div>
        <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
          <DialogTrigger asChild>
            <Button data-testid="add-supplier-btn">
              <Plus className="h-4 w-4 mr-2" />
              Adaugă Furnizor
            </Button>
          </DialogTrigger>
          <DialogContent className="sm:max-w-lg">
            <DialogHeader>
              <DialogTitle>Adaugă Furnizor Nou</DialogTitle>
            </DialogHeader>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="name">Nume Companie *</Label>
                <Input
                  id="name"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  required
                  data-testid="supplier-name-input"
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="contact_email">Email</Label>
                  <Input
                    id="contact_email"
                    type="email"
                    value={formData.contact_email}
                    onChange={(e) => setFormData({ ...formData, contact_email: e.target.value })}
                    data-testid="supplier-email-input"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="phone">Telefon</Label>
                  <Input
                    id="phone"
                    value={formData.phone}
                    onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                    data-testid="supplier-phone-input"
                  />
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="address">Adresă</Label>
                <Input
                  id="address"
                  value={formData.address}
                  onChange={(e) => setFormData({ ...formData, address: e.target.value })}
                  data-testid="supplier-address-input"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="tags">Etichete (separate prin virgulă)</Label>
                <Input
                  id="tags"
                  placeholder="oțel, structural, armătură"
                  value={formData.tags}
                  onChange={(e) => setFormData({ ...formData, tags: e.target.value })}
                  data-testid="supplier-tags-input"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="notes">Note</Label>
                <Textarea
                  id="notes"
                  value={formData.notes}
                  onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                  data-testid="supplier-notes-input"
                />
              </div>
              <div className="flex justify-end gap-3 pt-4">
                <Button type="button" variant="outline" onClick={() => setDialogOpen(false)}>
                  Anulează
                </Button>
                <Button type="submit" data-testid="submit-supplier-btn">
                  Adaugă Furnizor
                </Button>
              </div>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      {/* Search */}
      <form onSubmit={handleSearch} className="flex gap-3">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Caută furnizori..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-9"
            data-testid="supplier-search-input"
          />
        </div>
        <Button type="submit" variant="secondary" data-testid="search-btn">
          Caută
        </Button>
      </form>

      {/* Table */}
      <Card>
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow className="bg-slate-50">
                <TableHead className="font-semibold">Companie</TableHead>
                <TableHead className="font-semibold">Contact</TableHead>
                <TableHead className="font-semibold">Etichete</TableHead>
                <TableHead className="font-semibold w-24">Acțiuni</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading ? (
                [...Array(5)].map((_, i) => (
                  <TableRow key={i}>
                    <TableCell><div className="h-4 bg-slate-200 rounded w-32 animate-pulse"></div></TableCell>
                    <TableCell><div className="h-4 bg-slate-200 rounded w-40 animate-pulse"></div></TableCell>
                    <TableCell><div className="h-4 bg-slate-200 rounded w-24 animate-pulse"></div></TableCell>
                    <TableCell><div className="h-4 bg-slate-200 rounded w-16 animate-pulse"></div></TableCell>
                  </TableRow>
                ))
              ) : suppliers.length > 0 ? (
                suppliers.map((supplier) => (
                  <TableRow key={supplier.id} data-testid={`supplier-row-${supplier.id}`}>
                    <TableCell>
                      <div className="flex items-center gap-3">
                        <div className="h-9 w-9 rounded-md bg-slate-100 flex items-center justify-center">
                          <Building2 className="h-4 w-4 text-slate-600" />
                        </div>
                        <div>
                          <p className="font-medium">{supplier.name}</p>
                          {supplier.address && (
                            <p className="text-xs text-muted-foreground">{supplier.address}</p>
                          )}
                        </div>
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="space-y-1">
                        {supplier.contact_email && (
                          <div className="flex items-center gap-1.5 text-sm">
                            <Mail className="h-3.5 w-3.5 text-muted-foreground" />
                            <span>{supplier.contact_email}</span>
                          </div>
                        )}
                        {supplier.phone && (
                          <div className="flex items-center gap-1.5 text-sm">
                            <Phone className="h-3.5 w-3.5 text-muted-foreground" />
                            <span>{supplier.phone}</span>
                          </div>
                        )}
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="flex flex-wrap gap-1">
                        {supplier.tags?.slice(0, 3).map((tag) => (
                          <Badge key={tag} variant="secondary" className="text-xs">
                            {tag}
                          </Badge>
                        ))}
                        {supplier.tags?.length > 3 && (
                          <Badge variant="secondary" className="text-xs">
                            +{supplier.tags.length - 3}
                          </Badge>
                        )}
                      </div>
                    </TableCell>
                    <TableCell>
                      <Button variant="ghost" size="sm" asChild>
                        <Link to={`/suppliers/${supplier.id}`} data-testid={`view-supplier-${supplier.id}`}>
                          Vezi
                        </Link>
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              ) : (
                <TableRow>
                  <TableCell colSpan={4} className="text-center py-12">
                    <Building2 className="h-8 w-8 text-muted-foreground/40 mx-auto mb-2" />
                    <p className="text-muted-foreground">Niciun furnizor găsit</p>
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
              onClick={() => fetchSuppliers(pagination.page - 1, search)}
              disabled={pagination.page <= 1}
              data-testid="prev-page"
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => fetchSuppliers(pagination.page + 1, search)}
              disabled={pagination.page >= pagination.totalPages}
              data-testid="next-page"
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
