<%@ page isErrorPage="true" contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta
            name="viewport"
            content="width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0"
    >
    <title>Error</title>
    <link
            rel="stylesheet"
            href="${pageContext.request.contextPath}/static/styles.css"
    >
</head>
<body>
<header>
    <div class="header-left">
        <h1>Произошла ошибка</h1>
    </div>
    <div class="header-right">
        <form action="controller" method="get">
            <button name="action" value="home">🏠 На главную</button>
        </form>
    </div>
</header>

<main>
    <div class="error-card">
        <h1>Что-то пошло не так 😢</h1>

        <p class="error-message">
            <%= request.getAttribute("errorMessage") != null
                    ? request.getAttribute("errorMessage")
                    : "Произошла непредвиденная ошибка. Попробуйте позже." %>
        </p>
    </div>
</main>
<script src="${pageContext.request.contextPath}/static/app.js"></script>
</body>
</html>