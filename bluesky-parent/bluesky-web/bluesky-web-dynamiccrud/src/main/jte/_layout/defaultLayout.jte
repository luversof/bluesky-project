@param gg.jte.Content content

<!DOCTYPE html>
<html lang="ko" data-theme="bluesky">

<head>
        <meta charset="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <title>Dynamic CRUD</title>
        <link href="/main.css" rel="stylesheet" />
        <script type="text/javascript" src="/webjars/theme-change/index.js"></script>
        <script type="text/javascript" src="/webjars/htmx.org/dist/htmx.min.js"></script>
        <script src="https://unpkg.com/dayjs"></script>
        <script src="/js/common.js"></script>
</head>

<body class="min-h-screen flex flex-col bg-base-100 text-base-content">
        @template._components.navbar()

        <div id="fragmentArea"></div>

        <main class="grow px-6">${content}</main>

        <footer class="px-6 py-4 text-xs text-base-content/50">
                &copy; Bluesky
        </footer>
</body>

</html>
