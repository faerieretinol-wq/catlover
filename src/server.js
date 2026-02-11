require('dotenv').config();
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const http = require('http');
const path = require('path');
const { dbInit } = require('./db');
const { initSocket } = require('./socket');
const { startCleanupTask } = require('./cleanup');

const authRoutes = require('./routes/auth');
const profileRoutes = require('./routes/profile');
const chatRoutes = require('./routes/chat');
const keysRoutes = require('./routes/keys');
const uploadRoutes = require('./routes/upload');
const storiesRoutes = require('./routes/stories');
const socialRoutes = require('./routes/social');
const settingsRoutes = require('./routes/settings');

const app = express();
const server = http.createServer(app);
const port = process.env.PORT || 3000;

app.use(helmet());
app.use(cors());
app.use(express.json());

app.use('/api/auth', authRoutes);
app.use('/api/profile', profileRoutes);
app.use('/api/chat', chatRoutes);
app.use('/api/keys', keysRoutes);
app.use('/api/upload', uploadRoutes);
app.use('/api/stories', storiesRoutes);
app.use('/api/social', socialRoutes);
app.use('/api/settings', settingsRoutes);
app.use('/uploads', express.static(path.join(__dirname, '../uploads')));

app.get('/health', (req, res) => res.json({ status: 'ok' }));

async function start() {
  try {
    await dbInit();
    initSocket(server);
    startCleanupTask();
    server.listen(port, () => {
      console.log(`📡 CatLover Backend running on port ${port}`);
    });
  } catch (err) {
    console.error('❌ Failed to start server:', err);
    process.exit(1);
  }
}

start();
