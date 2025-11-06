// DOM-элементы (будут инициализированы после загрузки DOM)
let canvas = null;
let ctx = null;
let xGroup = null;
let yInput = null;
let rSelect = null;
let form = null;
let historyTableBody = null;
let notificationsContainer = null;

let selectedR = null;
const AXIS_MIN = -5;
const AXIS_MAX = 6;

// Текущий sessionId
const currentSessionId = window.CURRENT_SESSION_ID || '';

// Хранилище точек для отрисовки
let drawnPoints = [];

// WebSocket подключение
let ws = null;
let reconnectAttempts = 0;
const maxReconnectAttempts = 5;

function connectWebSocket() {
  if (!currentSessionId) {
    console.error('CURRENT_SESSION_ID is not defined! Cannot connect WebSocket.');
    return;
  }
  
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  // Используем текущий путь для определения контекста
  const pathname = window.location.pathname;
  // Убираем имя файла, оставляем только контекст
  let contextPath = pathname.substring(0, pathname.lastIndexOf('/'));
  // Если contextPath пустой, используем "/"
  if (!contextPath) {
    contextPath = '/';
  }
  // Передаем HTTP session ID в query параметрах
  const wsUrl = `${protocol}//${window.location.host}${contextPath}/websocket?sessionId=${encodeURIComponent(currentSessionId)}`;
  
  console.log('Connecting to WebSocket:', wsUrl);
  console.log('Current session ID:', currentSessionId);
  
  try {
    ws = new WebSocket(wsUrl);
    
    ws.onopen = () => {
      console.log('WebSocket connected successfully');
      reconnectAttempts = 0;
    };
    
    ws.onmessage = (event) => {
      console.log('Raw WebSocket message received:', event.data);
      try {
        const message = JSON.parse(event.data);
        console.log('Parsed WebSocket message:', message);
        if (message.type === 'clear') {
          handleClearHistory(message.sessionId);
        } else {
          // Проверяем, что это сообщение о точке (HistoryEntry)
          if (message.point && message.sessionId !== undefined) {
            handlePointUpdate(message);
          } else {
            console.warn('Unknown message format:', message);
          }
        }
      } catch (e) {
        console.error('Error parsing WebSocket message:', e, event.data);
      }
    };
    
    ws.onerror = (error) => {
      console.error('WebSocket error:', error);
    };
    
    ws.onclose = (event) => {
      console.log('WebSocket disconnected. Code:', event.code, 'Reason:', event.reason);
      // Попытка переподключения
      if (reconnectAttempts < maxReconnectAttempts) {
        reconnectAttempts++;
        setTimeout(() => {
          console.log(`Reconnecting... Attempt ${reconnectAttempts}`);
          connectWebSocket();
        }, 2000 * reconnectAttempts);
      }
    };
  } catch (e) {
    console.error('Error creating WebSocket:', e);
  }
}

// Удаление точек определенной сессии с сервера
async function removePointsFromServer(sessionId) {
  try {
    const response = await fetch('removePoints', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ sessionId: sessionId })
    });
    
    if (response.ok) {
      console.log('Points removed from server session');
    } else {
      console.error('Failed to remove points from server:', response.statusText);
    }
  } catch (e) {
    console.error('Error removing points from server:', e);
  }
}

// Обработка очистки истории (удаление точек определенной сессии)
function handleClearHistory(clearedSessionId) {
  if (!clearedSessionId) {
    // Если sessionId не указан, очищаем все (старое поведение для обратной совместимости)
    drawnPoints = [];
    if (historyTableBody) {
      historyTableBody.innerHTML = '<tr><td colspan="6">История пуста</td></tr>';
    }
    drawCanvas();
    return;
  }
  
  const isOwnSession = clearedSessionId === currentSessionId;
  
  // Если это не наша сессия, удаляем точки из серверной сессии и показываем уведомление
  if (!isOwnSession) {
    // Удаляем точки из серверной сессии
    removePointsFromServer(clearedSessionId);
    
    // Показываем уведомление
    showNotification('📋 История одного из пользователей была очищена');
  }
  
  // Удаляем только точки из указанной сессии
  drawnPoints = drawnPoints.filter(p => p.sessionId !== clearedSessionId);
  
  // Удаляем строки таблицы с указанным sessionId
  if (historyTableBody) {
    const rowsToRemove = historyTableBody.querySelectorAll(`tr[data-session-id="${clearedSessionId}"]`);
    const removedCount = rowsToRemove.length;
    rowsToRemove.forEach(row => row.remove());
    
    // Если таблица пуста, показываем "История пуста"
    if (historyTableBody.children.length === 0) {
      historyTableBody.innerHTML = '<tr><td colspan="6">История пуста</td></tr>';
    }
  }
  
  drawCanvas();
}

