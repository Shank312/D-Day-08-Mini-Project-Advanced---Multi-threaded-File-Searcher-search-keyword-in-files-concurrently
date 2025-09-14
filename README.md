# Multi-threaded File Searcher (Java)

This is an **advanced Java mini project** that demonstrates practical use of:
- JVM internals (Heap, Stack, GC impact)
- Java Multithreading (`ExecutorService`, `Future`, `Locks`)
- Streams and Lambda Expressions
- Building a real-world concurrent file search utility

---

## 📂 Project Structure
```
java-advanced/day8/
 ├── FileSearcher.java   # Main implementation
 └── README.md           # Documentation
```

---

## 🚀 Features
- Recursively traverses directories
- Spawns concurrent search tasks with a fixed thread pool
- Line-by-line file scanning
- Thread-safe result collection with `ConcurrentLinkedQueue`
- Progress updates while scanning
- Supports case-sensitive & case-insensitive modes
- Skips unreadable or very large files (>200MB)
- Sorts results by file path and line number before display

---

## 🛠️ Requirements
- Java 8 or later

---

## 📝 Usage
### Compile
```bash
cd java-advanced/day8
javac FileSearcher.java
```

### Run
```bash
java FileSearcher <root-directory> <keyword> [threads] [case-insensitive]
```

### Example
```bash
java FileSearcher /home/shankar/projects "TODO" 8 true
```

### Arguments
1. **root-directory** → starting folder to search
2. **keyword** → text to look for
3. **threads** *(optional)* → number of worker threads (default: available processors)
4. **case-insensitive** *(optional)* → `true` or `false` (default: false)

---

## 📊 Sample Output
```
Starting search
Root: /home/shankar/projects
Keyword: 'TODO' (case-insensitive=true)
Threads: 8
Files to scan: 120
Files scanned: 50/120
Files scanned: 100/120

Search completed in 832 ms
Total matches: 7

/home/shankar/projects/Main.java:12: // TODO: refactor method
/home/shankar/projects/utils/FileUtils.java:44: // TODO: add logging
```

---

## 🧠 Concepts Covered
- **JVM Internals**: heap vs stack, GC effect on many short-lived `String` objects
- **Multithreading**: `ExecutorService`, `Future`, `AtomicInteger`, `ReentrantLock`
- **Streams & Lambdas**: `Files.walk`, `.filter(Files::isRegularFile)`, lambda `Callable`

---

## 🔮 Possible Enhancements
- Regex-based search
- Show surrounding context lines
- File-type filters (e.g., `.java`, `.txt`)
- JavaFX GUI with live updates
- Streaming producer-consumer model for very large directories
- Integration with indexing engines (e.g., Apache Lucene)

---

💡 **Learning Goal:** This project blends advanced Java theory with practical concurrency, I/O, and modern language features — preparing you for real-world, high-performance systems.
