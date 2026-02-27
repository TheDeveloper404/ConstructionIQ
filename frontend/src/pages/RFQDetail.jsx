import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { API } from '../App';
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
import { ArrowLeft, FileText, Send, Calendar, Building2, Trash2 } from 'lucide-react';
import { toast } from 'sonner';

export default function RFQDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [rfq, setRfq] = useState(null);
  const [project, setProject] = useState(null);
  const [suppliers, setSuppliers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const rfqRes = await API.get(`/rfqs/${id}`);
        setRfq(rfqRes.data);
        
        if (rfqRes.data.project_id) {
          const projectRes = await API.get(`/projects/${rfqRes.data.project_id}`);
          setProject(projectRes.data);
        }
        
        if (rfqRes.data.supplier_ids?.length > 0) {
          const suppliersRes = await API.get('/suppliers?page_size=100');
          const selectedSuppliers = suppliersRes.data.items.filter(
            (s) => rfqRes.data.supplier_ids.includes(s.id)
          );
          setSuppliers(selectedSuppliers);
        }
      } catch (error) {
        toast.error('Eroare la încărcarea cererii de ofertă');
        navigate('/rfqs');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [id, navigate]);

  const handleSend = async () => {
    if (!window.confirm('Trimiteți această cerere tuturor furnizorilor selectați?')) return;
    setSending(true);
    try {
      await API.post(`/rfqs/${id}/send`);
      setRfq({ ...rfq, status: 'sent' });
      toast.success('Cerere trimisă furnizorilor');
    } catch (error) {
      toast.error(error.response?.data?.detail || 'Eroare la trimiterea cererii');
    } finally {
      setSending(false);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm('Ștergeți această cerere? Această acțiune nu poate fi anulată.')) return;
    try {
      await API.delete(`/rfqs/${id}`);
      toast.success('Cerere ștearsă');
      navigate('/rfqs');
    } catch (error) {
      toast.error('Eroare la ștergerea cererii');
    }
  };

  const getStatusBadge = (status) => {
    const labels = {
      draft: 'Ciornă',
      sent: 'Trimisă',
      closed: 'Închisă',
    };
    const styles = {
      draft: 'bg-slate-100 text-slate-700',
      sent: 'bg-blue-100 text-blue-700',
      closed: 'bg-slate-100 text-slate-600',
    };
    return (
      <Badge variant="secondary" className={styles[status] || styles.draft}>
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
            <div className="h-4 bg-slate-200 rounded w-1/2"></div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6" data-testid="rfq-detail">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" asChild>
          <Link to="/rfqs">
            <ArrowLeft className="h-5 w-5" />
          </Link>
        </Button>
        <div className="flex-1">
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-heading font-bold">{rfq?.title}</h1>
            {getStatusBadge(rfq?.status)}
          </div>
          <p className="text-muted-foreground font-mono text-sm">{rfq?.id}</p>
        </div>
        <div className="flex gap-2">
          {rfq?.status === 'draft' && (
            <Button onClick={handleSend} disabled={sending || suppliers.length === 0} data-testid="send-rfq-btn">
              <Send className="h-4 w-4 mr-2" />
              {sending ? 'Se trimite...' : 'Trimite Cererea'}
            </Button>
          )}
          <Button variant="destructive" size="icon" onClick={handleDelete} data-testid="delete-rfq-btn">
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
              <CardTitle className="text-lg">Articole Solicitate</CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              <Table>
                <TableHeader>
                  <TableRow className="bg-slate-50">
                    <TableHead className="font-semibold">#</TableHead>
                    <TableHead className="font-semibold">Descriere</TableHead>
                    <TableHead className="font-semibold text-right">Cantitate</TableHead>
                    <TableHead className="font-semibold">Unitate</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {rfq?.items?.map((item, index) => (
                    <TableRow key={item.id}>
                      <TableCell className="font-mono text-muted-foreground">{index + 1}</TableCell>
                      <TableCell>{item.raw_text}</TableCell>
                      <TableCell className="text-right font-mono">{item.requested_qty}</TableCell>
                      <TableCell>{item.requested_uom}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>

          {/* Notes */}
          {rfq?.notes && (
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">Note</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-sm text-muted-foreground whitespace-pre-wrap">{rfq.notes}</p>
              </CardContent>
            </Card>
          )}
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Details */}
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Detalii</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center gap-3">
                <FileText className="h-5 w-5 text-muted-foreground" />
                <div>
                  <p className="text-sm font-medium">Proiect</p>
                  <p className="text-sm text-muted-foreground">{project?.name || 'Necunoscut'}</p>
                </div>
              </div>
              {rfq?.due_date && (
                <div className="flex items-center gap-3">
                  <Calendar className="h-5 w-5 text-muted-foreground" />
                  <div>
                    <p className="text-sm font-medium">Termen Limită</p>
                    <p className="text-sm text-muted-foreground">
                      {new Date(rfq.due_date).toLocaleDateString('ro-RO')}
                    </p>
                  </div>
                </div>
              )}
              <div>
                <p className="text-sm font-medium mb-1">Data Creării</p>
                <p className="text-sm text-muted-foreground">
                  {new Date(rfq?.created_at).toLocaleString('ro-RO')}
                </p>
              </div>
            </CardContent>
          </Card>

          {/* Suppliers */}
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Furnizori Selectați</CardTitle>
            </CardHeader>
            <CardContent>
              {suppliers.length > 0 ? (
                <div className="space-y-2">
                  {suppliers.map((supplier) => (
                    <div key={supplier.id} className="flex items-center gap-3 p-2 rounded-md bg-slate-50">
                      <Building2 className="h-4 w-4 text-muted-foreground" />
                      <div>
                        <p className="text-sm font-medium">{supplier.name}</p>
                        {supplier.contact_email && (
                          <p className="text-xs text-muted-foreground">{supplier.contact_email}</p>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-muted-foreground text-center py-4">
                  Niciun furnizor selectat
                </p>
              )}
            </CardContent>
          </Card>

          {/* Quick Actions */}
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Acțiuni</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              <Button variant="outline" className="w-full" asChild>
                <Link to={`/quotes/new?rfq=${id}`} data-testid="add-quote-for-rfq">
                  Adaugă Ofertă Primită
                </Link>
              </Button>
              <Button variant="outline" className="w-full" asChild>
                <Link to={`/quotes?rfq_id=${id}`}>
                  Vezi Ofertele Asociate
                </Link>
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
