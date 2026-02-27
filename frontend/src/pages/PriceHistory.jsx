import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { API } from '../App';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import { TrendingUp, TrendingDown, Minus } from 'lucide-react';
import { toast } from 'sonner';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts';

export default function PriceHistory() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [products, setProducts] = useState([]);
  const [selectedProduct, setSelectedProduct] = useState(searchParams.get('product') || '');
  const [priceData, setPriceData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [days, setDays] = useState('90');

  useEffect(() => {
    const fetchProducts = async () => {
      try {
        const response = await API.get('/catalog/products?page_size=100');
        setProducts(response.data.items);
        
        if (!selectedProduct && response.data.items.length > 0) {
          setSelectedProduct(response.data.items[0].id);
        }
      } catch (error) {
        toast.error('Eroare la încărcarea produselor');
      } finally {
        setLoading(false);
      }
    };
    fetchProducts();
  }, []);

  useEffect(() => {
    if (selectedProduct) {
      fetchPriceHistory();
      setSearchParams({ product: selectedProduct });
    }
  }, [selectedProduct, days]);

  const fetchPriceHistory = async () => {
    try {
      const response = await API.get(`/price-history/product/${selectedProduct}?days=${days}`);
      setPriceData(response.data);
    } catch (error) {
      toast.error('Eroare la încărcarea istoricului prețurilor');
    }
  };

  const formatChartData = () => {
    if (!priceData?.price_points?.length) return [];
    
    return priceData.price_points.map((pp) => ({
      date: new Date(pp.observed_at).toLocaleDateString('ro-RO'),
      price: pp.unit_price_normalized,
      supplier: pp.supplier_name || 'Necunoscut',
    }));
  };

  const calculateStats = () => {
    if (!priceData?.price_points?.length) return null;
    
    const prices = priceData.price_points.map((pp) => pp.unit_price_normalized);
    const latest = prices[prices.length - 1];
    const first = prices[0];
    const avg = prices.reduce((a, b) => a + b, 0) / prices.length;
    const min = Math.min(...prices);
    const max = Math.max(...prices);
    const change = first > 0 ? ((latest - first) / first) * 100 : 0;
    
    return { latest, first, avg, min, max, change, count: prices.length };
  };

  const stats = calculateStats();
  const chartData = formatChartData();

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="h-8 bg-slate-200 rounded w-48 animate-pulse"></div>
        <Card className="animate-pulse">
          <CardContent className="p-6">
            <div className="h-64 bg-slate-200 rounded"></div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6" data-testid="price-history-page">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-heading font-bold tracking-tight">Istoric Prețuri</h1>
          <p className="text-muted-foreground mt-1">Urmărește tendințele prețurilor pe produs</p>
        </div>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap gap-4">
        <div className="w-80">
          <Select value={selectedProduct} onValueChange={setSelectedProduct}>
            <SelectTrigger data-testid="product-select">
              <SelectValue placeholder="Selectează un produs" />
            </SelectTrigger>
            <SelectContent>
              {products.map((product) => (
                <SelectItem key={product.id} value={product.id}>
                  {product.canonical_name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <Select value={days} onValueChange={setDays}>
          <SelectTrigger className="w-40" data-testid="days-select">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="30">Ultimele 30 zile</SelectItem>
            <SelectItem value="90">Ultimele 90 zile</SelectItem>
            <SelectItem value="180">Ultimele 6 luni</SelectItem>
            <SelectItem value="365">Ultimul an</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {selectedProduct && priceData ? (
        <>
          {/* Stats */}
          {stats && (
            <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
              <Card>
                <CardContent className="p-4">
                  <p className="text-sm text-muted-foreground">Ultimul Preț</p>
                  <p className="text-2xl font-heading font-bold font-mono">
                    {stats.latest?.toLocaleString('ro-RO')}
                  </p>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-4">
                  <p className="text-sm text-muted-foreground">Medie</p>
                  <p className="text-2xl font-heading font-bold font-mono">
                    {stats.avg?.toLocaleString('ro-RO', { maximumFractionDigits: 2 })}
                  </p>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-4">
                  <p className="text-sm text-muted-foreground">Min / Max</p>
                  <p className="text-lg font-mono">
                    {stats.min?.toLocaleString('ro-RO')} - {stats.max?.toLocaleString('ro-RO')}
                  </p>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-4">
                  <p className="text-sm text-muted-foreground">Puncte Date</p>
                  <p className="text-2xl font-heading font-bold font-mono">{stats.count}</p>
                </CardContent>
              </Card>
              <Card>
                <CardContent className="p-4">
                  <p className="text-sm text-muted-foreground">Tendință</p>
                  <div className="flex items-center gap-2">
                    {stats.change > 0 ? (
                      <TrendingUp className="h-5 w-5 text-red-500" />
                    ) : stats.change < 0 ? (
                      <TrendingDown className="h-5 w-5 text-emerald-500" />
                    ) : (
                      <Minus className="h-5 w-5 text-slate-400" />
                    )}
                    <span className={`text-lg font-mono font-bold ${
                      stats.change > 0 ? 'text-red-600' : stats.change < 0 ? 'text-emerald-600' : ''
                    }`}>
                      {stats.change > 0 ? '+' : ''}{stats.change.toFixed(1)}%
                    </span>
                  </div>
                </CardContent>
              </Card>
            </div>
          )}

          {/* Chart */}
          <Card>
            <CardHeader>
              <CardTitle className="text-lg">
                {priceData.product?.canonical_name} - Evoluție Preț
              </CardTitle>
            </CardHeader>
            <CardContent>
              {chartData.length > 0 ? (
                <div className="h-80">
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                      <XAxis
                        dataKey="date"
                        tick={{ fontSize: 12 }}
                        stroke="#94a3b8"
                      />
                      <YAxis
                        tick={{ fontSize: 12 }}
                        stroke="#94a3b8"
                        tickFormatter={(value) => value.toLocaleString('ro-RO')}
                      />
                      <Tooltip
                        contentStyle={{
                          backgroundColor: 'white',
                          border: '1px solid #e2e8f0',
                          borderRadius: '8px',
                        }}
                        formatter={(value) => [value.toLocaleString('ro-RO'), 'Preț']}
                      />
                      <Legend />
                      <Line
                        type="monotone"
                        dataKey="price"
                        stroke="hsl(24, 95%, 53%)"
                        strokeWidth={2}
                        dot={{ fill: 'hsl(24, 95%, 53%)', strokeWidth: 2 }}
                        activeDot={{ r: 6 }}
                        name="Preț Unitar"
                      />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              ) : (
                <div className="h-64 flex items-center justify-center">
                  <p className="text-muted-foreground">Nu există date pentru această perioadă</p>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Price Points Table */}
          {priceData.price_points?.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">Puncte Preț</CardTitle>
              </CardHeader>
              <CardContent className="p-0">
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="bg-slate-50 border-b">
                        <th className="text-left p-3 font-semibold">Data</th>
                        <th className="text-left p-3 font-semibold">Furnizor</th>
                        <th className="text-left p-3 font-semibold">Sursă</th>
                        <th className="text-right p-3 font-semibold">Preț</th>
                        <th className="text-left p-3 font-semibold">UM</th>
                      </tr>
                    </thead>
                    <tbody>
                      {priceData.price_points.slice().reverse().map((pp) => (
                        <tr key={pp.id} className="border-b hover:bg-slate-50">
                          <td className="p-3">{new Date(pp.observed_at).toLocaleDateString('ro-RO')}</td>
                          <td className="p-3">{pp.supplier_name || '—'}</td>
                          <td className="p-3 capitalize">{pp.source_type === 'quote' ? 'Ofertă' : 'Achiziție'}</td>
                          <td className="p-3 text-right font-mono">
                            {pp.unit_price_normalized?.toLocaleString('ro-RO')}
                          </td>
                          <td className="p-3">{pp.uom_normalized}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </CardContent>
            </Card>
          )}
        </>
      ) : (
        <Card>
          <CardContent className="py-12 text-center">
            <TrendingUp className="h-12 w-12 text-muted-foreground/40 mx-auto mb-4" />
            <h3 className="font-medium text-lg mb-2">Selectează un Produs</h3>
            <p className="text-muted-foreground">
              Alege un produs din lista pentru a vedea istoricul prețurilor
            </p>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
