const express = require('express');
const { getDb } = require('../db');
const authMiddleware = require('../middleware/auth');
const router = express.Router();

router.get('/status', authMiddleware, async (req, res) => {
  const userId = req.user.userId;
  const db = getDb();
  try {
    const result = await db.query('SELECT enabled FROM user_2fa WHERE user_id = $1', [userId]);
    res.json({ enabled: result.rows.length > 0 && result.rows[0].enabled });
  } catch (err) {
    res.status(500).json({ error: 'Failed' });
  }
});

router.post('/presence', authMiddleware, async (req, res) => {
  const userId = req.user.userId;
  const { status } = req.body;
  const db = getDb();
  try {
    await db.query('UPDATE user_profiles SET status = $1, updated_at = CURRENT_TIMESTAMP WHERE user_id = $2', [status, userId]);
    res.json({ success: true });
  } catch (err) {
    res.status(500).json({ error: 'Failed' });
  }
});

module.exports = router;
