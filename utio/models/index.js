/**
 * 数据库模型统一导出
 * 包含所有数据表的模型定义
 */

const SensorData = require('./SensorData')
const Recipe = require('./Recipe')
const Plot = require('./Plot')
const PlotAssignment = require('./PlotAssignment')
const PlotSchedule = require('./PlotSchedule')
const ExecutionLog = require('./ExecutionLog')
const Alert = require('./Alert')
const AutomationSetting = require('./AutomationSetting')
const Image = require('./Image')
const ControlLog = require('./ControlLog')

module.exports = {
  SensorData,
  Recipe,
  Plot,
  PlotAssignment,
  PlotSchedule,
  ExecutionLog,
  Alert,
  AutomationSetting,
  Image,
  ControlLog,
}

