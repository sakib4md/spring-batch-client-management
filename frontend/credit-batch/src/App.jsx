import { useEffect, useMemo, useState } from "react";

const API_URL = "http://localhost:8080/api";
const PAGE_SIZE = 12;
const currency = new Intl.NumberFormat("en-IN", { maximumFractionDigits: 2 });

const normalize = (client) => ({
  clientId: client.clientId || client.client_id || "—",
  clientName: client.clientName || client.client_name || "Unknown",
  industry: client.industry || "—",
  country: client.country || "—",
  creditScore: client.creditScore ?? client.credit_score ?? 0,
  creditRating: client.creditRating || client.credit_rating || "—",
  annualRevenueInr: Number(client.annualRevenueInr ?? client.annual_revenue_inr ?? 0),
  creditExposureInr: Number(client.creditExposureInr ?? client.credit_exposure_inr ?? 0),
  packageName: client.packageName || client.package || "—",
  onboardingStatus: client.onboardingStatus || client.onboarding_status || "—"
});

const formatCurrency = (value) => `₹${currency.format(value || 0)}`;

export default function App() {
  const [clients, setClients] = useState([]);
  const [search, setSearch] = useState("");
  const [country, setCountry] = useState("");
  const [status, setStatus] = useState("");
  const [rating, setRating] = useState("");
  const [sort, setSort] = useState({ key: "clientName", direction: "asc" });
  const [page, setPage] = useState(1);
  const [notice, setNotice] = useState("");
  const [loading, setLoading] = useState(true);

  async function loadClients() {
    setLoading(true);
    try {
      const response = await fetch(`${API_URL}/clients`);
      if (!response.ok) throw new Error(`Backend returned ${response.status}`);
      setClients((await response.json()).map(normalize));
      setNotice("");
    } catch {
      setClients([]);
      setNotice("Backend unavailable. Start the Java service or upload a CSV.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { loadClients(); }, []);

  const countries = useMemo(() => [...new Set(clients.map((item) => item.country))].sort(), [clients]);
  const statuses = useMemo(() => [...new Set(clients.map((item) => item.onboardingStatus))].sort(), [clients]);
  const ratings = useMemo(() => [...new Set(clients.map((item) => item.creditRating))].sort(), [clients]);

  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase();
    return clients.filter((client) => {
      const matchesSearch = !query || Object.values(client).join(" ").toLowerCase().includes(query);
      return matchesSearch && (!country || client.country === country) &&
        (!status || client.onboardingStatus === status) &&
        (!rating || client.creditRating === rating);
    }).sort((left, right) => {
      const a = left[sort.key], b = right[sort.key];
      const comparison = typeof a === "number" && typeof b === "number"
        ? a - b : String(a).localeCompare(String(b));
      return sort.direction === "asc" ? comparison : -comparison;
    });
  }, [clients, search, country, status, rating, sort]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const visible = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);
  useEffect(() => { setPage(1); }, [search, country, status, rating]);

  function changeSort(key) {
    setSort((current) => ({
      key,
      direction: current.key === key && current.direction === "asc" ? "desc" : "asc"
    }));
  }

  async function uploadCsv(event) {
    const file = event.target.files?.[0];
    if (!file) return;
    const form = new FormData();
    form.append("file", file);
    setNotice("Uploading and importing...");
    try {
      const response = await fetch(`${API_URL}/clients/import`, { method: "POST", body: form });
      const result = await response.json();
      if (!response.ok) throw new Error(result.message || "CSV import failed");
      setNotice(`Imported ${result.imported} clients from ${file.name}`);
      await loadClients();
    } catch (error) {
      setNotice(error.message);
    } finally {
      event.target.value = "";
    }
  }

  const active = clients.filter((client) => String(client.onboardingStatus).toLowerCase() === "active").length;
  const scores = clients.map((client) => Number(client.creditScore)).filter(Number.isFinite);
  const averageScore = scores.length ? Math.round(scores.reduce((sum, score) => sum + score, 0) / scores.length) : 0;

  return (
    <main className="shell">
      <header className="topbar">
        <div><p className="eyebrow">PORTFOLIO MANAGEMENT</p><h1>Credit Clients</h1></div>
        <label className="upload-button" htmlFor="csvFile">Upload CSV</label>
        <input id="csvFile" type="file" accept=".csv,text/csv" hidden onChange={uploadCsv} />
      </header>

      {notice && <div className="notice">{notice}</div>}

      <section className="stats" aria-label="Client summary">
        <article><span>Total clients</span><strong>{clients.length}</strong></article>
        <article><span>Active</span><strong>{active}</strong></article>
        <article><span>Average score</span><strong>{averageScore}</strong></article>
        <article><span>Countries</span><strong>{countries.length}</strong></article>
      </section>

      <section className="table-card">
        <div className="toolbar">
          <div><h2>Client directory</h2><p>{filtered.length} of {clients.length} clients</p></div>
          <div className="filters">
            <input type="search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search directory..." aria-label="Search directory" />
            <select value={country} onChange={(event) => setCountry(event.target.value)} aria-label="Filter by country">
              <option value="">All countries</option>{countries.map((item) => <option key={item}>{item}</option>)}
            </select>
            <select value={status} onChange={(event) => setStatus(event.target.value)} aria-label="Filter by status">
              <option value="">All statuses</option>{statuses.map((item) => <option key={item}>{item}</option>)}
            </select>
            <select value={rating} onChange={(event) => setRating(event.target.value)} aria-label="Filter by rating">
              <option value="">All ratings</option>{ratings.map((item) => <option key={item}>{item}</option>)}
            </select>
          </div>
        </div>
        <div className="table-wrap">
          <table>
            <thead><tr>
              {[["clientName", "Client"], ["industry", "Industry"], ["country", "Country"], ["creditScore", "Score"], ["creditRating", "Rating"], ["annualRevenueInr", "Revenue (INR)"], ["creditExposureInr", "Exposure (INR)"], ["packageName", "Package"], ["onboardingStatus", "Status"]].map(([key, label]) =>
                <th key={key}><button className="sort-button" onClick={() => changeSort(key)}>{label}<span>{sort.key === key ? (sort.direction === "asc" ? " ▲" : " ▼") : ""}</span></button></th>
              )}
            </tr></thead>
            <tbody>{visible.map((client) => (
              <tr key={client.clientId}>
                <td><div className="client-name">{client.clientName}</div><div className="client-id">{client.clientId}</div></td>
                <td>{client.industry}</td><td>{client.country}</td><td><span className="score">{client.creditScore}</span></td>
                <td><span className="rating">{client.creditRating}</span></td><td>{formatCurrency(client.annualRevenueInr)}</td>
                <td>{formatCurrency(client.creditExposureInr)}</td><td>{client.packageName}</td>
                <td><span className={`status ${String(client.onboardingStatus).toLowerCase() === "active" ? "status-active" : "status-review"}`}>{client.onboardingStatus}</span></td>
              </tr>
            ))}</tbody>
          </table>
        </div>
        {loading && <div className="empty-state">Loading clients...</div>}
        {!loading && !visible.length && <div className="empty-state">No clients match your filters.</div>}
        <div className="pagination">
          <button disabled={page === 1} onClick={() => setPage((value) => value - 1)}>Previous</button>
          <span>Page {page} of {totalPages}</span>
          <button disabled={page === totalPages} onClick={() => setPage((value) => value + 1)}>Next</button>
        </div>
      </section>
    </main>
  );
}
