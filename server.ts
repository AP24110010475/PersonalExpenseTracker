import express, { Request, Response } from 'express';
import { createServer as createViteServer } from 'vite';
import Database from 'better-sqlite3';
import path from 'path';
import fs from 'fs';
import { exec } from 'child_process';
import JSZip from 'jszip';

const app = express();
const PORT = process.env.PORT ? parseInt(process.env.PORT, 10) : 3000;

app.use(express.json());

// Resolve database path (shared with Java application)
const dbDir = path.resolve(process.cwd(), 'PersonalExpenseTracker');
if (!fs.existsSync(dbDir)) {
  fs.mkdirSync(dbDir, { recursive: true });
}
const dbPath = path.resolve(dbDir, 'expense_tracker.db');

const db = new Database(dbPath);

// Initialize SQLite Schema
db.exec(`
  CREATE TABLE IF NOT EXISTS transactions (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      title TEXT NOT NULL,
      amount REAL NOT NULL,
      type TEXT NOT NULL,
      category TEXT NOT NULL,
      payment_method TEXT,
      transaction_date TEXT NOT NULL,
      created_at TEXT DEFAULT CURRENT_TIMESTAMP
  );

  CREATE INDEX IF NOT EXISTS idx_transactions_date ON transactions(transaction_date);
  CREATE INDEX IF NOT EXISTS idx_transactions_type ON transactions(type);
  CREATE INDEX IF NOT EXISTS idx_transactions_category ON transactions(category);
`);

// Seed initial academic demo data if database is brand new
const countRow = db.prepare('SELECT COUNT(*) as count FROM transactions').get() as { count: number };
if (countRow.count === 0) {
  seedAcademicDemoData();
}

function seedAcademicDemoData() {
  const now = new Date();
  const currentYearMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  const prevDate = new Date(now.getFullYear(), now.getMonth() - 1, 1);
  const prevYearMonth = `${prevDate.getFullYear()}-${String(prevDate.getMonth() + 1).padStart(2, '0')}`;

  const insertStmt = db.prepare(`
    INSERT INTO transactions (title, amount, type, category, payment_method, transaction_date)
    VALUES (?, ?, ?, ?, ?, ?)
  `);

  const initialRows = [
    // Previous month records
    ['College Semester Tuition', 35000.0, 'Expense', 'Education', 'Bank Transfer', `${prevYearMonth}-04`],
    ['Monthly Fellowship / Stipend', 45000.0, 'Income', 'Salary', 'Bank Transfer', `${prevYearMonth}-01`],
    ['Hostel & Mess Advance', 12000.0, 'Expense', 'Bills', 'UPI', `${prevYearMonth}-06`],
    ['Textbooks & Stationery', 2800.0, 'Expense', 'Education', 'Debit Card', `${prevYearMonth}-10`],
    ['Local Metro & Bus Pass', 1500.0, 'Expense', 'Transport', 'UPI', `${prevYearMonth}-12`],
    ['Grocery & Provisions', 4200.0, 'Expense', 'Food', 'Cash', `${prevYearMonth}-18`],
    ['Freelance Web Project', 18500.0, 'Income', 'Salary', 'UPI', `${prevYearMonth}-22`],
    ['Weekend Movie & Cafe', 1450.0, 'Expense', 'Entertainment', 'UPI', `${prevYearMonth}-25`],

    // Current month records
    ['Monthly Fellowship / Stipend', 45000.0, 'Income', 'Salary', 'Bank Transfer', `${currentYearMonth}-01`],
    ['Campus Canteen & Meals', 3450.0, 'Expense', 'Food', 'UPI', `${currentYearMonth}-02`],
    ['Monthly High-Speed Internet', 999.0, 'Expense', 'Bills', 'UPI', `${currentYearMonth}-03`],
    ['Online Research Certification', 3200.0, 'Expense', 'Education', 'Credit Card', `${currentYearMonth}-04`],
    ['Campus Cab Fare', 480.0, 'Expense', 'Transport', 'UPI', `${currentYearMonth}-05`],
    ['Pharmacy & Health Essentials', 850.0, 'Expense', 'Healthcare', 'Debit Card', `${currentYearMonth}-05`]
  ];

  const transaction = db.transaction(() => {
    for (const row of initialRows) {
      insertStmt.run(...row);
    }
  });
  transaction();
}

// ----------------------------------------------------------------------
// API Routes
// ----------------------------------------------------------------------

