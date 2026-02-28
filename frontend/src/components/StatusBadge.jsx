import React from 'react';
import { Badge } from './ui/badge';

const STATUS_LABELS = {
  draft: 'Ciornă',
  sent: 'Trimisă',
  closed: 'Închisă',
  received: 'Primită',
  validated: 'Validată',
  archived: 'Arhivată',
  active: 'Activ',
  on_hold: 'Suspendat',
  completed: 'Finalizat',
  new: 'Nouă',
  ack: 'Confirmată',
  resolved: 'Rezolvată',
};

const STATUS_STYLES = {
  draft: 'bg-slate-100 text-slate-700',
  sent: 'bg-blue-100 text-blue-700',
  closed: 'bg-slate-100 text-slate-600',
  received: 'bg-amber-100 text-amber-700',
  validated: 'bg-emerald-100 text-emerald-700',
  archived: 'bg-slate-100 text-slate-500',
  active: 'bg-emerald-100 text-emerald-700',
  on_hold: 'bg-amber-100 text-amber-700',
  completed: 'bg-blue-100 text-blue-700',
  new: 'bg-red-100 text-red-700',
  ack: 'bg-amber-100 text-amber-700',
  resolved: 'bg-emerald-100 text-emerald-700',
};

export default function StatusBadge({ status }) {
  return (
    <Badge variant="secondary" className={STATUS_STYLES[status] || 'bg-slate-100 text-slate-700'}>
      {STATUS_LABELS[status] || status}
    </Badge>
  );
}
