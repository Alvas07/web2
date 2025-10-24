<%@ page import="model.models.HistoryEntry" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta
            name="viewport"
            content="width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0"
    >
    <title>HitChecker</title>
    <link
            rel="stylesheet"
            href="${pageContext.request.contextPath}/static/styles.css"
    >
</head>
<body>
<header>
    <div class="header-left">
        <h1>Дядев Владислав Александрович • P3231 • Вариант 2824</h1>
    </div>
</header>

<main>
    <div class="top-section">
        <section class="controls">
            <form action="controller" id="coordsForm" method="get">
                <input type="hidden" name="action" value="check">
                <div class="form-group">
                    <label>X:</label>
                    <div class="x-checkboxes" id="xGroup">
                        <label><input type="checkbox" name="x" value="-3">-3</label>
                        <label><input type="checkbox" name="x" value="-2">-2</label>
                        <label><input type="checkbox" name="x" value="-1">-1</label>
                        <label><input type="checkbox" name="x" value="0">0</label>
                        <label><input type="checkbox" name="x" value="1">1</label>
                        <label><input type="checkbox" name="x" value="2">2</label>
                        <label><input type="checkbox" name="x" value="3">3</label>
                        <label><input type="checkbox" name="x" value="4">4</label>
                        <label><input type="checkbox" name="x" value="5">5</label>
                    </div>
                </div>

                <div class="form-group">
                    <label for="yInput">Y:</label>
                    <input
                            type="text"
                            name="y"
                            id="yInput"
                            placeholder="-3 ... 3"
                    >
                </div>

                <div class="form-group">
                    <label for="rSelect">R:</label>
                    <select name="r" id="rSelect">
                        <option value="" selected disabled>Выберите R</option>
                        <option value="1">1</option>
                        <option value="1.5">1.5</option>
                        <option value="2">2</option>
                        <option value="2.5">2.5</option>
                        <option value="3">3</option>
                    </select>
                </div>

                <button id="submitBtn" type="submit">Проверить</button>
            </form>
        </section>

        <section class="graph">
            <canvas id="graph"></canvas>
        </section>
    </div>

    <section class="history">
        <div class="history-header">
            <h2>История попаданий</h2>
            <form method="get" action="controller">
                <input type="hidden" name="action" value="clear">
                <button type="submit">🗑 Очистить</button>
            </form>
        </div>

        <div class="history-table-wrapper">
            <table id="history-table">
                <thead>
                    <tr>
                        <th>Время</th>
                        <th>Результат</th>
                        <th>X</th>
                        <th>Y</th>
                        <th>R</th>
                        <th>Выполнение (мс)</th>
                    </tr>
                </thead>
                <tbody>
                    <%
                        Double execTime = (Double) request.getAttribute("execTime");
                        List<HistoryEntry> history = (List<HistoryEntry>) request.getAttribute("historyRecords");
                        if (history != null && !history.isEmpty()) {
                            for (HistoryEntry entry : history) {
                    %>
                    <tr class="<%= entry.result() ? "history-item hit" : "history-item miss" %>">
                        <td><%= entry.now() %></td>
                        <td><%= entry.result() ? "Попал 🎯" : "Мимо ❌" %></td>
                        <td><%= entry.point().x() %></td>
                        <td><%= entry.point().y() %></td>
                        <td><%= entry.point().r() %></td>
                        <td><%= execTime != null ? String.format("%.3f", execTime) : "-" %></td>
                    </tr>
                    <%
                            }
                        } else {
                    %>
                    <tr><td colspan="6">История пуста</td></tr>
                    <% } %>
                </tbody>
            </table>
        </div>
    </section>
</main>

<script src="${pageContext.request.contextPath}/static/app.js"></script>
</body>
</html>