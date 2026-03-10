<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>PingMe - Network Diagnostics</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: 'Courier New', monospace;
            background: #0a0f1e;
            color: #e0e0e0;
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .container {
            background: #111827;
            border: 1px solid #1e3a5f;
            border-radius: 8px;
            padding: 2rem;
            width: 600px;
            max-width: 90%;
        }
        h1 {
            color: #3b82f6;
            margin-bottom: 0.5rem;
        }
        p.subtitle {
            color: #6b7280;
            margin-bottom: 1.5rem;
            font-size: 0.9rem;
        }
        label {
            display: block;
            margin-bottom: 0.5rem;
            color: #9ca3af;
        }
        input[type="text"] {
            width: 100%;
            padding: 0.75rem;
            background: #1f2937;
            border: 1px solid #374151;
            border-radius: 4px;
            color: #e0e0e0;
            font-family: 'Courier New', monospace;
            font-size: 1rem;
            margin-bottom: 1rem;
        }
        input[type="text"]:focus {
            outline: none;
            border-color: #3b82f6;
        }
        button {
            background: #3b82f6;
            color: white;
            border: none;
            padding: 0.75rem 1.5rem;
            border-radius: 4px;
            cursor: pointer;
            font-family: 'Courier New', monospace;
            font-size: 1rem;
        }
        button:hover { background: #2563eb; }
        .output {
            margin-top: 1.5rem;
            background: #000;
            border: 1px solid #374151;
            border-radius: 4px;
            padding: 1rem;
            white-space: pre-wrap;
            font-size: 0.85rem;
            max-height: 300px;
            overflow-y: auto;
            color: #10b981;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>&gt; PingMe</h1>
        <p class="subtitle">Network Diagnostics Tool</p>

        <form method="POST">
            <label for="host">Enter a hostname or IP to ping:</label>
            <input type="text" id="host" name="host" placeholder="e.g. 8.8.8.8" required>
            <button type="submit">Ping</button>
        </form>

        <?php if ($_SERVER['REQUEST_METHOD'] === 'POST' && !empty($_POST['host'])): ?>
            <div class="output"><?php
                $host = $_POST['host'];
                // VULNERABLE: user input passed directly into shell command
                $output = shell_exec("ping -c 3 " . $host . " 2>&1");
                echo htmlspecialchars($output);
            ?></div>
        <?php endif; ?>
    </div>
</body>
</html>
