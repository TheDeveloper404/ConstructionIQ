import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { API } from '../App';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Checkbox } from '../components/ui/checkbox';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import { ArrowLeft, Plus, Trash2, Save } from 'lucide-react';
import { toast } from 'sonner';

export default function QuoteCreate() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const preselectedRfq = searchParams.get('rfq');
  
  const [suppliers, setSuppliers] = useState([]);
  const [products, setProducts] = useState([]);
  const [rfqs, setRfqs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const [formData, setFormData] = useState({
    supplier_id: '',
    rfq_id: preselectedRfq || '',
    currency: 'RON',
    payment_terms: '',
    delivery_terms: '',
    items: [{ raw_line_text: '', qty: 1, uom: 'unit', unit_price: 0, normalized_product_id: '', vat_included: false, lead_time_days: null, notes: '' }],
  });

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [suppliersRes, productsRes, rfqsRes] = await Promise.all([
          API.get('/suppliers?page_size=100'),
          API.get('/catalog/products?page_size=100'),
          API.get('/rfqs?page_size=100'),
        ]);
        setSuppliers(suppliersRes.data.items);
        setProducts(productsRes.data.items);
        setRfqs(rfqsRes.data.items);
      } catch (error) {
        toast.error('Eroare la încărcarea datelor');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const addItem = () => {
    setFormData({
      ...formData,
      items: [...formData.items, { raw_line_text: '', qty: 1, uom: 'unit', unit_price: 0, normalized_product_id: '', vat_included: false, lead_time_days: null, notes: '' }],
    });
  };

  const removeItem = (index) => {
    if (formData.items.length === 1) return;
    setFormData({
      ...formData,
      items: formData.items.filter((_, i) => i !== index),
    });
  };

  const updateItem = (index, field, value) => {
    const newItems = [...formData.items];
    newItems[index] = { ...newItems[index], [field]: value };
    setFormData({ ...formData, items: newItems });
  };

  const calculateTotal = () => {
    return formData.items.reduce((sum, item) => sum + (item.qty * item.unit_price), 0);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!formData.supplier_id) {
      toast.error('Selectați un furnizor');
      return;
    }
    if (formData.items.some((item) => !item.raw_line_text.trim())) {
      toast.error('Completați descrierea tuturor articolelor');
      return;
    }

    setSubmitting(true);
    try {
      const payload = {
        ...formData,
        rfq_id: formData.rfq_id || null,
        items: formData.items.map((item) => ({
          ...item,
          normalized_product_id: item.normalized_product_id || null,
          lead_time_days: item.lead_time_days ? parseInt(item.lead_time_days) : null,
        })),
      };
      
      const response = await API.post('/quotes', payload);
      toast.success('Ofertă adăugată cu succes');
      navigate(`/quotes/${response.data.id}`);
    } catch (error) {
      toast.error('Eroare la adăugarea ofertei');
    } finally {
      setSubmitting(false);
    }
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
    <div className="space-y-6" data-testid="quote-create-page">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={() => navigate('/quotes')}>
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <div>
          <h1 className="text-2xl font-heading font-bold">Adaugă Ofertă</h1>
          <p className="text-muted-foreground">Înregistrează o ofertă primită de la furnizor</p>
        </div>
      </div>

      <form onSubmit={handleSubmit}>
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Main Form */}
          <div className="lg:col-span-2 space-y-6">
            {/* Basic Info */}
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">Informații Ofertă</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="supplier">Furnizor *</Label>
                    <Select
                      value={formData.supplier_id}
                      onValueChange={(value) => setFormData({ ...formData, supplier_id: value })}
                    >
                      <SelectTrigger data-testid="quote-supplier-select">
                        <SelectValue placeholder="Selectează furnizor" />
                      </SelectTrigger>
                      <SelectContent>
                        {suppliers.map((supplier) => (
                          <SelectItem key={supplier.id} value={supplier.id}>
                            {supplier.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="rfq">Cerere Asociată</Label>
                    <Select
                      value={formData.rfq_id || "none"}
                      onValueChange={(value) => setFormData({ ...formData, rfq_id: value === "none" ? "" : value })}
                    >
                      <SelectTrigger data-testid="quote-rfq-select">
                        <SelectValue placeholder="Opțional" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="none">Niciuna</SelectItem>
                        {rfqs.map((rfq) => (
                          <SelectItem key={rfq.id} value={rfq.id}>
                            {rfq.title}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                </div>
                <div className="grid grid-cols-3 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="currency">Monedă</Label>
                    <Select
                      value={formData.currency}
                      onValueChange={(value) => setFormData({ ...formData, currency: value })}
                    >
                      <SelectTrigger data-testid="quote-currency-select">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="RON">RON</SelectItem>
                        <SelectItem value="EUR">EUR</SelectItem>
                        <SelectItem value="USD">USD</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="payment_terms">Termeni Plată</Label>
                    <Input
                      id="payment_terms"
                      placeholder="ex: 30 zile"
                      value={formData.payment_terms}
                      onChange={(e) => setFormData({ ...formData, payment_terms: e.target.value })}
                      data-testid="quote-payment-terms"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="delivery_terms">Termeni Livrare</Label>
                    <Input
                      id="delivery_terms"
                      placeholder="ex: Franco șantier"
                      value={formData.delivery_terms}
                      onChange={(e) => setFormData({ ...formData, delivery_terms: e.target.value })}
                      data-testid="quote-delivery-terms"
                    />
                  </div>
                </div>
              </CardContent>
            </Card>

            {/* Items */}
            <Card>
              <CardHeader className="flex flex-row items-center justify-between">
                <CardTitle className="text-lg">Articole</CardTitle>
                <Button type="button" variant="outline" size="sm" onClick={addItem} data-testid="add-quote-item-btn">
                  <Plus className="h-4 w-4 mr-1" />
                  Adaugă Articol
                </Button>
              </CardHeader>
              <CardContent className="space-y-4">
                {formData.items.map((item, index) => (
                  <div key={index} className="p-4 border rounded-lg space-y-3" data-testid={`quote-item-${index}`}>
                    <div className="flex items-start justify-between gap-4">
                      <div className="flex-1 space-y-2">
                        <Label>Descriere *</Label>
                        <Input
                          placeholder="ex: Oțel beton Ø12mm - bare 12m"
                          value={item.raw_line_text}
                          onChange={(e) => updateItem(index, 'raw_line_text', e.target.value)}
                          data-testid={`quote-item-${index}-desc`}
                        />
                      </div>
                      {formData.items.length > 1 && (
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon"
                          onClick={() => removeItem(index)}
                          className="text-destructive"
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      )}
                    </div>
                    <div className="grid grid-cols-4 gap-3">
                      <div className="space-y-2">
                        <Label>Cantitate</Label>
                        <Input
                          type="number"
                          min="0"
                          step="0.01"
                          value={item.qty}
                          onChange={(e) => updateItem(index, 'qty', parseFloat(e.target.value) || 0)}
                          data-testid={`quote-item-${index}-qty`}
                        />
                      </div>
                      <div className="space-y-2">
                        <Label>Unitate</Label>
                        <Select
                          value={item.uom}
                          onValueChange={(value) => updateItem(index, 'uom', value)}
                        >
                          <SelectTrigger>
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
                      <div className="space-y-2">
                        <Label>Preț Unitar</Label>
                        <Input
                          type="number"
                          min="0"
                          step="0.01"
                          value={item.unit_price}
                          onChange={(e) => updateItem(index, 'unit_price', parseFloat(e.target.value) || 0)}
                          data-testid={`quote-item-${index}-price`}
                        />
                      </div>
                      <div className="space-y-2">
                        <Label>Total Linie</Label>
                        <div className="h-10 flex items-center px-3 bg-slate-50 rounded-md border font-mono text-sm">
                          {(item.qty * item.unit_price).toLocaleString('ro-RO')}
                        </div>
                      </div>
                    </div>
                    <div className="grid grid-cols-3 gap-3">
                      <div className="space-y-2">
                        <Label>Mapare Produs</Label>
                        <Select
                          value={item.normalized_product_id || "none"}
                          onValueChange={(value) => updateItem(index, 'normalized_product_id', value === "none" ? "" : value)}
                        >
                          <SelectTrigger>
                            <SelectValue placeholder="Opțional" />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="none">Niciunul</SelectItem>
                            {products.map((product) => (
                              <SelectItem key={product.id} value={product.id}>
                                {product.canonical_name}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>
                      <div className="space-y-2">
                        <Label>Termen Livrare (zile)</Label>
                        <Input
                          type="number"
                          min="0"
                          placeholder="Opțional"
                          value={item.lead_time_days || ''}
                          onChange={(e) => updateItem(index, 'lead_time_days', e.target.value)}
                        />
                      </div>
                      <div className="space-y-2 flex items-end">
                        <label className="flex items-center gap-2 h-10">
                          <Checkbox
                            checked={item.vat_included}
                            onCheckedChange={(checked) => updateItem(index, 'vat_included', checked)}
                          />
                          <span className="text-sm">TVA Inclus</span>
                        </label>
                      </div>
                    </div>
                  </div>
                ))}
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
                  <span className="font-mono">{formData.items.length}</span>
                </div>
                <div className="flex justify-between items-center border-t pt-4">
                  <span className="font-medium">Total</span>
                  <span className="font-mono text-lg font-bold">
                    {calculateTotal().toLocaleString('ro-RO')} {formData.currency}
                  </span>
                </div>
              </CardContent>
            </Card>

            {/* Actions */}
            <Card>
              <CardContent className="pt-6">
                <Button
                  type="submit"
                  className="w-full"
                  disabled={submitting}
                  data-testid="save-quote-btn"
                >
                  <Save className="h-4 w-4 mr-2" />
                  {submitting ? 'Se salvează...' : 'Salvează Oferta'}
                </Button>
              </CardContent>
            </Card>
          </div>
        </div>
      </form>
    </div>
  );
}
