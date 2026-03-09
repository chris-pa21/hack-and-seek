<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>NoteKeeper</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: 'Courier New', monospace;
            background: #0a0f1e;
            color: #e0e0e0;
            min-height: 100vh;
            display: flex;
        }
        .sidebar {
            width: 220px;
            background: #111827;
            border-right: 1px solid #1e3a5f;
            padding: 1.5rem 1rem;
        }
        .sidebar h2 {
            color: #3b82f6;
            font-size: 1.1rem;
            margin-bottom: 1rem;
        }
        .sidebar a {
            display: block;
            color: #9ca3af;
            text-decoration: none;
            padding: 0.5rem 0.75rem;
            border-radius: 4px;
            margin-bottom: 0.25rem;
            font-size: 0.9rem;
        }
        .sidebar a:hover, .sidebar a.active {
            background: #1f2937;
            color: #3b82f6;
        }
        .content {
            flex: 1;
            padding: 2rem;
        }
        .content h1 {
            color: #3b82f6;
            margin-bottom: 1rem;
        }
        .note-content {
            background: #111827;
            border: 1px solid #1e3a5f;
            border-radius: 8px;
            padding: 1.5rem;
            white-space: pre-wrap;
            font-size: 0.95rem;
            line-height: 1.6;
            color: #d1d5db;
        }
        .error {
            color: #ef4444;
            background: #1f1215;
            border: 1px solid #7f1d1d;
            border-radius: 8px;
            padding: 1rem;
        }
    </style>
</head>
<body>
    <div class="sidebar">
        <h2>&gt; NoteKeeper</h2>
        <a href="?page=welcome" <?= ($_GET['page'] ?? '') === 'welcome' ? 'class="active"' : '' ?>>Welcome</a>
        <a href="?page=network" <?= ($_GET['page'] ?? '') === 'network' ? 'class="active"' : '' ?>>Network</a>
        <a href="?page=todo" <?= ($_GET['page'] ?? '') === 'todo' ? 'class="active"' : '' ?>>TODO</a>
    </div>
    <div class="content">
        <h1>Notes</h1>
        <div class="note-content"><?php
            $page = $_GET['page'] ?? 'welcome';

            // VULNERABLE: user input used directly in file path without sanitization
            $file = "/var/www/notes/" . $page . ".txt";

            if (file_exists($file)) {
                echo htmlspecialchars(file_get_contents($file));
            } else {
                echo '<span class="error">Note not found: ' . htmlspecialchars($page) . '</span>';
            }
        ?></div>
    </div>
</body>
</html>
