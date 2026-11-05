# DAYBOOK – Personal Expense Tracker (Java Desktop Application)

A complete, medium-level Java desktop application for tracking personal income, managing expenses, and visualizing spending habits. Built as a college academic project with a clean, layered Object-Oriented architecture, **Java Swing** GUI, and local **SQLite** database via **JDBC**.

---

## 1. Project Introduction & Objectives

College students and individuals often struggle to keep track of daily outlays across canteen food, books, transport, and hostel charges. **DAYBOOK** provides an offline, self-contained desktop system that records transactions, categorizes cash flows, visualizes monthly habits, and calculates remaining balances without requiring internet access or external database servers.

### Core Objectives:
1. **Record Transactions**: Log daily income (scholarships, stipends, allowances) and expenses.
2. **Categorize Spending**: Classify transactions under Food, Transport, Education, Bills, Healthcare, etc.
3. **Automated Analytics**: Real-time balance calculations, monthly totals, and spending comparison with previous months.
4. **Data Integrity & Security**: Use PreparedStatements to safeguard against SQL injection and enforce field validation.
5. **Zero Cloud Dependencies**: Store data locally in an embedded SQLite database (`expense_tracker.db`).

---

## 2. Technology Stack

| Component | Technology | Version | Purpose |
|---|---|---|---|
| **Language** | Java (JDK) | 17 LTS or later | Core application language |
| **GUI Framework** | Java Swing / AWT | Built-in | Desktop user interface |
| **Database** | SQLite | 3.x | Lightweight embedded SQL database |
| **Driver** | Xerial SQLite JDBC | 3.45.1.0 | JDBC database connectivity |
| **Chart Visualization**| JFreeChart / Custom Java2D | 1.5.4 | Analytical bar charts & category distribution |
| **Build Tool** | Apache Maven | 3.8+ | Dependency management & compilation |
| **Testing** | JUnit Jupiter | 5.10.2 | Automated unit & integration tests |
| **IDEs Supported** | IntelliJ IDEA, VS Code, Eclipse, NetBeans | Cross-platform | Full compatibility |

---

## 3. UI/UX Design System

DAYBOOK rejects generic, noisy AI dashboards and adopts an authentic **editorial desktop software** aesthetic:
- **Main Background**: `#F4F0E8` (Warm antique linen)
- **Content Background**: `#FBF9F4` (Warm parchment)
- **Sidebar**: `#E8E0D3` (Muted putty)
- **Primary Text**: `#292820` (Dark charcoal espresso)
- **Secondary Text**: `#777267` (Muted slate stone)
- **Borders**: `#D8D0C4` (Thin 1px crisp lines)
- **Income Accent**: `#777D5A` (Muted olive green — positive metrics)
- **Expense Accent**: `#B9785B` (Soft terracotta — expenditures)
- **Zero Blue Policy**: No blue buttons, glows, or generic corporate accents.
- **Form & Typography**: Standard SansSerif / Arial typography, sharp corners, and functional layouts.

---

## 4. Layered Architecture

```
com.daybook/
├── Main.java                        # Application entry point (EDT bootstrap)
├── database/
│   ├── DatabaseConnection.java      # Singleton SQLite connection & schema initialization
│   └── DatabaseException.java       # Custom database checked exception
├── model/
│   ├── Transaction.java             # Entity model (Encapsulation, getters/setters)
│   ├── MonthlyComparison.java       # Analytical metrics value object
│   └── ValidationException.java     # Custom validation exception
├── dao/
│   ├── TransactionDAO.java          # Abstraction interface for data persistence
│   └── TransactionDAOImpl.java      # Concrete JDBC PreparedStatement implementation
├── service/
│   └── TransactionService.java      # Business logic, validations, & metrics calculations
└── ui/
    ├── MainFrame.java               # 1200x750 frame with fixed sidebar and CardLayout
    ├── UITheme.java                 # Centralized color palette, borders, & typography
    ├── DashboardPanel.java          # Overview metrics, comparison indicator & recent list
    ├── AddTransactionPanel.java     # Data entry form with instant validation
    ├── TransactionPanel.java        # Structured JTable with multi-filter & CRUD actions
    ├── EditTransactionDialog.java   # Modal dialog for modifying records
    ├── ReportsPanel.java            # Monthly savings rate & category breakdown tables
    ├── SettingsPanel.java           # Diagnostics, clear data, and academic demo seed
    └── components/
        ├── SpendingBarChart.java    # Java2D high-fidelity spending chart
        └── CategoryBreakdownPanel.java # Category horizontal proportion meters
```

