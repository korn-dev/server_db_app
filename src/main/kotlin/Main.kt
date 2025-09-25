import io.javalin.Javalin
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.sql.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Модели данных
data class Record(
    val id: Long = 0,
    val name: String,
    val value: String,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

data class RecordRequest(
    val name: String,
    val value: String
)

// JSON утилиты
object JsonUtils {
    val mapper: ObjectMapper = ObjectMapper().registerKotlinModule()
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun toJson(obj: Any): String = mapper.writeValueAsString(obj)

    inline fun <reified T> fromJson(json: String): T = mapper.readValue(json, T::class.java)

    fun recordToMap(record: Record): Map<String, Any> = mapOf(
        "id" to record.id,
        "name" to record.name,
        "value" to record.value,
        "createdAt" to record.createdAt.format(dateFormatter)
    )

    fun recordsToMap(records: List<Record>): List<Map<String, Any>> =
        records.map { recordToMap(it) }
}

// Сервис базы данных
class DatabaseService {
    private val url = "jdbc:sqlite:records.db"
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    init {
        createTable()
    }

    private fun getConnection(): Connection {
        return DriverManager.getConnection(url)
    }

    private fun createTable() {
        val sql = """
            CREATE TABLE IF NOT EXISTS records (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                value TEXT NOT NULL,
                created_at TEXT NOT NULL
            )
        """.trimIndent()

        getConnection().use { conn ->
            conn.createStatement().execute(sql)
        }
    }

    fun createRecord(name: String, value: String): Record {
        val sql = "INSERT INTO records (name, value, created_at) VALUES (?, ?, ?)"
        val now = LocalDateTime.now()

        getConnection().use { conn ->
            conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                stmt.setString(1, name)
                stmt.setString(2, value)
                stmt.setString(3, now.format(dateFormatter))
                stmt.executeUpdate()

                val generatedKeys = stmt.generatedKeys
                if (generatedKeys.next()) {
                    return Record(
                        id = generatedKeys.getLong(1),
                        name = name,
                        value = value,
                        createdAt = now
                    )
                }
            }
        }
        throw RuntimeException("Failed to create record")
    }

    fun getAllRecords(): List<Record> {
        val sql = "SELECT * FROM records ORDER BY created_at DESC"
        val records = mutableListOf<Record>()

        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery(sql)
                while (rs.next()) {
                    records.add(
                        Record(
                            id = rs.getLong("id"),
                            name = rs.getString("name"),
                            value = rs.getString("value"),
                            createdAt = LocalDateTime.parse(
                                rs.getString("created_at"),
                                dateFormatter
                            )
                        )
                    )
                }
            }
        }
        return records
    }

    fun getRecordById(id: Long): Record? {
        val sql = "SELECT * FROM records WHERE id = ?"

        getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, id)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    return Record(
                        id = rs.getLong("id"),
                        name = rs.getString("name"),
                        value = rs.getString("value"),
                        createdAt = LocalDateTime.parse(rs.getString("created_at"), dateFormatter)
                    )
                }
            }
        }
        return null
    }
}