// Сохранение точки на сервере (для чужих точек)
async function savePointToServer(message) {
  try {
    const response = await fetch('addPoint', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(message)
    });
    
    if (response.ok) {
      console.log('Point saved to server session');
    } else {
      console.error('Failed to save point to server:', response.statusText);
    }
  } catch (e) {
    console.error('Error saving point to server:', e);
  }
}

// Обработка обновления точки через WebSocket
function handlePointUpdate(message) {
  console.log('handlePointUpdate called with:', message);
  console.log('Current session ID:', currentSessionId);
  console.log('Message session ID:', message.sessionId);
  
  const isOwnPoint = message.sessionId === currentSessionId;
  console.log('Is own point:', isOwnPoint);
  
  // Сохраняем точку на сервере (в свою сессию), чтобы она не пропала после перезагрузки
  if (!isOwnPoint) {
    savePointToServer(message);
  }
  
  // Добавляем точку в хранилище
  drawnPoints.push({
    point: message.point,
    result: message.result,
    sessionId: message.sessionId,
    isOwn: isOwnPoint
  });
  
  console.log('Total drawn points:', drawnPoints.length);
  
  // Перерисовываем график
  if (canvas && ctx) {
    drawCanvas();
  }
  
  // Добавляем строку в таблицу
  if (historyTableBody) {
    addHistoryRow(message, isOwnPoint);
  }
  
  // Показываем уведомление, если это не своя точка
  if (!isOwnPoint) {
    showNotification(`Новая точка от другого пользователя: (${message.point.x}, ${message.point.y}, R=${message.point.r}) - ${message.result ? 'Попал 🎯' : 'Мимо ❌'}`);
  }
}

// Показ уведомления
function showNotification(message) {
  const notification = document.createElement('div');
  notification.className = 'notification';
  notification.textContent = message;
  notificationsContainer.appendChild(notification);
  
  // Анимация появления
  setTimeout(() => {
    notification.classList.add('show');
  }, 10);
  
  // Удаление через 5 секунд
  setTimeout(() => {
    notification.classList.remove('show');
    setTimeout(() => {
      notification.remove();
    }, 300);
  }, 5000);
}

