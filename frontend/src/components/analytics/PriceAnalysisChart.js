import React, { useEffect, useState } from "react";
import {
  Area,
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import axios from "axios";

const tooltipStyle = {
  backgroundColor: "#16161A",
  border: "1px solid #33333D",
  borderRadius: "8px",
  color: "#EDEDF0",
  fontSize: 13,
  padding: "10px 14px",
};

const PriceAnalysisChart = ({ modelName }) => {
  const [priceData, setPriceData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!modelName) return;
    let alive = true;
    (async () => {
      try {
        setLoading(true);
        setError(null);
        const res = await axios.get(
          `http://localhost:8080/api/analytics/price-by-year/${encodeURIComponent(modelName)}`
        );
        const sorted = [...res.data].sort(
          (a, b) => parseInt(a.originalYear, 10) - parseInt(b.originalYear, 10)
        );
        if (alive) setPriceData(sorted);
      } catch (err) {
        console.error(err);
        if (alive) setError("가격 데이터를 불러오지 못했습니다.");
      } finally {
        if (alive) setLoading(false);
      }
    })();
    return () => {
      alive = false;
    };
  }, [modelName]);

  if (loading) {
    return (
      <div className="card p-12 flex flex-col items-center gap-3">
        <div className="h-6 w-6 border-2 border-brand border-t-transparent rounded-full animate-spin" />
        <p className="text-sm text-fg-muted">가격 분포를 계산하는 중…</p>
      </div>
    );
  }
  if (error) {
    return (
      <div className="card p-6 border-danger/30 bg-danger/5">
        <p className="text-sm text-danger">{error}</p>
      </div>
    );
  }
  if (priceData.length === 0) {
    return (
      <div className="card p-12 text-center">
        <p className="text-sm text-fg-muted">
          "{modelName}"에 대한 가격 데이터가 충분하지 않습니다.
        </p>
      </div>
    );
  }

  return (
    <div className="card overflow-hidden">
      <div className="h-80 p-4">
        <ResponsiveContainer width="100%" height="100%">
          <ComposedChart
            data={priceData}
            margin={{ top: 20, right: 20, left: 10, bottom: 10 }}
          >
            <CartesianGrid stroke="#26262E" strokeDasharray="3 3" vertical={false} />
            <XAxis
              dataKey="year"
              tick={{ fill: "#A1A1AA", fontSize: 12 }}
              axisLine={{ stroke: "#26262E" }}
              tickLine={{ stroke: "#26262E" }}
            />
            <YAxis
              tick={{ fill: "#A1A1AA", fontSize: 12 }}
              axisLine={{ stroke: "#26262E" }}
              tickLine={{ stroke: "#26262E" }}
              tickFormatter={(v) => `${v}만`}
            />
            <Tooltip
              formatter={(v) => `${Number(v).toLocaleString()} 만원`}
              contentStyle={tooltipStyle}
              cursor={{ stroke: "#7C9CFF", strokeWidth: 1, strokeDasharray: "4 4" }}
            />
            <Legend
              wrapperStyle={{ color: "#A1A1AA", fontSize: 13, paddingTop: 8 }}
            />
            <Area
              type="monotone"
              dataKey="maxPrice"
              name="최고가"
              fill="#7C9CFF"
              stroke="#7C9CFF"
              fillOpacity={0.08}
              strokeWidth={1}
            />
            <Area
              type="monotone"
              dataKey="minPrice"
              name="최저가"
              fill="#0E0E10"
              stroke="#71717A"
              fillOpacity={0}
              strokeWidth={1}
            />
            <Line
              type="monotone"
              dataKey="avgPrice"
              name="평균가"
              stroke="#7C9CFF"
              strokeWidth={2}
              dot={{ r: 3, fill: "#7C9CFF", strokeWidth: 0 }}
              activeDot={{ r: 5, fill: "#A4BBFF", strokeWidth: 0 }}
            />
          </ComposedChart>
        </ResponsiveContainer>
      </div>

      <div className="border-t border-line overflow-x-auto">
        <table className="min-w-full text-sm">
          <thead className="bg-bg-inset">
            <tr className="text-xs text-fg-subtle uppercase tracking-wider">
              <th className="px-5 py-3 text-left font-medium">연식</th>
              <th className="px-5 py-3 text-right font-medium">최저</th>
              <th className="px-5 py-3 text-right font-medium">평균</th>
              <th className="px-5 py-3 text-right font-medium">최고</th>
              <th className="px-5 py-3 text-right font-medium">대수</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-line">
            {priceData.map((row, i) => (
              <tr key={i} className="text-fg">
                <td className="px-5 py-3 font-medium">{row.year}</td>
                <td className="px-5 py-3 text-right text-fg-muted">
                  {row.minPrice.toLocaleString()}만
                </td>
                <td className="px-5 py-3 text-right">
                  {row.avgPrice.toLocaleString()}만
                </td>
                <td className="px-5 py-3 text-right text-fg-muted">
                  {row.maxPrice.toLocaleString()}만
                </td>
                <td className="px-5 py-3 text-right text-fg-subtle">
                  {row.count}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default PriceAnalysisChart;
