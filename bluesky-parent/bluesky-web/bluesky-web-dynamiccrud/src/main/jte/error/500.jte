@param String result

<!DOCTYPE html>
<html lang="ko" data-theme="bluesky">
<head>
        <meta charset="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <title>500</title>
        <link href="/main.css" rel="stylesheet" />
        <script type="text/javascript" src="/webjars/theme-change/index.js"></script>
</head>
<body class="min-h-screen bg-base-100 text-base-content">
        <main role="alert" class="mx-auto max-w-xl px-6 py-24">
                <p class="font-mono text-sm text-base-content/60">500</p>
                <h1 class="mt-2 text-xl font-medium">서버에서 오류가 발생했습니다</h1>
                <p class="mt-4 font-mono text-sm text-base-content/70 break-all">${result}</p>
                <a class="btn btn-sm mt-8" href="/">처음으로</a>
        </main>
</body>
</html>
