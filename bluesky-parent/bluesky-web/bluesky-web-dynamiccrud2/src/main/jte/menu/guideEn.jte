<%-- Guide body (English). Uses the same names as the screen text (messages_en.properties) - keep them in step --%>
<nav class="guide-toc" aria-label="Contents">
	<a href="#structure">Structure</a>
	<a href="#start">Getting started</a>
	<a href="#fields">Table fields</a>
	<a href="#queries">Queries</a>
	<a href="#search">Search settings</a>
	<a href="#access">Permissions and hiding</a>
	<a href="#use">Using a screen</a>
	<a href="#history">Change history</a>
	<a href="#json">JSON calls</a>
</nav>

<section id="structure">
	<h2>Structure</h2>
	<p>Turns one DB table into a list, search, insert, update and delete screen. Screens are grouped in three levels.</p>
	<ul>
		<li><b>Project</b> › <b>Main menu</b> › <b>Menu</b>. The screen address is <code>/project/main-menu/menu</code> (each path name).</li>
		<li>A <b>menu</b> has one base table, one <b>read query</b> for the list, and as many <b>insert, update and delete queries</b> as needed.</li>
		<li>How columns look (label, input type, value list and so on) is set in <b>table fields</b> and shared by every menu on that table.</li>
	</ul>
</section>

<section id="start">
	<h2>Getting started</h2>
	<ol>
		<li>Top <b>Admin</b> → add a project (name, path name).</li>
		<li>Add a main menu under the project.</li>
		<li>Under the main menu, <b>Add menu</b>: pick a connection and a table, and the read query and fields are created.</li>
		<li>Click the menu name to refine queries and search settings. The address next to the title opens the screen.</li>
	</ol>
	<p class="hint">Path names use lowercase letters, digits and -. Changing one stops the old address from opening.</p>
</section>

<section id="fields">
	<h2>Table fields</h2>
	<p>Open them with the <b>table fields</b> button on the menu settings screen. Set them per column.</p>
	<ul>
		<li><b>Label</b>: column title and input name. Empty uses the DB comment, then the column name. It wins over a query alias (AS).</li>
		<li><b>Input type</b>: text, multi-line, number, date, date-time, boolean. Sets the input, how search works and how the list looks (numbers and booleans align right).</li>
		<li><b>Max length</b>: characters allowed on the insert and update forms.</li>
		<li><b>Value list</b>: one <code>stored=shown</code> per line (for example <code>1=Active</code>). The list shows the text (the stored value on hover) and inputs become a select. Works for text values too, not only numbers.</li>
		<li><b>Date format</b>: how date and date-time fields look in the list (for example <code>yyyy.MM.dd</code>, <code>yyyy-MM-dd HH:mm</code>).</li>
		<li><b>Sortable</b>: lets users sort by clicking the column title. At first only fields that lead an index are on; whether an index is used is shown.</li>
		<li><b>Update</b>: whether update actions can change the value. Primary key, unique and auto-increment columns start off.</li>
	</ul>
	<p class="hint">Value lists and date formats change only how values look; Excel, JSON and change history keep stored values. When table columns change, press <b>Reread columns</b> (your own Sortable and Update choices are kept).</p>
</section>

<section id="queries">
	<h2>Queries</h2>
	<ul>
		<li><b>Read (READ)</b>: the SELECT that reads the list, one per menu. Paging, sorting and search conditions are added without editing it.</li>
		<li><b>Insert, update, delete</b>: <b>Add insert</b>, <b>Add update</b> and <b>Add delete</b> open a draft query for the table in a layer.</li>
		<li>Values come in as <code>:name</code> parameters, each with a <b>role</b>:
			<ul>
				<li><b>Input</b>: a value from the insert or update form</li>
				<li><b>Row key (from the selected row)</b>: a value of the row picked in the list (the target of update or delete)</li>
				<li><b>Search</b>: a search box of the read query (top of the screen)</li>
			</ul>
		</li>
		<li><b>Test query</b>: the DB checks it without running it and the parameter and result tables are rebuilt. Saving runs the same check and refuses a failing query.</li>
		<li>UPDATE or DELETE without a parameter in WHERE (changing every row) is not saved.</li>
	</ul>