// 1. Get filtered transactions
app.get('/api/transactions', (req: Request, res: Response) => {
  try {
    const { query, type, category, startDate, endDate, sort } = req.query as Record<string, string>;

    let sql = 'SELECT * FROM transactions WHERE 1=1';
    const params: any[] = [];

    if (query && query.trim()) {
      sql += ' AND LOWER(title) LIKE ?';
      params.push(`%${query.trim().toLowerCase()}%`);
    }

    if (type && type !== 'All') {
      sql += ' AND type = ?';
      params.push(type.trim());
    }

    if (category && category !== 'All') {
      sql += ' AND category = ?';
      params.push(category.trim());
    }

    if (startDate && startDate.trim()) {
      sql += ' AND transaction_date >= ?';
      params.push(startDate.trim());
    }

    if (endDate && endDate.trim()) {
      sql += ' AND transaction_date <= ?';
      params.push(endDate.trim());
    }

    if (sort === 'Oldest First') {
      sql += ' ORDER BY transaction_date ASC, id ASC';
    } else {
      sql += ' ORDER BY transaction_date DESC, id DESC';
    }

    const rows = db.prepare(sql).all(...params);
    res.json({ success: true, data: rows });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
});

// 2. Add transaction
app.post('/api/transactions', (req: Request, res: Response) => {
  try {
    const { title, amount, type, category, payment_method, transaction_date } = req.body;

    if (!title || !title.trim()) {
      return res.status(400).json({ success: false, error: 'Description / Title cannot be empty.' });
    }
    const numAmount = parseFloat(amount);
    if (isNaN(numAmount) || numAmount <= 0) {
      return res.status(400).json({ success: false, error: 'Amount must be greater than zero.' });
    }
    if (!['Income', 'Expense'].includes(type)) {
      return res.status(400).json({ success: false, error: 'Transaction type must be Income or Expense.' });
    }
    if (!category || !category.trim()) {
      return res.status(400).json({ success: false, error: 'Category is required.' });
    }
    if (!transaction_date || !/^\d{4}-\d{2}-\d{2}$/.test(transaction_date.trim())) {
      return res.status(400).json({ success: false, error: 'Date must be formatted as YYYY-MM-DD.' });
    }

    const stmt = db.prepare(`
      INSERT INTO transactions (title, amount, type, category, payment_method, transaction_date)
      VALUES (?, ?, ?, ?, ?, ?)
    `);

    const result = stmt.run(title.trim(), numAmount, type, category.trim(), payment_method || 'Cash', transaction_date.trim());
    res.json({ success: true, id: result.lastInsertRowid });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
});

// 3. Update transaction
app.put('/api/transactions/:id', (req: Request, res: Response) => {
  try {
    const id = parseInt(req.params.id, 10);
    const { title, amount, type, category, payment_method, transaction_date } = req.body;

    if (!title || !title.trim()) {
      return res.status(400).json({ success: false, error: 'Description cannot be empty.' });
    }
    const numAmount = parseFloat(amount);
    if (isNaN(numAmount) || numAmount <= 0) {
      return res.status(400).json({ success: false, error: 'Amount must be greater than zero.' });
    }

    const stmt = db.prepare(`
      UPDATE transactions
      SET title = ?, amount = ?, type = ?, category = ?, payment_method = ?, transaction_date = ?
      WHERE id = ?
    `);

    const result = stmt.run(title.trim(), numAmount, type, category.trim(), payment_method || 'Cash', transaction_date.trim(), id);
    if (result.changes === 0) {
      return res.status(404).json({ success: false, error: 'Transaction not found.' });
    }
    res.json({ success: true });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
});

// 4. Delete transaction
app.delete('/api/transactions/:id', (req: Request, res: Response) => {
  try {
    const id = parseInt(req.params.id, 10);
    const stmt = db.prepare('DELETE FROM transactions WHERE id = ?');
    stmt.run(id);
    res.json({ success: true });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
});

// 5. Clear all transactions
app.post('/api/transactions/clear', (req: Request, res: Response) => {
  try {
    db.prepare('DELETE FROM transactions').run();
    try {
      db.prepare("DELETE FROM sqlite_sequence WHERE name='transactions'").run();
    } catch (ignored) {}
    res.json({ success: true });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
});

// 6. Reset with demo data
app.post('/api/transactions/demo', (req: Request, res: Response) => {
  try {
    db.prepare('DELETE FROM transactions').run();
    seedAcademicDemoData();
    res.json({ success: true });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
});

// 7. Overview statistics
app.get('/api/stats/overview', (req: Request, res: Response) => {
  try {
    const all = db.prepare('SELECT * FROM transactions').all() as any[];

    let totalIncome = 0;
    let totalExpenses = 0;
    for (const t of all) {
      if (t.type === 'Income') totalIncome += t.amount;
      else if (t.type === 'Expense') totalExpenses += t.amount;
    }
    const totalBalance = totalIncome - totalExpenses;

    const now = new Date();
    const curYearMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
    const prevDate = new Date(now.getFullYear(), now.getMonth() - 1, 1);
    const prevYearMonth = `${prevDate.getFullYear()}-${String(prevDate.getMonth() + 1).padStart(2, '0')}`;

    let currentMonthExpenses = 0;
    let prevMonthExpenses = 0;
    let hasPreviousData = false;

    for (const t of all) {
      if (t.type === 'Expense') {
        if (t.transaction_date.startsWith(curYearMonth)) {
          currentMonthExpenses += t.amount;
        } else if (t.transaction_date.startsWith(prevYearMonth)) {
          prevMonthExpenses += t.amount;
          hasPreviousData = true;
        }
      }
    }

    const diff = currentMonthExpenses - prevMonthExpenses;
    let pctChange = 0;
    if (hasPreviousData && prevMonthExpenses > 0) {
      pctChange = ((currentMonthExpenses - prevMonthExpenses) / prevMonthExpenses) * 100.0;
    }

    // Category breakdown
    const categoryBreakdown: Record<string, number> = {};
    for (const t of all) {
      if (t.type === 'Expense') {
        categoryBreakdown[t.category] = (categoryBreakdown[t.category] || 0) + t.amount;
      }
    }

    // Monthly Trends (last 6 months)
    const monthlyTrends: Record<string, { income: number; expense: number }> = {};
    for (let i = 5; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
      const ym = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
      monthlyTrends[ym] = { income: 0, expense: 0 };
    }

    for (const t of all) {
      const ym = t.transaction_date.substring(0, 7);
      if (monthlyTrends[ym]) {
        if (t.type === 'Income') monthlyTrends[ym].income += t.amount;
        else if (t.type === 'Expense') monthlyTrends[ym].expense += t.amount;
      }
    }

    // Recent 6 transactions
    const recent = db.prepare('SELECT * FROM transactions ORDER BY transaction_date DESC, id DESC LIMIT 6').all();

    res.json({
      success: true,
      data: {
        totalBalance,
        totalIncome,
        totalExpenses,
        currentMonthExpenses,
        comparison: {
          hasPreviousData,
          previousMonthExpenses: prevMonthExpenses,
          currentMonthExpenses,
          difference: diff,
          percentageChange: pctChange,
          isSpendingMore: hasPreviousData && diff > 0.01,
          isSpendingLess: hasPreviousData && diff < -0.01,
        },
        categoryBreakdown,
        monthlyTrends,
        recent,
        count: all.length,
        dbLocation: dbPath,
      },
    });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
});

// 8. Reports statistics
app.get('/api/stats/reports', (req: Request, res: Response) => {
  try {
    const { period } = req.query as { period?: string };
    const all = db.prepare('SELECT * FROM transactions').all() as any[];

    const filtered = (!period || period === 'All Time')
      ? all
      : all.filter((t) => t.transaction_date.startsWith(period));

    let income = 0;
    let expense = 0;
    const catMap: Record<string, number> = {};

    for (const t of filtered) {
      if (t.type === 'Income') {
        income += t.amount;
      } else if (t.type === 'Expense') {
        expense += t.amount;
        catMap[t.category] = (catMap[t.category] || 0) + t.amount;
      }
    }

    const savings = income - expense;
    const savingsRate = income > 0 ? (savings / income) * 100.0 : 0.0;

    // Highest category
    let highestCategory = 'None';
    let highestAmount = 0;
    for (const [cat, amt] of Object.entries(catMap)) {
      if (amt > highestAmount) {
        highestAmount = amt;
        highestCategory = cat;
      }
    }

    res.json({
      success: true,
      data: {
        income,
        expense,
        savings,
        savingsRate,
        highestCategory,
        highestAmount,
        categoryBreakdown: catMap,
      },
    });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
});

// 9. Get Java project files list and contents
app.get('/api/project/files', (req: Request, res: Response) => {
  try {
    const baseDir = path.resolve(process.cwd(), 'PersonalExpenseTracker');
    const resultFiles: { path: string; name: string; content: string; language: string; description: string }[] = [];

    function scan(dir: string, relPath: string = '') {
      const items = fs.readdirSync(dir);
      for (const item of items) {
        if (item === 'target' || item === '.git' || item.endsWith('.db') || item.endsWith('.class')) continue;
        const full = path.join(dir, item);
        const rel = path.join(relPath, item);
        const stat = fs.statSync(full);
        if (stat.isDirectory()) {
          scan(full, rel);
        } else if (stat.isFile()) {
          const content = fs.readFileSync(full, 'utf-8');
          const ext = path.extname(item);
          let lang = 'plaintext';
          if (ext === '.java') lang = 'java';
          else if (ext === '.xml') lang = 'xml';
          else if (ext === '.md') lang = 'markdown';
          else if (ext === '.json') lang = 'json';

          let desc = '';
          if (item === 'Main.java') desc = 'Application Entry Point & SwingUtilities.invokeLater EDT bootstrap';
          else if (item === 'DatabaseConnection.java') desc = 'Singleton SQLite JDBC connection & table auto-initialization';
          else if (item === 'Transaction.java') desc = 'Encapsulated Entity Model with validations and accessors';
          else if (item === 'TransactionDAO.java') desc = 'DAO Interface defining database access contract';
          else if (item === 'TransactionDAOImpl.java') desc = 'JDBC PreparedStatement implementation protecting against SQL injection';
          else if (item === 'TransactionService.java') desc = 'Business logic, balance calculations & month-over-month metrics';
          else if (item === 'UITheme.java') desc = 'Centralized editorial warm neutral palette, square corners & typography';
          else if (item === 'MainFrame.java') desc = '1200x750 JFrame with fixed sidebar and CardLayout page switcher';
          else if (item === 'DashboardPanel.java') desc = 'Overview metrics, spending comparisons, chart & recent items';
          else if (item === 'AddTransactionPanel.java') desc = 'Form for new transactions with input validation';
          else if (item === 'TransactionPanel.java') desc = 'Structured JTable with multi-filtering, search, and CRUD';
          else if (item === 'ReportsPanel.java') desc = 'Monthly reports, savings rate, and category summaries';
          else if (item === 'SettingsPanel.java') desc = 'System diagnostics, database status, and demo seed action';
          else if (item === 'pom.xml') desc = 'Maven Project Object Model with SQLite, JFreeChart, and Java 17';
          else if (item === 'README.md') desc = 'Comprehensive project documentation, architecture, and viva notes';
          else if (item.endsWith('Test.java')) desc = 'JUnit 5 automated tests for CRUD, mathematics, and persistence';

          resultFiles.push({
            path: rel,
            name: item,
            content,
            language: lang,
            description: desc,
          });
        }
      }
    }

    scan(baseDir);
    res.json({ success: true, files: resultFiles });
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
});

// 10. Run Maven tests / compile on demand
app.post('/api/project/compile', (req: Request, res: Response) => {
  const pomPath = path.resolve(process.cwd(), 'PersonalExpenseTracker/pom.xml');
  exec(`mvn -f "${pomPath}" test`, { timeout: 45000 }, (error, stdout, stderr) => {
    res.json({
      success: !error,
      output: stdout + (stderr ? '\n' + stderr : ''),
      exitCode: error ? error.code : 0,
    });
  });
});

// 11. Download ZIP of the Maven project
app.get('/api/project/download-zip', async (req: Request, res: Response) => {
  try {
    const zip = new JSZip();
    const baseDir = path.resolve(process.cwd(), 'PersonalExpenseTracker');

    function addDirToZip(dir: string, zipFolder: JSZip) {
      const items = fs.readdirSync(dir);
      for (const item of items) {
        if (item === 'target' || item === '.git' || item.endsWith('.class')) continue;
        const full = path.join(dir, item);
        const stat = fs.statSync(full);
        if (stat.isDirectory()) {
          const sub = zipFolder.folder(item)!;
          addDirToZip(full, sub);
        } else {
          const content = fs.readFileSync(full);
          zipFolder.file(item, content);
        }
      }
    }

    addDirToZip(baseDir, zip);
    const content = await zip.generateAsync({ type: 'nodebuffer', compression: 'DEFLATE' });

    res.setHeader('Content-Type', 'application/zip');
    res.setHeader('Content-Disposition', 'attachment; filename="PersonalExpenseTracker-DAYBOOK.zip"');
    res.send(content);
  } catch (error: any) {
    res.status(500).json({ success: false, error: error.message });
  }
});

// ----------------------------------------------------------------------
// Vite Middleware / Static Serving
// ----------------------------------------------------------------------
async function startServer() {
  if (process.env.NODE_ENV === 'production' && fs.existsSync(path.resolve(process.cwd(), 'dist'))) {
    app.use(express.static(path.resolve(process.cwd(), 'dist')));
    app.get('*', (req, res) => {
      res.sendFile(path.resolve(process.cwd(), 'dist', 'index.html'));
    });
  } else {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`[DAYBOOK Web Companion] Running on port ${PORT}`);
  });
}

startServer();
