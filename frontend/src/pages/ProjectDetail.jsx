import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { API } from '../App';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Textarea } from '../components/ui/textarea';
import { Badge } from '../components/ui/badge';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import { ArrowLeft, FolderKanban, MapPin, Calendar, Save, Trash2 } from 'lucide-react';
import { toast } from 'sonner';

const statusOptions = [
  { value: 'active', label: 'Activ' },
  { value: 'completed', label: 'Finalizat' },
  { value: 'on_hold', label: 'În Așteptare' },
];

export default function ProjectDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [project, setProject] = useState(null);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [formData, setFormData] = useState({});

  useEffect(() => {
    const fetchProject = async () => {
      try {
        const response = await API.get(`/projects/${id}`);
        setProject(response.data);
        setFormData({
          name: response.data.name,
          location: response.data.location || '',
          status: response.data.status || 'active',
          description: response.data.description || '',
        });
      } catch (error) {
        toast.error('Eroare la încărcarea proiectului');
        navigate('/projects');
      } finally {
        setLoading(false);
      }
    };
    fetchProject();
  }, [id, navigate]);

  const handleUpdate = async (e) => {
    e.preventDefault();
    try {
      const response = await API.put(`/projects/${id}`, formData);
      setProject(response.data);
      setEditing(false);
      toast.success('Proiect actualizat');
    } catch (error) {
      toast.error('Eroare la actualizarea proiectului');
    }
  };

  const handleDelete = async () => {
    if (!window.confirm('Ștergeți acest proiect? Această acțiune nu poate fi anulată.')) return;
    try {
      await API.delete(`/projects/${id}`);
      toast.success('Proiect șters');
      navigate('/projects');
    } catch (error) {
      toast.error('Eroare la ștergerea proiectului');
    }
  };

  const getStatusBadge = (status) => {
    const labels = {
      active: 'Activ',
      completed: 'Finalizat',
      on_hold: 'În Așteptare',
    };
    const styles = {
      active: 'bg-emerald-100 text-emerald-700',
      completed: 'bg-slate-100 text-slate-700',
      on_hold: 'bg-amber-100 text-amber-700',
    };
    return (
      <Badge variant="secondary" className={styles[status] || styles.active}>
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
    <div className="space-y-6" data-testid="project-detail">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" asChild>
          <Link to="/projects">
            <ArrowLeft className="h-5 w-5" />
          </Link>
        </Button>
        <div className="flex-1">
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-heading font-bold">{project?.name}</h1>
            {getStatusBadge(project?.status)}
          </div>
          <p className="text-muted-foreground">Detalii Proiect</p>
        </div>
        {!editing && (
          <div className="flex gap-2">
            <Button variant="outline" onClick={() => setEditing(true)} data-testid="edit-project-btn">
              Editează
            </Button>
            <Button variant="destructive" size="icon" onClick={handleDelete} data-testid="delete-project-btn">
              <Trash2 className="h-4 w-4" />
            </Button>
          </div>
        )}
      </div>

      {editing ? (
        <Card>
          <CardHeader>
            <CardTitle>Editare Proiect</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleUpdate} className="space-y-4 max-w-xl">
              <div className="space-y-2">
                <Label htmlFor="name">Nume Proiect *</Label>
                <Input
                  id="name"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  required
                  data-testid="edit-project-name"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="location">Locație</Label>
                <Input
                  id="location"
                  value={formData.location}
                  onChange={(e) => setFormData({ ...formData, location: e.target.value })}
                  data-testid="edit-project-location"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="status">Status</Label>
                <Select
                  value={formData.status}
                  onValueChange={(value) => setFormData({ ...formData, status: value })}
                >
                  <SelectTrigger data-testid="edit-project-status">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {statusOptions.map((opt) => (
                      <SelectItem key={opt.value} value={opt.value}>
                        {opt.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="description">Descriere</Label>
                <Textarea
                  id="description"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  data-testid="edit-project-description"
                />
              </div>
              <div className="flex gap-3 pt-4">
                <Button type="submit" data-testid="save-project-btn">
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
              <CardTitle className="text-lg">Informații Proiect</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center gap-3">
                <div className="h-12 w-12 rounded-lg bg-blue-100 flex items-center justify-center">
                  <FolderKanban className="h-6 w-6 text-blue-600" />
                </div>
                <div>
                  <p className="font-medium">{project?.name}</p>
                  <p className="text-sm text-muted-foreground">Nume Proiect</p>
                </div>
              </div>
              {project?.location && (
                <div className="flex items-center gap-3">
                  <MapPin className="h-5 w-5 text-muted-foreground" />
                  <span className="text-sm">{project.location}</span>
                </div>
              )}
              <div className="flex items-center gap-3">
                <Calendar className="h-5 w-5 text-muted-foreground" />
                <span className="text-sm">
                  Creat la {new Date(project?.created_at).toLocaleDateString('ro-RO')}
                </span>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-lg">Descriere</CardTitle>
            </CardHeader>
            <CardContent>
              {project?.description ? (
                <p className="text-sm text-muted-foreground">{project.description}</p>
              ) : (
                <p className="text-sm text-muted-foreground italic">Nicio descriere</p>
              )}
            </CardContent>
          </Card>
        </div>
      )}

      {/* Quick Actions */}
      {!editing && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Acțiuni Rapide</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex gap-3">
              <Button asChild>
                <Link to={`/rfqs/new?project=${id}`} data-testid="create-rfq-for-project">
                  Creează Cerere de Ofertă
                </Link>
              </Button>
              <Button variant="outline" asChild>
                <Link to={`/rfqs?project_id=${id}`}>
                  Vezi Cererile Proiectului
                </Link>
              </Button>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