</section>

<section id="search">
	<h2>Search settings</h2>
	<p>Adds search boxes above the list without editing the read query. Pick result columns with <b>+ Add a search field</b> and reorder with ⠿.</p>
	<ul>
		<li><b>Match</b>: partial match, prefix match, exact match, range. A badge shows whether it can use an index (partial and prefix matches cannot).</li>
		<li><b>Pick</b>: what fills the box.
			<ul>
				<li><b>Type</b>: a text input</li>
				<li><b>Value list</b>: a select of the field's value list (fields that have one)</li>
				<li><b>All DB values</b>: a select of the values actually in that column (up to 200; more falls back to a text input)</li>
				<li><b>Search-only list</b>: a <code>stored=shown</code> list used only for search (to offer some values only)</li>
			</ul>
			Picking from a select searches by exact match.
		</li>
		<li><b>Required</b>: the list shows only after searching with that box filled. When empty, nothing is read and a notice is shown (Excel and JSON too).</li>
		<li><b>Keyword search columns</b>: one keyword searches the chosen text columns together (case-insensitive).</li>
	</ul>
</section>

<section id="access">
	<h2>Permissions and hiding</h2>
	<ul>
		<li><b>Permission</b> (menu): one role (for example <code>ROLE_ADMIN</code>). Empty means any signed-in user.</li>
		<li><b>Run permission</b> (per query): empty follows the menu permission.</li>
		<li>Roles: <code>ROLE_MASTER</code> &gt; <code>ROLE_ADMIN</code> &gt; <code>ROLE_USER</code>, a higher role includes the lower ones. Admin screens need <code>ROLE_ADMIN</code> or higher.</li>
		<li><b>Hiding</b> (eye icon):
			<ul>
				<li>Menu: leaves the menu list but still opens by address.</li>
				<li>Project, main menu: the menus under it leave the list and no longer open by address either.</li>
			</ul>
		</li>
		<li><b>Copy menu</b>: copies queries and search settings, hidden. Show it after editing.</li>
	</ul>
</section>

<section id="use">
	<h2>Using a screen</h2>
	<ul>
		<li>Search with the search boxes and keyword search; click a sortable column title to sort. Page size is set in the menu's basic settings.</li>
		<li>The insert button, a row's update button and the delete button for checked rows (named after their queries) open in a layer. Failures show the reason there.</li>
		<li>Update and delete are refused when someone changed the row after you opened the list. Reopen the list and try again.</li>
		<li><b>Excel</b>: downloads with the current search conditions (menus that turned it on).</li>
		<li>The left side lists the project's menus; click the project name at the top to switch to another project you can see.</li>
	</ul>
</section>

<section id="history">
	<h2>Change history</h2>
	<ul>
		<li><b>Menu setting changes</b>: menu, query, search and field settings changed on admin screens (who, when, before → after).</li>
		<li><b>Data changes</b>: inserts, updates and deletes made on screens, filtered by action, user and dates.</li>
	</ul>
</section>

<section id="json">
	<h2>JSON calls</h2>
	<p>Read and run a menu as JSON instead of the screen. Call with the signed-in session (cookie); the same permissions and checks apply.</p>
	<ul>
		<li>Read: <code>GET /project/main-menu/menu/api</code> (search parameters as on the screen)</li>
		<li>Action: <code>POST …/api/action/{query id}</code>, body <code>{"inputs": {...}, "rows": [...]}</code></li>
		<li>Each menu's addresses, parameters and examples are in the <b>JSON calls</b> card on its settings screen.</li>
	</ul>
	<p>Addresses, bodies and responses of every JSON call are in the <a class="link" href="/swagger-ui.html">API docs (Swagger UI)</a>, where you can also try them (pick the use or admin group at the top).</p>
</section>
