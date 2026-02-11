const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { v4: uuidv4 } = require('uuid');
const { getDb } = require('../db');
const { z } = require('zod');
const router = express.Router();

const loginAttempts = new Map();
const RATE_LIMIT = 5;
const RATE_WINDOW = 15 * 60 * 1000;

const registerSchema = z.object({
  email: z.string().email().max(255),
  username: z.string().min(3).max(30).regex(/^[a-zA-Z0-9_]+$/),
  password: z.string().min(8).max(128).regex(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/)
});

const loginSchema = z.object({
  email: z.string().email().max(255),
  password: z.string().min(1).max(128)
});

function checkRateLimit(ip) {
  const now = Date.now();
  const attempts = loginAttempts.get(ip) || [];
  const recentAttempts = attempts.filter(t => now - t < RATE_WINDOW);
  if (recentAttempts.length >= RATE_LIMIT) return false;
  recentAttempts.push(now);
  loginAttempts.set(ip, recentAttempts);
  return true;
}

router.post('/register', async (req, res) => {
  try {
    const { email, username, password } = registerSchema.parse(req.body);
    const db = getDb();
    
    const existing = await db.query('SELECT id FROM users WHERE email = $1 OR username = $2', [email, username]);
    if (existing.rows.length > 0) return res.status(400).json({ error: 'User already exists' });

    const salt = await bcrypt.genSalt(12);
    const hash = await bcrypt.hash(password, salt);
    const userId = uuidv4();

    await db.query('BEGIN');
    await db.query('INSERT INTO users (id, email, username, password_hash) VALUES ($1, $2, $3, $4)', [userId, email, username, hash]);
    await db.query('INSERT INTO user_profiles (user_id) VALUES ($1)', [userId]);
    await db.query('COMMIT');

    const token = jwt.sign({ userId }, process.env.JWT_SECRET, { expiresIn: '7d' });
    res.status(201).json({ userId, username, token });
  } catch (err) {
    if (err instanceof z.ZodError) return res.status(400).json({ error: 'Invalid input' });
    await getDb().query('ROLLBACK').catch(() => {});
    res.status(500).json({ error: 'Registration failed' });
  }
});

router.post('/login', async (req, res) => {
  const ip = req.ip || req.connection.remoteAddress;
  if (!checkRateLimit(ip)) return res.status(429).json({ error: 'Too many attempts' });

  try {
    const { email, password } = loginSchema.parse(req.body);
    const db = getDb();
    
    const result = await db.query('SELECT * FROM users WHERE email = $1', [email]);
    if (result.rows.length === 0) return res.status(401).json({ error: 'Invalid credentials' });

    const user = result.rows[0];
    const isMatch = await bcrypt.compare(password, user.password_hash);
    if (!isMatch) return res.status(401).json({ error: 'Invalid credentials' });

    const token = jwt.sign({ userId: user.id }, process.env.JWT_SECRET, { expiresIn: '7d' });
    res.json({ userId: user.id, username: user.username, token });
  } catch (err) {
    if (err instanceof z.ZodError) return res.status(400).json({ error: 'Invalid input' });
    res.status(500).json({ error: 'Login failed' });
  }
});

module.exports = router;
