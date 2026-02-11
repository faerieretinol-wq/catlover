const { Server } = require('socket.io');
const jwt = require('jsonwebtoken');

let io;

function initSocket(server) {
  io = new Server(server, {
    cors: {
      origin: process.env.ALLOWED_ORIGINS?.split(',') || ["http://localhost:3000"],
      methods: ["GET", "POST"],
      credentials: true
    }
  });

  io.use((socket, next) => {
    const token = socket.handshake.auth.token;
    if (!token) return next(new Error('Authentication error'));

    jwt.verify(token, process.env.JWT_SECRET, (err, decoded) => {
      if (err) return next(new Error('Authentication error'));
      socket.userId = decoded.userId;
      next();
    });
  });

  io.on('connection', (socket) => {
    console.log(`🔌 User connected: ${socket.userId}`);
    socket.join(socket.userId);

    socket.on('call_offer', (data) => {
      io.to(data.to).emit('call_offer', { from: socket.userId, offer: data.offer });
    });

    socket.on('call_answer', (data) => {
      io.to(data.to).emit('call_answer', { from: socket.userId, answer: data.answer });
    });

    socket.on('ice_candidate', (data) => {
      io.to(data.to).emit('ice_candidate', { from: socket.userId, candidate: data.candidate });
    });

    socket.on('disconnect', () => {
      console.log(`🔌 User disconnected: ${socket.userId}`);
    });
  });

  return io;
}

const getIo = () => io;

module.exports = { initSocket, getIo };
