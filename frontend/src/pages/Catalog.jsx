import React, { useState, useEffect } from 'react';
import { API } from '../App';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import { Plus, Package, Search, ChevronLeft, ChevronRight, Trash2 } from 'lucide-react';
import { toast } from 'sonner';

export default function Catalog() {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [pagination, setPagination] = useState({ page: 1, total: 0, totalPages: 0 });
  const [dialogOpen, setDialogOpen] = useState(false);
  const [formData, setFormData] = useState({
    canonical_name: '',
    category: '',
    base_uom: 'unit',
  });

  const fetchProducts = async (page = 1, searchQuery = '', category = '') => {
    setLoading(true);
    try {
      const params = new URLSearchParams({ page, page_size: 10 });
      if (searchQuery) params.append('search', searchQuery);
      if (category) params.append('category', category);
      
      const response = await API.get(`/catalog/products?${params}`);
      setProducts(response.data.items);
      setPagination({
        page: response.data.page,
        total: response.data.total,
        totalPages: response.data.total_pages,
      });
    } catch (error) {
      toast.error('Eroare la încărcarea produselor');
    } finally {
      setLoading(false);
    }
  };

  const fetchCategories = async () => {
    try {
      const response = await API.get('/catalog/categories');
      setCategories(response.data.categories || []);
    } catch (error) {
      console.error('Failed to load categories');
    }
  };

  useEffect(() => {
    fetchProducts(1, search, categoryFilter);
    fetchCategories();
  }, []);

  useEffect(() => {
    fetchProducts(1, search, categoryFilter);
  }, [categoryFilter]);

  const handleSearch = (e) => {
    e.preventDefault();
    fetchProducts(1, search, categoryFilter);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await API.post('/catalog/products', formData);
      toast.success('Produs adăugat cu succes');
      setDialogOpen(false);
      setFormData({ canonical_name: '', category: '', base_uom: 'unit' });
      fetchProducts(pagination.page, search, categoryFilter);
      fetchCategories();
    } catch (error) {
      toast.error('Eroare la adăugarea produsului');
    }
  };

  const handleDelete = async (productId) => {
    if (!window.confirm('Ștergeți acest produs? Această acțiune nu poate fi anulată.')) return;
    try {
      await API.delete(`/catalog/products/${productId}`);
      toast.success('Produs șters');
      fetchProducts(pagination.page, search, categoryFilter);
    } catch (error) {
      toast.error('Eroare la ștergerea produsului');
    }
  };

  return (
    <div className="space-y-6" data-testid="catalog-page">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-heading font-bold tracking-tight">Catalog Produse</h1>
          <p className="text-muted-foreground mt-1">Produse normalizate pentru urmărirea prețurilor</p>
        </div>
        <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
          <DialogTrigger asChild>
            <Button data-testid="add-product-btn">
              <Plus className="h-4 w-4 mr-2" />
              Adaugă Produs
            </Button>
          </DialogTrigger>
          <DialogContent className="sm:max-w-lg">
            <DialogHeader>
              <DialogTitle>Adaugă Produs Normalizat</DialogTitle>
            </DialogHeader>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="canonical_name">Nume Produs *</Label>
                <Input
                  id="canonical_name"
                  placeholder="ex: Oțel beton Ø12mm"
                  value={formData.canonical_name}
                  onChange={(e) => setFormData({ ...formData, canonical_name: e.target.value })}
                  required
                  data-testid="product-name-input"
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="category">Categorie *</Label>
                  <Input
                    id="category"
                    placeholder="ex: Oțel"
                    value={formData.category}
                    onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                    required
                    list="categories"
                    data-testid="product-category-input"
                  />
                  <datalist id="categories">
                    {categories.map((cat) => (
                      <option key={cat} value={cat} />
                    ))}
                  </datalist>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="base_uom">Unitate de Bază *</Label>
                  <Select
                    value={formData.base_uom}
                    onValueChange={(value) => setFormData({ ...formData, base_uom: value })}
                  >
                    <SelectTrigger data-testid="product-uom-select">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="unit">Bucată</SelectItem>
                      <SelectItem value="ton">Tonă</SelectItem>
                      <SelectItem value="kg">Kg</SelectItem>
                      <SelectItem value="m">Metru</SelectItem>
                      <SelectItem value="mp">Metru pătrat</SelectItem>
                      <SelectItem value="mc">Metru cub</SelectItem>
                      <SelectItem value="sac">Sac</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
              <div className="flex justify-end gap-3 pt-4">
                <Button type="button" variant="outline" onClick={() => setDialogOpen(false)}>
                  Anulează
                </Button>
                <Button type="submit" data-testid="submit-product-btn">
                  Adaugă Produs
                </Button>
              </div>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap gap-3">
        <form onSubmit={handleSearch} className="flex gap-3">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Caută produse..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="pl-9 w-60"
              data-testid="product-search-input"
            />
          </div>
          <Button type="submit" variant="secondary">
            Caută
          </Button>
        </form>
        <Select value={categoryFilter || "all"} onValueChange={(v) => setCategoryFilter(v === "all" ? "" : v)}>
          <SelectTrigger className="w-40" data-testid="category-filter">
            <SelectValue placeholder="Toate Categoriile" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Toate Categoriile</SelectItem>
            {categories.map((cat) => (
              <SelectItem key={cat} value={cat}>
                {cat}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {/* Table */}
      <Card>
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow className="bg-slate-50">
                <TableHead className="font-semibold">Nume Produs</TableHead>
                <TableHead className="font-semibold">Categorie</TableHead>
                <TableHead className="font-semibold">Unitate</TableHead>
                <TableHead className="font-semibold w-24">Acțiuni</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading ? (
                [...Array(5)].map((_, i) => (
                  <TableRow key={i}>
                    <TableCell><div className="h-4 bg-slate-200 rounded w-48 animate-pulse"></div></TableCell>
                    <TableCell><div className="h-4 bg-slate-200 rounded w-24 animate-pulse"></div></TableCell>
                    <TableCell><div className="h-4 bg-slate-200 rounded w-16 animate-pulse"></div></TableCell>
                    <TableCell><div className="h-4 bg-slate-200 rounded w-16 animate-pulse"></div></TableCell>
                  </TableRow>
                ))
              ) : products.length > 0 ? (
                products.map((product) => (
                  <TableRow key={product.id} data-testid={`product-row-${product.id}`}>
                    <TableCell>
                      <div className="flex items-center gap-3">
                        <div className="h-9 w-9 rounded-md bg-emerald-100 flex items-center justify-center">
                          <Package className="h-4 w-4 text-emerald-600" />
                        </div>
                        <div>
                          <p className="font-medium">{product.canonical_name}</p>
                          <p className="text-xs text-muted-foreground font-mono">{product.id.slice(0, 8)}...</p>
                        </div>
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge variant="secondary">{product.category}</Badge>
                    </TableCell>
                    <TableCell>
                      <span className="font-mono text-sm">{product.base_uom}</span>
                    </TableCell>
                    <TableCell>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => handleDelete(product.id)}
                        className="text-destructive hover:text-destructive"
                        data-testid={`delete-product-${product.id}`}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              ) : (
                <TableRow>
                  <TableCell colSpan={4} className="text-center py-12">
                    <Package className="h-8 w-8 text-muted-foreground/40 mx-auto mb-2" />
                    <p className="text-muted-foreground">Niciun produs găsit</p>
                    <Button className="mt-4" onClick={() => setDialogOpen(true)}>
                      Adaugă primul produs
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
              onClick={() => fetchProducts(pagination.page - 1, search, categoryFilter)}
              disabled={pagination.page <= 1}
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => fetchProducts(pagination.page + 1, search, categoryFilter)}
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
