@import net.luversof.web.dynamiccrud.util.TemplateUtil
@param gg.jte.Content content

<!DOCTYPE html>
<html data-theme="${TemplateUtil.getRandomTheme()}">

<head>
        <link href="/main.css" rel="stylesheet" />
        <script type="text/javascript" src="/webjars/theme-change/index.js"></script>
        <script type="text/javascript" src="/webjars/htmx.org/dist/htmx.min.js"></script>
        <script src="https://unpkg.com/dayjs"></script>
        <script src="/js/common.js"></script>
</head>

<body>
        <div>
                @template._components.navbar()

                <div id="fragmentArea"></div>

                <div class="px-2">${content}</div>

                <footer class="footer footer-center p-4 bg-base-100 text-base-content">
                        <aside>
                                <p>Copyright  2023 - All right reserved by Bluesky</p>
                        </aside>
                </footer>
        </div>
</body>

</html>
