import React, { useState, useEffect, useMemo } from 'react';

// Design Palette Constants matching the prompt
// Main BG: #F4F0E8, Content BG: #FBF9F4, Sidebar: #E8E0D3
// Primary Text: #292820, Muted Text: #777267, Borders: #D8D0C4
// Olive: #777D5A, Terracotta: #B9785B, Light Beige: #EDE5D9

interface Transaction {
  id: number;
  title: string;
  amount: number;
  type: 'Income' | 'Expense';
  category: string;
  payment_method: string;
  transaction_date: string;
  created_at?: string;
}

interface OverviewStats {
  totalBalance: number;
  totalIncome: number;
  totalExpenses: number;
  currentMonthExpenses: number;
  comparison: {
    hasPreviousData: boolean;
    previousMonthExpenses: number;
    currentMonthExpenses: number;
    difference: number;
    percentageChange: number;
    isSpendingMore: boolean;
    isSpendingLess: boolean;
  };
  categoryBreakdown: Record<string, number>;
  monthlyTrends: Record<string, { income: number; expense: number }>;
  recent: Transaction[];
  count: number;
  dbLocation: string;
}

interface ReportStats {
  income: number;
  expense: number;
  savings: number;
  savingsRate: number;
  highestCategory: string;
  highestAmount: number;
  categoryBreakdown: Record<string, number>;
}

interface ProjectFile {
  path: string;
  name: string;
  content: string;
  language: string;
  description: string;
}

const CATEGORIES = [
  'Food',
  'Transport',
  'Education',
  'Shopping',
  'Entertainment',
  'Healthcare',
  'Bills',
  'Salary',
  'Other',
];

const PAYMENT_METHODS = [
  'Cash',
  'UPI',
  'Debit Card',
  'Credit Card',
  'Bank Transfer',
];

