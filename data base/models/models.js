const sequelize = require('../db');
const { DataTypes } = require('sequelize');

const User = sequelize.define('user', {
    id: { type: DataTypes.INTEGER, primaryKey: true, autoIncrement: true },
    username: { type: DataTypes.STRING, unique: true, allowNull: false },
    email: { type: DataTypes.STRING, unique: true, allowNull: false },
    password: { type: DataTypes.STRING, allowNull: false },
    role: { type: DataTypes.STRING, defaultValue: "USER" }
});

const Project = sequelize.define('project', {
    id: { type: DataTypes.INTEGER, primaryKey: true, autoIncrement: true },
    name: { type: DataTypes.STRING, unique: true, allowNull: false },
    description: { type: DataTypes.TEXT }
});

const Release = sequelize.define('release', {
    id: { type: DataTypes.INTEGER, primaryKey: true, autoIncrement: true },
    version: { type: DataTypes.STRING, allowNull: false },
    createdAt: { type: DataTypes.DATE, defaultValue: DataTypes.NOW },
    baseBranch: { type: DataTypes.STRING },
    skipPipeline: { type: DataTypes.BOOLEAN, defaultValue: false }
});

const Task = sequelize.define('task', {
    id: { type: DataTypes.INTEGER, primaryKey: true, autoIncrement: true },
    taskKey: { type: DataTypes.STRING, allowNull: false }, 
    title: { type: DataTypes.STRING },
    status: { type: DataTypes.STRING }, 
    tags: { type: DataTypes.ARRAY(DataTypes.STRING) },
    author: { type: DataTypes.STRING },
    developer: { type: DataTypes.STRING }
});

const MergeRequest = sequelize.define('merge_request', {
    id: { type: DataTypes.INTEGER, primaryKey: true, autoIncrement: true },
    sourceBranch: { type: DataTypes.STRING },
    targetBranch: { type: DataTypes.STRING },
    autoMerge: { type: DataTypes.BOOLEAN, defaultValue: false },
    status: { type: DataTypes.STRING }
});

const Commit = sequelize.define('commit', {
    id: { type: DataTypes.INTEGER, primaryKey: true, autoIncrement: true },
    hash: { type: DataTypes.STRING, unique: true },
    message: { type: DataTypes.STRING },
    author: { type: DataTypes.STRING }
});

User.hasMany(Release);
Release.belongsTo(User);

Project.hasMany(Release);
Release.belongsTo(Project);

Release.hasMany(Task);
Task.belongsTo(Release);

Task.hasMany(Commit);
Commit.belongsTo(Task);

Release.hasMany(MergeRequest);
MergeRequest.belongsTo(Release);

Task.hasMany(MergeRequest);
MergeRequest.belongsTo(Task);

module.exports = {
    User,
    Project,
    Release,
    Task,
    MergeRequest,
    Commit
};
