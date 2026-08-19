const { get } = require('./request');

function getDashboardStats() {
  return get('/dashboard/stats');
}

module.exports = {
  getDashboardStats
};
