import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { API } from '../App';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Textarea } from '../components/ui/textarea';
import { Checkbox } from '../components/ui/checkbox';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import { Calendar } from '../components/ui/calendar';
import { Popover, PopoverContent, PopoverTrigger } from '../components/ui/popover';
import { cn } from '../lib/utils';
import { format } from 'date-fns';
import { ro } from 'date-fns/locale';
import { ArrowLeft, Plus, Trash2, CalendarIcon, Send } from 'lucide-react';
import { toast } from 'sonner';

export default function RFQCreate() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const preselectedProject = searchParams.get('project');
  
  const [projects, setProjects] = useState([]);
  const [suppliers, setSuppliers] = useState([]);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const [formData, setFormData] = useState({
    title: '',
    project_id: preselectedProject || '',
    due_date: null,
    notes: '',
    supplier_ids: [],
    items: [{ raw_text: '', requested_qty: 1, requested_uom: 'unit', normalized_product_id: '', specs: {} }],
  });

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [projectsRes, suppliersRes, productsRes] = await Promise.all([
          API.get('/projects?page_size=100'),
          API.get('/suppliers?page_size=100'),
          API.get('/catalog/products?page_size=100'),
        ]);
        setProjects(projectsRes.data.items);
        setSuppliers(suppliersRes.data.items);
        setProducts(productsRes.data.items);
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
      items: [...formData.items, { raw_text: '', requested_qty: 1, requested_uom: 'unit', normalized_product_id: '', specs: {} }],
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

  const toggleSupplier = (supplierId) => {
    const newIds = formData.supplier_ids.includes(supplierId)
      ? formData.supplier_ids.filter((id) => id !== supplierId)
      : [...formData.supplier_ids, supplierId];
    setFormData({ ...formData, supplier_ids: newIds });
  };

  const handleSubmit = async (e, sendNow = false) => {
    e.preventDefault();
    
    if (!formData.title.trim()) {
      toast.error('Introduceți un titlu');
      return;
    }
    if (!formData.project_id) {
      toast.error('Selectați un proiect');
      return;
    }
    if (formData.items.some((item) => !item.raw_text.trim())) {
      toast.error('Completați descrierea tuturor articolelor');
      return;
    }

    setSubmitting(true);
    try {
      const payload = {
        ...formData,
        due_date: formData.due_date ? formData.due_date.toISOString() : null,
        items: formData.items.map((item) => ({
          ...item,
          normalized_product_id: item.normalized_product_id || null,
        })),
      };
      
      const response = await API.post('/rfqs', payload);
      
      if (sendNow && formData.supplier_ids.length > 0) {
        await API.post(`/rfqs/${response.data.id}/send`);
        toast.success('Cerere creată și trimisă furnizorilor');
      } else {
        toast.success('Cerere salvată ca ciornă');
      }
      
      navigate(`/rfqs/${response.data.id}`);
    } catch (error) {
      toast.error('Eroare la crearea cererii');
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
            <div className="h-4 bg-slate-200 rounded w-1/2"></div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6" data-testid="rfq-create-page">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={() => navigate('/rfqs')}>
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <div>
          <h1 className="text-2xl font-heading font-bold">Cerere de Ofertă Nouă</h1>
          <p className="text-muted-foreground">Solicită oferte de la furnizori</p>
        </div>
      </div>

      <form onSubmit={(e) => handleSubmit(e, false)}>
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Main Form */}
          <div className="lg:col-span-2 space-y-6">
            {/* Basic Info */}
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">Informații de Bază</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="title">Titlu Cerere *</Label>
                  <Input
                    id="title"
                    placeholder="ex: Oțel Beton pentru Faza 2"
                    value={formData.title}
                    onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                    required
                    data-testid="rfq-title-input"
                  />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="project">Proiect *</Label>
                    <Select
                      value={formData.project_id}
                      onValueChange={(value) => setFormData({ ...formData, project_id: value })}
                    >
                      <SelectTrigger data-testid="rfq-project-select">
                        <SelectValue placeholder="Selectează proiect" />
                      </SelectTrigger>
                      <SelectContent>
                        {projects.map((project) => (
                          <SelectItem key={project.id} value={project.id}>
                            {project.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="space-y-2">
                    <Label>Termen Limită</Label>
                    <Popover>
                      <PopoverTrigger asChild>
                        <Button
                          variant="outline"
                          className={cn(
                            'w-full justify-start text-left font-normal',
                            !formData.due_date && 'text-muted-foreground'
                          )}
                          data-testid="rfq-due-date-btn"
                        >
                          <CalendarIcon className="mr-2 h-4 w-4" />
                          {formData.due_date ? format(formData.due_date, 'PPP', { locale: ro }) : 'Alege data'}
                        </Button>
                      </PopoverTrigger>
                      <PopoverContent className="w-auto p-0">
                        <Calendar
                          mode="single"
                          selected={formData.due_date}
                          onSelect={(date) => setFormData({ ...formData, due_date: date })}
                          locale={ro}
                          initialFocus
                        />
                      </PopoverContent>
                    </Popover>
                  </div>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="notes">Note</Label>
                  <Textarea
                    id="notes"
                    placeholder="Instrucțiuni sau cerințe suplimentare..."
                    value={formData.notes}
                    onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                    data-testid="rfq-notes-input"
                  />
                </div>
              </CardContent>
            </Card>

            {/* Items */}
            <Card>
              <CardHeader className="flex flex-row items-center justify-between">
                <CardTitle className="text-lg">Articole Solicitate</CardTitle>
                <Button type="button" variant="outline" size="sm" onClick={addItem} data-testid="add-item-btn">
                  <Plus className="h-4 w-4 mr-1" />
                  Adaugă Articol
                </Button>
              </CardHeader>
              <CardContent className="space-y-4">
                {formData.items.map((item, index) => (
                  <div key={index} className="p-4 border rounded-lg space-y-3" data-testid={`rfq-item-${index}`}>
                    <div className="flex items-start justify-between gap-4">
                      <div className="flex-1 space-y-2">
                        <Label>Descriere *</Label>
                        <Input
                          placeholder="ex: Oțel beton Ø12mm"
                          value={item.raw_text}
                          onChange={(e) => updateItem(index, 'raw_text', e.target.value)}
                          data-testid={`item-${index}-description`}
                        />
                      </div>
                      {formData.items.length > 1 && (
                        <Button
                          type="button"
                          variant="ghost"
                          size="icon"
                          onClick={() => removeItem(index)}
                          className="text-destructive"
                          data-testid={`remove-item-${index}`}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      )}
                    </div>
                    <div className="grid grid-cols-3 gap-3">
                      <div className="space-y-2">
                        <Label>Cantitate</Label>
                        <Input
                          type="number"
                          min="1"
                          value={item.requested_qty}
                          onChange={(e) => updateItem(index, 'requested_qty', parseFloat(e.target.value) || 1)}
                          data-testid={`item-${index}-qty`}
                        />
                      </div>
                      <div className="space-y-2">
                        <Label>Unitate</Label>
                        <Select
                          value={item.requested_uom}
                          onValueChange={(value) => updateItem(index, 'requested_uom', value)}
                        >
                          <SelectTrigger data-testid={`item-${index}-uom`}>
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
                        <Label>Legătură Produs</Label>
                        <Select
                          value={item.normalized_product_id || "none"}
                          onValueChange={(value) => updateItem(index, 'normalized_product_id', value === "none" ? "" : value)}
                        >
                          <SelectTrigger data-testid={`item-${index}-product`}>
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
                    </div>
                  </div>
                ))}
              </CardContent>
            </Card>
          </div>

          {/* Sidebar */}
          <div className="space-y-6">
            {/* Suppliers */}
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">Selectează Furnizori</CardTitle>
              </CardHeader>
              <CardContent>
                {suppliers.length > 0 ? (
                  <div className="space-y-2 max-h-80 overflow-y-auto">
                    {suppliers.map((supplier) => (
                      <label
                        key={supplier.id}
                        className="flex items-center gap-3 p-2 rounded-md hover:bg-slate-50 cursor-pointer"
                        data-testid={`supplier-checkbox-${supplier.id}`}
                      >
                        <Checkbox
                          checked={formData.supplier_ids.includes(supplier.id)}
                          onCheckedChange={() => toggleSupplier(supplier.id)}
                        />
                        <div>
                          <p className="text-sm font-medium">{supplier.name}</p>
                          {supplier.contact_email && (
                            <p className="text-xs text-muted-foreground">{supplier.contact_email}</p>
                          )}
                        </div>
                      </label>
                    ))}
                  </div>
                ) : (
                  <p className="text-sm text-muted-foreground text-center py-4">
                    Niciun furnizor disponibil. Adăugați furnizori întâi.
                  </p>
                )}
                <p className="text-xs text-muted-foreground mt-3">
                  {formData.supplier_ids.length} furnizor(i) selectat(i)
                </p>
              </CardContent>
            </Card>

            {/* Actions */}
            <Card>
              <CardContent className="pt-6 space-y-3">
                <Button
                  type="submit"
                  variant="outline"
                  className="w-full"
                  disabled={submitting}
                  data-testid="save-draft-btn"
                >
                  Salvează ca Ciornă
                </Button>
                <Button
                  type="button"
                  className="w-full"
                  onClick={(e) => handleSubmit(e, true)}
                  disabled={submitting || formData.supplier_ids.length === 0}
                  data-testid="send-rfq-btn"
                >
                  <Send className="h-4 w-4 mr-2" />
                  {submitting ? 'Se trimite...' : 'Creează și Trimite'}
                </Button>
                {formData.supplier_ids.length === 0 && (
                  <p className="text-xs text-muted-foreground text-center">
                    Selectați furnizori pentru trimitere imediată
                  </p>
                )}
              </CardContent>
            </Card>
          </div>
        </div>
      </form>
    </div>
  );
}
