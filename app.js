/* ==========================================================================
   ACUÉRDATE — LÓGICA DE APLICACIÓN Y EXPERIENCIA PREMIUM (JS)
   ========================================================================== */

document.addEventListener('DOMContentLoaded', () => {
  
  // Elementos de la interfaz (DOM)
  const elements = {
    dialog: document.getElementById('reminderDialog'),
    openDialogBtn: document.getElementById('openDialogBtn'),
    emptyStateAddBtn: document.getElementById('emptyStateAddBtn'),
    closeDialogBtn: document.getElementById('closeDialogBtn'),
    cancelDialogBtn: document.getElementById('cancelDialogBtn'),
    reminderForm: document.getElementById('reminderForm'),
    reminderText: document.getElementById('reminderText'),
    voiceBtn: document.getElementById('voiceBtn'),
    voiceStatus: document.getElementById('voiceStatus'),
    remindersGrid: document.getElementById('remindersGrid'),
    historyList: document.getElementById('historyList'),
    historyDetails: document.getElementById('historyDetails'),
    historyCount: document.getElementById('historyCount'),
    clearHistoryBtn: document.getElementById('clearHistoryBtn'),
    emptyState: document.getElementById('emptyState'),
    
    // Stats (KPIs)
    statActiveCount: document.getElementById('statActiveCount'),
    statCompletedCount: document.getElementById('statCompletedCount'),
    statEfficiency: document.getElementById('statEfficiency'),
    streakValue: document.getElementById('streakValue'),
    
    // Confeti
    confettiCanvas: document.getElementById('confettiCanvas'),
    
    // Filtros
    filterBtns: document.querySelectorAll('.filter-btn'),

    // Vista de Calendario / Reservas
    listViewBtn: document.getElementById('listViewBtn'),
    calendarViewBtn: document.getElementById('calendarViewBtn'),
    calendarViewContainer: document.getElementById('calendarViewContainer'),
    calendarCurrentMonth: document.getElementById('calendarCurrentMonth'),
    prevMonthBtn: document.getElementById('prevMonthBtn'),
    nextMonthBtn: document.getElementById('nextMonthBtn'),
    calendarDays: document.getElementById('calendarDays'),
    calendarDayDetails: document.getElementById('calendarDayDetails'),
    selectedDayTitle: document.getElementById('selectedDayTitle'),
    calendarAddReminderBtn: document.getElementById('calendarAddReminderBtn'),
    calendarDetailsList: document.getElementById('calendarDetailsList'),
    enableBooking: document.getElementById('enableBooking'),
    bookingDateGroup: document.getElementById('bookingDateGroup'),
    reminderDate: document.getElementById('reminderDate'),
    recurrence: document.getElementById('recurrence'),
    planetSphere: document.getElementById('planetSphere'),
    planetStatus: document.getElementById('planetStatus')
  };

  // ==========================================================================
  // 1. ESTADO DE LA APLICACIÓN
  // ==========================================================================
  
  // ==========================================================================
  // 1. ESTADO DE LA APLICACIÓN
  // ==========================================================================
  
  let state = {
    reminders: [],         // Recordatorios activos
    completed: [],         // Recordatorios cumplidos (salida)
    streak: 0,             // Racha actual de días cumpliendo tareas
    lastCompletedDate: null, // Fecha del último recordatorio cumplido
    currentFilter: 'all',  // Filtro activo de categoría
    isRecording: false,    // Estado de la entrada de voz
    viewMode: 'list',      // Vista activa ('list' o 'calendar')
    calendarCurrentDate: new Date(), // Fecha de navegación del calendario
    selectedDate: null     // Fecha seleccionada en el calendario
  };

  // ==========================================================================
  // 2. MOTOR DE SONIDO NATIVO (Web Audio API)
  // ==========================================================================
  
  let audioCtx = null;

  function initAudio() {
    if (!audioCtx) {
      audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    }
    // Si el contexto de audio está en suspensión (restricción del navegador), reactivarlo
    if (audioCtx.state === 'suspended') {
      audioCtx.resume();
    }
  }

  // Desbloquear automáticamente el contexto de audio en la primera interacción del usuario (clics, toques, teclas)
  ['click', 'touchstart', 'keydown'].forEach(event => {
    document.addEventListener(event, () => {
      try {
        initAudio();
        if (audioCtx) {
          // Generar un oscilador inaudible de silencio para desbloquear el audio en Safari, Chrome e iOS
          const osc = audioCtx.createOscillator();
          const gain = audioCtx.createGain();
          osc.connect(gain);
          gain.connect(audioCtx.destination);
          gain.gain.setValueAtTime(0, audioCtx.currentTime);
          osc.start(0);
          osc.stop(0.001);
          console.log("AudioContext desbloqueado con éxito mediante interacción del usuario.");
        }
      } catch (e) {
        console.warn("Fallo al desbloquear AudioContext en interacción:", e);
      }
    }, { once: true });
  });

  /**
   * Sintetiza efectos de sonido premium y limpios usando osciladores nativos.
   */
  function playChime(type) {
    try {
      initAudio();
      
      const osc = audioCtx.createOscillator();
      const gainNode = audioCtx.createGain();
      
      osc.connect(gainNode);
      gainNode.connect(audioCtx.destination);
      
      const now = audioCtx.currentTime;
      
      if (type === 'create') {
        // Tono ascendente limpio para creación de recordatorios
        osc.type = 'sine';
        osc.frequency.setValueAtTime(440, now); // La4
        osc.frequency.exponentialRampToValueAtTime(880, now + 0.35); // La5
        
        gainNode.gain.setValueAtTime(0.001, now);
        gainNode.gain.linearRampToValueAtTime(0.12, now + 0.05);
        gainNode.gain.exponentialRampToValueAtTime(0.001, now + 0.35);
        
        osc.start(now);
        osc.stop(now + 0.35);
        
      } else if (type === 'success') {
        // Acorde celestial arpegiado (Do mayor: Do5 -> Mi5 -> Sol5 -> Do6)
        const notes = [523.25, 659.25, 783.99, 1046.50]; 
        notes.forEach((freq, idx) => {
          const itemOsc = audioCtx.createOscillator();
          const itemGain = audioCtx.createGain();
          
          itemOsc.connect(itemGain);
          itemGain.connect(audioCtx.destination);
          
          itemOsc.type = 'triangle';
          itemOsc.frequency.setValueAtTime(freq, now + idx * 0.06);
          
          itemGain.gain.setValueAtTime(0.001, now + idx * 0.06);
          itemGain.gain.linearRampToValueAtTime(0.1, now + idx * 0.06 + 0.03);
          itemGain.gain.exponentialRampToValueAtTime(0.001, now + idx * 0.06 + 0.6);
          
          itemOsc.start(now + idx * 0.06);
          itemOsc.stop(now + idx * 0.06 + 0.6);
        });
        
      } else if (type === 'micStart') {
        // Tono corto y agudo (micrófono activado)
        osc.type = 'sine';
        osc.frequency.setValueAtTime(660, now);
        
        gainNode.gain.setValueAtTime(0.001, now);
        gainNode.gain.linearRampToValueAtTime(0.08, now + 0.02);
        gainNode.gain.exponentialRampToValueAtTime(0.001, now + 0.15);
        
        osc.start(now);
        osc.stop(now + 0.15);
        
      } else if (type === 'micStop') {
        // Tono corto descendente (micrófono desactivado)
        osc.type = 'sine';
        osc.frequency.setValueAtTime(660, now);
        osc.frequency.setValueAtTime(520, now + 0.05);
        
        gainNode.gain.setValueAtTime(0.001, now);
        gainNode.gain.linearRampToValueAtTime(0.08, now + 0.02);
        gainNode.gain.exponentialRampToValueAtTime(0.001, now + 0.18);
        
        osc.start(now);
        osc.stop(now + 0.18);
        
      } else if (type === 'error') {
        // Tono grave doble menor (aviso de error)
        osc.type = 'sawtooth';
        osc.frequency.setValueAtTime(180, now);
        osc.frequency.setValueAtTime(170, now + 0.1);
        
        gainNode.gain.setValueAtTime(0.001, now);
        gainNode.gain.linearRampToValueAtTime(0.08, now + 0.03);
        gainNode.gain.exponentialRampToValueAtTime(0.001, now + 0.25);
        
        osc.start(now);
        osc.stop(now + 0.25);
        
      } else if (type === 'bookingAlert') {
        // Alarma polifónica premium: Secuencia de acordes espaciales (Emaj7 -> Amaj7 -> Bmaj7) con vibrato analógico (LFO)
        const chords = [
          { freq: [164.81, 246.94, 329.63, 415.30, 493.88, 622.25], timeOffset: 0.0, dur: 0.7 },  // E3 + B3 + E4 + G#4 + B4 + D#5 (Emaj7)
          { freq: [220.00, 329.63, 440.00, 554.37, 659.25, 830.61], timeOffset: 0.6, dur: 0.7 },  // A3 + E4 + A4 + C#5 + E5 + G#5 (Amaj7)
          { freq: [246.94, 369.99, 493.88, 622.25, 739.99, 932.33], timeOffset: 1.2, dur: 1.4 }   // B3 + F#4 + B4 + D#5 + F#5 + A#5 (Bmaj7)
        ];

        chords.forEach((chord) => {
          const startTime = now + chord.timeOffset;
          chord.freq.forEach((freq) => {
            const itemOsc = audioCtx.createOscillator();
            const itemGain = audioCtx.createGain();
            
            itemOsc.connect(itemGain);
            itemGain.connect(audioCtx.destination);
            
            // Usamos ondas triangulares para un timbre cálido, aterciopelado y premium estilo sintetizador analógico
            itemOsc.type = 'triangle';
            itemOsc.frequency.setValueAtTime(freq, startTime);
            
            // Añadir un sutil vibrato (LFO) para conseguir un efecto espacial de coro polifónico analógico
            const lfo = audioCtx.createOscillator();
            const lfoGain = audioCtx.createGain();
            lfo.frequency.setValueAtTime(5.8, startTime); // 5.8 Hz de oscilación (vibrato)
            lfoGain.gain.setValueAtTime(freq * 0.006, startTime); // Variación de tono sutil y proporcional
            
            lfo.connect(lfoGain);
            lfoGain.connect(itemOsc.frequency);
            
            // Envolvente de volumen de cada nota (ataque suave de 50ms, decaimiento exponencial)
            itemGain.gain.setValueAtTime(0.001, startTime);
            itemGain.gain.linearRampToValueAtTime(0.035, startTime + 0.05); // Volumen calibrado para evitar saturación
            itemGain.gain.exponentialRampToValueAtTime(0.001, startTime + chord.dur);
            
            lfo.start(startTime);
            lfo.stop(startTime + chord.dur);
            
            itemOsc.start(startTime);
            itemOsc.stop(startTime + chord.dur);
          });
        });

      } else if (type === 'sos') {
        // SOS: S = 3 short, O = 3 long, S = 3 short high-pitched bursts
        const pattern = [
          { dur: 0.08, gap: 0.10 }, // S
          { dur: 0.08, gap: 0.10 },
          { dur: 0.08, gap: 0.22 },
          { dur: 0.20, gap: 0.10 }, // O
          { dur: 0.20, gap: 0.10 },
          { dur: 0.20, gap: 0.22 },
          { dur: 0.08, gap: 0.10 }, // S
          { dur: 0.08, gap: 0.10 },
          { dur: 0.08, gap: 0.0 }
        ];
        let t = now;
        pattern.forEach(beat => {
          const bOsc = audioCtx.createOscillator();
          const bGain = audioCtx.createGain();
          bOsc.connect(bGain);
          bGain.connect(audioCtx.destination);
          bOsc.type = 'square';
          bOsc.frequency.setValueAtTime(1200, t);
          bGain.gain.setValueAtTime(0.001, t);
          bGain.gain.linearRampToValueAtTime(0.09, t + 0.01);
          bGain.gain.exponentialRampToValueAtTime(0.001, t + beat.dur);
          bOsc.start(t);
          bOsc.stop(t + beat.dur);
          t += beat.dur + beat.gap;
        });

      } else if (type === 'bell') {
        // Deep resonant bell: fundamental + harmonics with long exponential decay
        const harmonics = [
          { freq: 220, gain: 0.18, decay: 2.8 },
          { freq: 440, gain: 0.12, decay: 2.0 },
          { freq: 880, gain: 0.08, decay: 1.4 },
          { freq: 1320, gain: 0.04, decay: 0.9 },
          { freq: 1760, gain: 0.025, decay: 0.6 },
          { freq: 293.66, gain: 0.06, decay: 2.2 }, // D4 — slight inharmonic partials
          { freq: 554.37, gain: 0.04, decay: 1.6 }  // C#5
        ];
        harmonics.forEach(h => {
          const hOsc = audioCtx.createOscillator();
          const hGain = audioCtx.createGain();
          hOsc.connect(hGain);
          hGain.connect(audioCtx.destination);
          hOsc.type = 'sine';
          hOsc.frequency.setValueAtTime(h.freq, now);
          hGain.gain.setValueAtTime(0.001, now);
          hGain.gain.linearRampToValueAtTime(h.gain, now + 0.01);
          hGain.gain.exponentialRampToValueAtTime(0.0001, now + h.decay);
          hOsc.start(now);
          hOsc.stop(now + h.decay);
        });

      } else if (type === 'cyber') {
        // CyberPulse: two rapid ascending sweeps with sawtooth
        [0, 0.22].forEach(offset => {
          const cOsc = audioCtx.createOscillator();
          const cGain = audioCtx.createGain();
          cOsc.connect(cGain);
          cGain.connect(audioCtx.destination);
          cOsc.type = 'sawtooth';
          const t = now + offset;
          cOsc.frequency.setValueAtTime(300, t);
          cOsc.frequency.exponentialRampToValueAtTime(1800, t + 0.18);
          cGain.gain.setValueAtTime(0.001, t);
          cGain.gain.linearRampToValueAtTime(0.11, t + 0.03);
          cGain.gain.exponentialRampToValueAtTime(0.001, t + 0.18);
          cOsc.start(t);
          cOsc.stop(t + 0.18);
        });
      }
    } catch (e) {
      console.warn("Audio Context falló al reproducir sonido:", e);
    }
  }

  // ==========================================================================
  // 3. ENTRADA DE VOZ Y TRANSCRIPCIÓN (Web Speech API)
  // ==========================================================================
  
  let recognition = null;
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;

  if (SpeechRecognition) {
    recognition = new SpeechRecognition();
    recognition.lang = 'es-ES';
    recognition.interimResults = false;
    recognition.maxAlternatives = 1;
    recognition.continuous = false; // Detiene tras completar frase principal

    // Eventos del reconocedor de voz
    recognition.onstart = () => {
      state.isRecording = true;
      elements.voiceBtn.classList.add('recording');
      elements.voiceStatus.classList.add('listening');
      elements.voiceStatus.querySelector('.status-text').textContent = 'Escuchando... Habla ahora';
    };

    recognition.onresult = (event) => {
      const speechToText = event.results[0][0].transcript;
      if (speechToText.trim()) {
        const text = elements.reminderText.value.trim();
        // Capitalizar frase transcrita
        const formattedSpeech = speechToText.charAt(0).toUpperCase() + speechToText.slice(1);
        
        if (text) {
          // Si ya hay texto, concatenar elegantemente
          elements.reminderText.value = text + '. ' + formattedSpeech;
        } else {
          elements.reminderText.value = formattedSpeech;
        }
        
        // Efecto hover sutil en el textarea para indicar actualización
        elements.reminderText.focus();
      }
      elements.voiceStatus.querySelector('.status-text').textContent = 'Transcrito con éxito';
    };

    recognition.onerror = (event) => {
      console.error('Speech recognition error:', event.error);
      playChime('error');
      
      let errorMsg = 'Error en el reconocimiento';
      if (event.error === 'no-speech') {
        errorMsg = 'No se detectó voz. Vuelve a intentarlo';
      } else if (event.error === 'audio-capture') {
        errorMsg = 'No se encontró micrófono conectado';
      } else if (event.error === 'not-allowed') {
        errorMsg = 'Permiso de micrófono denegado';
      }
      
      elements.voiceStatus.querySelector('.status-text').textContent = errorMsg;
      setTimeout(resetVoiceUI, 3000);
    };

    recognition.onend = () => {
      state.isRecording = false;
      elements.voiceBtn.classList.remove('recording');
      elements.voiceStatus.classList.remove('listening');
      if (elements.voiceStatus.querySelector('.status-text').textContent === 'Escuchando... Habla ahora') {
        elements.voiceStatus.querySelector('.status-text').textContent = 'Grabación finalizada';
        setTimeout(resetVoiceUI, 2000);
      }
    };
  } else {
    // Si el navegador no lo soporta, actualizar UI del micrófono
    elements.voiceBtn.style.opacity = '0.5';
    elements.voiceBtn.style.cursor = 'not-allowed';
    elements.voiceBtn.title = 'Reconocimiento de voz no soportado en este navegador';
    elements.voiceStatus.querySelector('.status-text').textContent = 'Reconocimiento de voz no compatible';
  }

  function resetVoiceUI() {
    if (!state.isRecording) {
      elements.voiceStatus.querySelector('.status-text').textContent = 'Haz clic en el micrófono para dictar con tu voz';
    }
  }

  function toggleVoiceRecording() {
    if (!SpeechRecognition) {
      alert('Tu navegador no es compatible con el dictado por voz. Recomendamos usar Google Chrome, Safari o Microsoft Edge.');
      return;
    }
    
    // Inicializar audio primero por la política de interacción del navegador
    initAudio();

    if (state.isRecording) {
      playChime('micStop');
      recognition.stop();
    } else {
      playChime('micStart');
      try {
        recognition.start();
      } catch (err) {
        console.warn('Reconocimiento ya activo o falló:', err);
      }
    }
  }

  // ==========================================================================
  // 4. MOTOR DE CELEBRACIÓN DE PARTÍCULAS (Canvas Confetti)
  // ==========================================================================
  
  const ctx = elements.confettiCanvas.getContext('2d');
  let confettiActive = false;
  let confettiParticles = [];
  const confettiColors = [
    '#A259FF', '#00F2C3', '#FFA502', '#FF4757', '#33CCFF', '#FF9F43'
  ];

  function resizeCanvas() {
    elements.confettiCanvas.width = window.innerWidth;
    elements.confettiCanvas.height = window.innerHeight;
  }
  
  window.addEventListener('resize', resizeCanvas);
  resizeCanvas();

  class Confetti {
    constructor(x, y, angle, spread) {
      this.x = x;
      this.y = y;
      this.radius = Math.random() * 4 + 3;
      this.color = confettiColors[Math.floor(Math.random() * confettiColors.length)];
      
      const velocity = Math.random() * 12 + 8;
      const radAngle = (angle + (Math.random() * spread - spread / 2)) * Math.PI / 180;
      
      this.vx = Math.cos(radAngle) * velocity;
      this.vy = Math.sin(radAngle) * velocity;
      this.gravity = 0.35;
      this.friction = 0.98;
      this.rotation = Math.random() * 360;
      this.rotationSpeed = Math.random() * 8 - 4;
      this.opacity = 1.0;
    }

    update() {
      this.vx *= this.friction;
      this.vy *= this.friction;
      this.vy += this.gravity;
      this.x += this.vx;
      this.y += this.vy;
      this.rotation += this.rotationSpeed;
      this.opacity -= 0.015;
    }

    draw() {
      ctx.save();
      ctx.translate(this.x, this.y);
      ctx.rotate(this.rotation * Math.PI / 180);
      ctx.globalAlpha = this.opacity;
      ctx.fillStyle = this.color;
      
      // Dibujar rectángulos o círculos pequeños aleatorios
      if (Math.random() > 0.5) {
        ctx.fillRect(-this.radius, -this.radius, this.radius * 2, this.radius);
      } else {
        ctx.beginPath();
        ctx.arc(0, 0, this.radius, 0, Math.PI * 2);
        ctx.fill();
      }
      
      ctx.restore();
    }
  }

  function launchCelebration() {
    confettiParticles = [];
    confettiActive = true;
    
    // Lanzar confeti desde la izquierda inferior
    for (let i = 0; i < 70; i++) {
      confettiParticles.push(new Confetti(0, window.innerHeight, -45, 35));
    }
    
    // Lanzar confeti desde la derecha inferior
    for (let i = 0; i < 70; i++) {
      confettiParticles.push(new Confetti(window.innerWidth, window.innerHeight, -135, 35));
    }
    
    animateConfetti();
  }

  function animateConfetti() {
    if (!confettiActive) return;
    
    ctx.clearRect(0, 0, elements.confettiCanvas.width, elements.confettiCanvas.height);
    
    confettiParticles.forEach((particle, idx) => {
      particle.update();
      particle.draw();
      
      if (particle.opacity <= 0 || particle.y > window.innerHeight) {
        confettiParticles.splice(idx, 1);
      }
    });

    if (confettiParticles.length > 0) {
      requestAnimationFrame(animateConfetti);
    } else {
      confettiActive = false;
      ctx.clearRect(0, 0, elements.confettiCanvas.width, elements.confettiCanvas.height);
    }
  }

  // ==========================================================================
  // 5. CÁLCULO DE RACHAS (Streak System)
  // ==========================================================================
  
  function checkAndCalculateStreak() {
    if (state.completed.length === 0) {
      state.streak = 0;
      state.lastCompletedDate = null;
      return;
    }

    // Ordenar completadas por fecha de finalización descendente
    const sortedCompleted = [...state.completed].sort((a, b) => new Date(b.completedAt) - new Date(a.completedAt));
    const latest = new Date(sortedCompleted[0].completedAt);
    
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    const latestDateOnly = new Date(latest);
    latestDateOnly.setHours(0, 0, 0, 0);

    const diffTime = today - latestDateOnly;
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));

    if (diffDays > 1) {
      // Si ha pasado más de un día sin cumplir tareas, la racha se rompe
      state.streak = 0;
    } else {
      // Calcular racha de días consecutivos
      let calculatedStreak = 1;
      let lastDate = latestDateOnly;
      
      // Creamos un Set de fechas únicas completadas para comprobar consecutividad
      const dateSet = new Set();
      sortedCompleted.forEach(item => {
        const d = new Date(item.completedAt);
        d.setHours(0, 0, 0, 0);
        dateSet.add(d.getTime());
      });

      let checkDate = new Date(lastDate);
      while (true) {
        // Retroceder un día
        checkDate.setDate(checkDate.getDate() - 1);
        if (dateSet.has(checkDate.getTime())) {
          calculatedStreak++;
        } else {
          break;
        }
      }
      state.streak = calculatedStreak;
    }
    
    state.lastCompletedDate = latest.toISOString();
  }

  // ==========================================================================
  // 6. PERSISTENCIA (localStorage)
  // ==========================================================================
  
  function saveToLocalStorage() {
    localStorage.setItem('acuerdate_active', JSON.stringify(state.reminders));
    localStorage.setItem('acuerdate_completed', JSON.stringify(state.completed));
    localStorage.setItem('acuerdate_streak', state.streak.toString());
    localStorage.setItem('acuerdate_last_completed_date', state.lastCompletedDate || '');
  }

  function loadFromLocalStorage() {
    try {
      const activeData = localStorage.getItem('acuerdate_active');
      const completedData = localStorage.getItem('acuerdate_completed');
      const streakData = localStorage.getItem('acuerdate_streak');
      const lastCompletedDateData = localStorage.getItem('acuerdate_last_completed_date');
      
      state.reminders = activeData ? JSON.parse(activeData) : [];
      state.completed = completedData ? JSON.parse(completedData) : [];
      state.streak = streakData ? parseInt(streakData, 10) : 0;
      state.lastCompletedDate = lastCompletedDateData || null;
      
      // Validar racha en la carga
      checkAndCalculateStreak();
      saveToLocalStorage();
      
    } catch (e) {
      console.error("Error cargando desde localStorage, reiniciando datos:", e);
      state.reminders = [];
      state.completed = [];
      state.streak = 0;
    }
  }

  // ==========================================================================
  // 7. MOTOR DE RENDERIZADO (DOM Rendering)
  // ==========================================================================
  
  /**
   * Traduce la categoría técnica a formato estético legible.
   */
  function getCategoryText(cat) {
    const cats = {
      importante: 'Importante',
      habito: 'Hábito',
      tarea: 'Tarea',
      idea: 'Idea',
      nota: 'Nota'
    };
    return cats[cat] || cat;
  }

  /**
   * Traduce la prioridad técnica a formato estético legible.
   */
  function getPriorityText(priority) {
    const prios = {
      high: 'Alta',
      medium: 'Media',
      low: 'Baja'
    };
    return prios[priority] || priority;
  }

  /**
   * Formatea la fecha y calcula la duración transcurrida en texto amigable.
   */
  function getElapsedTimeText(createdAt) {
    const start = new Date(createdAt);
    const now = new Date();
    const diffMs = now - start;
    
    const diffSecs = Math.floor(diffMs / 1000);
    const diffMins = Math.floor(diffSecs / 60);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffSecs < 60) return `hace ${diffSecs} s`;
    if (diffMins < 60) return `hace ${diffMins} min`;
    if (diffHours < 24) return `hace ${diffHours} h`;
    return `hace ${diffDays} d`;
  }

  /**
   * Determina la intensidad de pulsación del ticker según el tiempo activo.
   * Promueve urgencia para recordatorios antiguos.
   */
  function getTimerPulseClass(createdAt) {
    const start = new Date(createdAt);
    const now = new Date();
    const diffHours = (now - start) / (1000 * 60 * 60);

    if (diffHours >= 72) return 'pulse-fast';   // Más de 3 días: Pulsación rápida roja
    if (diffHours >= 24) return 'pulse-medium'; // Más de 1 día: Pulsación media ámbar
    return 'pulse-slow';                        // Menos de 1 día: Pulsación lenta verde
  }

  /**
   * Calcula el tiempo total invertido en cumplir un recordatorio.
   */
  function getCompletionDurationText(createdAt, completedAt) {
    const start = new Date(createdAt);
    const end = new Date(completedAt);
    const diffMs = end - start;
    
    const diffMins = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffMins < 1) return 'en menos de un minuto';
    if (diffMins < 60) return `en ${diffMins} min`;
    if (diffHours < 24) return `en ${diffHours} horas`;
    return `en ${diffDays} días`;
  }

  /**
   * Formatea una cadena ISO a una fecha legible en español.
   */
  function formatDate(isoString) {
    const date = new Date(isoString);
    return date.toLocaleDateString('es-ES', { 
      day: 'numeric', 
      month: 'short', 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  }

  /**
   * Genera el HTML de la insignia de fecha de vencimiento (Booking)
   */
  function renderDueDateBadge(dueDateIso) {
    const dueDate = new Date(dueDateIso);
    const now = new Date();
    const diffMs = dueDate - now;
    const isOverdue = diffMs < 0;
    const absDiff = Math.abs(diffMs);
    
    const diffSecs = Math.floor(absDiff / 1000);
    const diffMins = Math.floor(diffSecs / 60);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);
    
    let timeStr = '';
    if (diffDays > 0) {
      timeStr = `${diffDays}d ${diffHours % 24}h`;
    } else if (diffHours > 0) {
      timeStr = `${diffHours}h ${diffMins % 60}m`;
    } else if (diffMins > 0) {
      timeStr = `${diffMins}m ${diffSecs % 60}s`;
    } else {
      timeStr = `${diffSecs}s`;
    }

    const badgeClass = isOverdue ? 'reminder-date-badge overdue' : 'reminder-date-badge';
    const label = isOverdue ? `Vencido hace ${timeStr}` : `Vence en ${timeStr}`;
    const formattedDate = dueDate.toLocaleString('es-ES', { 
      day: 'numeric', 
      month: 'short', 
      hour: '2-digit', 
      minute: '2-digit' 
    });
    
    return `
      <div class="${badgeClass}" title="Programado para: ${formattedDate}">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
          <line x1="16" y1="2" x2="16" y2="6"></line>
          <line x1="8" y1="2" x2="8" y2="6"></line>
          <line x1="3" y1="10" x2="21" y2="10"></line>
        </svg>
        <span>${label} (${formattedDate})</span>
      </div>
    `;
  }

  /**
   * Renderiza el dashboard de estadísticas
   */
  function renderStats() {
    const activeCount = state.reminders.length;
    const completedCount = state.completed.length;
    const total = activeCount + completedCount;
    
    // Tasa de eficiencia / éxito
    const efficiency = total > 0 ? Math.round((completedCount / total) * 100) : 0;
    
    // Asignación de textos con transiciones de números
    elements.statActiveCount.textContent = activeCount;
    elements.statCompletedCount.textContent = completedCount;
    elements.statEfficiency.textContent = `${efficiency}%`;
    elements.streakValue.textContent = state.streak;
    
    updateHabitPlanet();
  }

  /**
   * Actualiza dinámicamente el estado estético y del Planeta de Hábitos
   */
  function updateHabitPlanet() {
    if (!elements.planetSphere || !elements.planetStatus) return;
    
    // Comprobar si hay alguna reserva activa que ya se venció
    const nowStr = new Date().toISOString();
    const hasOverdue = state.reminders.some(r => r.dueDate && r.dueDate < nowStr);
    
    elements.planetSphere.className = 'planet-sphere';
    
    if (hasOverdue) {
      elements.planetSphere.classList.add('state-peligro');
      elements.planetStatus.textContent = '¡Peligro!';
      elements.planetStatus.style.color = 'var(--color-danger)';
      document.getElementById('planetCard').style.setProperty('--planet-glow-color', 'var(--color-danger)');
    } else if (state.streak > 3) {
      elements.planetSphere.classList.add('state-radiante');
      elements.planetStatus.textContent = 'Radiante';
      elements.planetStatus.style.color = '#ffd43b';
      document.getElementById('planetCard').style.setProperty('--planet-glow-color', '#e8590c');
    } else if (state.streak >= 1) {
      elements.planetSphere.classList.add('state-estable');
      elements.planetStatus.textContent = 'Estable';
      elements.planetStatus.style.color = '#34d399';
      document.getElementById('planetCard').style.setProperty('--planet-glow-color', 'var(--color-success)');
    } else {
      elements.planetSphere.classList.add('state-inerte');
      elements.planetStatus.textContent = 'Inerte';
      elements.planetStatus.style.color = '#9ca3af';
      document.getElementById('planetCard').style.setProperty('--planet-glow-color', '#9ca3af');
    }
  }

  /**
   * Renderiza la lista principal de recordatorios activos con sus contadores en vivo
   */
  function renderActiveReminders() {
    const filtered = state.reminders.filter(item => 
      state.currentFilter === 'all' || item.category === state.currentFilter
    );

    // Si está vacío, mostrar el empty state
    if (filtered.length === 0) {
      elements.emptyState.style.display = 'flex';
      
      // Ajustar texto de empty state si hay un filtro aplicado
      if (state.currentFilter !== 'all') {
        elements.emptyState.querySelector('h3').textContent = 'No hay recordatorios en esta categoría';
        elements.emptyState.querySelector('p').textContent = `Agrega un nuevo recordatorio de tipo "${getCategoryText(state.currentFilter)}" para empezar.`;
        elements.emptyStateAddBtn.style.display = 'inline-flex';
      } else {
        elements.emptyState.querySelector('h3').textContent = '¡Tu mente está libre de pendientes!';
        elements.emptyState.querySelector('p').textContent = 'Añade un recordatorio continuo que persistirá todos los días hasta que lo cumplas.';
        elements.emptyStateAddBtn.style.display = 'inline-flex';
      }
      
      // Limpiar tarjetas anteriores excepto el empty state
      const cards = elements.remindersGrid.querySelectorAll('.reminder-card');
      cards.forEach(c => c.remove());
      return;
    }

    elements.emptyState.style.display = 'none';

    // Algoritmo de reconciliación simple basado en IDs.
    const currentCards = Array.from(elements.remindersGrid.querySelectorAll('.reminder-card'));
    const currentIds = currentCards.map(c => c.dataset.id);
    const newIds = filtered.map(item => item.id);

    // 1. Eliminar tarjetas que ya no existen en los recordatorios activos
    currentCards.forEach(card => {
      if (!newIds.includes(card.dataset.id)) {
        card.classList.add('reminder-fade-out');
        card.addEventListener('animationend', () => card.remove());
      }
    });

    // 2. Insertar o actualizar tarjetas
    filtered.forEach((item, index) => {
      let card = elements.remindersGrid.querySelector(`.reminder-card[data-id="${item.id}"]`);
      
      const timerPulse = getTimerPulseClass(item.createdAt);
      const elapsedTime = getElapsedTimeText(item.createdAt);

      if (!card) {
        // CREAR TARJETA NUEVA
        card = document.createElement('div');
        card.className = 'reminder-card reminder-fade-in';
        card.dataset.id = item.id;
        
        // Colores y glows específicos según su categoría en HSL
        let catHue = 265; // Violet default
        if (item.category === 'importante') catHue = 345;
        if (item.category === 'habito') catHue = 175;
        if (item.category === 'tarea') catHue = 38;
        if (item.category === 'idea') catHue = 195;
        if (item.category === 'nota') catHue = 265;
        
        card.style.setProperty('--cat-hue', catHue);
        card.style.setProperty('--cat-color', `var(--color-${item.category})`);
        card.style.setProperty('--cat-color-glow', `var(--color-${item.category}-glow)`);

        card.innerHTML = `
          <div class="card-header">
            <span class="card-badge-category">${getCategoryText(item.category)}</span>
            ${item.recurrence && item.recurrence !== 'none' ? `
              <span class="card-badge-recurrence recurrence-${item.recurrence}" title="Hábito recurrente">
                ${item.recurrence === 'daily' ? 'Diario 🔁' : 'Semanal 🔁'}
              </span>
            ` : ''}
            <span class="card-badge-priority priority-${item.priority}">${getPriorityText(item.priority)}</span>
          </div>
          <div class="card-body">
            <p class="reminder-text-content">${escapeHTML(item.text)}</p>
            ${item.dueDate ? renderDueDateBadge(item.dueDate) : ''}
          </div>
          <div class="card-footer">
            <div class="timer-wrapper">
              <span class="timer-dot ${timerPulse}"></span>
              <span class="timer-text">${elapsedTime}</span>
            </div>
            <div class="card-actions">
              <button class="action-btn action-btn-complete" title="Marcar como cumplido">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
              </button>
              <button class="action-btn action-btn-delete" title="Eliminar recordatorio">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="3 6 5 6 21 6"></polyline>
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                </svg>
              </button>
            </div>
          </div>
        `;

        // Asignación de oyentes
        card.querySelector('.action-btn-complete').addEventListener('click', () => completeReminder(item.id));
        card.querySelector('.action-btn-delete').addEventListener('click', () => deleteReminder(item.id));

        // Insertar en la posición correcta en el grid
        if (index === 0) {
          elements.remindersGrid.prepend(card);
        } else {
          const prevCard = elements.remindersGrid.querySelector(`.reminder-card[data-id="${filtered[index - 1].id}"]`);
          if (prevCard) {
            prevCard.after(card);
          } else {
            elements.remindersGrid.appendChild(card);
          }
        }
      } else {
        // ACTUALIZAR CONTENIDOS DINÁMICOS EXISTENTES (Para evitar parpadeos)
        const textElement = card.querySelector('.reminder-text-content');
        if (textElement.textContent !== item.text) {
          textElement.textContent = item.text;
        }
        
        // Actualizar el contador de tiempo y su clase de pulsación
        const dotElement = card.querySelector('.timer-dot');
        dotElement.className = `timer-dot ${timerPulse}`;
        
        const timerTextElement = card.querySelector('.timer-text');
        timerTextElement.textContent = elapsedTime;

        // Actualizar o añadir la insignia de fecha programada
        const bodyElement = card.querySelector('.card-body');
        const existingBadge = bodyElement.querySelector('.reminder-date-badge');
        if (item.dueDate) {
          const newBadgeHTML = renderDueDateBadge(item.dueDate);
          if (existingBadge) {
            const tempDiv = document.createElement('div');
            tempDiv.innerHTML = newBadgeHTML;
            existingBadge.replaceWith(tempDiv.firstElementChild);
          } else {
            const tempDiv = document.createElement('div');
            tempDiv.innerHTML = newBadgeHTML;
            bodyElement.appendChild(tempDiv.firstElementChild);
          }
        } else if (existingBadge) {
          existingBadge.remove();
        }
      }
    });
  }

  /**
   * Renderiza el historial de recordatorios cumplidos (salida)
   */
  function renderHistory() {
    const count = state.completed.length;
    elements.historyCount.textContent = count;

    if (count === 0) {
      elements.historyList.innerHTML = `<p class="history-empty">Aún no has cumplido ningún recordatorio. ¡Da el primer paso hoy!</p>`;
      elements.clearHistoryBtn.style.display = 'none';
      return;
    }

    elements.clearHistoryBtn.style.display = 'inline-flex';

    // Ordenar completados por fecha de finalización descendente (más recientes arriba)
    const sortedCompleted = [...state.completed].sort((a, b) => new Date(b.completedAt) - new Date(a.completedAt));

    elements.historyList.innerHTML = sortedCompleted.map(item => `
      <div class="history-item" data-id="${item.id}">
        <div class="history-item-details">
          <p class="history-item-text">${escapeHTML(item.text)}</p>
          <div class="history-item-meta">
            <span class="history-item-category" style="color: var(--color-${item.category});">${getCategoryText(item.category)}</span>
            <span>&bull;</span>
            <span>Cumplido el ${formatDate(item.completedAt)}</span>
            <span>&bull;</span>
            <span class="history-item-duration">Logrado ${getCompletionDurationText(item.createdAt, item.completedAt)}</span>
          </div>
        </div>
        <div class="history-item-actions">
          <button class="btn btn-secondary btn-sm btn-restore" title="Restaurar a recordatorios activos" onclick="restoreReminder('${item.id}')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="23 4 23 10 17 10"></polyline>
              <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
            </svg>
            <span>Restaurar</span>
          </button>
        </div>
      </div>
    `).join('');
  }

  // Registrar globalmente la función de restauración para que funcione el evento onclick del string HTML
  window.restoreReminder = restoreReminder;

  /**
   * Escapa caracteres HTML para evitar ataques XSS
   */
  function escapeHTML(str) {
    return str
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");
  }

  function updateUI() {
    renderStats();
    renderActiveReminders();
    renderHistory();
  }

  // ==========================================================================
  // 8. FUNCIONES DE ACCIONES (Controladores de Estado)
  // ==========================================================================
  
  /**
   * Crea un nuevo recordatorio activo
   */
  function createReminder(text, category, priority, dueDate = null, recurrence = 'none') {
    const newReminder = {
      id: 'rem_' + Date.now() + '_' + Math.random().toString(36).substr(2, 5),
      text: text,
      category: category,
      priority: priority,
      createdAt: new Date().toISOString(),
      dueDate: dueDate,
      recurrence: recurrence,
      alerted: false
    };

    state.reminders.unshift(newReminder); // Agregar al inicio para visualización inmediata
    
    // Play the user's selected alert tone on creation
    const toneKey = localStorage.getItem('remember_selected_tone') || 'chime';
    const toneMap = {
      chime:     () => playChime('create'),
      celestial: () => playChime('success'),
      sos:       () => playChime('sos'),
      bell:      () => playChime('bell'),
      cyber:     () => playChime('cyber')
    };
    (toneMap[toneKey] || toneMap.chime)();
    if ('vibrate' in navigator) {
      navigator.vibrate(30); // Microvibración táctil al crear
    }
    saveToLocalStorage();
    updateUI();
  }

  /**
   * Completa un recordatorio (Salida de datos con efectos de celebración)
   */
  function completeReminder(id) {
    const index = state.reminders.findIndex(r => r.id === id);
    if (index === -1) return;

    // Buscar tarjeta correspondiente en el DOM para aplicar animación de salida
    const card = elements.remindersGrid.querySelector(`.reminder-card[data-id="${id}"]`);
    if (card) {
      card.classList.add('reminder-fade-out');
      card.addEventListener('animationend', () => {
        executeCompletion(index);
      }, { once: true });
    } else {
      executeCompletion(index);
    }
  }

  function executeCompletion(index) {
    const reminder = state.reminders[index];
    const completedItem = {
      ...reminder,
      completedAt: new Date().toISOString()
    };

    // Celebrar!
    playChime('success');
    launchCelebration();
    if ('vibrate' in navigator) {
      navigator.vibrate([60, 40, 60]); // Patrón de éxito de dos pulsos táctiles
    }

    if (reminder.recurrence && reminder.recurrence !== 'none') {
      // Hábito Recurrente: Mantener activo y reprogramar
      state.completed.push(completedItem);
      
      if (reminder.dueDate) {
        const currentDue = new Date(reminder.dueDate);
        const daysToAdd = reminder.recurrence === 'daily' ? 1 : 7;
        currentDue.setDate(currentDue.getDate() + daysToAdd);
        reminder.dueDate = currentDue.toISOString();
      }
      
      // Reiniciar tiempo transcurrido (createdAt) y bandera de alerta
      reminder.createdAt = new Date().toISOString();
      reminder.alerted = false;
    } else {
      // Recordatorio Único: Quitar de activos y añadir a completados
      state.reminders.splice(index, 1);
      state.completed.push(completedItem);
    }
    
    // Recalcular racha
    checkAndCalculateStreak();
    
    saveToLocalStorage();
    updateUI();
    
    // Abrir automáticamente el panel de logros colapsado
    if (!elements.historyDetails.hasAttribute('open')) {
      elements.historyDetails.setAttribute('open', '');
    }
  }

  /**
   * Elimina permanentemente un recordatorio activo
   */
  function deleteReminder(id) {
    if (confirm('¿Estás seguro de que deseas descartar permanentemente este recordatorio continuo sin completarlo?')) {
      const card = elements.remindersGrid.querySelector(`.reminder-card[data-id="${id}"]`);
      const index = state.reminders.findIndex(r => r.id === id);
      
      if (index === -1) return;

      if ('vibrate' in navigator) {
        navigator.vibrate([100, 50, 100]); // Vibración táctil de advertencia
      }
      if (card) {
        card.classList.add('reminder-fade-out');
        card.addEventListener('animationend', () => {
          state.reminders.splice(index, 1);
          saveToLocalStorage();
          updateUI();
        }, { once: true });
      } else {
        state.reminders.splice(index, 1);
        saveToLocalStorage();
        updateUI();
      }
    }
  }

  /**
   * Restaura un recordatorio cumplido de vuelta a la lista de activos
   */
  function restoreReminder(id) {
    const index = state.completed.findIndex(r => r.id === id);
    if (index === -1) return;

    const completedItem = state.completed[index];
    
    const restoredReminder = {
      id: completedItem.id,
      text: completedItem.text,
      category: completedItem.category,
      priority: completedItem.priority,
      createdAt: completedItem.createdAt,
      dueDate: completedItem.dueDate || null,
      alerted: false
    };

    state.completed.splice(index, 1);
    state.reminders.push(restoredReminder); // Enviar a activos

    playChime('create');
    
    checkAndCalculateStreak();
    saveToLocalStorage();
    updateUI();
  }

  /**
   * Limpia todo el historial de recordatorios completados
   */
  function clearHistory() {
    if (confirm('¿Deseas vaciar por completo tu historial de recuerdos cumplidos? Esta acción no se puede deshacer.')) {
      state.completed = [];
      state.streak = 0;
      state.lastCompletedDate = null;
      
      saveToLocalStorage();
      updateUI();
    }
  }

  // ==========================================================================
  // 9. EVENTOS Y CONTROLADORES DE DIÁLOGOS
  // ==========================================================================
  
  function openModal() {
    elements.reminderText.value = '';
    elements.enableBooking.checked = false;
    elements.bookingDateGroup.classList.add('hidden');
    elements.reminderDate.value = '';
    resetVoiceUI();
    
    elements.dialog.showModal();
    elements.reminderText.focus();
    
    initAudio();
  }

  function closeModal() {
    if (state.isRecording) {
      recognition.stop();
    }
    elements.dialog.close();
  }

  // Eventos de apertura/cierre
  elements.openDialogBtn.addEventListener('click', openModal);

  // ==========================================================================
  // SOUND SETTINGS — Tone Selection, Persistence & Modal Logic
  // ==========================================================================

  const TONE_STORAGE_KEY = 'remember_selected_tone';
  const TONE_DEFAULTS = {
    chime:     { play: () => playChime('create') },
    celestial: { play: () => playChime('success') },
    sos:       { play: () => playChime('sos') },
    bell:      { play: () => playChime('bell') },
    cyber:     { play: () => playChime('cyber') }
  };

  // Load persisted tone or default to 'chime'
  let selectedAlertTone = localStorage.getItem(TONE_STORAGE_KEY) || 'chime';

  const soundDialog = document.getElementById('soundSettingsDialog');
  const soundSettingsBtn = document.getElementById('soundSettingsBtn');
  const closeSoundSettingsBtn = document.getElementById('closeSoundSettingsBtn');
  const saveSoundSettingsBtn = document.getElementById('saveSoundSettingsBtn');
  const toneOptions = document.querySelectorAll('.tone-option');

  function syncToneUI() {
    toneOptions.forEach(opt => {
      const isActive = opt.dataset.tone === selectedAlertTone;
      opt.classList.toggle('is-selected', isActive);
      const radio = opt.querySelector('input[type="radio"]');
      if (radio) radio.checked = isActive;
    });
    // Update soundSettingsBtn icon color to indicate active tone
    if (soundSettingsBtn) {
      soundSettingsBtn.style.color = selectedAlertTone === 'cyber' ? '#00F2FE' :
        selectedAlertTone === 'celestial' ? '#a259ff' :
        selectedAlertTone === 'sos' ? '#ff4757' :
        selectedAlertTone === 'bell' ? '#ffd200' : '#00F2FE';
    }
  }

  function openSoundSettings() {
    syncToneUI();
    if (soundDialog) soundDialog.showModal();
  }

  function closeSoundSettings() {
    if (soundDialog) soundDialog.close();
  }

  if (soundSettingsBtn) {
    soundSettingsBtn.addEventListener('click', () => {
      initAudio();
      openSoundSettings();
      soundSettingsBtn.style.transform = 'scale(0.85)';
      setTimeout(() => { soundSettingsBtn.style.transform = 'scale(1)'; }, 150);
    });
  }

  if (closeSoundSettingsBtn) {
    closeSoundSettingsBtn.addEventListener('click', closeSoundSettings);
  }

  if (saveSoundSettingsBtn) {
    saveSoundSettingsBtn.addEventListener('click', () => {
      localStorage.setItem(TONE_STORAGE_KEY, selectedAlertTone);
      closeSoundSettings();
      // Confirm save with the newly selected tone
      initAudio();
      if (TONE_DEFAULTS[selectedAlertTone]) TONE_DEFAULTS[selectedAlertTone].play();
    });
  }

  // Click-to-preview on each tone row
  toneOptions.forEach(opt => {
    opt.addEventListener('click', () => {
      const tone = opt.dataset.tone;
      if (!tone) return;
      selectedAlertTone = tone;
      syncToneUI();
      // Instant preview
      initAudio();
      if (TONE_DEFAULTS[tone]) TONE_DEFAULTS[tone].play();
    });
  });

  // Light-dismiss for sound settings dialog
  if (soundDialog) {
    soundDialog.addEventListener('click', e => {
      const rect = soundDialog.getBoundingClientRect();
      const inside = rect.top <= e.clientY && e.clientY <= rect.bottom &&
                     rect.left <= e.clientX && e.clientX <= rect.right;
      if (!inside) closeSoundSettings();
    });
  }

  // Initial UI sync on load
  syncToneUI();

  elements.emptyStateAddBtn.addEventListener('click', openModal);
  elements.closeDialogBtn.addEventListener('click', closeModal);
  elements.cancelDialogBtn.addEventListener('click', closeModal);

  // Fallback de light-dismiss para diálogos en navegadores sin soporte de `closedby` nativo
  if (!('closedBy' in HTMLDialogElement.prototype)) {
    elements.dialog.addEventListener('click', (event) => {
      if (event.target !== elements.dialog) return;

      const rect = elements.dialog.getBoundingClientRect();
      const isDialogContent = (
        rect.top <= event.clientY &&
        event.clientY <= rect.top + rect.height &&
        rect.left <= event.clientX &&
        event.clientX <= rect.left + rect.width
      );

      if (!isDialogContent) {
        closeModal();
      }
    });
  }

  // Evento de envío del formulario
  elements.reminderForm.addEventListener('submit', (e) => {
    e.preventDefault();
    
    const text = elements.reminderText.value.trim();
    const category = elements.reminderForm.elements['category'].value;
    const priority = elements.reminderForm.elements['priority'].value;
    
    let dueDate = null;
    if (elements.enableBooking.checked && elements.reminderDate.value) {
      dueDate = new Date(elements.reminderDate.value).toISOString();
    }
    
    const recurrence = elements.recurrence ? elements.recurrence.value : 'none';

    if (text) {
      createReminder(text, category, priority, dueDate, recurrence);
      closeModal();
      
      // Si estamos en el calendario, actualizarlo inmediatamente
      if (state.viewMode === 'calendar') {
        renderCalendar();
        if (state.selectedDate) {
          renderDayDetails(state.selectedDate);
        }
      }
    }
  });

  // Evento del botón de micrófono de voz
  elements.voiceBtn.addEventListener('click', (e) => {
    e.preventDefault();
    e.stopPropagation();
    toggleVoiceRecording();
  });

  // Eventos de Filtro de Categoría (solo aplicable en Vista Lista)
  elements.filterBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      elements.filterBtns.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      
      state.currentFilter = btn.dataset.filter;
      renderActiveReminders();
    });
  });

  // Evento para limpiar historial
  elements.clearHistoryBtn.addEventListener('click', clearHistory);

  // ==========================================================================
  // 10. LÓGICA E INTERFAZ DE BOOKINGS (RESERVAS) Y CALENDARIO MENSUAL
  // ==========================================================================

  // Escuchar el switch de activación de fecha programada
  elements.enableBooking.addEventListener('change', () => {
    if (elements.enableBooking.checked) {
      elements.bookingDateGroup.classList.remove('hidden');
      if (!elements.reminderDate.value) {
        const defaultDate = new Date();
        defaultDate.setHours(defaultDate.getHours() + 1);
        
        const yyyy = defaultDate.getFullYear();
        const mm = String(defaultDate.getMonth() + 1).padStart(2, '0');
        const dd = String(defaultDate.getDate()).padStart(2, '0');
        const hh = String(defaultDate.getHours()).padStart(2, '0');
        const min = String(defaultDate.getMinutes()).padStart(2, '0');
        
        elements.reminderDate.value = `${yyyy}-${mm}-${dd}T${hh}:${min}`;
      }
    } else {
      elements.bookingDateGroup.classList.add('hidden');
    }
  });

  // Alternador de Vistas (Lista vs Calendario)
  function switchView(mode) {
    state.viewMode = mode;
    
    if (mode === 'list') {
      elements.listViewBtn.classList.add('active');
      elements.calendarViewBtn.classList.remove('active');
      
      elements.remindersGrid.classList.remove('hidden');
      document.querySelector('.section-header .filters').classList.remove('hidden');
      elements.calendarViewContainer.classList.add('hidden');
    } else {
      elements.listViewBtn.classList.remove('active');
      elements.calendarViewBtn.classList.add('active');
      
      elements.remindersGrid.classList.add('hidden');
      document.querySelector('.section-header .filters').classList.add('hidden');
      elements.calendarViewContainer.classList.remove('hidden');
      
      renderCalendar();
      
      // Auto-seleccionar hoy por defecto si no hay selección previa
      if (!state.selectedDate) {
        const today = new Date();
        setTimeout(() => {
          const todayCell = elements.calendarDays.querySelector('.day-today');
          if (todayCell) {
            selectCalendarDay(today, todayCell);
          } else {
            state.selectedDate = today;
            elements.calendarDayDetails.classList.remove('hidden');
            renderDayDetails(today);
          }
        }, 50);
      } else {
        renderDayDetails(state.selectedDate);
      }
    }
  }

  elements.listViewBtn.addEventListener('click', () => switchView('list'));
  elements.calendarViewBtn.addEventListener('click', () => switchView('calendar'));

  // Botones de navegación del calendario mensual
  elements.prevMonthBtn.addEventListener('click', () => {
    state.calendarCurrentDate.setMonth(state.calendarCurrentDate.getMonth() - 1);
    renderCalendar();
  });
  
  elements.nextMonthBtn.addEventListener('click', () => {
    state.calendarCurrentDate.setMonth(state.calendarCurrentDate.getMonth() + 1);
    renderCalendar();
  });

  // Botón para añadir recordatorio desde los detalles del día seleccionado
  elements.calendarAddReminderBtn.addEventListener('click', () => {
    if (state.selectedDate) {
      openModalWithDate(state.selectedDate);
    } else {
      openModal();
    }
  });

  /**
   * Abre el modal preconfigurando la fecha del día seleccionado
   */
  function openModalWithDate(date) {
    openModal();
    elements.enableBooking.checked = true;
    elements.bookingDateGroup.classList.remove('hidden');
    
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    
    const now = new Date();
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    
    elements.reminderDate.value = `${year}-${month}-${day}T${hours}:${minutes}`;
  }

  /**
   * Dibuja dinámicamente la cuadrícula de 42 celdas del mes
   */
  function renderCalendar() {
    const year = state.calendarCurrentDate.getFullYear();
    const month = state.calendarCurrentDate.getMonth();
    
    // Configurar título del mes actual
    const monthNames = [
      'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
      'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
    ];
    elements.calendarCurrentMonth.textContent = `${monthNames[month]} ${year}`;
    
    // Primer día del mes (1)
    const firstDay = new Date(year, month, 1);
    // Su día de la semana (0 = Dom, 1 = Lun, ..., 6 = Sáb)
    let startDay = firstDay.getDay();
    // Ajustar para que la semana empiece en Lunes (Lunes = 0, ..., Domingo = 6)
    startDay = startDay === 0 ? 6 : startDay - 1;
    
    // Días del mes actual
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    // Días del mes anterior
    const daysInPrevMonth = new Date(year, month, 0).getDate();
    
    elements.calendarDays.innerHTML = '';
    
    let dayCount = 1;
    let nextMonthCount = 1;
    
    for (let i = 0; i < 42; i++) {
      const dayCell = document.createElement('div');
      dayCell.className = 'calendar-day';
      
      let cellDayNum;
      let cellDate;
      
      if (i < startDay) {
        // Relleno de mes anterior
        cellDayNum = daysInPrevMonth - startDay + i + 1;
        cellDate = new Date(year, month - 1, cellDayNum);
        dayCell.classList.add('day-other-month');
      } else if (dayCount <= daysInMonth) {
        // Mes actual
        cellDayNum = dayCount;
        cellDate = new Date(year, month, cellDayNum);
        dayCount++;
      } else {
        // Relleno de mes siguiente
        cellDayNum = nextMonthCount;
        cellDate = new Date(year, month + 1, cellDayNum);
        nextMonthCount++;
        dayCell.classList.add('day-other-month');
      }
      
      dayCell.dataset.date = cellDate.toISOString();
      dayCell.innerHTML = `<span class="day-number">${cellDayNum}</span>`;
      
      // Resaltar Hoy
      const today = new Date();
      if (cellDate.getDate() === today.getDate() &&
          cellDate.getMonth() === today.getMonth() &&
          cellDate.getFullYear() === today.getFullYear()) {
        dayCell.classList.add('day-today');
      }
      
      // Resaltar Seleccionado
      if (state.selectedDate && 
          cellDate.getDate() === state.selectedDate.getDate() &&
          cellDate.getMonth() === state.selectedDate.getMonth() &&
          cellDate.getFullYear() === state.selectedDate.getFullYear()) {
        dayCell.classList.add('day-selected');
      }
      
      // Neon Dots por Categorías
      const dayReminders = state.reminders.filter(r => {
        if (!r.dueDate) return false;
        const d = new Date(r.dueDate);
        return d.getDate() === cellDate.getDate() &&
               d.getMonth() === cellDate.getMonth() &&
               d.getFullYear() === cellDate.getFullYear();
      });
      
      if (dayReminders.length > 0) {
        const dotsContainer = document.createElement('div');
        dotsContainer.className = 'day-dots';
        
        // Mostrar máximo 4 puntos para mantener la limpieza visual
        dayReminders.slice(0, 4).forEach(r => {
          const dot = document.createElement('span');
          dot.className = 'day-dot';
          dot.style.setProperty('--dot-color', `var(--color-${r.category})`);
          dotsContainer.appendChild(dot);
        });
        
        dayCell.appendChild(dotsContainer);
      }
      
      // Control de clicks
      dayCell.addEventListener('click', () => {
        selectCalendarDay(cellDate, dayCell);
      });
      
      // Control de doble click para creación rápida
      dayCell.addEventListener('dblclick', () => {
        openModalWithDate(cellDate);
      });
      
      elements.calendarDays.appendChild(dayCell);
    }
  }

  /**
   * Selecciona un día específico e inicia el pintado de sus detalles
   */
  function selectCalendarDay(date, cellElement) {
    state.selectedDate = date;
    
    const cells = elements.calendarDays.querySelectorAll('.calendar-day');
    cells.forEach(c => c.classList.remove('day-selected'));
    cellElement.classList.add('day-selected');
    
    elements.calendarDayDetails.classList.remove('hidden');
    
    const formatted = date.toLocaleDateString('es-ES', {
      weekday: 'long',
      day: 'numeric',
      month: 'long'
    });
    elements.selectedDayTitle.textContent = formatted.charAt(0).toUpperCase() + formatted.slice(1);
    
    renderDayDetails(date);
  }

  /**
   * Renderiza los recordatorios agendados específicos para la fecha indicada
   */
  function renderDayDetails(date) {
    const dayReminders = state.reminders.filter(r => {
      if (!r.dueDate) return false;
      const d = new Date(r.dueDate);
      return d.getDate() === date.getDate() &&
             d.getMonth() === date.getMonth() &&
             d.getFullYear() === date.getFullYear();
    });
    
    elements.calendarDetailsList.innerHTML = '';
    
    if (dayReminders.length === 0) {
      elements.calendarDetailsList.innerHTML = `<p class="details-empty">No hay recordatorios programados para este día.</p>`;
      return;
    }
    
    dayReminders.forEach(item => {
      const detailItem = document.createElement('div');
      detailItem.className = 'history-item'; // Reutilizar estilos premium
      
      const formattedTime = new Date(item.dueDate).toLocaleTimeString('es-ES', {
        hour: '2-digit',
        minute: '2-digit'
      });
      
      detailItem.innerHTML = `
        <div class="history-item-details">
          <p class="reminder-text-content" style="font-size: 0.95rem; text-decoration: none; opacity: 1;">${escapeHTML(item.text)}</p>
          <div class="history-item-meta">
            <span class="history-item-category" style="color: var(--color-${item.category});">${getCategoryText(item.category)}</span>
            <span>&bull;</span>
            ${item.recurrence && item.recurrence !== 'none' ? `
              <span class="card-badge-recurrence recurrence-${item.recurrence}" style="display:inline-flex; vertical-align:middle; padding:0.1rem 0.35rem; font-size:0.65rem; margin-left:0; margin-right:0.35rem;">
                ${item.recurrence === 'daily' ? 'Diario' : 'Semanal'}
              </span>
              <span>&bull;</span>
            ` : ''}
            <span style="color: var(--color-primary); font-weight: 600;">Hora: ${formattedTime}</span>
            <span>&bull;</span>
            <span class="card-badge-priority priority-${item.priority}" style="padding: 0.1rem 0.3rem;">${getPriorityText(item.priority)}</span>
          </div>
        </div>
        <div class="history-item-actions">
          <button class="btn btn-secondary btn-sm btn-restore" title="Marcar como cumplido" onclick="completeReminderFromCalendar('${item.id}')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" width="14" height="14">
              <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
            <span>Cumplir</span>
          </button>
        </div>
      `;
      
      elements.calendarDetailsList.appendChild(detailItem);
    });
  }

  // Registro de cumplimiento simplificado desde el calendario para que lo invoque el HTML inline
  window.completeReminderFromCalendar = (id) => {
    completeReminder(id);
    setTimeout(() => {
      renderCalendar();
      if (state.selectedDate) {
        renderDayDetails(state.selectedDate);
      }
    }, 350);
  };

  // ==========================================================================
  // 11. SISTEMA TOAST DE NOTIFICACIONES GLASSMORPHIC Y ALARMAS DE RESERVAS
  // ==========================================================================
  
  /**
   * Crea un elegante aviso flotante cristalizado
   */
  function showNotification(message) {
    const toast = document.createElement('div');
    toast.className = 'toast-notification';
    toast.innerHTML = `
      <div class="toast-content" style="display: flex; align-items: center; gap: 0.75rem;">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" width="20" height="20" style="color: var(--color-danger); filter: drop-shadow(0 0 5px var(--color-danger-glow));">
          <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
          <line x1="12" y1="9" x2="12" y2="13"></line>
          <line x1="12" y1="17" x2="12.01" y2="17"></line>
        </svg>
        <div style="display: flex; flex-direction: column;">
          <span style="font-size: 0.8rem; text-transform: uppercase; color: var(--color-danger); letter-spacing: 0.5px; font-weight: 700;">¡Reserva Vencida!</span>
          <span style="margin-top: 0.1rem;">${escapeHTML(message)}</span>
        </div>
      </div>
    `;
    
    // Estilos cristal HSL Obsidian con resplandor neón
    Object.assign(toast.style, {
      position: 'fixed',
      bottom: '24px',
      right: '24px',
      backdropFilter: 'blur(20px)',
      webkitBackdropFilter: 'blur(20px)',
      background: 'hsla(354, 85%, 55%, 0.14)',
      border: '1px solid hsla(354, 85%, 55%, 0.35)',
      boxShadow: '0 20px 40px rgba(0, 0, 0, 0.6), 0 0 25px hsla(354, 85%, 55%, 0.25)',
      color: 'var(--text-main)',
      padding: '1rem 1.5rem',
      borderRadius: 'var(--radius-md)',
      zIndex: '10000',
      fontFamily: 'var(--font-body)',
      fontSize: '0.95rem',
      fontWeight: '600',
      animation: 'card-enter 0.35s cubic-bezier(0.34, 1.56, 0.64, 1) forwards',
      pointerEvents: 'auto'
    });
    
    document.body.appendChild(toast);
    
    setTimeout(() => {
      toast.style.animation = 'card-exit 0.3s cubic-bezier(0.25, 0.8, 0.25, 1) forwards';
      toast.addEventListener('animationend', () => toast.remove());
    }, 6000);
  }

  /**
   * Comprueba alarmas vencidas (Bookings) y reproduce avisos y vibración háptica SOS
   */
  function checkBookingAlerts() {
    const nowStr = new Date().toISOString();
    let stateChanged = false;
    
    state.reminders.forEach(r => {
      if (r.dueDate && r.dueDate <= nowStr && !r.alerted) {
        r.alerted = true;
        stateChanged = true;
        playChime('bookingAlert');
        showNotification(r.text);
        
        // Vibración háptica SOS premium en dispositivos móviles compatibles
        if ('vibrate' in navigator) {
          navigator.vibrate([200, 100, 200, 100, 200, 500, 200, 100, 200]);
        }
      }
    });
    
    if (stateChanged) {
      saveToLocalStorage();
      updateUI();
      if (state.viewMode === 'calendar') {
        renderCalendar();
        if (state.selectedDate) {
          renderDayDetails(state.selectedDate);
        }
      }
    } else {
      // Incluso si no hay disparos de alarmas, refrescar tarjetas activas o calendario para actualizar contadores s/m/d en vivo
      if (state.reminders.some(r => r.dueDate)) {
        renderActiveReminders();
        if (state.viewMode === 'calendar' && state.selectedDate) {
          renderDayDetails(state.selectedDate);
        }
      }
    }
  }

  // Background alarms and countdowns update loop (cada 5 segundos)
  setInterval(checkBookingAlerts, 5000);

  // Escuchar el cambio de visibilidad de la página para forzar catch-up instantáneo en dispositivos móviles (evita congelamiento de hilos en fondo)
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
      console.log("Acuérdate: Visibilidad recuperada. Ejecutando verificación de alertas retrasadas...");
      checkBookingAlerts();
    }
  });

  // ==========================================================================
  // 12. INICIALIZACIÓN DE LA APP
  // ==========================================================================
  
  loadFromLocalStorage();
  updateUI();
  
  console.log("Acuérdate cargada de forma exitosa y lista para transcribir voz y gestionar reservas!");
});

// ==========================================================================
// 13. REGISTRO DEL SERVICE WORKER (PWA / Offline)
// ==========================================================================
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('./sw.js')
      .then((reg) => console.log('Service Worker registrado con éxito:', reg.scope))
      .catch((err) => console.warn('Fallo al registrar Service Worker:', err));
  });
}