// Добавление строки в таблицу истории
function addHistoryRow(message, isOwnPoint) {
  // Убираем строку "История пуста" если она есть
  const emptyRow = historyTableBody.querySelector('tr:only-child td[colspan]');
  if (emptyRow) {
    emptyRow.parentElement.remove();
  }
  
  const row = document.createElement('tr');
  row.className = `history-item ${message.result ? 'hit' : 'miss'} ${isOwnPoint ? 'own-point' : 'other-point'}`;
  row.setAttribute('data-session-id', message.sessionId);
  
  // Парсим timestamp (может быть строкой в формате "yyyy-MM-dd HH:mm:ss")
  let timestampStr;
  if (typeof message.now === 'string') {
    // Если это строка, форматируем её
    timestampStr = message.now.replace('T', ' ').substring(0, 19);
  } else {
    timestampStr = new Date(message.now).toLocaleString('ru-RU', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  }
  
  // Форматируем execTime с точкой (не запятой)
  const execTimeStr = message.execTime.toFixed(3).replace(',', '.');
  
  row.innerHTML = `
    <td>${timestampStr}</td>
    <td>${message.result ? 'Попал 🎯' : 'Мимо ❌'}</td>
    <td>${message.point.x}</td>
    <td>${message.point.y}</td>
    <td>${message.point.r}</td>
    <td>${execTimeStr}</td>
  `;
  
  // Вставляем в начало таблицы
  if (historyTableBody.firstChild) {
    historyTableBody.insertBefore(row, historyTableBody.firstChild);
  } else {
    historyTableBody.appendChild(row);
  }
}

// canvas setup
function resizeCanvas() {
  const dpr = window.devicePixelRatio || 1;
  const rect = canvas.getBoundingClientRect();
  canvas.width = rect.width * dpr;
  canvas.height = rect.height * dpr;
  ctx.setTransform(1, 0, 0, 1, 0, 0);
  ctx.scale(dpr, dpr);
}

function scaleX(x) { 
  const rect = canvas.getBoundingClientRect(); 
  return rect.width/2 + x*(rect.width/(2*AXIS_MAX)); 
}

function scaleY(y) { 
  const rect = canvas.getBoundingClientRect(); 
  return rect.height/2 - y*(rect.height/(2*AXIS_MAX)); 
}

function drawAxes() {
  const rect = canvas.getBoundingClientRect();
  const w = rect.width, h = rect.height;

  ctx.strokeStyle = "#000"; 
  ctx.lineWidth = 1;
  ctx.beginPath(); 
  ctx.moveTo(0, h/2); 
  ctx.lineTo(w, h/2); 
  ctx.stroke();
  ctx.beginPath(); 
  ctx.moveTo(w/2, 0); 
  ctx.lineTo(w/2, h); 
  ctx.stroke();

  ctx.fillStyle = "#000"; 
  ctx.font = "12px Arial";
  for(let i = AXIS_MIN; i <= AXIS_MAX; i++){
    if(i === 0) continue;
    ctx.fillText(i, scaleX(i), h/2-5);
    ctx.fillText(i, w/2+5, scaleY(i));
  }
}

function drawArea() {
  if (!selectedR || isNaN(selectedR)) return;
  drawAreaWithR(selectedR);
}

function drawAreaWithR(R) {
  if (!R || isNaN(R)) return;
  
  ctx.fillStyle = "rgba(0,128,255,0.3)";

  // 2 четверть: четверть круга
  ctx.beginPath();
  ctx.moveTo(scaleX(0), scaleY(0));
  ctx.arc(scaleX(0), scaleY(0), scaleX(R)-scaleX(0), Math.PI, 1.5*Math.PI, false);
  ctx.closePath();
  ctx.fill();

  // 3 четверть: квадрат
  ctx.fillRect(scaleX(-R), scaleY(0), scaleX(R)-scaleX(0), scaleY(-R)-scaleY(0));

  // 4 четверть: треугольник
  ctx.beginPath();
  ctx.moveTo(scaleX(0), scaleY(0));
  ctx.lineTo(scaleX(R), scaleY(0));
  ctx.lineTo(scaleX(0), scaleY(-R));
  ctx.closePath();
  ctx.fill();
}

// Отрисовка точек на графике
function drawPoints() {
  // Если R не выбран, не рисуем точки (но рисуем оси и область при наличии R из истории)
  if (!selectedR || isNaN(selectedR)) {
    // Пытаемся найти R из истории точек
    if (drawnPoints.length > 0) {
      // Берем R из первой точки для отрисовки области
      const firstPointR = drawnPoints[0].point.r;
      drawAreaWithR(firstPointR);
      // Рисуем все точки без фильтрации по R
      drawnPoints.forEach(p => {
        const x = scaleX(p.point.x);
        const y = scaleY(p.point.y);
        
        if (p.isOwn) {
          ctx.beginPath();
          ctx.arc(x, y, 5, 0, 2 * Math.PI);
          ctx.fillStyle = p.result ? '#2ecc71' : '#e74c3c';
          ctx.fill();
          ctx.strokeStyle = '#000';
          ctx.lineWidth = 1;
          ctx.stroke();
        } else {
          ctx.fillStyle = p.result ? 'rgba(46, 204, 113, 0.6)' : 'rgba(231, 76, 60, 0.6)';
          ctx.fillRect(x - 5, y - 5, 10, 10);
          ctx.strokeStyle = '#000';
          ctx.lineWidth = 1;
          ctx.strokeRect(x - 5, y - 5, 10, 10);
        }
      });
    }
    return;
  }
  
  // Фильтруем точки с текущим R
  const pointsToDraw = drawnPoints.filter(p => Math.abs(p.point.r - selectedR) < 0.001);
  
  pointsToDraw.forEach(p => {
    const x = scaleX(p.point.x);
    const y = scaleY(p.point.y);
    
    if (p.isOwn) {
      // Свои точки - круги
      ctx.beginPath();
      ctx.arc(x, y, 5, 0, 2 * Math.PI);
      ctx.fillStyle = p.result ? '#2ecc71' : '#e74c3c';
      ctx.fill();
      ctx.strokeStyle = '#000';
      ctx.lineWidth = 1;
      ctx.stroke();
    } else {
      // Чужие точки - квадраты
      ctx.fillStyle = p.result ? 'rgba(46, 204, 113, 0.6)' : 'rgba(231, 76, 60, 0.6)';
      ctx.fillRect(x - 5, y - 5, 10, 10);
      ctx.strokeStyle = '#000';
      ctx.lineWidth = 1;
      ctx.strokeRect(x - 5, y - 5, 10, 10);
    }
  });
}

// Функции для инициализации обработчиков событий
function setupEventListeners() {
  // выбор X (визуальный)
  if (xGroup) {
    xGroup.addEventListener("change", e => {
      if(e.target.tagName === "INPUT"){
        e.target.parentElement.classList.toggle("active", e.target.checked);
      }
    });
  }

  // валидация Y в основной форме
  if (yInput) {
    yInput.addEventListener("input", () => {
      let val = yInput.value;
      val = val.replace(/[^0-9.,-]/g, "");
      if (val.includes("-")) val = "-" + val.replace(/-/g, "");
      val = val.replace(",", ".");
      const firstDot = val.indexOf(".");
      if (firstDot !== -1) val = val.slice(0, firstDot + 1) + val.slice(firstDot + 1).replace(/\./g, "");
      val = val.replace(/^(-?)0+(\d)/, "$1$2");
      yInput.value = val;
      const y = parseFloat(val);
      if (isNaN(y) || y < -3 || y > 3) yInput.classList.add("invalid");
      else yInput.classList.remove("invalid");
    });
  }

  // изменение R
  if (rSelect) {
    rSelect.addEventListener("change", () => {
      selectedR = parseFloat(rSelect.value);
      console.log('R changed to:', selectedR);
      drawCanvas();
    });
  }

  // клик по графику
  if (canvas) {
    canvas.addEventListener("click", (event) => {
      if (!selectedR || isNaN(selectedR)) {
        alert("Сначала выберите R!");
        return;
      }

      const rect = canvas.getBoundingClientRect();
      const x = ((event.clientX - rect.left) / rect.width - 0.5) * 2 * AXIS_MAX;
      const y = (0.5 - (event.clientY - rect.top) / rect.height) * 2 * AXIS_MAX;

      if (!form) {
        console.error('Form not found!');
        return;
      }

      const formActionAttr = form.getAttribute('action') || 'controller';
      const url = new URL(formActionAttr, window.location.href);
      const sp = url.searchParams;
      sp.set("action", "check");
      sp.set("fromGraph", "true");
      sp.set("x", x.toFixed(3));
      sp.set("y", y.toFixed(3));
      sp.set("r", String(selectedR));
      sp.set("axisMin", String(AXIS_MIN));
      sp.set("axisMax", String(AXIS_MAX));

      window.location.assign(url.toString());
    });
  }

  // submit основной формы с валидацией
  if (form) {
    form.addEventListener("submit", e => {
      if (!xGroup || !yInput || !rSelect) {
        console.error('Form elements not found!');
        return;
      }
      const selectedXs = [...xGroup.querySelectorAll("input:checked")].map(cb => parseFloat(cb.value));
      const yVal = parseFloat(yInput.value.trim().replace(',','.'));
      const rVal = parseFloat(rSelect.value);
      let errors = [];
      if(selectedXs.length === 0) errors.push("Выберите хотя бы один X.");
      if(isNaN(yVal) || yVal < -3 || yVal > 3) errors.push("Y должен быть в диапазоне [-3;3].");
      if(isNaN(rVal)) errors.push("Выберите R.");
      if(errors.length > 0){
        e.preventDefault();
        alert(errors.join("\n"));
      }
    });
  }
}

// перерисовка графика
function drawCanvas(){
  if (!canvas || !ctx) {
    console.warn('Canvas or context not available, skipping draw');
    return;
  }
  resizeCanvas();
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  drawAxes();
  drawArea();
  drawPoints();
}

// Инициализация точек из существующей таблицы
function initializePointsFromTable() {
  if (!historyTableBody) return;
  
  const rows = historyTableBody.querySelectorAll('tr');
  rows.forEach(row => {
    if (row.cells.length < 6) return;
    
    const x = parseFloat(row.cells[2].textContent);
    const y = parseFloat(row.cells[3].textContent);
    const r = parseFloat(row.cells[4].textContent);
    const result = row.cells[1].textContent.includes('Попал');
    
    // Получаем sessionId из data-атрибута или определяем по классу
    const rowSessionId = row.getAttribute('data-session-id') || (row.classList.contains('own-point') ? currentSessionId : null);
    const isOwn = rowSessionId === currentSessionId;
    
    drawnPoints.push({
      point: { x, y, r },
      result: result,
      sessionId: rowSessionId || 'unknown',
      isOwn: isOwn
    });
  });
}

// Извлечение R из URL параметров
function getRFromURL() {
  const urlParams = new URLSearchParams(window.location.search);
  const rParam = urlParams.get('r');
  if (rParam) {
    const r = parseFloat(rParam);
    if (!isNaN(r) && rSelect) {
      // Устанавливаем значение в select
      rSelect.value = r;
      selectedR = r;
      return r;
    }
  }
  return null;
}

document.addEventListener("DOMContentLoaded", () => {
  // Инициализируем DOM элементы
  canvas = document.getElementById('graph');
  if (canvas) {
    ctx = canvas.getContext('2d');
  }
  xGroup = document.getElementById('xGroup');
  yInput = document.getElementById('yInput');
  rSelect = document.getElementById('rSelect');
  form = document.getElementById('coordsForm');
  historyTableBody = document.querySelector('#history-table tbody');
  notificationsContainer = document.getElementById('notifications-container');
  
  // Настраиваем обработчики событий
  setupEventListeners();
  
  // Сначала пытаемся получить R из URL
  const rFromURL = getRFromURL();
  
  // Если R не в URL, берем из select
  if (!rFromURL && rSelect && rSelect.value) {
    selectedR = parseFloat(rSelect.value);
  }
  
  // Инициализируем точки из таблицы
  initializePointsFromTable();
  
  // Если есть точки, но R не выбран, используем R из первой точки для отрисовки
  if ((!selectedR || isNaN(selectedR)) && drawnPoints.length > 0) {
    const firstPointR = drawnPoints[0].point.r;
    if (rSelect) {
      // Проверяем, есть ли такой R в select
      const option = Array.from(rSelect.options).find(opt => parseFloat(opt.value) === firstPointR);
      if (option) {
        rSelect.value = firstPointR;
        selectedR = firstPointR;
      }
    }
  }
  
  // Рисуем график только если есть canvas
  if (canvas && ctx) {
    drawCanvas();
  }
  
  // Подключаемся к WebSocket только на главной странице (где есть и canvas, и historyTableBody)
  if (canvas && historyTableBody && currentSessionId) {
    console.log('Initializing WebSocket on main page');
    connectWebSocket();
  } else {
    console.log('Skipping WebSocket connection - not on main page or sessionId missing');
    console.log('Canvas:', !!canvas, 'HistoryTableBody:', !!historyTableBody, 'SessionId:', !!currentSessionId);
  }
});