---

## 5. Database Schema

The SQLite database file `expense_tracker.db` is automatically created on first launch in the working directory:

```sql
CREATE TABLE IF NOT EXISTS transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    amount REAL NOT NULL,
    type TEXT NOT NULL,           -- 'Income' or 'Expense'
    category TEXT NOT NULL,       -- 'Food', 'Transport', 'Education', etc.
    payment_method TEXT,          -- 'Cash', 'UPI', 'Debit Card', 'Credit Card', 'Bank Transfer'
    transaction_date TEXT NOT NULL, -- ISO-8601 'YYYY-MM-DD'
    created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_transactions_date ON transactions(transaction_date);
CREATE INDEX IF NOT EXISTS idx_transactions_type ON transactions(type);
CREATE INDEX IF NOT EXISTS idx_transactions_category ON transactions(category);
```

---

## 6. Java OOP Concepts Demonstrated (Viva / Project Evaluation)

1. **Encapsulation**:
   - `Transaction` entity maintains private fields (`id`, `title`, `amount`, `type`, etc.) exposed strictly through guarded getters and setters.
   - `DatabaseConnection` keeps its connection instance private and manages connection pooling through synchronized static accessors.

2. **Abstraction & Interfaces**:
   - `TransactionDAO` interface decouples the application's business logic from raw SQL mechanics, allowing the storage engine to be swapped without modifying the UI or Service layers.

3. **Inheritance & Polymorphism**:
   - UI panels extend `JPanel` and override lifecycle hooks (`paintComponent`, layout configurations).
   - Custom exceptions (`DatabaseException`, `ValidationException`) inherit from standard Java `Exception`.

4. **Exception Handling**:
   - Rigorous `try-with-resources` statements ensure `Connection`, `PreparedStatement`, and `ResultSet` objects are closed automatically to prevent memory leaks and database locks.
   - Distinguishes business validation errors (`ValidationException`) from low-level SQL connectivity faults (`DatabaseException`).

5. **Design Patterns**:
   - **Singleton Pattern**: `DatabaseConnection` guarantees a single shared connection instance.
   - **DAO (Data Access Object) Pattern**: Separates database access code from business services.
   - **Service Layer Pattern**: Centralizes calculation of net balance, month-over-month comparison, and category metrics.

6. **Event-Driven Programming & Thread Safety**:
   - Swing components communicate via `ActionListener`, `MouseAdapter`, and `DocumentListener`.
   - The application launches on the **Swing Event Dispatch Thread (EDT)** using `SwingUtilities.invokeLater()`.

---

## 7. How to Build & Run

### Prerequisites
- **Java Development Kit (JDK) 17** or higher installed (`java -version`).
- **Apache Maven 3.8+** installed (`mvn -version`).

### Method A: Run with Maven (Quickest)
```bash
# 1. Clone or navigate to the project directory
cd PersonalExpenseTracker

# 2. Compile and execute directly
mvn clean compile exec:java
```

### Method B: Package into Standalone Executable JAR
```bash
# Build fat jar with dependencies
mvn clean package

# Run the standalone JAR (works on Windows, macOS, and Linux)
java -jar target/PersonalExpenseTracker-1.0.0-jar-with-dependencies.jar
```

### Method C: Running in IntelliJ IDEA
1. Open IntelliJ IDEA → **File** → **Open...** → Select the `PersonalExpenseTracker` directory.
2. IntelliJ will automatically detect `pom.xml` and import dependencies.
3. Open `src/main/java/com/daybook/Main.java`.
4. Click the green **Run ▶** icon beside `public static void main`.

### Method D: Running in Visual Studio Code
1. Open the `PersonalExpenseTracker` folder in VS Code.
2. Install the **Extension Pack for Java** (Microsoft).
3. Navigate to `src/main/java/com/daybook/Main.java`.
4. Click **Run** above the `main` method.

---

## 8. Running Automated Tests

Run the test suite to verify all CRUD operations, mathematical balances, and validation logic:

```bash
mvn test
```

Expected result:
```
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 9. Academic Evaluation Notes & Sample Data

To quickly showcase the application to faculty with realistic data:
1. Launch the application.
2. Navigate to **Settings** in the left sidebar.
3. Click **Load Sample Academic Data**.
4. Return to **Overview** to observe populated KPI cards, monthly trends, comparison percentage indicators, and category charts.

---
*Developed with Java 17, Java Swing, and SQLite.*
