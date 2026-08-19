const { get, delete: del } = require('./request');

function getUploadRecords(params) {
  return get('/files', params);
}

function deleteUploadRecord(fileId) {
  return del(`/files/${fileId}`);
}

function getArchiveFiles(archiveId) {
  return get(`/files/archive/${archiveId}`);
}

module.exports = { getUploadRecords, deleteUploadRecord, getArchiveFiles };
