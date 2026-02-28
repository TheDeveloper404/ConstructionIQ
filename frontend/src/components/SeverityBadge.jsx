import React from 'react';
import { Badge } from './ui/badge';
import { AlertTriangle } from 'lucide-react';

const SEVERITY_LABELS = {
  high: 'Ridicată',
  medium: 'Medie',
  low: 'Scăzută',
};

const SEVERITY_STYLES = {
  high: 'bg-red-100 text-red-700',
  medium: 'bg-amber-100 text-amber-700',
  low: 'bg-slate-100 text-slate-600',
};

const SEVERITY_ICON_STYLES = {
  high: 'text-red-500',
  medium: 'text-amber-500',
  low: 'text-slate-400',
};

export default function SeverityBadge({ severity, showIcon = false }) {
  return (
    <div className="flex items-center gap-1.5">
      {showIcon && (
        <AlertTriangle className={`h-4 w-4 ${SEVERITY_ICON_STYLES[severity] || 'text-slate-400'}`} />
      )}
      <Badge variant="secondary" className={SEVERITY_STYLES[severity] || SEVERITY_STYLES.low}>
        {SEVERITY_LABELS[severity] || severity}
      </Badge>
    </div>
  );
}
