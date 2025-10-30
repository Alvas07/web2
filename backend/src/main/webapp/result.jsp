<%@ page import="model.models.HistoryEntry" %>
<%@ page import="java.util.List" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page contentType="text/html; charset=UTF-8" %>

<%
    List<HistoryEntry> results = (List<HistoryEntry>) request.getAttribute("results");
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
%>

<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta
            name="viewport"
            content="width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0"
    >
    <title>Check Result</title>
    <link
            rel="stylesheet"
            href="${pageContext.request.contextPath}/static/styles.css?v=2"
    >
</head>
<body>
<header>
    <div class="header-left">
        <h1>Результаты проверки</h1>
    </div>
    <div class="header-right">
        <form action="controller" method="get">
            <button name="action" value="home">🏠 На главную</button>
        </form>
    </div>
</header>

<main>
    <div class="card result-layout">
        <div class="result-left">
            <h2>Проверенные точки</h2>
            <table id="history-table">
                <tr>
                    <th>Время</th>
                    <th>Результат</th>
                    <th>X</th>
                    <th>Y</th>
                    <th>R</th>
                    <th>Время выполнения (мс)</th>
                </tr>
                <%
                    if (results != null && !results.isEmpty()) {
                        for (HistoryEntry entry : results) {
                %>
                <tr class="<%= entry.result() ? "history-item hit" : "history-item miss" %>">
                    <td><%= entry.now().format(fmt) %></td>
                    <td><%= entry.result() ? "Попал 🎯" : "Мимо ❌" %></td>
                    <td><%= entry.point().x() %></td>
                    <td><%= entry.point().y() %></td>
                    <td><%= entry.point().r() %></td>
                    <td><%= String.format("%.3f", entry.execTime()) %></td>
                </tr>
                <%
                    }
                } else {
                %>
                <tr><td colspan="6">История пуста</td></tr>
                <% } %>
            </table>
        </div>
    </div>
</main>
<script src="${pageContext.request.contextPath}/static/app.js"></script>
</body>
</html>