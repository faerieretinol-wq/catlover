const express = require('express');
const multer = require('multer');
const path = require('path');
const { v4: uuidv4 } = require('uuid');
const authMiddleware = require('../middleware/auth');
const router = express.Router();

const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'video/mp4', 'video/webm'];
const MAX_SIZE = 50 * 1024 * 1024;

const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, 'uploads/'),
  filename: (req, file, cb) => cb(null, `${uuidv4()}${path.extname(file.originalname)}`)
});

const fileFilter = (req, file, cb) => {
  if (!ALLOWED_TYPES.includes(file.mimetype)) {
    return cb(new Error('Invalid file type'), false);
  }
  cb(null, true);
};

const upload = multer({ 
  storage, 
  fileFilter,
  limits: { fileSize: MAX_SIZE }
});

router.post('/', authMiddleware, upload.single('file'), (req, res) => {
  if (!req.file) return res.status(400).json({ error: 'No file' });
  res.json({ url: `/uploads/${req.file.filename}` });
});

router.use((err, req, res, next) => {
  if (err instanceof multer.MulterError) {
    return res.status(400).json({ error: 'Upload error' });
  }
  if (err) return res.status(400).json({ error: err.message });
  next();
});

module.exports = router;