export default function App() {
  // Top view switcher: 'app' (desktop Swing simulator) | 'code' (Java source code & viva) | 'maven' (live test runner)
  const [activeView, setActiveView] = useState<'app' | 'code' | 'maven'>('app');

  // Desktop App Page navigation
  const [activePage, setActivePage] = useState<string>('Overview');

  // Data states
  const [overview, setOverview] = useState<OverviewStats | null>(null);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [reports, setReports] = useState<ReportStats | null>(null);
  const [selectedReportPeriod, setSelectedReportPeriod] = useState<string>('All Time');
  const [loading, setLoading] = useState<boolean>(true);

  // Filters for Transaction History
  const [searchQuery, setSearchQuery] = useState('');
  const [typeFilter, setTypeFilter] = useState('All');
  const [categoryFilter, setCategoryFilter] = useState('All');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [sortOrder, setSortOrder] = useState<'Newest First' | 'Oldest First'>('Newest First');

  // Add Transaction Form
  const [newType, setNewType] = useState<'Expense' | 'Income'>('Expense');
  const [newTitle, setNewTitle] = useState('');
  const [newAmount, setNewAmount] = useState('');
  const [newCategory, setNewCategory] = useState('Food');
  const [newDate, setNewDate] = useState(() => new Date().toISOString().split('T')[0]);
  const [newMethod, setNewMethod] = useState('UPI');
  const [formFeedback, setFormFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  // Edit Transaction Dialog
  const [editingTx, setEditingTx] = useState<Transaction | null>(null);
  const [editError, setEditError] = useState<string | null>(null);

  // Code Inspector state
  const [projectFiles, setProjectFiles] = useState<ProjectFile[]>([]);
  const [selectedFile, setSelectedFile] = useState<ProjectFile | null>(null);

  // Maven runner state
  const [mavenOutput, setMavenOutput] = useState<string>('');
  const [isCompiling, setIsCompiling] = useState<boolean>(false);

  // Delete confirmation dialog
  const [deleteCandidate, setDeleteCandidate] = useState<Transaction | null>(null);

  // Fetch overview and transactions
  const fetchData = async () => {
    try {
      setLoading(true);
      const [ovRes, txRes] = await Promise.all([
        fetch('/api/stats/overview'),
        fetch(`/api/transactions?query=${encodeURIComponent(searchQuery)}&type=${typeFilter}&category=${categoryFilter}&startDate=${startDate}&endDate=${endDate}&sort=${sortOrder}`)
      ]);
      const ovJson = await ovRes.json();
      const txJson = await txRes.json();

      if (ovJson.success) setOverview(ovJson.data);
      if (txJson.success) setTransactions(txJson.data);
    } catch (err) {
      console.error('Failed to load financial records', err);
    } finally {
      setLoading(false);
    }
  };

  // Fetch report data
  const fetchReportData = async (period: string) => {
    try {
      const res = await fetch(`/api/stats/reports?period=${encodeURIComponent(period)}`);
      const json = await res.json();
      if (json.success) setReports(json.data);
    } catch (err) {
      console.error('Failed to load reports', err);
    }
  };

  // Fetch project files for Code Inspector
  const fetchProjectFiles = async () => {
    try {
      const res = await fetch('/api/project/files');
      const json = await res.json();
      if (json.success && json.files.length > 0) {
        setProjectFiles(json.files);
        if (!selectedFile) {
          // Default to Main.java
          const main = json.files.find((f: ProjectFile) => f.name === 'Main.java') || json.files[0];
          setSelectedFile(main);
        }
      }
    } catch (err) {
      console.error('Failed to load project files', err);
    }
  };

  useEffect(() => {
    fetchData();
    fetchReportData(selectedReportPeriod);
    fetchProjectFiles();
  }, []);

  useEffect(() => {
    fetchData();
  }, [searchQuery, typeFilter, categoryFilter, startDate, endDate, sortOrder]);

  useEffect(() => {
    fetchReportData(selectedReportPeriod);
  }, [selectedReportPeriod]);

  // Currency formatter
  const formatINR = (amt: number) => {
    return '₹' + amt.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  };

  const formatCompactINR = (amt: number) => {
    return '₹' + Math.round(amt).toLocaleString('en-IN');
  };

  // Submit new transaction
  const handleSaveTransaction = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormFeedback(null);

    if (!newTitle.trim()) {
      setFormFeedback({ type: 'error', message: 'Description cannot be empty.' });
      return;
    }
    const val = parseFloat(newAmount);
    if (isNaN(val) || val <= 0) {
      setFormFeedback({ type: 'error', message: 'Amount must be greater than zero.' });
      return;
    }
    if (!newDate) {
      setFormFeedback({ type: 'error', message: 'Valid date is required (YYYY-MM-DD).' });
      return;
    }

    try {
      const res = await fetch('/api/transactions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          title: newTitle.trim(),
          amount: val,
          type: newType,
          category: newCategory,
          payment_method: newMethod,
          transaction_date: newDate,
        }),
      });
      const data = await res.json();
      if (data.success) {
        setFormFeedback({
          type: 'success',
          message: `Transaction saved: "${newTitle}" (${formatINR(val)})`,
        });
        setNewTitle('');
        setNewAmount('');
        fetchData();
        fetchReportData(selectedReportPeriod);
      } else {
        setFormFeedback({ type: 'error', message: data.error || 'Failed to save transaction.' });
      }
    } catch (err: any) {
      setFormFeedback({ type: 'error', message: err.message || 'Network error.' });
    }
  };

  // Submit edit
  const handleUpdateTransaction = async () => {
    if (!editingTx) return;
    setEditError(null);

    if (!editingTx.title.trim()) {
      setEditError('Description cannot be empty.');
      return;
    }
    if (editingTx.amount <= 0) {
      setEditError('Amount must be positive.');
      return;
    }

    try {
      const res = await fetch(`/api/transactions/${editingTx.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(editingTx),
      });
      const json = await res.json();
      if (json.success) {
        setEditingTx(null);
        fetchData();
        fetchReportData(selectedReportPeriod);
      } else {
        setEditError(json.error || 'Failed to update transaction.');
      }
    } catch (err: any) {
      setEditError(err.message || 'Network error');
    }
  };

  // Submit delete
  const handleDeleteTransaction = async (id: number) => {
    try {
      const res = await fetch(`/api/transactions/${id}`, { method: 'DELETE' });
      const json = await res.json();
      if (json.success) {
        setDeleteCandidate(null);
        fetchData();
        fetchReportData(selectedReportPeriod);
      }
    } catch (err) {
      console.error('Delete error', err);
    }
  };

  // Reset database or seed demo data
  const handleSeedDemo = async () => {
    try {
      await fetch('/api/transactions/demo', { method: 'POST' });
      await fetchData();
      await fetchReportData(selectedReportPeriod);
      setActivePage('Overview');
    } catch (err) {
      console.error('Failed to seed demo data', err);
    }
  };

  const handleClearAll = async () => {
    try {
      await fetch('/api/transactions/clear', { method: 'POST' });
      await fetchData();
      await fetchReportData(selectedReportPeriod);
      setActivePage('Overview');
    } catch (err) {
      console.error('Failed to clear database', err);
    }
  };

  // Run live Maven compiler test
  const handleRunMaven = async () => {
    setIsCompiling(true);
    setMavenOutput('Running Apache Maven: mvn -f PersonalExpenseTracker/pom.xml test...\nCompiling Java 17 sources, connecting SQLite JDBC, and executing JUnit 5 suite...');
    try {
      const res = await fetch('/api/project/compile', { method: 'POST' });
      const json = await res.json();
      setMavenOutput(json.output || (json.success ? 'BUILD SUCCESS: All tests passed.' : 'Compilation error.'));
    } catch (err: any) {
      setMavenOutput('Execution error: ' + err.message);
    } finally {
      setIsCompiling(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#EDE5D9] text-[#292820] flex flex-col font-sans select-none">
      {/* Top Banner Navigation Bar */}
      <header className="bg-[#E8E0D3] border-b border-[#D8D0C4] px-6 py-2.5 flex items-center justify-between text-xs">
        <div className="flex items-center space-x-3">
          <span className="font-bold tracking-wider text-sm uppercase text-[#292820]">DAYBOOK</span>
          <span className="text-[#777267]">|</span>
          <span className="text-[#777267]">Personal Expense Tracker — Java Desktop Application</span>
          <span className="px-1.5 py-0.5 bg-[#EDE5D9] border border-[#D8D0C4] text-[#777D5A] font-medium text-[11px]">
            ● Java 17 + Swing + SQLite
          </span>
        </div>

        <div className="flex items-center space-x-2">
          <button
            onClick={() => setActiveView('app')}
            className={`px-3 py-1.5 border border-[#D8D0C4] font-medium transition-colors ${
              activeView === 'app' ? 'bg-[#292820] text-[#FBF9F4]' : 'bg-[#FBF9F4] text-[#292820] hover:bg-[#EDE5D9]'
            }`}
          >
            Desktop App Window
          </button>
          <button
            onClick={() => {
              setActiveView('code');
              if (!selectedFile && projectFiles.length > 0) setSelectedFile(projectFiles[0]);
            }}
            className={`px-3 py-1.5 border border-[#D8D0C4] font-medium transition-colors ${
              activeView === 'code' ? 'bg-[#292820] text-[#FBF9F4]' : 'bg-[#FBF9F4] text-[#292820] hover:bg-[#EDE5D9]'
            }`}
          >
            Java Code & Viva Guide ({projectFiles.length} files)
          </button>
          <button
            onClick={() => {
              setActiveView('maven');
              if (!mavenOutput) handleRunMaven();
            }}
            className={`px-3 py-1.5 border border-[#D8D0C4] font-medium transition-colors ${
              activeView === 'maven' ? 'bg-[#292820] text-[#FBF9F4]' : 'bg-[#FBF9F4] text-[#292820] hover:bg-[#EDE5D9]'
            }`}
          >
            Maven Build & Test
          </button>
          <a
            href="/api/project/download-zip"
            download
            className="px-3 py-1.5 border border-[#777D5A] bg-[#777D5A] text-[#FBF9F4] font-medium hover:bg-[#666B4D] transition-colors flex items-center gap-1"
          >
            <span>⬇</span> Download Maven Project (.ZIP)
          </a>
        </div>
      </header>

      {/* Main Container */}
      <main className="flex-1 p-4 md:p-6 flex justify-center items-start overflow-y-auto">
        {activeView === 'app' && (
          /* Desktop Application Frame (Exact 1200x750 Aspect Ratio & Layout) */
          <div className="w-full max-w-[1240px] bg-[#F4F0E8] border border-[#D8D0C4] shadow-sm flex flex-col overflow-hidden">
            {/* Desktop Window Titlebar */}
            <div className="bg-[#E8E0D3] border-b border-[#D8D0C4] px-4 py-2 flex items-center justify-between text-xs text-[#777267]">
              <div className="flex items-center space-x-2">
                <div className="flex space-x-1.5">
                  <div className="w-3 h-3 bg-[#D8D0C4] border border-[#BDB5A8]"></div>
                  <div className="w-3 h-3 bg-[#D8D0C4] border border-[#BDB5A8]"></div>
                  <div className="w-3 h-3 bg-[#D8D0C4] border border-[#BDB5A8]"></div>
                </div>
                <span className="font-semibold text-[#292820] ml-2">DAYBOOK – Personal Finance Manager</span>
                <span className="text-[11px] text-[#777267]">(Java Swing Frame: 1200 × 750)</span>
              </div>
              <div className="text-[11px] font-mono text-[#777267]">
                Local SQLite: <span className="text-[#292820] font-semibold">{overview?.count || 0} records</span>
              </div>
            </div>

            {/* Desktop Window Body: Sidebar + Main Content */}
            <div className="flex flex-1 min-h-[680px]">
              {/* Left Fixed Sidebar */}
              <aside className="w-60 bg-[#E8E0D3] border-r border-[#D8D0C4] flex flex-col justify-between p-5">
                <div>
                  {/* Brand header */}
                  <div className="mb-8">
                    <h1 className="text-xl font-bold tracking-tight text-[#292820]">DAYBOOK</h1>
                    <p className="text-xs text-[#777267] mt-0.5">Personal Finance Manager</p>
                  </div>

                  {/* Navigation list */}
                  <nav className="space-y-1">
                    {[
                      { name: 'Overview', id: 'Overview' },
                      { name: 'Transactions', id: 'Transactions' },
                      { name: 'Add Transaction', id: 'Add Transaction' },
                      { name: 'Reports', id: 'Reports' },
                      { name: 'Settings', id: 'Settings' },
                    ].map((item) => {
                      const isActive = activePage === item.id;
                      return (
                        <button
                          key={item.id}
                          onClick={() => setActivePage(item.id)}
                          className={`w-full text-left px-3 py-2 text-xs font-medium transition-colors ${
                            isActive
                              ? 'bg-[#D4CBBD] text-[#292820] border-l-4 border-[#292820] font-bold pl-2.5'
                              : 'text-[#292820] hover:bg-[#DFD7CA]'
                          }`}
                        >
                          {item.name}
                        </button>
                      );
                    })}
                  </nav>
                </div>

                {/* Sidebar footer */}
                <div className="pt-4 border-t border-[#D8D0C4] text-[11px] text-[#777267] space-y-1">
                  <div>Academic Project v1.0</div>
                  <div className="text-[#777D5A] font-medium">● Local SQLite Mode</div>
                </div>
              </aside>

              {/* Main Content Area */}
              <div className="flex-1 bg-[#F4F0E8] overflow-y-auto">
                {/* 1. OVERVIEW PAGE */}
                {activePage === 'Overview' && (
                  <div className="p-7 space-y-6">
                    {/* Header */}
                    <div className="flex items-center justify-between border-b border-[#D8D0C4] pb-4">
                      <div>
                        <h2 className="text-lg font-bold text-[#292820]">Financial Overview</h2>
                        <p className="text-xs text-[#777267]">Summary of cash flows, balances, and recent activity.</p>
                      </div>
                      <div className="text-xs text-[#777267]">
                        Date: <span className="font-semibold text-[#292820]">{new Date().toISOString().split('T')[0]}</span>
                      </div>
                    </div>

                    {/* Metric Cards Row */}
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                      <div className="bg-[#FBF9F4] border border-[#D8D0C4] p-4">
                        <div className="text-[11px] font-medium uppercase tracking-wider text-[#777267]">Total Balance</div>
                        <div className="text-xl font-bold text-[#292820] mt-1">
                          {formatINR(overview?.totalBalance || 0)}
                        </div>
                      </div>

                      <div className="bg-[#FBF9F4] border border-[#D8D0C4] p-4">
                        <div className="text-[11px] font-medium uppercase tracking-wider text-[#777267]">Total Income</div>
                        <div className="text-xl font-bold text-[#777D5A] mt-1">
                          {formatINR(overview?.totalIncome || 0)}
                        </div>
                      </div>

                      <div className="bg-[#FBF9F4] border border-[#D8D0C4] p-4">
                        <div className="text-[11px] font-medium uppercase tracking-wider text-[#777267]">Total Expenses</div>
                        <div className="text-xl font-bold text-[#B9785B] mt-1">
                          {formatINR(overview?.totalExpenses || 0)}
                        </div>
                      </div>

                      <div className="bg-[#FBF9F4] border border-[#D8D0C4] p-4">
                        <div className="text-[11px] font-medium uppercase tracking-wider text-[#777267]">Current Month</div>
                        <div className="text-xl font-bold text-[#B9785B] mt-1">
                          {formatINR(overview?.currentMonthExpenses || 0)}
                        </div>
                        <div className="mt-1 text-[11px]">
                          {overview?.comparison?.hasPreviousData ? (
                            overview.comparison.isSpendingMore ? (
                              <span className="text-[#B9785B] font-medium">
                                ▲ +{overview.comparison.percentageChange.toFixed(1)}% vs last mo ({formatCompactINR(overview.comparison.difference)})
                              </span>
                            ) : overview.comparison.isSpendingLess ? (
                              <span className="text-[#777D5A] font-medium">
                                ▼ {overview.comparison.percentageChange.toFixed(1)}% vs last mo ({formatCompactINR(Math.abs(overview.comparison.difference))})
                              </span>
                            ) : (
                              <span className="text-[#777267]">Same as last month</span>
                            )
                          ) : (
                            <span className="text-[#777267]">First month on record</span>
                          )}
                        </div>
                      </div>
                    </div>

                    {/* Middle Two-Column Grid: Trend Chart + Category Breakdown */}
                    <div className="grid grid-cols-1 lg:grid-cols-5 gap-4">
                      {/* Left: Monthly Spending Trend Chart */}
                      <div className="lg:col-span-3 bg-[#FBF9F4] border border-[#D8D0C4] p-4 flex flex-col">
                        <div className="flex items-center justify-between mb-4">
                          <h3 className="text-xs font-bold uppercase tracking-wide text-[#292820]">
                            Monthly Cashflow (Last 6 Months)
                          </h3>
                          <div className="flex items-center space-x-3 text-[11px]">
                            <div className="flex items-center space-x-1">
                              <span className="w-2.5 h-2.5 bg-[#777D5A] border border-[#292820]"></span>
                              <span className="text-[#292820]">Income</span>
                            </div>
                            <div className="flex items-center space-x-1">
                              <span className="w-2.5 h-2.5 bg-[#B9785B] border border-[#292820]"></span>
                              <span className="text-[#292820]">Expense</span>
                            </div>
                          </div>
                        </div>

                        {/* Chart Render */}
                        <div className="h-44 w-full flex items-end justify-between pt-4 pb-2 border-b border-[#D8D0C4] px-2">
                          {overview?.monthlyTrends &&
                            Object.entries(overview.monthlyTrends).map(([month, data]) => {
                              // Calculate max for proportional height
                              const allVals = Object.values(overview.monthlyTrends).flatMap((d) => [d.income, d.expense]);
                              const maxVal = Math.max(...allVals, 5000);

                              const incH = Math.max(2, Math.min(100, (data.income / maxVal) * 100));
                              const expH = Math.max(2, Math.min(100, (data.expense / maxVal) * 100));

                              return (
                                <div key={month} className="flex-1 flex flex-col items-center group relative h-full justify-end px-1">
                                  {/* Bar container */}
                                  <div className="flex items-end space-x-1 h-36">
                                    <div
                                      style={{ height: `${incH}%` }}
                                      className="w-3.5 bg-[#777D5A] border border-[#292820]"
                                      title={`Income: ${formatINR(data.income)}`}
                                    ></div>
                                    <div
                                      style={{ height: `${expH}%` }}
                                      className="w-3.5 bg-[#B9785B] border border-[#292820]"
                                      title={`Expense: ${formatINR(data.expense)}`}
                                    ></div>
                                  </div>
                                  {/* Month label */}
                                  <span className="text-[10px] text-[#777267] mt-2 font-mono">
                                    {month.substring(5)}
                                  </span>
                                </div>
                              );
                            })}
                        </div>
                      </div>

                      {/* Right: Category Breakdown */}
                      <div className="lg:col-span-2 bg-[#FBF9F4] border border-[#D8D0C4] p-4 flex flex-col">
                        <h3 className="text-xs font-bold uppercase tracking-wide text-[#292820] mb-3">
                          Category Breakdown
                        </h3>
                        <div className="flex-1 overflow-y-auto space-y-2.5 pr-1 max-h-48">
                          {overview?.categoryBreakdown && Object.keys(overview.categoryBreakdown).length > 0 ? (
                            Object.entries(overview.categoryBreakdown)
                              .sort((a, b) => b[1] - a[1])
                              .map(([category, amt]) => {
                                const total = overview.totalExpenses || 1;
                                const pct = (amt / total) * 100;
                                return (
                                  <div key={category} className="text-xs">
                                    <div className="flex justify-between text-[11px] mb-1">
                                      <span className="font-medium text-[#292820]">{category}</span>
                                      <span className="text-[#777267]">
                                        {formatCompactINR(amt)} ({pct.toFixed(0)}%)
                                      </span>
                                    </div>
                                    <div className="h-1.5 w-full bg-[#EDE5D9] border border-[#D8D0C4]">
                                      <div
                                        className="h-full bg-[#B9785B]"
                                        style={{ width: `${Math.min(100, pct)}%` }}
                                      ></div>
                                    </div>
                                  </div>
                                );
                              })
                          ) : (
                            <div className="text-xs text-[#777267] py-6 text-center">
                              No expense records registered.
                            </div>
                          )}
                        </div>
                      </div>
                    </div>

                    {/* Bottom: Recent Transactions Table */}
                    <div className="bg-[#FBF9F4] border border-[#D8D0C4] p-4">
                      <div className="flex items-center justify-between mb-3">
                        <h3 className="text-xs font-bold uppercase tracking-wide text-[#292820]">
                          Recent Transactions
                        </h3>
                        <button
                          onClick={() => setActivePage('Transactions')}
                          className="text-xs font-medium text-[#292820] hover:underline"
                        >
                          View All ({overview?.count || 0}) →
                        </button>
                      </div>

                      {overview?.recent && overview.recent.length > 0 ? (
                        <div className="overflow-x-auto border border-[#D8D0C4]">
                          <table className="w-full text-left text-xs border-collapse">
                            <thead>
                              <tr className="bg-[#EDE5D9] border-b border-[#D8D0C4] text-[#777267] font-medium">
                                <th className="p-2.5">Date</th>
                                <th className="p-2.5">Description</th>
                                <th className="p-2.5">Category</th>
                                <th className="p-2.5">Type</th>
                                <th className="p-2.5">Method</th>
                                <th className="p-2.5 text-right">Amount</th>
                              </tr>
                            </thead>
                            <tbody className="divide-y divide-[#D8D0C4]">
                              {overview.recent.map((t) => (
                                <tr key={t.id} className="hover:bg-[#EDE5D9]/50">
                                  <td className="p-2.5 font-mono text-[11px]">{t.transaction_date}</td>
                                  <td className="p-2.5 font-medium text-[#292820]">{t.title}</td>
                                  <td className="p-2.5">{t.category}</td>
                                  <td className="p-2.5">
                                    <span
                                      className={`px-1.5 py-0.5 text-[10px] font-bold border ${
                                        t.type === 'Income'
                                          ? 'border-[#777D5A] text-[#777D5A] bg-[#EDE5D9]'
                                          : 'border-[#B9785B] text-[#B9785B] bg-[#EDE5D9]'
                                      }`}
                                    >
                                      {t.type}
                                    </span>
                                  </td>
                                  <td className="p-2.5 text-[#777267]">{t.payment_method}</td>
                                  <td
                                    className={`p-2.5 text-right font-medium font-mono ${
                                      t.type === 'Income' ? 'text-[#777D5A]' : 'text-[#B9785B]'
                                    }`}
                                  >
                                    {t.type === 'Income' ? '+ ' : '- '}
                                    {formatINR(t.amount)}
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      ) : (
                        /* Empty state */
                        <div className="p-8 text-center border border-dashed border-[#D8D0C4] bg-[#F4F0E8]">
                          <p className="text-sm font-semibold text-[#292820]">No Transactions Recorded</p>
                          <p className="text-xs text-[#777267] mt-1 mb-4">
                            Your expense tracker database is currently empty.
                          </p>
                          <div className="flex justify-center gap-3">
                            <button
                              onClick={() => setActivePage('Add Transaction')}
                              className="px-3.5 py-1.5 bg-[#292820] text-[#FBF9F4] text-xs font-medium hover:bg-[#403E34]"
                            >
                              + Record First Transaction
                            </button>
                            <button
                              onClick={handleSeedDemo}
                              className="px-3.5 py-1.5 border border-[#D8D0C4] bg-[#FBF9F4] text-[#292820] text-xs font-medium hover:bg-[#EDE5D9]"
                            >
                              Load Academic Demo Records
                            </button>
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                )}

                {/* 2. TRANSACTIONS HISTORY PAGE */}
                {activePage === 'Transactions' && (
                  <div className="p-7 space-y-4">
                    <div className="flex items-center justify-between border-b border-[#D8D0C4] pb-4">
                      <div>
                        <h2 className="text-lg font-bold text-[#292820]">Transaction History</h2>
                        <p className="text-xs text-[#777267]">
                          Filter, sort, inspect, edit, or delete transactions from SQLite.
                        </p>
                      </div>
                      <div className="text-xs font-mono text-[#777267]">
                        {transactions.length} records matched
                      </div>
                    </div>

                    {/* Filter Bar Card */}
                    <div className="bg-[#FBF9F4] border border-[#D8D0C4] p-3 text-xs space-y-3">
                      <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
                        <div>
                          <label className="block text-[11px] text-[#777267] mb-1 font-medium">Search Description</label>
                          <input
                            type="text"
                            placeholder="e.g. books, tuition, stipend"
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            className="w-full bg-[#FBF9F4] border border-[#D8D0C4] px-2.5 py-1.5 text-xs text-[#292820] focus:outline-none focus:border-[#292820]"
                          />
                        </div>

                        <div>
                          <label className="block text-[11px] text-[#777267] mb-1 font-medium">Type</label>
                          <select
                            value={typeFilter}
                            onChange={(e) => setTypeFilter(e.target.value)}
                            className="w-full bg-[#FBF9F4] border border-[#D8D0C4] px-2.5 py-1.5 text-xs text-[#292820] focus:outline-none focus:border-[#292820]"
                          >
                            <option value="All">All Types</option>
                            <option value="Income">Income</option>
                            <option value="Expense">Expense</option>
                          </select>
                        </div>

                        <div>
                          <label className="block text-[11px] text-[#777267] mb-1 font-medium">Category</label>
                          <select
                            value={categoryFilter}
                            onChange={(e) => setCategoryFilter(e.target.value)}
                            className="w-full bg-[#FBF9F4] border border-[#D8D0C4] px-2.5 py-1.5 text-xs text-[#292820] focus:outline-none focus:border-[#292820]"
                          >
                            <option value="All">All Categories</option>
                            {CATEGORIES.map((cat) => (
                              <option key={cat} value={cat}>
                                {cat}
                              </option>
                            ))}
                          </select>
                        </div>

                        <div>
                          <label className="block text-[11px] text-[#777267] mb-1 font-medium">Sort Order</label>
                          <select
                            value={sortOrder}
                            onChange={(e) => setSortOrder(e.target.value as any)}
                            className="w-full bg-[#FBF9F4] border border-[#D8D0C4] px-2.5 py-1.5 text-xs text-[#292820] focus:outline-none focus:border-[#292820]"
                          >
                            <option value="Newest First">Newest First</option>
                            <option value="Oldest First">Oldest First</option>
                          </select>
                        </div>
                      </div>

                      <div className="flex flex-wrap items-center justify-between gap-3 pt-2 border-t border-[#D8D0C4]">
                        <div className="flex items-center space-x-2">
                          <span className="text-[11px] text-[#777267]">Date Range:</span>
                          <input
                            type="date"
                            value={startDate}
                            onChange={(e) => setStartDate(e.target.value)}
                            className="bg-[#FBF9F4] border border-[#D8D0C4] px-2 py-1 text-xs text-[#292820] focus:outline-none"
                          />
                          <span className="text-[#777267]">to</span>
                          <input
                            type="date"
                            value={endDate}
                            onChange={(e) => setEndDate(e.target.value)}
                            className="bg-[#FBF9F4] border border-[#D8D0C4] px-2 py-1 text-xs text-[#292820] focus:outline-none"
                          />
                        </div>

                        <div className="flex items-center space-x-2">
                          <button
                            onClick={() => {
                              setSearchQuery('');
                              setTypeFilter('All');
                              setCategoryFilter('All');
                              setStartDate('');
                              setEndDate('');
                              setSortOrder('Newest First');
                            }}
                            className="px-3 py-1 bg-[#FBF9F4] border border-[#D8D0C4] text-[#292820] hover:bg-[#EDE5D9]"
                          >
                            Reset Filters
                          </button>
                          <button
                            onClick={() => setActivePage('Add Transaction')}
                            className="px-3 py-1 bg-[#292820] text-[#FBF9F4] font-medium hover:bg-[#403E34]"
                          >
                            + Add Transaction
                          </button>
                        </div>
                      </div>
                    </div>

                    {/* Table View */}
                    <div className="bg-[#FBF9F4] border border-[#D8D0C4] overflow-hidden">
                      <div className="max-h-[460px] overflow-y-auto">
                        <table className="w-full text-left text-xs border-collapse">
                          <thead className="sticky top-0 bg-[#EDE5D9] border-b border-[#D8D0C4] z-10">
                            <tr className="text-[#777267] font-semibold">
                              <th className="p-2.5 w-12">ID</th>
                              <th className="p-2.5 w-24">Date</th>
                              <th className="p-2.5">Description</th>
                              <th className="p-2.5 w-28">Category</th>
                              <th className="p-2.5 w-20">Type</th>
                              <th className="p-2.5 w-28">Method</th>
                              <th className="p-2.5 w-28 text-right">Amount</th>
                              <th className="p-2.5 w-24 text-center">Actions</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-[#D8D0C4]">
                            {transactions.length > 0 ? (
                              transactions.map((t) => (
                                <tr key={t.id} className="hover:bg-[#EDE5D9]/40">
                                  <td className="p-2.5 text-[#777267] font-mono text-[11px]">#{t.id}</td>
                                  <td className="p-2.5 font-mono text-[11px]">{t.transaction_date}</td>
                                  <td className="p-2.5 font-medium text-[#292820]">{t.title}</td>
                                  <td className="p-2.5 text-[#292820]">{t.category}</td>
                                  <td className="p-2.5">
                                    <span
                                      className={`px-1.5 py-0.5 text-[10px] font-bold border ${
                                        t.type === 'Income'
                                          ? 'border-[#777D5A] text-[#777D5A] bg-[#EDE5D9]'
                                          : 'border-[#B9785B] text-[#B9785B] bg-[#EDE5D9]'
                                      }`}
                                    >
                                      {t.type}
                                    </span>
                                  </td>
                                  <td className="p-2.5 text-[#777267]">{t.payment_method}</td>
                                  <td
                                    className={`p-2.5 text-right font-medium font-mono ${
                                      t.type === 'Income' ? 'text-[#777D5A]' : 'text-[#B9785B]'
                                    }`}
                                  >
                                    {t.type === 'Income' ? '+ ' : '- '}
                                    {formatINR(t.amount)}
                                  </td>
                                  <td className="p-2.5 text-center space-x-1">
                                    <button
                                      onClick={() => setEditingTx({ ...t })}
                                      className="px-2 py-0.5 border border-[#D8D0C4] bg-[#FBF9F4] text-[#292820] hover:bg-[#EDE5D9] text-[11px]"
                                    >
                                      Edit
                                    </button>
                                    <button
                                      onClick={() => setDeleteCandidate(t)}
                                      className="px-2 py-0.5 border border-[#B9785B] bg-[#FBF9F4] text-[#B9785B] hover:bg-[#B9785B] hover:text-white text-[11px]"
                                    >
                                      Del
                                    </button>
                                  </td>
                                </tr>
                              ))
                            ) : (
                              <tr>
                                <td colSpan={8} className="p-8 text-center text-[#777267]">
                                  No transactions match the selected filter criteria.
                                </td>
                              </tr>
                            )}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  </div>
                )}

                {/* 3. ADD TRANSACTION PAGE */}
                {activePage === 'Add Transaction' && (
                  <div className="p-7 space-y-6">
                    <div className="border-b border-[#D8D0C4] pb-4">
                      <h2 className="text-lg font-bold text-[#292820]">Record New Transaction</h2>
                      <p className="text-xs text-[#777267]">
                        Fill in all required fields to commit a new entry to the SQLite database.
                      </p>
                    </div>

                    <div className="max-w-xl bg-[#FBF9F4] border border-[#D8D0C4] p-6 space-y-4">
                      {/* Form Banner */}
                      {formFeedback && (
                        <div
                          className={`p-3 text-xs border ${
                            formFeedback.type === 'success'
                              ? 'bg-[#EDE5D9] border-[#777D5A] text-[#777D5A]'
                              : 'bg-[#EDE5D9] border-[#B9785B] text-[#B9785B]'
                          }`}
                        >
                          {formFeedback.type === 'success' ? '✓ ' : '⚠ '}
                          {formFeedback.message}
                        </div>
                      )}

                      <form onSubmit={handleSaveTransaction} className="space-y-4 text-xs">
                        {/* Transaction Type */}
                        <div>
                          <label className="block text-xs font-semibold text-[#292820] mb-1.5">
                            Transaction Type *
                          </label>
                          <div className="flex space-x-6">
                            <label className="flex items-center space-x-2 cursor-pointer">
                              <input
                                type="radio"
                                name="txType"
                                value="Expense"
                                checked={newType === 'Expense'}
                                onChange={() => {
                                  setNewType('Expense');
                                  if (newCategory === 'Salary') setNewCategory('Food');
                                }}
                                className="accent-[#B9785B]"
                              />
                              <span className="font-medium text-[#B9785B]">Expense</span>
                            </label>
                            <label className="flex items-center space-x-2 cursor-pointer">
                              <input
                                type="radio"
                                name="txType"
                                value="Income"
                                checked={newType === 'Income'}
                                onChange={() => {
                                  setNewType('Income');
                                  setNewCategory('Salary');
                                }}
                                className="accent-[#777D5A]"
                              />
                              <span className="font-medium text-[#777D5A]">Income</span>
                            </label>
                          </div>
                        </div>

                        {/* Description */}
                        <div>
                          <label className="block text-xs font-semibold text-[#292820] mb-1">
                            Title / Description *
                          </label>
                          <input
                            type="text"
                            required
                            placeholder="e.g. Textbooks, Hostel Mess Fee, Stipend"
                            value={newTitle}
                            onChange={(e) => setNewTitle(e.target.value)}
                            className="w-full bg-[#FBF9F4] border border-[#D8D0C4] px-3 py-2 text-xs text-[#292820] focus:outline-none focus:border-[#292820]"
                          />
                        </div>

                        {/* Amount */}
                        <div>
                          <label className="block text-xs font-semibold text-[#292820] mb-1">
                            Amount (Indian Rupees ₹) *
                          </label>
                          <input
                            type="number"
                            step="0.01"
                            min="0.01"
                            required
                            placeholder="0.00"
                            value={newAmount}
                            onChange={(e) => setNewAmount(e.target.value)}
                            className="w-full bg-[#FBF9F4] border border-[#D8D0C4] px-3 py-2 text-xs text-[#292820] focus:outline-none focus:border-[#292820]"
                          />
                        </div>

                        {/* Category */}
                        <div>
                          <label className="block text-xs font-semibold text-[#292820] mb-1">
                            Category *
                          </label>
                          <select
                            value={newCategory}
                            onChange={(e) => setNewCategory(e.target.value)}
                            className="w-full bg-[#FBF9F4] border border-[#D8D0C4] px-3 py-2 text-xs text-[#292820] focus:outline-none focus:border-[#292820]"
                          >
                            {CATEGORIES.map((cat) => (
                              <option key={cat} value={cat}>
                                {cat}
                              </option>
                            ))}
                          </select>
                        </div>

                        {/* Transaction Date */}
                        <div>
                          <label className="block text-xs font-semibold text-[#292820] mb-1">
                            Transaction Date *
                          </label>
                          <input
                            type="date"
                            required
                            value={newDate}
                            onChange={(e) => setNewDate(e.target.value)}
                            className="w-full bg-[#FBF9F4] border border-[#D8D0C4] px-3 py-2 text-xs text-[#292820] focus:outline-none focus:border-[#292820]"
                          />
                        </div>

                        {/* Payment Method */}
                        <div>
                          <label className="block text-xs font-semibold text-[#292820] mb-1">
                            Payment Method
                          </label>
                          <select
                            value={newMethod}
                            onChange={(e) => setNewMethod(e.target.value)}
                            className="w-full bg-[#FBF9F4] border border-[#D8D0C4] px-3 py-2 text-xs text-[#292820] focus:outline-none focus:border-[#292820]"
                          >
                            {PAYMENT_METHODS.map((pm) => (
                              <option key={pm} value={pm}>
                                {pm}
                              </option>
                            ))}
                          </select>
                        </div>

                        {/* Action Buttons */}
                        <div className="pt-4 border-t border-[#D8D0C4] flex justify-end space-x-2">
                          <button
                            type="button"
                            onClick={() => {
                              setNewTitle('');
                              setNewAmount('');
                              setFormFeedback(null);
                            }}
                            className="px-4 py-2 bg-[#FBF9F4] border border-[#D8D0C4] text-[#292820] hover:bg-[#EDE5D9]"
                          >
                            Clear Form
                          </button>
                          <button
                            type="submit"
                            className="px-4 py-2 bg-[#292820] text-[#FBF9F4] font-medium hover:bg-[#403E34]"
                          >
                            Save Transaction
                          </button>
                        </div>
                      </form>
                    </div>
                  </div>
                )}

                {/* 4. REPORTS PAGE */}
                {activePage === 'Reports' && (
                  <div className="p-7 space-y-6">
                    <div className="flex items-center justify-between border-b border-[#D8D0C4] pb-4">
                      <div>
                        <h2 className="text-lg font-bold text-[#292820]">Reports & Analytics</h2>
                        <p className="text-xs text-[#777267]">
                          Period analytics, spending distributions, and net savings rate.
                        </p>
                      </div>

                      <div className="flex items-center space-x-2 text-xs">
                        <span className="text-[#777267] font-medium">Select Period:</span>
                        <select
                          value={selectedReportPeriod}
                          onChange={(e) => setSelectedReportPeriod(e.target.value)}
                          className="bg-[#FBF9F4] border border-[#D8D0C4] px-2.5 py-1.5 text-xs text-[#292820] focus:outline-none"
                        >
                          <option value="All Time">All Time</option>
                          {overview?.monthlyTrends &&
                            Object.keys(overview.monthlyTrends).map((ym) => (
                              <option key={ym} value={ym}>
                                {ym}
                              </option>
                            ))}
                        </select>
                      </div>
                    </div>

                    {/* KPI Cards */}
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                      <div className="bg-[#FBF9F4] border border-[#D8D0C4] p-4">
                        <div className="text-[11px] font-medium uppercase tracking-wider text-[#777267]">Period Income</div>
                        <div className="text-xl font-bold text-[#777D5A] mt-1">
                          {formatINR(reports?.income || 0)}
                        </div>
                      </div>

                      <div className="bg-[#FBF9F4] border border-[#D8D0C4] p-4">
                        <div className="text-[11px] font-medium uppercase tracking-wider text-[#777267]">Period Expenses</div>
                        <div className="text-xl font-bold text-[#B9785B] mt-1">
                          {formatINR(reports?.expense || 0)}
                        </div>
                      </div>

                      <div className="bg-[#FBF9F4] border border-[#D8D0C4] p-4">
                        <div className="text-[11px] font-medium uppercase tracking-wider text-[#777267]">Net Savings</div>
                        <div
                          className={`text-xl font-bold mt-1 ${
                            (reports?.savings || 0) >= 0 ? 'text-[#777D5A]' : 'text-[#B9785B]'
                          }`}
                        >
                          {formatINR(reports?.savings || 0)}
                        </div>
                      </div>

                      <div className="bg-[#FBF9F4] border border-[#D8D0C4] p-4">
                        <div className="text-[11px] font-medium uppercase tracking-wider text-[#777267]">Savings Rate</div>
                        <div
                          className={`text-xl font-bold mt-1 ${
                            (reports?.savingsRate || 0) >= 0 ? 'text-[#777D5A]' : 'text-[#B9785B]'
                          }`}
                        >
                          {(reports?.savingsRate || 0).toFixed(1)}%
                        </div>
                      </div>
                    </div>

                    {/* Highest Spending Category Banner */}
                    <div className="bg-[#EDE5D9] border border-[#D8D0C4] p-3 flex items-center justify-between text-xs">
                      <span className="font-medium text-[#777267] uppercase tracking-wide">Highest Spending Category:</span>
                      <span className="font-bold text-[#B9785B]">
                        {reports?.highestCategory !== 'None'
                          ? `${reports?.highestCategory} — ${formatINR(reports?.highestAmount || 0)}`
                          : 'No expense transactions recorded'}
                      </span>
                    </div>

                    {/* Breakdown Visual and Table */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      {/* Visual Category Proportion */}
                      <div className="bg-[#FBF9F4] border border-[#D8D0C4] p-4">
                        <h3 className="text-xs font-bold uppercase tracking-wide text-[#292820] mb-3">
                          Proportional Expenditure
                        </h3>
                        <div className="space-y-3">
                          {reports?.categoryBreakdown && Object.keys(reports.categoryBreakdown).length > 0 ? (
                            Object.entries(reports.categoryBreakdown)
                              .sort((a, b) => b[1] - a[1])
                              .map(([category, amt]) => {
                                const total = reports.expense || 1;
                                const pct = (amt / total) * 100;
                                return (
                                  <div key={category} className="text-xs">
                                    <div className="flex justify-between text-[11px] mb-1">
                                      <span className="font-medium text-[#292820]">{category}</span>
                                      <span className="text-[#777267]">
                                        {formatINR(amt)} ({pct.toFixed(1)}%)
                                      </span>
                                    </div>
                                    <div className="h-2 w-full bg-[#EDE5D9] border border-[#D8D0C4]">
                                      <div
                                        className="h-full bg-[#B9785B]"
                                        style={{ width: `${Math.min(100, pct)}%` }}
                                      ></div>
                                    </div>
                                  </div>
                                );
                              })
                          ) : (
                            <p className="text-xs text-[#777267]">No expenses recorded for this period.</p>
                          )}
                        </div>
                      </div>

                      {/* Detailed Table */}
                      <div className="bg-[#FBF9F4] border border-[#D8D0C4] p-4">
                        <h3 className="text-xs font-bold uppercase tracking-wide text-[#292820] mb-3">
                          Category Breakdown Table
                        </h3>
                        <div className="border border-[#D8D0C4]">
                          <table className="w-full text-left text-xs border-collapse">
                            <thead>
                              <tr className="bg-[#EDE5D9] border-b border-[#D8D0C4] text-[#777267] font-semibold">
                                <th className="p-2">Category</th>
                                <th className="p-2 text-right">Amount</th>
                                <th className="p-2 text-right">Share (%)</th>
                              </tr>
                            </thead>
                            <tbody className="divide-y divide-[#D8D0C4]">
                              {reports?.categoryBreakdown && Object.keys(reports.categoryBreakdown).length > 0 ? (
                                Object.entries(reports.categoryBreakdown)
                                  .sort((a, b) => b[1] - a[1])
                                  .map(([category, amt]) => {
                                    const total = reports.expense || 1;
                                    const pct = (amt / total) * 100;
                                    return (
                                      <tr key={category}>
                                        <td className="p-2 font-medium text-[#292820]">{category}</td>
                                        <td className="p-2 text-right font-mono text-[#B9785B]">{formatINR(amt)}</td>
                                        <td className="p-2 text-right text-[#777267]">{pct.toFixed(1)}%</td>
                                      </tr>
                                    );
                                  })
                              ) : (
                                <tr>
                                  <td colSpan={3} className="p-4 text-center text-[#777267]">
                                    No records available.
                                  </td>
                                </tr>
                              )}
                            </tbody>
                          </table>
                        </div>
                      </div>
                    </div>
                  </div>
                )}

                {/* 5. SETTINGS PAGE */}
                {activePage === 'Settings' && (
                  <div className="p-7 space-y-6">
                    <div className="border-b border-[#D8D0C4] pb-4">
                      <h2 className="text-lg font-bold text-[#292820]">System Settings & Data</h2>
                      <p className="text-xs text-[#777267]">
                        Application configuration, runtime diagnostics, and SQLite maintenance.
                      </p>
                    </div>

                    <div className="space-y-4 max-w-2xl">
                      {/* Application Info */}
                      <div className="bg-[#FBF9F4] border border-[#D8D0C4] p-4 text-xs">
                        <h3 className="font-bold text-[#292820] mb-3 uppercase tracking-wide text-[11px] text-[#777267]">
                          Application Information
                        </h3>
                        <div className="grid grid-cols-2 gap-2 text-xs">
                          <span className="text-[#777267]">Application:</span>
                          <span className="font-semibold text-[#292820]">DAYBOOK – Personal Finance Manager</span>
                          <span className="text-[#777267]">Version:</span>
                          <span className="text-[#292820]">1.0.0 (Academic Project Edition)</span>
                          <span className="text-[#777267]">Language & UI:</span>
                          <span className="text-[#292820]">Java 17 LTS / Java Swing Desktop GUI</span>
                          <span className="text-[#777267]">Active Currency:</span>
                          <span className="text-[#292820]">Indian Rupee (INR — ₹)</span>
                        </div>
                      </div>

                      {/* Database Info */}
                      <div className="bg-[#FBF9F4] border border-[#D8D0C4] p-4 text-xs">
                        <h3 className="font-bold text-[#292820] mb-3 uppercase tracking-wide text-[11px] text-[#777267]">
                          Database Diagnostics
                        </h3>
                        <div className="grid grid-cols-2 gap-2 text-xs">
                          <span className="text-[#777267]">Database Engine:</span>
                          <span className="font-semibold text-[#292820]">SQLite 3 (Embedded JDBC)</span>
                          <span className="text-[#777267]">Database File:</span>
                          <span className="font-mono text-[11px] text-[#292820]">PersonalExpenseTracker/expense_tracker.db</span>
                          <span className="text-[#777267]">Stored Records:</span>
                          <span className="font-semibold text-[#292820]">{overview?.count || 0} transactions</span>
                          <span className="text-[#777267]">Table Schema:</span>
                          <span className="text-[#777267]">transactions (id, title, amount, type, category, ...)</span>
                        </div>
                      </div>

                      {/* Maintenance Actions */}
                      <div className="bg-[#FBF9F4] border border-[#D8D0C4] p-4 text-xs">
                        <h3 className="font-bold text-[#292820] mb-3 uppercase tracking-wide text-[11px] text-[#777267]">
                          Data Maintenance
                        </h3>
                        <div className="flex flex-wrap gap-3">
                          <button
                            onClick={handleSeedDemo}
                            className="px-3.5 py-1.5 border border-[#D8D0C4] bg-[#EDE5D9] text-[#292820] font-medium hover:bg-[#D4CBBD]"
                          >
                            Load Sample Academic Data
                          </button>
                          <button
                            onClick={handleClearAll}
                            className="px-3.5 py-1.5 border border-[#B9785B] bg-[#FBF9F4] text-[#B9785B] font-medium hover:bg-[#B9785B] hover:text-white"
                          >
                            Clear All Transaction Data
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Edit Transaction Modal */}
        {editingTx && (
          <div className="fixed inset-0 bg-black/40 flex items-center justify-center p-4 z-50">
            <div className="bg-[#FBF9F4] border border-[#D8D0C4] shadow-lg max-w-md w-full p-6 space-y-4 text-xs">
              <div className="flex items-center justify-between border-b border-[#D8D0C4] pb-2">
                <h3 className="text-sm font-bold text-[#292820]">Edit Transaction #{editingTx.id}</h3>
                <button
                  onClick={() => setEditingTx(null)}
                  className="text-sm font-bold text-[#777267] hover:text-[#292820]"
                >
                  ✕
                </button>
              </div>

              {editError && (
                <div className="p-2 bg-[#EDE5D9] border border-[#B9785B] text-[#B9785B]">
                  ⚠ {editError}
                </div>
              )}

              <div className="space-y-3">
                <div>
                  <label className="block text-[11px] text-[#777267] font-medium mb-1">Type</label>
                  <div className="flex space-x-4">
                    <label className="flex items-center space-x-1.5">
                      <input
                        type="radio"
                        checked={editingTx.type === 'Expense'}
                        onChange={() => setEditingTx({ ...editingTx, type: 'Expense' })}
                        className="accent-[#B9785B]"
                      />
                      <span className="text-[#B9785B] font-medium">Expense</span>
                    </label>
                    <label className="flex items-center space-x-1.5">
                      <input
                        type="radio"
                        checked={editingTx.type === 'Income'}
                        onChange={() => setEditingTx({ ...editingTx, type: 'Income' })}
                        className="accent-[#777D5A]"
                      />
                      <span className="text-[#777D5A] font-medium">Income</span>
                    </label>
                  </div>
                </div>

                <div>
                  <label className="block text-[11px] text-[#777267] font-medium mb-1">Description</label>
                  <input
                    type="text"
                    value={editingTx.title}
                    onChange={(e) => setEditingTx({ ...editingTx, title: e.target.value })}
                    className="w-full bg-[#FBF9F4] border border-[#D8D0C4] px-2.5 py-1.5 text-xs text-[#292820] focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block text-[11px] text-[#777267] font-medium mb-1">Amount (₹)</label>
                  <input
                    type="number"
                    step="0.01"
                    value={editingTx.amount}
                    onChange={(e) => setEditingTx({ ...editingTx, amount: parseFloat(e.target.value) || 0 })}
                    className="w-full bg-[#FBF9F4] border border-[#D8D0C4] px-2.5 py-1.5 text-xs text-[#292820] focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block text-[11px] text-[#777267] font-medium mb-1">Category</label>
                  <select
                    value={editingTx.category}
                    onChange={(e) => setEditingTx({ ...editingTx, category: e.target.value })}
                    className="w-full bg-[#FBF9F4] border border-[#D8D0C4] px-2.5 py-1.5 text-xs text-[#292820] focus:outline-none"
                  >
                    {CATEGORIES.map((cat) => (
                      <option key={cat} value={cat}>
                        {cat}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-[11px] text-[#777267] font-medium mb-1">Date (YYYY-MM-DD)</label>
                  <input
                    type="date"
                    value={editingTx.transaction_date}
                    onChange={(e) => setEditingTx({ ...editingTx, transaction_date: e.target.value })}
                    className="w-full bg-[#FBF9F4] border border-[#D8D0C4] px-2.5 py-1.5 text-xs text-[#292820] focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block text-[11px] text-[#777267] font-medium mb-1">Payment Method</label>
                  <select
                    value={editingTx.payment_method}
                    onChange={(e) => setEditingTx({ ...editingTx, payment_method: e.target.value })}
                    className="w-full bg-[#FBF9F4] border border-[#D8D0C4] px-2.5 py-1.5 text-xs text-[#292820] focus:outline-none"
                  >
                    {PAYMENT_METHODS.map((pm) => (
                      <option key={pm} value={pm}>
                        {pm}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="pt-3 border-t border-[#D8D0C4] flex justify-end space-x-2">
                <button
                  onClick={() => setEditingTx(null)}
                  className="px-3 py-1.5 border border-[#D8D0C4] bg-[#FBF9F4] text-[#292820] hover:bg-[#EDE5D9]"
                >
                  Cancel
                </button>
                <button
                  onClick={handleUpdateTransaction}
                  className="px-3 py-1.5 bg-[#292820] text-[#FBF9F4] font-medium hover:bg-[#403E34]"
                >
                  Update Record
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Delete Confirmation Modal */}
        {deleteCandidate && (
          <div className="fixed inset-0 bg-black/40 flex items-center justify-center p-4 z-50">
            <div className="bg-[#FBF9F4] border border-[#D8D0C4] shadow-lg max-w-sm w-full p-5 space-y-4 text-xs">
              <h3 className="text-sm font-bold text-[#B9785B]">Confirm Deletion</h3>
              <p className="text-xs text-[#292820]">
                Are you sure you want to permanently delete transaction #{deleteCandidate.id}:
                <br />
                <span className="font-semibold">"{deleteCandidate.title}"</span> ({formatINR(deleteCandidate.amount)})?
              </p>
              <div className="pt-2 border-t border-[#D8D0C4] flex justify-end space-x-2">
                <button
                  onClick={() => setDeleteCandidate(null)}
                  className="px-3 py-1.5 border border-[#D8D0C4] bg-[#FBF9F4] text-[#292820] hover:bg-[#EDE5D9]"
                >
                  Cancel
                </button>
                <button
                  onClick={() => handleDeleteTransaction(deleteCandidate.id)}
                  className="px-3 py-1.5 bg-[#B9785B] text-white font-medium hover:bg-[#A3664B]"
                >
                  Delete Record
                </button>
              </div>
            </div>
          </div>
        )}

        {/* CODE INSPECTOR & VIVA GUIDE VIEW */}
        {activeView === 'code' && (
          <div className="w-full max-w-[1240px] bg-[#FBF9F4] border border-[#D8D0C4] p-5 space-y-4">
            <div className="flex items-center justify-between border-b border-[#D8D0C4] pb-3">
              <div>
                <h2 className="text-base font-bold text-[#292820]">Java Project Source Code & Viva Guide</h2>
                <p className="text-xs text-[#777267]">
                  Examine every Java class, design pattern, and OOP principle implemented for academic evaluation.
                </p>
              </div>
              <a
                href="/api/project/download-zip"
                download
                className="px-3 py-1.5 bg-[#777D5A] text-[#FBF9F4] text-xs font-medium hover:bg-[#666B4D]"
              >
                Download Complete Project .ZIP
              </a>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              {/* File list sidebar */}
              <div className="border border-[#D8D0C4] bg-[#F4F0E8] p-2 max-h-[640px] overflow-y-auto">
                <div className="text-[11px] font-bold text-[#777267] uppercase tracking-wider px-2 py-1">
                  Maven Project Files
                </div>
                <div className="space-y-0.5 mt-1">
                  {projectFiles.map((f) => {
                    const isSelected = selectedFile?.path === f.path;
                    return (
                      <button
                        key={f.path}
                        onClick={() => setSelectedFile(f)}
                        className={`w-full text-left px-2 py-1.5 text-xs font-mono transition-colors ${
                          isSelected
                            ? 'bg-[#292820] text-[#FBF9F4] font-semibold'
                            : 'text-[#292820] hover:bg-[#EDE5D9]'
                        }`}
                      >
                        <div className="truncate">{f.name}</div>
                        <div className={`text-[10px] font-sans truncate ${isSelected ? 'text-[#EDE5D9]' : 'text-[#777267]'}`}>
                          {f.path.includes('ui')
                            ? 'UI Layer'
                            : f.path.includes('service')
                            ? 'Service Layer'
                            : f.path.includes('dao')
                            ? 'DAO Layer'
                            : f.path.includes('database')
                            ? 'Database Layer'
                            : f.path.includes('model')
                            ? 'Model Layer'
                            : 'Config'}
                        </div>
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Code viewer & explanation */}
              <div className="md:col-span-3 flex flex-col border border-[#D8D0C4] bg-[#FBF9F4]">
                {selectedFile ? (
                  <>
                    <div className="bg-[#E8E0D3] border-b border-[#D8D0C4] px-4 py-2 flex items-center justify-between text-xs">
                      <div>
                        <span className="font-mono font-bold text-[#292820]">{selectedFile.path}</span>
                        <div className="text-[11px] text-[#777267]">{selectedFile.description}</div>
                      </div>
                      <span className="text-[10px] uppercase font-mono px-2 py-0.5 bg-[#FBF9F4] border border-[#D8D0C4]">
                        {selectedFile.language}
                      </span>
                    </div>

                    <pre className="p-4 text-xs font-mono text-[#292820] overflow-x-auto max-h-[580px] overflow-y-auto leading-relaxed bg-[#FBF9F4]">
                      {selectedFile.content}
                    </pre>
                  </>
                ) : (
                  <div className="p-8 text-center text-xs text-[#777267]">Select a file from the left to view.</div>
                )}
              </div>
            </div>
          </div>
        )}

        {/* MAVEN BUILD & TEST VIEW */}
        {activeView === 'maven' && (
          <div className="w-full max-w-[1240px] bg-[#FBF9F4] border border-[#D8D0C4] p-5 space-y-4">
            <div className="flex items-center justify-between border-b border-[#D8D0C4] pb-3">
              <div>
                <h2 className="text-base font-bold text-[#292820]">Apache Maven Compiler & JUnit 5 Test Runner</h2>
                <p className="text-xs text-[#777267]">
                  Executes `mvn test` directly inside the Java 17 runtime container against the SQLite test database.
                </p>
              </div>

              <button
                onClick={handleRunMaven}
                disabled={isCompiling}
                className="px-4 py-1.5 bg-[#292820] text-[#FBF9F4] text-xs font-medium hover:bg-[#403E34] disabled:opacity-50"
              >
                {isCompiling ? 'Running Maven...' : '▶ Re-Run Maven Build & Tests'}
              </button>
            </div>

            <div className="bg-[#292820] text-[#EDE5D9] p-4 font-mono text-xs border border-[#D8D0C4] max-h-[600px] overflow-y-auto whitespace-pre-wrap leading-relaxed">
              {mavenOutput || 'Click "Re-Run Maven Build & Tests" to execute the build cycle.'}
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
