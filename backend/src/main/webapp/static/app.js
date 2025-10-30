// DOM-элементы
const canvas = document.getElementById('graph');
const ctx = canvas.getContext('2d');

const xGroup = document.getElementById('xGroup');
const yInput = document.getElementById('yInput');
const rSelect = document.getElementById('rSelect');
const form = document.getElementById('coordsForm');

let selectedR = parseFloat(rSelect.value);
const AXIS_MIN = -5;
const AXIS_MAX = 6;

// canvas setup
function resizeCanvas() {
  const dpr = window.devicePixelRatio || 1;
  const rect = canvas.getBoundingClientRect();
  canvas.width = rect.width * dpr;
  canvas.height = rect.height * dpr;
  ctx.setTransform(1, 0, 0, 1, 0, 0);
  ctx.scale(dpr, dpr);
}

function scaleX(x) { const rect = canvas.getBoundingClientRect(); return rect.width/2 + x*(rect.width/(2*AXIS_MAX)); }
function scaleY(y) { const rect = canvas.getBoundingClientRect(); return rect.height/2 - y*(rect.height/(2*AXIS_MAX)); }

function drawAxes() {
  const rect = canvas.getBoundingClientRect();
  const w = rect.width, h = rect.height;

  ctx.strokeStyle = "#000"; ctx.lineWidth=1;
  ctx.beginPath(); ctx.moveTo(0,h/2); ctx.lineTo(w,h/2); ctx.stroke();
  ctx.beginPath(); ctx.moveTo(w/2,0); ctx.lineTo(w/2,h); ctx.stroke();

  ctx.fillStyle="#000"; ctx.font="12px Arial";
  for(let i=AXIS_MIN;i<=AXIS_MAX;i++){
    if(i===0) continue;
    ctx.fillText(i, scaleX(i), h/2-5);
    ctx.fillText(i, w/2+5, scaleY(i));
  }
}

function drawArea() {
  const R = selectedR;
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

// выбор X (визуальный)
xGroup.addEventListener("change", e => {
  if(e.target.tagName==="INPUT"){
    e.target.parentElement.classList.toggle("active", e.target.checked);
  }
});

// валидация Y в основной форме
yInput.addEventListener("input", ()=>{
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

// изменение R
rSelect.addEventListener("change", ()=>{
  selectedR = parseFloat(rSelect.value);
  drawCanvas();
});

// клик по графику
canvas.addEventListener("click", (event) => {
  if (!selectedR) {
    alert("Сначала выберите R!");
    return;
  }

  const rect = canvas.getBoundingClientRect();
  const x = ((event.clientX - rect.left) / rect.width - 0.5) * 2 * AXIS_MAX;
  const y = (0.5 - (event.clientY - rect.top) / rect.height) * 2 * AXIS_MAX;

  // Строим URL: берём исходный атрибут action (избегаем коллизии с input name="action")
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

// submit основной формы с валидацией
form.addEventListener("submit", e=>{
  const selectedXs = [...xGroup.querySelectorAll("input:checked")].map(cb=>parseFloat(cb.value));
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

// перерисовка графика
function drawCanvas(){
  resizeCanvas();
  ctx.clearRect(0,0,canvas.width,canvas.height);
  drawAxes();
  drawArea();
}

document.addEventListener("DOMContentLoaded", () => {
  selectedR = parseFloat(rSelect.value);
  drawCanvas();
});