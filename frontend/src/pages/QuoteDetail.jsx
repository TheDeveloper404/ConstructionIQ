import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { API, fetchAllPages } from '../lib/api';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
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
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import { ArrowLeft, Building2, Calendar, Trash2, Link as LinkIcon } from 'lucide-react';
import { toast } from 'sonner';

export default function QuoteDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [quote, setQuote] = useState(null);
  const [supplier, setSupplier] = useState(null);
  const [products, setProducts] = useState({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const controller = new AbortController();
    const fetchData = async () => {
      try {
        const [quoteRes, productsList] = await Promise.all([
          API.get(`/quotes/${id}`, { signal: controller.signal }),
          fetchAllPages('/catalog/products'),
        ]);
        setQuote(quoteRes.data);

        const productMap = {};
        productsList.forEach((p) => { productMap[p.id] = p; });
        setProducts(productMap);

        if (quoteRes.data.supplier_id) {
          const supplierRes = await API.get(`/suppliers/${quoteRes.data.supplier_id}`, { signal: controller.signal });
          setSupplier(supplierRes.data);
        }
      } catch (error) {
        if (error.name !== 'CanceledError' && error.name !== 'AbortError') {
          toast.error('Eroare la încărcarea ofertei');
          navigate('/quotes');
        }
      } finally {
        setLoading(false);
      }
    };
    fetchData();
    return () => controller.abort();
  }, [id, navigate]);

  const handleStatusChange = async (newStatus) => {
    try {
      await API.put(`/quotes/${id}`, { status: newStatus });
      setQuote({ ...quote, status: newStatus });
      toast.success('Status actualizat');
    } catch (error) {
      toast.error('Eroare la actualizarea statusului');
    }
  };

  const handleMapItem = async (itemId, productId) => {
    try {
      await API.post(`/quotes/${id}/map-item/${itemId}?product_id=${productId}`);
      const response = await API.get(`/quotes/${id}`);
      setQuote(response.data);
      toast.success('Articol mapat la produs');
    } catch (error) {
      toast.error('Eroare la maparea articolului');
    }
  };

  const handleDelete = async () => {
    if (!window.confirm('Ștergeți această ofertă? Această acțiune nu poate fi anulată.')) return;
    try {
      await API.delete(`/quotes/${id}`);
      toast.success('Ofertă ștearsă');
      navigate('/quotes');
    } catch (error) {
      toast.error('Eroare la ștergerea ofertei');
    }
  };

  const getStatusBadge = (status) => {
    const labels = {
      received: 'Primită',
      validated: 'Validată',
      archived: 'Arhivată',
    };
    const styles = {
      received: 'bg-amber-100 text-amber-700',
      validated: 'bg-emerald-100 text-emerald-700',
      archived: 'bg-slate-100 text-slate-600',
    };
    return (
      <Badge variant="secondary" className={styles[status] || styles.received}>
        {labels[status] || status}
      </Badge>
    );
  };

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="h-8 bg-slate-200 rounded w-48 animate-pulse"></div>
        <Card className="animate-pulse">
          <CardContent className="p-6 space-y-4">
            <div className="h-4 bg-slate-200 rounded w-1/3"></div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6" data-testid="quote-detail">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" asChild>
          <Link to="/quotes">
            <ArrowLeft className="h-5 w-5" />
          </Link>
        </Button>
        <div className="flex-1">
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-heading font-bold">Detalii Ofertă</h1>
            {getStatusBadge(quote?.status)}
          </div>
          <p className="text-muted-foreground font-mono text-sm">{quote?.id}</p>
        </div>
        <div className="flex gap-2">
          <Select value={quote?.status} onValueChange={handleStatusChange}>
            <SelectTrigger className="w-36" data-testid="quote-status-select">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="received">Primită</SelectItem>
              <SelectItem value="validated">Validată</SelectItem>
              <SelectItem value="archived">Arhivată</SelectItem>
            </SelectContent>
          </Select>
          <Button variant="destructive" size="icon" onClick={handleDelete} data-testid="delete-quote-btn">
            <Trash2 className="h-4 w-4" />
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Content */}
        <div className="lg:col-span-2 space-y-6">
          {/* Items */}
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Articole</CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              <Table>
                <TableHeader>
                  <TableRow className="bg-slate-50">
                    <TableHead className="font-semibold">#</TableHead>
                    <TableHead className="font-semibold">Descriere</TableHead>
                    <TableHead className="font-semibold text-right">Cantitate</TableHead>
                    <TableHead className="font-semibold">UM</TableHead>
                    <TableHead className="font-semibold text-right">Preț Unitar</TableHead>
                    <TableHead className="font-semibold text-right">Total</TableHead>
                    <TableHead className="font-semibold">Produs Mapat</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {quote?.items?.map((item, index) => (
                    <TableRow key={item.id}>
                      <TableCell className="font-mono text-muted-foreground">{index + 1}</TableCell>
                      <TableCell>
                        <div>
                          <p>{item.raw_line_text}</p>
                          {item.lead_time_days && (
                            <p className="text-xs text-muted-foreground">Livrare: {item.lead_time_days} zile</p>
                          )}
                        </div>
                      </TableCell>
                      <TableCell className="text-right font-mono">{item.qty}</TableCell>
                      <TableCell>{item.uom}</TableCell>
                      <TableCell className="text-right font-mono">
                        {item.unit_price?.toLocaleString('ro-RO')}
                      </TableCell>
                      <TableCell className="text-right font-mono font-medium">
                        {item.total_price?.toLocaleString('ro-RO')}
                      </TableCell>
                      <TableCell>
                        {item.normalized_product_id ? (
                          <Link
                            to={`/price-history?product=${item.normalized_product_id}`}
                            className="flex items-center gap-1 text-sm text-blue-600 hover:underline"
                          >
                            <LinkIcon className="h-3 w-3" />
                            {products[item.normalized_product_id]?.canonical_name || 'Legat'}
                          </Link>
                        ) : (
                          <Select
                            value=""
                            onValueChange={(value) => handleMapItem(item.id, value)}
                          >
                            <SelectTrigger className="h-8 text-xs">
                              <SelectValue placeholder="Mapează la produs" />
                            </SelectTrigger>
                            <SelectContent>
                              {Object.values(products).map((product) => (
                                <SelectItem key={product.id} value={product.id}>
                                  {product.canonical_name}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Summary */}
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Sumar</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex justify-between items-center">
                <span className="text-muted-foreground">Articole</span>
                <span className="font-mono">{quote?.items?.length || 0}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-muted-foreground">Monedă</span>
                <span>{quote?.currency}</span>
              </div>
              {quote?.payment_terms && (
                <div className="flex justify-between items-center">
                  <span className="text-muted-foreground">Plată</span>
                  <span>{quote.payment_terms}</span>
                </div>
              )}
              {quote?.delivery_terms && (
                <div className="flex justify-between items-center">
                  <span className="text-muted-foreground">Livrare</span>
                  <span>{quote.delivery_terms}</span>
                </div>
              )}
              <div className="flex justify-between items-center border-t pt-4">
                <span className="font-medium">Total</span>
                <span className="font-mono text-xl font-bold">
                  {quote?.total_amount?.toLocaleString('ro-RO')} {quote?.currency}
                </span>
              </div>
            </CardContent>
          </Card>

          {/* Supplier */}
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Furnizor</CardTitle>
            </CardHeader>
            <CardContent>
              {supplier ? (
                <div className="space-y-3">
                  <div className="flex items-center gap-3">
                    <Building2 className="h-5 w-5 text-muted-foreground" />
                    <div>
                      <p className="font-medium">{supplier.name}</p>
                      {supplier.contact_email && (
                        <p className="text-sm text-muted-foreground">{supplier.contact_email}</p>
                      )}
                    </div>
                  </div>
                  <Button variant="outline" size="sm" className="w-full" asChild>
                    <Link to={`/suppliers/${supplier.id}`}>Vezi Furnizor</Link>
                  </Button>
                </div>
              ) : (
                <p className="text-sm text-muted-foreground">Furnizor necunoscut</p>
              )}
            </CardContent>
          </Card>

          {/* Details */}
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Detalii</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="flex items-center gap-3">
                <Calendar className="h-5 w-5 text-muted-foreground" />
                <div>
                  <p className="text-sm font-medium">Data Primirii</p>
                  <p className="text-sm text-muted-foreground">
                    {new Date(quote?.received_at).toLocaleString('ro-RO')}
                  </p>
                </div>
              </div>
              {quote?.rfq_id && (
                <Button variant="outline" size="sm" className="w-full" asChild>
                  <Link to={`/rfqs/${quote.rfq_id}`}>Vezi Cererea Asociată</Link>
                </Button>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
