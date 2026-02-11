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

    socket.on('heartbeat', () => {
      // compatibility no-op
    });

    // Old Android client compatibility
    socket.on('call_request', (data) => {
      const to = data?.toUserId;
      if (!to) return;
      io.to(to).emit('incoming_call', {
        fromUserId: socket.userId,
        type: data?.type || 'voice',
        sdp: data?.sdp,
        senderName: data?.senderName
      });
      io.to(to).emit('call_offer', { from: socket.userId, offer: data?.sdp });
    });

    socket.on('call_response', (data) => {
      const to = data?.toUserId;
      if (!to) return;
      if (data?.accepted) {
        io.to(to).emit('call_answer', { from: socket.userId, answer: data?.answer });
      } else {
        io.to(to).emit('call_end', { from: socket.userId });
      }
    });

    socket.on('call_end', (data) => {
      const to = data?.toUserId || data?.to;
      if (!to) return;
      io.to(to).emit('call_end', { from: socket.userId });
    });

    socket.on('call_offer', (data) => {
      io.to(data.to).emit('call_offer', { from: socket.userId, offer: data.offer });
    });

    socket.on('call_answer', (data) => {
      io.to(data.to).emit('call_answer', { from: socket.userId, answer: data.answer });
    });

    socket.on('ice_candidate', (data) => {
      const to = data?.to || data?.toUserId;
      if (!to) return;

      // old client sends { sdpMid, sdpMLineIndex, sdp }
      const candidate = data?.candidate || (data?.sdp ? {
        sdpMid: data.sdpMid,
        sdpMLineIndex: data.sdpMLineIndex,
        candidate: data.sdp
      } : null);

      io.to(to).emit('ice_candidate', { from: socket.userId, candidate });
    });

    socket.on('disconnect', () => {
      console.log(`🔌 User disconnected: ${socket.userId}`);
    });
  });

  return io;
}

const getIo = () => io;

module.exports = { initSocket, getIo };
