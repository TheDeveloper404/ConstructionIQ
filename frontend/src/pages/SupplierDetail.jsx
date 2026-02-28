import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { API } from '../lib/api';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Textarea } from '../components/ui/textarea';
import { Badge } from '../components/ui/badge';
import { ArrowLeft, Building2, Mail, Phone, MapPin, Save, Trash2 } from 'lucide-react';
import { toast } from 'sonner';

export default function SupplierDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [supplier, setSupplier] = useState(null);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [formData, setFormData] = useState({});

  useEffect(() => {
    const fetchSupplier = async () => {
      try {
        const response = await API.get(`/suppliers/${id}`);
        setSupplier(response.data);
        setFormData({
          name: response.data.name,
          contact_email: response.data.contact_email || '',
          phone: response.data.phone || '',
          address: response.data.address || '',
          tags: response.data.tags?.join(', ') || '',
          notes: response.data.notes || '',
        });
      } catch (error) {
        toast.error('Eroare la încărcarea furnizorului');
        navigate('/suppliers');
      } finally {
        setLoading(false);
      }
    };
    fetchSupplier();
  }, [id, navigate]);

  const handleUpdate = async (e) => {
    e.preventDefault();
    try {
      const data = {
        ...formData,
        tags: formData.tags.split(',').map(t => t.trim()).filter(Boolean),
      };
      const response = await API.put(`/suppliers/${id}`, data);
      setSupplier(response.data);
      setEditing(false);
      toast.success('Furnizor actualizat');
    } catch (error) {
      toast.error('Eroare la actualizarea furnizorului');
    }
  };

  const handleDelete = async () => {
    if (!window.confirm('Ștergeți acest furnizor? Această acțiune nu poate fi anulată.')) return;
    try {
      await API.delete(`/suppliers/${id}`);
      toast.success('Furnizor șters');
      navigate('/suppliers');
    } catch (error) {
      toast.error('Eroare la ștergerea furnizorului');
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
    <div className="space-y-6" data-testid="supplier-detail">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" asChild>
          <Link to="/suppliers">
            <ArrowLeft className="h-5 w-5" />
          </Link>
        </Button>
        <div className="flex-1">
          <h1 className="text-2xl font-heading font-bold">{supplier?.name}</h1>
          <p className="text-muted-foreground">Detalii Furnizor</p>
        </div>
        {!editing && (
          <div className="flex gap-2">
            <Button variant="outline" onClick={() => setEditing(true)} data-testid="edit-supplier-btn">
              Editează
            </Button>
            <Button variant="destructive" size="icon" onClick={handleDelete} data-testid="delete-supplier-btn">
              <Trash2 className="h-4 w-4" />
            </Button>
          </div>
        )}
      </div>

      {editing ? (
        <Card>
          <CardHeader>
            <CardTitle>Editare Furnizor</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleUpdate} className="space-y-4 max-w-xl">
              <div className="space-y-2">
                <Label htmlFor="name">Nume Companie *</Label>
                <Input
                  id="name"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  required
                  data-testid="edit-supplier-name"
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
                    data-testid="edit-supplier-email"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="phone">Telefon</Label>
                  <Input
                    id="phone"
                    value={formData.phone}
                    onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                    data-testid="edit-supplier-phone"
                  />
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="address">Adresă</Label>
                <Input
                  id="address"
                  value={formData.address}
                  onChange={(e) => setFormData({ ...formData, address: e.target.value })}
                  data-testid="edit-supplier-address"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="tags">Etichete (separate prin virgulă)</Label>
                <Input
                  id="tags"
                  value={formData.tags}
                  onChange={(e) => setFormData({ ...formData, tags: e.target.value })}
                  data-testid="edit-supplier-tags"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="notes">Note</Label>
                <Textarea
                  id="notes"
                  value={formData.notes}
                  onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                  data-testid="edit-supplier-notes"
                />
              </div>
              <div className="flex gap-3 pt-4">
                <Button type="submit" data-testid="save-supplier-btn">
                  <Save className="h-4 w-4 mr-2" />
                  Salvează
                </Button>
                <Button type="button" variant="outline" onClick={() => setEditing(false)}>
                  Anulează
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Informații Contact</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center gap-3">
                <div className="h-12 w-12 rounded-lg bg-slate-100 flex items-center justify-center">
                  <Building2 className="h-6 w-6 text-slate-600" />
                </div>
                <div>
                  <p className="font-medium">{supplier?.name}</p>
                  <p className="text-sm text-muted-foreground">Companie</p>
                </div>
              </div>
              {supplier?.contact_email && (
                <div className="flex items-center gap-3">
                  <Mail className="h-5 w-5 text-muted-foreground" />
                  <a href={`mailto:${supplier.contact_email}`} className="text-sm hover:underline">
                    {supplier.contact_email}
                  </a>
                </div>
              )}
              {supplier?.phone && (
                <div className="flex items-center gap-3">
                  <Phone className="h-5 w-5 text-muted-foreground" />
                  <a href={`tel:${supplier.phone}`} className="text-sm hover:underline">
                    {supplier.phone}
                  </a>
                </div>
              )}
              {supplier?.address && (
                <div className="flex items-center gap-3">
                  <MapPin className="h-5 w-5 text-muted-foreground" />
                  <span className="text-sm">{supplier.address}</span>
                </div>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Detalii Suplimentare</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <p className="text-sm font-medium mb-2">Etichete</p>
                <div className="flex flex-wrap gap-1">
                  {supplier?.tags?.length > 0 ? (
                    supplier.tags.map((tag) => (
                      <Badge key={tag} variant="secondary">{tag}</Badge>
                    ))
                  ) : (
                    <span className="text-sm text-muted-foreground">Fără etichete</span>
                  )}
                </div>
              </div>
              {supplier?.notes && (
                <div>
                  <p className="text-sm font-medium mb-2">Note</p>
                  <p className="text-sm text-muted-foreground">{supplier.notes}</p>
                </div>
              )}
              <div>
                <p className="text-sm font-medium mb-1">Data Înregistrării</p>
                <p className="text-sm text-muted-foreground">
                  {new Date(supplier?.created_at).toLocaleDateString('ro-RO')}
                </p>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}