// HTML генератор
class HtmlGenerator {
    fun generateIndexPage(records: List<Record>): String {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Records App</title>
                <style>
                    :root {
                        --md-sys-color-primary: #6750A4;
                        --md-sys-color-surface: #FFFBFE;
                        --md-sys-color-on-surface: #1C1B1F;
                        --md-sys-color-surface-container: #F3EDF7;
                        --glass-bg: rgba(255, 255, 255, 0.25);
                        --glass-border: rgba(255, 255, 255, 0.18);
                        --shadow-1: 0 1px 3px rgba(0,0,0,0.12), 0 1px 2px rgba(0,0,0,0.24);
                        --shadow-2: 0 3px 6px rgba(0,0,0,0.15), 0 2px 4px rgba(0,0,0,0.12);
                        --shadow-3: 0 10px 20px rgba(0,0,0,0.15), 0 3px 6px rgba(0,0,0,0.10);
                        --backdrop-blur: blur(12px);
                    }

                    body { 
                        font-family: 'Roboto', 'Segoe UI', system-ui, sans-serif; 
                        margin: 0;
                        padding: 40px 20px;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        min-height: 100vh;
                        color: var(--md-sys-color-on-surface);
                    }
                    
                    .container { 
                        max-width: 900px; 
                        margin: 0 auto;
                        background: var(--glass-bg);
                        backdrop-filter: var(--backdrop-blur);
                        -webkit-backdrop-filter: var(--backdrop-blur);
                        border: 1px solid var(--glass-border);
                        padding: 40px;
                        border-radius: 24px;
                        box-shadow: var(--shadow-3);
                    }
                    
                    h1 { 
                        color: white;
                        text-align: center;
                        margin-bottom: 40px;
                        font-weight: 700;
                        font-size: 2.5em;
                        text-shadow: 0 2px 4px rgba(0,0,0,0.3);
                        letter-spacing: -0.5px;
                    }
                    
                    .form-container {
                        background: var(--md-sys-color-surface-container);
                        padding: 32px;
                        border-radius: 20px;
                        margin-bottom: 40px;
                        box-shadow: var(--shadow-1);
                        border: 1px solid rgba(255,255,255,0.3);
                    }
                    
                    .form-group {
                        margin-bottom: 24px;
                    }
                    
                    label {
                        display: block;
                        margin-bottom: 8px;
                        font-weight: 600;
                        color: var(--md-sys-color-primary);
                        font-size: 0.95em;
                        letter-spacing: 0.5px;
                    }
                    
                    input, textarea {
                        width: 100%;
                        padding: 16px;
                        border: 2px solid #E7E0EC;
                        border-radius: 12px;
                        box-sizing: border-box;
                        font-size: 16px;
                        background: var(--md-sys-color-surface);
                        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
                        font-family: inherit;
                    }
                    
                    input:focus, textarea:focus {
                        outline: none;
                        border-color: var(--md-sys-color-primary);
                        box-shadow: 0 0 0 3px rgba(103, 80, 164, 0.15);
                        transform: translateY(-2px);
                    }
                    
                    textarea {
                        height: 120px;
                        resize: vertical;
                    }
                    
                    button {
                        background: var(--md-sys-color-primary);
                        color: white;
                        padding: 16px 32px;
                        border: none;
                        border-radius: 100px;
                        cursor: pointer;
                        font-size: 16px;
                        font-weight: 600;
                        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
                        box-shadow: 0 2px 8px rgba(103, 80, 164, 0.3);
                        letter-spacing: 0.5px;
                        text-transform: uppercase;
                    }
                    
                    button:hover {
                        background: #5A4A8C;
                        transform: translateY(-2px);
                        box-shadow: 0 4px 16px rgba(103, 80, 164, 0.4);
                    }
                    
                    button:active {
                        transform: translateY(0);
                    }
                    
                    .record {
                        background: var(--glass-bg);
                        backdrop-filter: var(--backdrop-blur);
                        -webkit-backdrop-filter: var(--backdrop-blur);
                        border: 1px solid var(--glass-border);
                        padding: 24px;
                        margin: 20px 0;
                        border-radius: 20px;
                        box-shadow: var(--shadow-1);
                        transition: all 0.3s ease;
                    }
                    
                    .record:hover {
                        transform: translateY(-2px);
                        box-shadow: var(--shadow-2);
                    }
                    
                    .record-header {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        margin-bottom: 12px;
                    }
                    
                    .record-name {
                        font-weight: 700;
                        font-size: 1.3em;
                        color: white;
                        text-shadow: 0 1px 2px rgba(0,0,0,0.3);
                    }
                    
                    .record-id {
                        color: rgba(255,255,255,0.8);
                        font-size: 0.85em;
                        background: rgba(255,255,255,0.2);
                        padding: 4px 12px;
                        border-radius: 12px;
                        backdrop-filter: blur(10px);
                    }
                    
                    .record-value {
                        margin: 16px 0;
                        color: rgba(255,255,255,0.9);
                        line-height: 1.6;
                        font-size: 1.05em;
                    }
                    
                    .record-date {
                        color: rgba(255,255,255,0.7);
                        font-size: 0.8em;
                        text-align: right;
                        font-style: italic;
                    }
                    
                    .no-records {
                        text-align: center;
                        color: rgba(255,255,255,0.8);
                        font-style: italic;
                        padding: 60px 40px;
                        background: var(--glass-bg);
                        backdrop-filter: var(--backdrop-blur);
                        border-radius: 20px;
                        border: 1px solid var(--glass-border);
                    }
                    
                    .api-links {
                        margin-top: 40px;
                        padding: 24px;
                        background: var(--glass-bg);
                        backdrop-filter: var(--backdrop-blur);
                        border-radius: 16px;
                        border: 1px solid var(--glass-border);
                    }
                    
                    .api-links h3 {
                        margin-top: 0;
                        color: white;
                        font-size: 1.2em;
                    }
                    
                    .api-links code {
                        background: rgba(255,255,255,0.2);
                        color: white;
                        padding: 6px 12px;
                        border-radius: 8px;
                        font-family: 'JetBrains Mono', monospace;
                        font-size: 0.9em;
                        border: 1px solid rgba(255,255,255,0.3);
                    }
                    
                    .api-links p {
                        margin: 12px 0;
                        color: rgba(255,255,255,0.9);
                    }
                    
                    /* Анимации */
                    @keyframes fadeIn {
                        from { opacity: 0; transform: translateY(20px); }
                        to { opacity: 1; transform: translateY(0); }
                    }
                    
                    .record {
                        animation: fadeIn 0.6s ease-out;
                    }
                    
                    .container {
                        animation: fadeIn 0.8s ease-out;
                    }
                    
                    /* Адаптивность */
                    @media (max-width: 768px) {
                        body {
                            padding: 20px 10px;
                        }
                        
                        .container {
                            padding: 24px;
                            border-radius: 16px;
                        }
                        
                        .form-container {
                            padding: 24px;
                        }
                        
                        .record-header {
                            flex-direction: column;
                            align-items: flex-start;
                            gap: 8px;
                        }
                        
                        h1 {
                            font-size: 2em;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>📝 Records Application</h1>
                    
                    <div class="form-container">
                        <h2>Add New Record</h2>
                        <form id="recordForm">
                            <div class="form-group">
                                <label for="name">Name:</label>
                                <input type="text" id="name" name="name" required placeholder="Enter record name">
                            </div>
                            <div class="form-group">
                                <label for="value">Value:</label>
                                <textarea id="value" name="value" required placeholder="Enter record value"></textarea>
                            </div>
                            <button type="submit">Add Record</button>
                        </form>
                    </div>
                    
                    <h2>All Records (${records.size})</h2>
                    
                    ${if (records.isEmpty()) """
                        <div class="no-records">
                            No records yet. Add your first record above!
                        </div>
                    """ else records.joinToString("") { record ->
            """
                        <div class="record">
                            <div class="record-header">
                                <span class="record-name">${escapeHtml(record.name)}</span>
                                <span class="record-id">ID: ${record.id}</span>
                            </div>
                            <div class="record-value">${escapeHtml(record.value)}</div>
                            <div class="record-date">Created: ${record.createdAt}</div>
                        </div>
                        """
        }}
                    
                    <div class="api-links">
                        <h3>API Endpoints:</h3>
                        <p><strong>GET</strong> <code>/api/records</code> - Get all records (JSON)</p>
                        <p><strong>POST</strong> <code>/api/records</code> - Create new record (JSON)</p>
                        <p><strong>GET</strong> <code>/api/records/{id}</code> - Get record by ID (JSON)</p>
                    </div>
                </div>
                
                <script>
                    document.getElementById('recordForm').addEventListener('submit', async function(e) {
                        e.preventDefault();
                        
                        const formData = new FormData(this);
                        const name = formData.get('name').toString().trim();
                        const value = formData.get('value').toString().trim();
                        
                        if (!name || !value) {
                            alert('Please fill in both fields');
                            return;
                        }
                        
                        const record = { name, value };
                        
                        try {
                            const response = await fetch('/api/records', {
                                method: 'POST',
                                headers: {
                                    'Content-Type': 'application/json',
                                },
                                body: JSON.stringify(record)
                            });
                            
                            if (response.ok) {
                                // Очищаем форму и обновляем страницу
                                this.reset();
                                window.location.reload();
                            } else {
                                const error = await response.json();
                                alert('Error: ' + error.error);
                            }
                        } catch (error) {
                            alert('Error: ' + error.message);
                        }
                    });
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}

// Главное приложение
fun main() {
    val databaseService = DatabaseService()
    val htmlGenerator = HtmlGenerator()

    val app = Javalin.create { config ->
        config.http.defaultContentType = "application/json"
    }.start(8080)

    // REST API - Создание записи
    app.post("/api/records") { ctx ->
        try {
            val recordRequest = JsonUtils.fromJson<RecordRequest>(ctx.body())

            if (recordRequest.name.isBlank() || recordRequest.value.isBlank()) {
                ctx.status(400).json(mapOf("error" to "Name and value are required"))
                return@post
            }

            val record = databaseService.createRecord(recordRequest.name, recordRequest.value)
            val recordMap = JsonUtils.recordToMap(record)
            ctx.status(201).json(recordMap)
        } catch (e: Exception) {
            ctx.status(400).json(mapOf("error" to "Invalid JSON data: ${e.message}"))
        }
    }

    // REST API - Получение всех записей
    app.get("/api/records") { ctx ->
        try {
            val records = databaseService.getAllRecords()
            val recordsMap = JsonUtils.recordsToMap(records)
            ctx.json(recordsMap)
        } catch (e: Exception) {
            ctx.status(500).json(mapOf("error" to "Internal server error: ${e.message}"))
        }
    }

    // Получение записи по ID
    app.get("/api/records/{id}") { ctx ->
        try {
            val id = ctx.pathParam("id").toLongOrNull()
            if (id == null) {
                ctx.status(400).json(mapOf("error" to "Invalid ID format"))
                return@get
            }

            val record = databaseService.getRecordById(id)
            if (record != null) {
                val recordMap = JsonUtils.recordToMap(record)
                ctx.json(recordMap)
            } else {
                ctx.status(404).json(mapOf("error" to "Record not found"))
            }
        } catch (e: Exception) {
            ctx.status(500).json(mapOf("error" to "Internal server error: ${e.message}"))
        }
    }

    // HTML страница
    app.get("/") { ctx ->
        try {
            val records = databaseService.getAllRecords()
            val html = htmlGenerator.generateIndexPage(records)
            ctx.html(html)
        } catch (e: Exception) {
            ctx.status(500).html("""
                <html>
                    <body>
                        <h1>Error loading page</h1>
                        <p>${e.message}</p>
                    </body>
                </html>
            """.trimIndent())
        }
    }

    // Удаление записи
    app.delete("/api/records/{id}") { ctx ->
        try {
            val id = ctx.pathParam("id").toLongOrNull()
            if (id == null) {
                ctx.status(400).json(mapOf("error" to "Invalid ID format"))
                return@delete
            }

            // Здесь можно добавить функциональность удаления
            ctx.status(501).json(mapOf("error" to "Delete functionality not implemented"))
        } catch (e: Exception) {
            ctx.status(500).json(mapOf("error" to "Internal server error: ${e.message}"))
        }
    }

    println("🚀 Server started at http://localhost:8080")
    println("📊 Database file: records.db")
    println("")
    println("Available endpoints:")
    println("  GET  /                 - HTML page with records")
    println("  GET  /api/records      - Get all records (JSON)")
    println("  POST /api/records      - Create new record (JSON)")
    println("  GET  /api/records/{id} - Get record by ID (JSON)")
    println("")
    println("Press Ctrl+C to stop the server")
}