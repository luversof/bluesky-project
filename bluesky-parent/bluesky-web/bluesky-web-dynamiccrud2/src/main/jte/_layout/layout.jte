@param String title
@param gg.jte.Content content

<!DOCTYPE html>
<html lang="ko">
<head>
	<meta charset="utf-8" />
	<meta name="viewport" content="width=device-width, initial-scale=1" />
	<title>${title} · Dynamic CRUD 2</title>
	<link href="/main.css" rel="stylesheet" />
	<script src="/webjars/htmx.org/dist/htmx.min.js"></script>
</head>
<body class="min-h-screen flex flex-col">
	<header class="border-b border-base-300 bg-base-100">
		<nav class="mx-auto max-w-6xl px-4 h-14 flex items-center gap-4">
			<a href="/use" class="font-semibold">Dynamic CRUD 2</a>
			<a href="/use" class="text-sm hover:underline">사용</a>
			<a href="/setting" class="text-sm hover:underline">설정</a>
		</nav>
	</header>
	<main class="grow mx-auto w-full max-w-6xl px-4 py-6">
		<h1 class="text-xl font-semibold mb-4">${title}</h1>
		${content}
	</main>
</body>
</html>
