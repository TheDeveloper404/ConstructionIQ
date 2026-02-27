import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { API } from '../App';
import { Card, CardContent } from '../components/ui/card';
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
import { Label } from '../components/ui/label';
import { Input } from '../components/ui/input';
import { Textarea } from '../components/ui/textarea';
import { Plus, FolderKanban, MapPin, ChevronLeft, ChevronRight } from 'lucide-react';
import { toast } from 'sonner';

const statusOptions = [
  { value: 'active', label: 'Activ' },
  { value: 'completed', label: 'Finalizat' },
  { value: 'on_hold', label: 'În Așteptare' },
];

export default function Projects() {
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [pagination, setPagination] = useState({ page: 1, total: 0, totalPages: 0 });
  const [statusFilter, setStatusFilter] = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    location: '',
    status: 'active',
    description: '',
  });

  const fetchProjects = async (page = 1, status = '') => {
    setLoading(true);
    try {
      const params = new URLSearchParams({ page, page_size: 10 });
      if (status) params.append('status', status);
      
      const response = await API.get(`/projects?${params}`);
      setProjects(response.data.items);
      setPagination({
        page: response.data.page,
        total: response.data.total,
        totalPages: response.data.total_pages,
      });
    } catch (error) {
      toast.error('Eroare la încărcarea proiectelor');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProjects(1, statusFilter);
  }, [statusFilter]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await API.post('/projects', formData);
      toast.success('Proiect creat cu succes');
      setDialogOpen(false);
      setFormData({ name: '', location: '', status: 'active', description: '' });
      fetchProjects(pagination.page, statusFilter);
    } catch (error) {
      toast.error('Eroare la crearea proiectului');
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

  return (
    <div className="space-y-6" data-testid="projects-page">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-heading font-bold tracking-tight">Proiecte</h1>
          <p className="text-muted-foreground mt-1">Gestionează proiectele de construcții</p>
        </div>
        <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
          <DialogTrigger asChild>
            <Button data-testid="add-project-btn">
              <Plus className="h-4 w-4 mr-2" />
              Adaugă Proiect
            </Button>
          </DialogTrigger>
          <DialogContent className="sm:max-w-lg">
            <DialogHeader>
              <DialogTitle>Adaugă Proiect Nou</DialogTitle>
            </DialogHeader>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="name">Nume Proiect *</Label>
                <Input
                  id="name"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  required
                  data-testid="project-name-input"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="location">Locație</Label>
                <Input
                  id="location"
                  placeholder="Oraș, Județ"
                  value={formData.location}
                  onChange={(e) => setFormData({ ...formData, location: e.target.value })}
                  data-testid="project-location-input"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="status">Status</Label>
                <Select
                  value={formData.status}
                  onValueChange={(value) => setFormData({ ...formData, status: value })}
                >
                  <SelectTrigger data-testid="project-status-select">
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
                  data-testid="project-description-input"
                />
              </div>
              <div className="flex justify-end gap-3 pt-4">
                <Button type="button" variant="outline" onClick={() => setDialogOpen(false)}>
                  Anulează
                </Button>
                <Button type="submit" data-testid="submit-project-btn">
                  Adaugă Proiect
                </Button>
              </div>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      {/* Filters */}
      <div className="flex gap-3">
        <Select value={statusFilter || "all"} onValueChange={(v) => setStatusFilter(v === "all" ? "" : v)}>
          <SelectTrigger className="w-40" data-testid="status-filter">
            <SelectValue placeholder="Toate Statusurile" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Toate Statusurile</SelectItem>
            {statusOptions.map((opt) => (
              <SelectItem key={opt.value} value={opt.value}>
                {opt.label}
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
                <TableHead className="font-semibold">Proiect</TableHead>
                <TableHead className="font-semibold">Locație</TableHead>
                <TableHead className="font-semibold">Status</TableHead>
                <TableHead className="font-semibold w-24">Acțiuni</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading ? (
                [...Array(5)].map((_, i) => (
                  <TableRow key={i}>
                    <TableCell><div className="h-4 bg-slate-200 rounded w-40 animate-pulse"></div></TableCell>
                    <TableCell><div className="h-4 bg-slate-200 rounded w-32 animate-pulse"></div></TableCell>
                    <TableCell><div className="h-4 bg-slate-200 rounded w-20 animate-pulse"></div></TableCell>
                    <TableCell><div className="h-4 bg-slate-200 rounded w-16 animate-pulse"></div></TableCell>
                  </TableRow>
                ))
              ) : projects.length > 0 ? (
                projects.map((project) => (
                  <TableRow key={project.id} data-testid={`project-row-${project.id}`}>
                    <TableCell>
                      <div className="flex items-center gap-3">
                        <div className="h-9 w-9 rounded-md bg-blue-100 flex items-center justify-center">
                          <FolderKanban className="h-4 w-4 text-blue-600" />
                        </div>
                        <div>
                          <p className="font-medium">{project.name}</p>
                          {project.description && (
                            <p className="text-xs text-muted-foreground line-clamp-1">{project.description}</p>
                          )}
                        </div>
                      </div>
                    </TableCell>
                    <TableCell>
                      {project.location ? (
                        <div className="flex items-center gap-1.5 text-sm">
                          <MapPin className="h-3.5 w-3.5 text-muted-foreground" />
                          <span>{project.location}</span>
                        </div>
                      ) : (
                        <span className="text-muted-foreground text-sm">—</span>
                      )}
                    </TableCell>
                    <TableCell>{getStatusBadge(project.status)}</TableCell>
                    <TableCell>
                      <Button variant="ghost" size="sm" asChild>
                        <Link to={`/projects/${project.id}`} data-testid={`view-project-${project.id}`}>
                          Vezi
                        </Link>
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              ) : (
                <TableRow>
                  <TableCell colSpan={4} className="text-center py-12">
                    <FolderKanban className="h-8 w-8 text-muted-foreground/40 mx-auto mb-2" />
                    <p className="text-muted-foreground">Niciun proiect găsit</p>
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
              onClick={() => fetchProjects(pagination.page - 1, statusFilter)}
              disabled={pagination.page <= 1}
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => fetchProjects(pagination.page + 1, statusFilter)}
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
